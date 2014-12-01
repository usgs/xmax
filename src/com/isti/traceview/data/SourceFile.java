package com.isti.traceview.data;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.Set;

import org.apache.log4j.Logger;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import org.apache.commons.collections.ListUtils;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.Wildcard;

import edu.iris.Fissures.seed.builder.SeedObjectBuilder;
import edu.iris.Fissures.seed.director.SeedImportDirector;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;
import edu.sc.seis.seisFile.segd.SegdException;
import edu.sc.seis.seisFile.segd.SegdRecord;

/**
 * Data source, data placed in the file
 * 
 * @author Max Kokoulin
 */
public abstract class SourceFile implements ISource {

	private static final long serialVersionUID = 1L;	
	private static final Logger logger = Logger.getLogger(SourceFile.class);

	/**
	 * @uml.property name="file"
	 */
	private File file;
	
	private boolean parsed = false;

	/**
	 */
	public SourceFile(File file) {
		this.file = file;
	}

	public abstract Set<RawDataProvider> parse(DataModule dataModule);

	public abstract void load(Segment segment);

	public abstract FormatType getFormatType();

	/**
	 * Getter of the property <tt>file</tt>
	 * 
	 * @return Returns the file.
	 * @uml.property name="file"
	 */
	protected File getFile() {
		return file;
	}

	public String getName() {
		return file.getAbsolutePath();
	}

	public SourceType getSourceType() {
		return SourceType.FILE;
	}
	
	public synchronized boolean isParsed() {
		return parsed;
	}

	public synchronized void setParsed(boolean parsed) {
		this.parsed = parsed;
	}

	public boolean equals(Object o) {
		if (o instanceof SourceFile) {
			SourceFile df = (SourceFile) o;
			return (getFile().equals(df.getFile()) && getFormatType().equals(df.getFormatType()));
		} else {
			return false;
		}
	}

	/**
	 * Class for storing number and remainder of thread chunks
	 */
	private static class Chunk {
		int maxIterations;	// max number of iterations per thread
		int remIterations;	// rem number of iterations for last thread
	}

	/**
	 * Calculates thread count for getDataFiles() pool
	 */
	private static int calculateThreadCount() {
		int numProc = Runtime.getRuntime().availableProcessors();
		int threadCount = 0;
		if (numProc % 2 == 0) 
			threadCount = numProc / 2;
		else
			threadCount = (numProc + 1) / 2;
		return threadCount;
	}

	/**
	 * Calculates max and remainder iterations per chunk
	 */
	private static Chunk calculateChunks(int numThreads, int listLength) {
		int remIter = 0;
		int maxIter = 0;
		remIter = listLength % numThreads;		// last iterations
		maxIter = (listLength - remIter) / numThreads;	// max iterations

		Chunk chunkIter = new Chunk();
		chunkIter.maxIterations = maxIter;
		chunkIter.remIterations = remIter;
		return chunkIter;
	}

