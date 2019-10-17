package com.isti.traceview.source;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.Wildcard;
import com.isti.traceview.data.ISource;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.data.SegyTimeSeries;
import edu.iris.Fissures.seed.builder.SeedObjectBuilder;
import edu.iris.Fissures.seed.director.SeedImportDirector;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;
import edu.sc.seis.seisFile.segd.SegdException;
import edu.sc.seis.seisFile.segd.SegdRecord;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

/**
 * Data source, data placed in the file
 * 
 * @author Max Kokoulin
 */
public abstract class SourceFile implements ISource {

	private static final long serialVersionUID = 1L;	
	private static final Logger logger = Logger.getLogger(SourceFile.class);

	private File file;
	
	private boolean parsed = false;

	/**
	 * Constructor
	 */
	public SourceFile(File file) {
		this.file = file;
	}

	public abstract Set<PlotDataProvider> parse();

	public abstract void load(Segment segment);

	public abstract FormatType getFormatType();

	/**
	 * Getter of the property <tt>file</tt>
	 * 
	 * @return Returns the file.
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
	 * Class to call data file type task
	 */
	private static class FileType implements Callable<ISource> {
		private File file;
		
		/**
		 * Constructor: sets input file
		 * 
		 * @param inputFile
		 *            input File to process
		 */
		private FileType(File inputFile) {
			this.file = inputFile;
		}
		
		/**
		 * Callable method to determine file type
		 * 
		 * @return ISource
		 * 		data file for type
		 */
		public ISource call() {
			ISource datafile = null;
			if (isASCII(file)) {
				datafile = new SourceFileASCII(file);
				logger.debug("ASCII data file added: " + file.getAbsolutePath());
			} else if (isIMS(file)) {
				datafile = new SourceFileIMS(file);
				logger.debug("IMS data file added: " + file.getAbsolutePath());
			} else if (isMSEED(file)) {
				datafile = new SourceFileMseed(file);
				logger.debug("MSEED data file added: " + file.getAbsolutePath());
			} else if (isSAC(file)) {
				datafile = new SourceFileSAC(file);
				logger.debug("SAC data file added: " + file.getAbsolutePath());
			} else if (isSEGY(file)) {
				datafile = new SourceFileSEGY(file);
				logger.debug("SEGY data file added: " + file.getAbsolutePath());
			} else if (isSEGD(file)) {
				datafile = new SourceFileSEGD(file);
				logger.debug("SEGD data file added: " + file.getAbsolutePath());
			} /* else if (isSEED(file)) {
				datafile = new SourceFileSEGD(file);
				logger.debug("SEED data file added: " + file.getAbsolutePath());
			}*/ else {
				logger.warn("Unknown file format: " + file.getName());
			}
			return datafile;
		}
	}

	/**
	 * Searches for files according wildcarded path
	 * 
	 * @param wildcardedMask
	 *            wildcarded path to search
	 */
	public static List<File> getDataFiles(String wildcardedMask) throws TraceViewException {
		logger.info("Loading data using path: " + wildcardedMask);
		List<ISource> dataFiles = new ArrayList<>();
		List<File> listFiles = new Wildcard().getFilesByMask(wildcardedMask);
		return listFiles;
	}

	/**
	 * Identify format type from file
	 * @param file File to load in
	 * @return Object representing parser/loading object of the given file
	 */
	public static ISource getDataFile(File file) {
		return new FileType(file).call();
	}

	/**
	 * tests given list of file for content format and initialize according data sources
	 * 
	 * @param files
	 *            list of files to test
	 * @return List of data sources
	 */
	public static List<ISource> getDataFiles(List<File> files){
		
		// Setup pool of workers to set data types for each file (using loop)
		// Submits FileType() tasks to multi-threaded executor
		int numProc = Runtime.getRuntime().availableProcessors();
		int filelen = files.size();
		int threadCount = 0;
		if (numProc % 2 == 0)
			threadCount = numProc / 2;
		else
			threadCount = (numProc + 1) / 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);	// multi-thread executor
		List<Future<ISource>> tasks = new ArrayList<>(filelen);	// list of future tasks
		List<ISource> dataFileList = new ArrayList<>(filelen);	// main datafiles list
		try {	
			long start = System.nanoTime();
			for (File file: files) {
				Future<ISource> future = executor.submit(new FileType(file));
				tasks.add(future);	// add future filetype to tasks list
			}
		
			// Loop through tasks and get futures	
			// ** May want to include future.get() in files loop
			// this will eliminate extra loop for getting futures
			for (Future<ISource> future: tasks) {
				try {
					dataFileList.add(future.get(3, TimeUnit.SECONDS));
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
			long endl = System.nanoTime() - start;
			double end = endl * Math.pow(10, -9);
			System.out.format("SourceFile: getDataFiles() execution time = %.9f sec\n", end);
		} catch (RejectedExecutionException e) {
			logger.error("Executor RejectedExecutionException:", e);
			executor.shutdownNow();
		} catch (NullPointerException e) {
			logger.error("Executor NullPointerException:", e);
			executor.shutdownNow();
		}
		// Shutdown executor and cancel lingering tasks
		shutdownExecutor(executor);
		
		return dataFileList;
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
			RandomAccessFile dis = null;
			//ControlHeader ch = null;
			try {
				dis = new RandomAccessFile(file.getCanonicalPath(), "r");
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

	/**
	 * Tests if file is ASCII bdf
	 * @param file file to test
	 * @return true if file can be parsed as BDF file
	 */
	public static boolean isASCII(File file) {
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(file));
			String line;
			// ASCII file header includes NET, STA, LOC, COMP, RATE, TIME, NSAM, DATA
			Set<String> lineStarts = new HashSet<>(
					Arrays.asList("NET ", "STA ", "LOC ", "COMP", "RATE", "TIME", "NSAM", "DATA"));
			for (int i = 0; i < 8; i++) {
				line = input.readLine().toUpperCase();
				String substring = line.substring(0, 4);
				if (lineStarts.contains(substring)) {
					lineStarts.remove(substring);
				} else {
					return false;
				}
			}
		} catch (IOException e) {
			logger.debug(String.format("== CheckData: [file:%s] Exception:\n", file.getName()), e);
			return false;
		} finally {
			try{
				input.close();
			} catch (IOException ex){
				logger.debug("IOException:", ex);
			}
		}
		return true;
	}
	
	public String getBlockHeaderText(long blockStartOffset){
		return "<html><i>File type:</i>" + getFormatType() + "<br>Header block text is unavailable</html>";
	}

	/**
	 * Shutdown future and executor
	 */
	private static void shutdownFuture(Future<?> task, ExecutorService executor) {
		task.cancel(true);
		executor.shutdownNow();
	}

	/**
	 * Shutdown executor and linger tasks (if necessary)
	 */
	private static void shutdownExecutor(ExecutorService pool) {
		pool.shutdown();
		try {
			// Wait awhile for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow();	// cancel current executing tasks
				// Wait awhile for tasks to respond to cancel
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					logger.error("Pool did not terminate: Shutting down!!");
			}
		} catch (InterruptedException e) {
			pool.shutdownNow();	
		}
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
