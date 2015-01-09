package com.isti.traceview.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.io.File;

import org.apache.log4j.Logger;

import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.SourceFile.DataTask;
import com.isti.traceview.data.DataModule.ParseTask;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

/**
* This class creates a pool of workers according to the number of
* files and system threads. The multithreaded pools are for storing 
* DataFile types and Parsing DataFiles
* 
* @author Alejandro Gonzales
*/
public class DataExecutor {
	private static final Logger logger = Logger.getLogger(DataModule.class);
	
	// Global thread variables
	private ExecutorService executor;
	private int threadCount = 0;
	private int timeout = 120;
	private int activeThreads = 0;
	private int chunks = 0;
	private int maxCount = 0;
	private int remCount = 0;
	
	// Global file type variables
	private List<?> files;
	private int filelen = 0;
	
	/**
	 * Constructor for list of data files
	 * 
	 * @param files
	 * 		can be of type ISource or File
	 */
	public DataExecutor(List<?> files) {
		this.files = files;
		this.filelen = files.size();
		calculateThreadCount();	// intialize threadCount
	}
	
	/**
	 * Set global variables
	 */
	public void setVariables() {
		// Initialize thread count, thread chunks and executor
		Chunk chunkStats = calculateChunks(filelen, threadCount);
		maxCount = chunkStats.maxIterations;
		remCount = chunkStats.remIterations;
		activeThreads = chunkStats.activeThreadCount;
		
		executor = Executors.newFixedThreadPool(activeThreads);	// multithread executor
		if (remCount != 0)
			chunks = activeThreads + 1;	// thread chunks for remainder
		else
			chunks = activeThreads;		// thread chunks for no remainder
	}
	
	/**
	 * Class for storing number and remainder of thread chunks
	 */
	private static class Chunk {
		int maxIterations;		// max number of iterations per thread
		int remIterations;		// rem number of iterations for last thread
		int activeThreadCount;	// number of active threads
	}
	
	/**
	 * Create pool of workers to determine DataFile Types
	 * 
	 * @return List<ISource> dataFileList
	 * 			list of ISource data files
	 */
	@SuppressWarnings("unchecked")
	public List<ISource> processDataTypes() {
		List<ISource> dataFileList = new ArrayList<ISource>();
		
		// Create a list of DataTasks
		DataTask task = null;
		DataTask[] tasks = new DataTask[chunks];	// array of object tasks
		List<File> taskFiles = null;	// split 'files' into threading tasks
		int start = 0;	// start:end for thread chunks
		int end = 0;
		int lastChunk = 0;
		for (int i = 0; i < chunks; i++) {
			taskFiles = new ArrayList<File>();	// initialize for each block
			if (i < (chunks-1)) {	// initial blocks (size = maxCount)
				start = maxCount * i;
				end = start + maxCount;
				taskFiles = (List<File>) files.subList(start, end);	// get sub files
				task = new DataTask(taskFiles);
				tasks[i] = task;
			} else if (i == (chunks-1)) {	// last block
				lastChunk = i;
				if (lastChunk == activeThreads)	{	// => last block size = remCount
					start = maxCount * i;
					end = start + remCount;
					taskFiles = (List<File>) files.subList(start, end);	// get sub files
					task = new DataTask(taskFiles);
					tasks[i] = task;
				} else {	// => last block size = maxCount
					start = maxCount * i;
					end = start + maxCount;
					taskFiles = (List<File>) files.subList(start, end);	// get sub files
					task = new DataTask(taskFiles);
					tasks[i] = task;
				}
			}
		}
		
		// Invoke all datafiles tasks (each task is a List<ISource>)
		// We have List of Future<List<ISource>> to be processed
		try {
			List<Future<List<ISource>>> dataFileTasks = executor.invokeAll(Arrays.asList(tasks));
		
			// Loop through dataFileTasks and get futures
			for (Future<List<ISource>> future: dataFileTasks) {
				try {
					dataFileList.addAll(future.get(timeout, TimeUnit.SECONDS));
				} catch (TimeoutException e) {
					logger.error("Future TimeoutException:", e);
					shutdownFuture(future, executor);	
				} catch (ExecutionException e) {
					logger.error("Future ExecutionException:", e);
					shutdownFuture(future, executor);	
				} catch (InterruptedException e) {
					logger.error("Future InterruptedException:", e);
					shutdownFuture(future, executor);	
				}
			}
		} catch (InterruptedException e) {
			logger.error("Executor InterruptedException:", e);
			executor.shutdownNow();
		}
		// Shutdown executor and cancel lingering tasks
		shutdownExecutor(executor);
		
		// Loop through List<ISources> (check datafiles)
		return dataFileList;
	}
	