	/**
	 * Shutdown and executor and lingering tasks (if necessary)
	 */
	private static void shutdownAndAwaitTermination(ExecutorService pool) {
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

	/**
	 * Class to run Future task (checks data file types)
	 *
	 * @param inputFiles 
	 * 	     partial input files (split from main files list)	
	 *
	 */
	private static class DataTask implements Callable<List<ISource>> {
		private List<File> inputFiles; 
		private int filelen;

		// Sets start/end indices for loop
		private DataTask(List<File> inputFiles) { // Construct
			this.inputFiles = inputFiles;	
			this.filelen = inputFiles.size();	
		}

		public List<ISource> call() throws Exception { // Callable
			List<ISource> dataFiles = new ArrayList<ISource>(filelen);
			for (File file: inputFiles) {
				if (isIMS(file)) {
					dataFiles.add(new SourceFileIMS(file));
					logger.debug("IMS data file added: " + file.getAbsolutePath());
				} else if (isMSEED(file)) {
					dataFiles.add(new SourceFileMseed(file));
					logger.debug("MSEED data file added: " + file.getAbsolutePath());
				} else if (isSAC(file)) {
					dataFiles.add(new SourceFileSAC(file));
					logger.debug("SAC data file added: " + file.getAbsolutePath());
				} else if (isSEGY(file)) {
					dataFiles.add(new SourceFileSEGY(file));
					logger.debug("SEGY data file added: " + file.getAbsolutePath());
				} else if (isSEGD(file)) {
					dataFiles.add(new SourceFileSEGD(file));
					logger.debug("SEGD data file added: " + file.getAbsolutePath());
				} else if (isSEED(file)) {
					dataFiles.add(new SourceFileSEGD(file));
					logger.debug("SEED data file added: " + file.getAbsolutePath());
				} else {
					logger.warn("Unknown file format: " + file.getName());
				}
			}
			return dataFiles;
		}
	}

	/**
	 * Searches for files according wildcarded path
	 * 
	 * @param wildcardedMask
	 *            wildcarded path to search
	 */
	public static List<ISource> getDataFiles(String wildcardedMask) throws TraceViewException {
		logger.info("Loading data using path: " + wildcardedMask);
		List<ISource> dataFiles = new ArrayList<ISource>();
		List<File> listFiles = new Wildcard().getFilesByMask(wildcardedMask);
		Iterator<File> it = listFiles.iterator();
		int listlen = listFiles.size();	
		//long start = 0;
		//long elapsed = 0;
		//start = System.nanoTime();
		while (it.hasNext()) {
			File file = it.next();
            		System.out.format("         Found file:%s\n", file.toString());
            		logger.debug("== getDataFiles: file=" + file.toString());
			if (file.getName().matches(".*\\.log(\\.\\d{1,2}){0,1}$")) {
				logger.warn("Excluding file " + file.getName() + " from loading list");
				it.remove();
			}
		}
		//elapsed = System.nanoTime() - start;
		//System.out.println("ListFiles length: " + listlen);	
		//System.out.println("Read data files elapsed: " + elapsed + "ns\n");
		dataFiles = getDataFiles(listFiles);
		return dataFiles;
	}

	/**
	 * tests given list of file for content format and initialize according data sources
	 * 
	 * @param files
	 *            list of files to test
	 * @return List of data sources
	 * @throws TraceViewException
	 */
	public static List<ISource> getDataFiles(List<File> files) throws TraceViewException {
		// Initialize variables
		List<ISource> dataFilesLst = new ArrayList<ISource>();	// datafiles list
		int timeout = 120;		// timeout for getting Futures	
		int threadCount = 0;		// num threads to use
		int chunks = 0;			// num of iteration chunks
		int maxCount = 0;		// max iterations per thread
		int remCount = 0;		// remainder iterations for last thread
		int filelen = files.size();	// used for calculating num of chunks
		
		// Initialize thread count, executor and thread chunks
		threadCount = calculateThreadCount();
		Chunk chunkStats = calculateChunks(threadCount, filelen);
		maxCount = chunkStats.maxIterations;	// max count for threads
		remCount = chunkStats.remIterations;	// remainder count for last thread
		//System.out.println("List length = " + filelen);
		//System.out.println("Thread count = " + threadCount);	
		//System.out.println("Max iterations per thread = " + maxCount);
		//System.out.println("Remainder iterations for last thread = " + remCount);

		// Split iterations into thread chunks
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		if (remCount != 0)
			chunks = threadCount + 1;
		else
			chunks = threadCount;
		//System.out.println("Number of thread chunks = " + chunks + "\n");

		// Set tasks for execution
		DataTask task = null;
		DataTask[] tasks = new DataTask[chunks];	// data type tasks
		List<File> taskFiles = null;	// split 'files' for threading tasks	
		int start = 0;	// start/end for thread chunks
		int end = 0;
		int lastChunk = 0;	
		for (int i = 0; i < chunks; i++) {
			taskFiles = new ArrayList<File>(); // initialize for each block
			if (i < (chunks-1)) { // initial blocks (size = maxCount)
				start = maxCount * i;
				end = start + maxCount;
				taskFiles = files.subList(start, end); // get sub files
				//System.out.println("start: " + start + "\tend: " + end);
				task = new DataTask(taskFiles);
				tasks[i] = task;
			} else if (i == (chunks-1)) { // last block
				lastChunk = i;
				if (lastChunk == threadCount) {	// last block size = remCount
					start = maxCount * i;
					end = start + remCount;
					taskFiles = files.subList(start, end);	// get sub files
					//System.out.println("start: " + start + "\tend: " + end + " (LastBlock)");
					task = new DataTask(taskFiles);
					tasks[i] = task;
				} else { // last block size = maxCount
					start = maxCount * i;
					end = start + maxCount;
					taskFiles = files.subList(start, end);	// get sub files
					//System.out.println("start: " + start + "\tend: " + end + " (LastBlock)");
					task = new DataTask(taskFiles);
					tasks[i] = task;
				}
			}
		}
	
		// Create list of Future<List<ISource>>
		try {	
			// Invoke all datafiles tasks	
			//long startTime = System.nanoTime();
			//System.out.println("\nExecuting getDataFiles.invokeAll()...");
			List<Future<List<ISource>>> dataFilesList = executor.invokeAll(Arrays.asList(tasks));
	
			// Loop through dataFileList and get futures
			for (Future<List<ISource>> future: dataFilesList) {
				try {	
					dataFilesLst.addAll(future.get(timeout, TimeUnit.SECONDS));	
				} catch (TimeoutException e) {
					logger.error("Future TimeoutException:", e);
					future.cancel(true);
					executor.shutdownNow();	
				} catch (ExecutionException e) {
					logger.error("Future ExecutionException:", e);
					future.cancel(true);
					executor.shutdownNow();
				} catch (InterruptedException e) {
					logger.error("Future InterruptedException:", e);
					future.cancel(true);
					executor.shutdownNow();
				}
			}
			//long endTime = System.nanoTime() - startTime;
			//System.out.println("Execution time (invokeAll()) = " + endTime + " ns");
			//System.out.println("DataFiles length = " + dataFilesLst.size() + "\n");
		} catch (InterruptedException e) {
			logger.error("Executor InterruptedException:", e);
			executor.shutdownNow();
		}
		// Shutdown executor and cancel lingering tasks	
		shutdownAndAwaitTermination(executor);	
		
		// Loop through List<ISources> (check datafiles)	
		return dataFilesLst;
	}

	/**
	 * @return flag if data searching path defined in the configuration
	 */
	public static boolean isDataPathDefined() {
		return !TraceView.getConfiguration().getDataPath().equals("!");
	}

	/**
	 * Checks if trace matches all configuration filters. Used during parsing files.
	 */
	public static boolean matchFilters(String network, String station, String location, String channel) {
		Configuration conf = TraceView.getConfiguration();
		return matchFilter(network.trim(), conf.getFilterNetwork()) && matchFilter(location.trim(), conf.getFilterLocation())
				&& matchFilter(channel.trim(), conf.getFilterChannel()) && matchFilter(station.trim(), conf.getFilterStation());
	}

	/**
	 * Checks if value matches configuration filter. Used during parsing files.
	 */
	public static boolean matchFilter(String value, Set<String> filter) {
		if (filter == null) {
			return true;
		} else {
			for (String flt: filter) {
				if (Wildcard.matches(flt, value)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get extension of file
	 * 
	 * @param f
	 *            file to explore
	 * @return file's extension
	 */
	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}

	/**
	 * Tests if file is mseed file
	 * 
	 * @param file
	 *            file to test
	 * @return flag
	 */
	public static boolean isMSEED(File file) {
		if (file.length() > 0) {
			BufferedRandomAccessFile dis = null;
			//ControlHeader ch = null;
			try {
				dis = new BufferedRandomAccessFile(file.getCanonicalPath(), "r");
				dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
				long blockNumber = 0;
 
			    while (blockNumber < 5) {
			    	SeedRecord sr = SeedRecord.read(dis, 4096);
			        if (sr instanceof DataRecord) {
			        	//DataRecord dr = (DataRecord)sr;
			        } else {
			            //control record, skip...
			        }
			        blockNumber++;
			    }
			} catch (EOFException ex) {
				//System.out.format("==     [file:%s] Caught EOFException:%s\n", file.getName(), ex.toString());
				StringBuilder message = new StringBuilder();
				message.append(String.format("== CheckData: [file:%s] Caught EOFException:\n", file.getName()));
				logger.debug(message.toString(), ex);	
				return true;
			} catch (FileNotFoundException e) {
				StringBuilder message = new StringBuilder();
				message.append(String.format("== CheckData: [file:%s] FileNotFoundException:\n", file.getName()));
				logger.debug(message.toString(), e);
				return false;
			} catch (IOException e) {
				StringBuilder message = new StringBuilder();
				message.append(String.format("== CheckData: [file:%s] IOException:\n", file.getName()));
				logger.debug(message.toString(), e);
				return false;
			} catch (SeedFormatException e) {
				//System.out.format("==     [file:%s] Caught SeedFormatException:%s\n", file.getName(), e.toString());
				StringBuilder message = new StringBuilder();
				message.append(String.format("== CheckData: [file:%s] Caught SeedFormatException:\n", file.getName()));
				logger.debug(message.toString(), e);
				return false;
            } catch (RuntimeException e) {
               	StringBuilder message = new StringBuilder();
               	message.append(String.format("== CheckData: [file:%s] Caught RuntimeException:\n", file.getName()));
               	logger.debug(message.toString(), e);
               	return false;
			} finally {
				try {
					dis.close();
				} catch (IOException e) {
					logger.debug("IOException:", e);	
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
	/**
	 * Tests if file is sac file
	 * 
	 * @param file
	 *            file to test
	 * @return flag
	 */
	public static boolean isSAC(File file) {
		byte[] buffer = new byte[4];
		RandomAccessFile ras = null;
		long dataLenFromFileSize = (file.length() - 632) / 4;
		try {
			ras = new RandomAccessFile(file, "r");
			ras.seek(316);
			ras.read(buffer, 0, 4);
		} catch (Exception e) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("== CheckData: [file:%s] Exception:\n", file.getName()));
			logger.debug(message.toString(), e);
			return false;
		} finally {
			try {
				ras.close();
			} catch (IOException e) {
				logger.debug("IOException:", e);	
			}
		}
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		bb.order(ByteOrder.BIG_ENDIAN);
		if (bb.getInt() == dataLenFromFileSize)
			return true;
		bb.position(0);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		if (bb.getInt() == dataLenFromFileSize)
			return true;
		else
			return false;
	}

	/**
	 * Tests if file is segy file
	 * 
	 * @param file
	 *            file to test
	 * @return flag
	 */
	public static boolean isSEGY(File file) {
		SegyTimeSeries ts = new SegyTimeSeries();
		try {
			ts.readHeader(file.getCanonicalPath());
		} catch (Exception e) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("== CheckData: [file:%s] Exception:\n", file.getName()));
			logger.debug(message.toString(), e);
			return false;
		}
		return true;
	}
	
	/**
	 * Tests if file is segd file
	 * 
	 * @param file
	 *            file to test
	 * @return flag
	 */
	public static boolean isSEGD(File file) {
		BufferedInputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(file));
			SegdRecord rec = new SegdRecord(file); 
			rec.readHeader1(new DataInputStream(inputStream));
		} catch (IOException e) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("== CheckData: [file:%s] IOException:\n", file.getName()));
			logger.debug(message.toString(), e);
			return false;
		} catch (SegdException e) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("== CheckData: [file:%s] SegdException:\n", file.getName()));
			logger.debug(message.toString(), e);
			return false;
		} catch (Exception e) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("== CheckData: [file:%s] Exception:\n", file.getName()));
			logger.debug(message.toString(), e);
			return false;
		} finally {
			try{
				inputStream.close();
			} catch (IOException ex){
				logger.debug("IOException:", ex);	
			}
		}
		return true;
	}

	/**
	 * Tests if file is seed file
	 * 
	 * @param file
	 *            file to test
	 * @return flag
	 */
	public static boolean isSEED(File file) {
		FileInputStream fileInputStream = null;
		SeedImportDirector importDirector = null;
		try {
			fileInputStream = new FileInputStream(file);
			importDirector = new SeedImportDirector();
			importDirector.assignBuilder(new SeedObjectBuilder());
			importDirector.open(fileInputStream);
			// read record, build objects, and optionally store the objects in a container
			importDirector.read(false);
		} catch (Exception e) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("== CheckData: [file:%s] Exception:\n", file.getName()));
			logger.debug(message.toString(), e);
			return false;
		} finally {
			try{
				importDirector.close();
				fileInputStream.close();
			} catch (IOException ex){
				logger.debug("IOException:", ex);	
			}
		}
		return true;
	}
	
	/**
	 * Tests if file is IMS file
	 * 
	 * @param file
	 *            file to test
	 * @return flag
	 */
	public static boolean isIMS(File file) {
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(file));
			String line = null;
			for(int i = 0; i<25;i++){
				line = input.readLine().toUpperCase();
				if(line.startsWith("BEGIN IMS") || line.startsWith("DATA_TYPE ") || line.startsWith("WID2 ")){
					input.close();
					return true;
				}
			}
		} catch (Exception e) {
			StringBuilder message = new StringBuilder();
			message.append(String.format("== CheckData: [file:%s] Exception:\n", file.getName()));
			logger.debug(message.toString(), e);
			return false;
		} finally {
			try{
				input.close();
			} catch (IOException ex){
				logger.debug("IOException:", ex);	
			}
		}
		return false;
	}
	
	public String getBlockHeaderText(long blockStartOffset){
		return "<html><i>File type:</i>" + getFormatType() + "<br>Header block text is unavailable</html>";
	}
	/*
	 * private void writeObject(ObjectOutputStream out) throws IOException { lg.debug("Serializing
	 * SourceFile" + toString()); dataSource = (ISource)in.readObject(); currentPos = in.readInt();
	 * startTime = in.readLong(); sampleRate = in.readDouble(); sampleCount = in.readInt();
	 * startOffset = in.readLong(); maxValue = in.readInt(); minValue = in.readInt();
	 * startOffsetSerial = in.readLong(); sourceSerialNumber = in.readInt(); channelSerialNumber =
	 * in.readInt(); } private void readObject(ObjectInputStream in) throws IOException,
	 * ClassNotFoundException { lg.debug("Deserializing SourceFile" + toString());
	 * out.writeObject(dataSource); out.writeInt(currentPos); out.writeLong(startTime);
	 * out.writeDouble(sampleRate); out.writeInt(sampleCount); out.writeLong(startOffset);
	 * out.writeInt(maxValue); out.writeInt(minValue); out.writeLong(dataStream.getFilePointer());
	 * out.writeInt(sourceSerialNumber); out.writeInt(channelSerialNumber); lg.debug("Deserialized
	 * SourceFile" + toString()); }
	 */
}
