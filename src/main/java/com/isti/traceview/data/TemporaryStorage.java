package com.isti.traceview.data;

import com.isti.traceview.source.SourceFile;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * <p>
 * This class provides space and api for work with dump trace files.
 * </p>
 * <p>
 * TraceView has two-stage mode for handling with very big data sets. On first stage library parse
 * files, then loads traces one-by-one and dumps it in temporary storage. On the second stage,
 * dumped traces load very fast and don't require random access memory for row trace data keeping -
 * dumped data used as disk cache.
 * </p>
 * 
 * @author Max Kokoulin
 */
public class TemporaryStorage {
	private static final Logger logger = Logger.getLogger(TemporaryStorage.class);
	private String tempdir = null;
	private Set<File> files = null;

	/**
	 * @param tempdir
	 *            directory path to use as temporary files storage
	 */
	public TemporaryStorage(String tempdir) {
		File[] dir;
		this.tempdir = tempdir;
		files = new HashSet<>();
		File f = new File(tempdir);
		if (f.isDirectory()) {
			dir = f.listFiles();
			if (dir.length > 0) {
				for (int i = 0; i < dir.length; i++) {
					if (!dir[i].isDirectory()) {
						if (SourceFile.getExtension(dir[i]).equals("sac")) {
							// deletes all SAC files which stored in previous sessions by full seed
							// reader
							dir[i].delete();
						} else if (SourceFile.getExtension(dir[i]).equals("ser")) {
							files.add(dir[i]);
							logger.debug("Tepmorary file added: " + dir[i].getName());
						}
					}
				}
			}
		}
		Runtime.getRuntime().addShutdownHook(new ClearTempShutDownHook());
	}

	/**
	 * @return a Set of strings listing full paths to temp files
	 */
	public Set<String> getAllTempFiles() {
		Set<String> ret = new HashSet<>();
		for (File file: files) {
			if (SourceFile.matchFilters(getFileNetwork(file), getFileStation(file), getFileLocation(file), getFileChannel(file))) {
				ret.add(tempdir + File.separator + file.getName());
			}
		}
		return ret;
	}

	public String getTempDir() {
		return tempdir;
	}

	public void addTempFile(File file) {
		files.add(file);
	}

	public void delTempFile(File file) {
		files.remove(file);
		file.delete();
		try {
			new File(getDataFileName(file.getName())).delete();
		} catch (Exception e) {
			// do nothing
			logger.error("Exception:", e);	
		}
		file = null;
	}

	/**
	 * Deletes temporary files with given extension. If extension omitted, deletes all temp files.
	 * 
	 */

	public void delAllTempFiles() {
		Iterator<File> it = files.iterator();
		while (it.hasNext()) {
			File file = it.next();
			it.remove();
			file.delete(); // deleting serial file
			try { // deleting datafile, if exist
				new File(tempdir + File.separator + getDataFileName(file.getName())).delete();
			} catch (Exception e) {
				logger.error("Exception:", e);	
			}
			file = null;
		}
	}

	public static String getDataFileName(String serialFileName) {
		return serialFileName.substring(0, serialFileName.indexOf(".SER")) + ".DATA";
	}

	/**
	 * @param channel
	 *            trace
	 * @return Name of file to serialize given trace
	 */
	public String getSerialFileName(Channel channel) {
		return getTempDir() + File.separator + channel.getNetworkName() + "." + channel.getStation().getName() + "." + channel.getLocationName()
				+ "." + channel.getChannelName();
	}

	private String getFileNetwork(File file) {
		String[] parts = file.getName().split("\\.");
		return parts[0];
	}

	private String getFileStation(File file) {
		String[] parts = file.getName().split("\\.");
		return parts[1];
	}

	private String getFileLocation(File file) {
		String[] parts = file.getName().split("\\.");
		return parts[2];
	}

	private String getFileChannel(File file) {
		String[] parts = file.getName().split("\\.");
		return parts[3];
	}

	/**
	 * Clears temporary storage area after program shutdown.
	 * 
	 * @author Max
	 */
	class ClearTempShutDownHook extends Thread {
		public void run() {
			File[] dir;
			File f = new File(tempdir);
			if (f.isDirectory()) {
				dir = f.listFiles();
				if (dir.length > 0) {
					for (int i = 0; i < dir.length; i++) {
						if (!dir[i].isDirectory()) {
							if (SourceFile.getExtension(dir[i]).equals("sac")) {
								// deletes all SAC files which stored in this session by full seed
								// reader
								dir[i].delete();
							}
						}
					}
				}
			}
		}
	}
}

/*
 * class TempChannel { private String channelName; private String station; private String
 * locationName; private String networkName; private File tempFile; private Set<String> datafiles; }
 */