	/**
	 * Create pool of workers to parse List<ISource> datafiles
	 * 
	 * @return Set<RawDataProvider> changedChannels
	 * 			set of parsed seed file RawDataProviders
	 */
	@SuppressWarnings("unchecked")
	public Set<RawDataProvider> processDataParse() {
		Set<RawDataProvider> changedChannels = new HashSet<RawDataProvider>();

		// Create list of ParseTasks
		ParseTask task = null;
		ParseTask[] tasks = new ParseTask[chunks];	// array of object tasks
		List<ISource> taskFiles = null;	// split 'datafiles' into threading tasks
		int start = 0;
		int end = 0;
		int lastChunk = 0;
		for (int i = 0; i < chunks; i++) {
			taskFiles = new ArrayList<ISource>();	// initialize for each block
			if (i < (chunks-1)) {	// initial blocks (size = maxCount)
				start = maxCount * i;
				end = start + maxCount;
				taskFiles = (List<ISource>) files.subList(start, end);	// sub files
				task = new ParseTask(taskFiles);
				tasks[i] = task;
			} else if (i == (chunks-1)) {	// last block
				lastChunk = i;
				if (lastChunk == activeThreads) {	// => last block size = remCount
					start = maxCount * i;
					end = start + remCount;
					taskFiles = (List<ISource>) files.subList(start, end);
					task = new ParseTask(taskFiles);
					tasks[i] = task;
				} else {	// => last block size = maxCount
					start = maxCount * i;
					end = start + maxCount;
					taskFiles = (List<ISource>) files.subList(start, end);
					task = new ParseTask(taskFiles);
					tasks[i] = task;
				}
			}
		}

		// Invoke all ParseTasks (each task is a Set of RawDataProviders)
		// We have a List of Future<Set<RawDataProvider>> to be processed
		try {
			List<Future<Set<RawDataProvider>>> dataParseTasks = executor.invokeAll(Arrays.asList(tasks));

			// Loop through dataParseTasks and get futures
			for (Future<Set<RawDataProvider>> future: dataParseTasks) {
				try {
					changedChannels.addAll(future.get(timeout, TimeUnit.SECONDS));
				} catch (TimeoutException e) {
					logger.error("Future TimeoutException:", e);
					shutdownFuture(future, executor);	
				} catch (ExecutionException e) {
					logger.error("Future ExecutionException:", e);
					shutdownFuture(future, executor);	
				} catch (InterruptedException e) {
					logger.error("Future InterruptedException:", e);
					shutdownFuture(future, executor);	
				}
			}
		} catch (InterruptedException e) {
			logger.error("Executor InterruptedException:", e);
			executor.shutdownNow();
		}
		shutdownExecutor(executor);

		// Return parsed changedChannels
		return changedChannels;
	}
	
	/**
	 * Calculates thread count for file pool
	 * @return 
	 */
	private void calculateThreadCount() {
		int numProc = Runtime.getRuntime().availableProcessors();

		if (numProc % 2 == 0)
			threadCount = numProc / 2;
		else
			threadCount = (numProc + 1) / 2;
	}
	
	/**
	 * Calculates max and remainder iterations per 
	 */
	private Chunk calculateChunks(int listLength, int numThreads) {
		int remIter = 0;
		int maxIter = 0;
		int maxtmp = 0;
		int activeThreads = 0;
		
		if ((listLength % numThreads) == listLength) {	// => listLength < numThreads
			remIter = 0;
			maxIter = listLength;	// max iterations for 1 thread
			activeThreads = 1;
		} else {	// => listLength >= numThreads
			remIter = listLength % numThreads;
			maxtmp = (listLength - remIter) / numThreads;
			if (maxtmp < numThreads) {	// max multiplier < number of threads
				activeThreads = maxtmp;	// set number of active threads = multiplier
				maxIter = numThreads;	// set max iterations = number of threads
			} else {	// max multiplier >= number of threads
				activeThreads = numThreads;	// set num of active threads = number of threads
				maxIter = maxtmp;			// set max iterations = multiplier
			}
		}
		
		Chunk chunkIter = new Chunk();
		chunkIter.maxIterations = maxIter;
		chunkIter.remIterations = remIter;
		chunkIter.activeThreadCount = activeThreads;
		return chunkIter;
	}
	
	/**
	 * Shutdown future and executor
	 */
	private static void shutdownFuture(Future task, ExecutorService executor) {
		task.cancel(true);
		executor.shutdownNow();
	}

	/**
	 * Shutdown executor and lingering tasks (if necessary)
	 */
	private static void shutdownExecutor(ExecutorService pool) {
		pool.shutdown();
		try {
			// Wait awhile for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow();	// cancel current executing tasks
				// Wait awhile for tasks to respond to cancel
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					logger.error("Pool did not terminate!! SHUTTING DOWN!!");
			}
		} catch (InterruptedException e) {
			pool.shutdownNow();
		}
	}
}
