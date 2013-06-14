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
import java.util.Set;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.Wildcard;

import edu.iris.Fissures.seed.builder.SeedObjectBuilder;
import edu.iris.Fissures.seed.director.SeedImportDirector;
import edu.sc.seis.seisFile.mseed.ControlHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;
import edu.sc.seis.seisFile.segd.SegdRecord;

/**
 * Data source, data placed in the file
 * 
 * @author Max Kokoulin
 */
public abstract class SourceFile implements ISource {

	private static Logger lg = Logger.getLogger(SourceFile.class);

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
	 * Searches for files according wildcarded path
	 * 
	 * @param wildcardedMask
	 *            wildcarded path to search
	 */
	public static List<ISource> getDataFiles(String wildcardedMask) throws TraceViewException {
		lg.debug("Loading data using path: " + wildcardedMask);
		List<File> listFiles = new Wildcard().getFilesByMask(wildcardedMask);
		Iterator<File> it = listFiles.iterator();
		while (it.hasNext()) {
			File file = it.next();
        System.out.format("== getDataFiles: file=[%s]\n", file.toString());
			if (file.getName().matches(".*\\.log(\\.\\d{1,2}){0,1}$")) {
				lg.warn("Excluding file " + file.getName() + " from loading list");
				it.remove();
			}
		}
		return getDataFiles(listFiles);
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
		List<ISource> lst = new ArrayList<ISource>();
		for (File file: files) {
			if (isIMS(file)) {
				lst.add(new SourceFileIMS(file));
				lg.debug("IMS data file added: " + file.getAbsolutePath());
			} else if (isMSEED(file)) {
				lst.add(new SourceFileMseed(file));
				lg.debug("MSEED data file added: " + file.getAbsolutePath());
			} else if (isSAC(file)) {
				lst.add(new SourceFileSAC(file));
				lg.debug("SAC data file added: " + file.getAbsolutePath());
			} else if (isSEGY(file)) {
				lst.add(new SourceFileSEGY(file));
				lg.debug("SEGY data file added: " + file.getAbsolutePath());
			} else if (isSEGD(file)) {
				lst.add(new SourceFileSEGD(file));
				lg.debug("SEGD data file added: " + file.getAbsolutePath());
			} else if (isSEED(file)) {
				lst.add(new SourceFileSeed(file));
				lg.debug("SEED data file added: " + file.getAbsolutePath());
			}else {
				lg.warn("Unknown file format: " + file.getName());
			}
		}
		return lst;
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
			ControlHeader ch = null;
			try {
				dis = new BufferedRandomAccessFile(file.getCanonicalPath(), "r");
				dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
				long blockNumber = 0;
 
			            while (blockNumber < 5) {
			                SeedRecord sr = SeedRecord.read(dis, 4096);
			                if (sr instanceof DataRecord) {
			                    DataRecord dr = (DataRecord)sr;
			                } else {
			                    //control record, skip...
			                }
			                blockNumber++;
			            }

   			 	
				/*
				while (blockNumber < 5) {
					long currentOffset = dis.getFilePointer();
					try {
						ch = rf.getNextRecord().getControlHeader();
						blockNumber++;
					} catch (MissingBlockette1000 e) {
						lg.warn("Missing Blockette1000, trying with record size of 4096");
						dis.seek(currentOffset);
						ch = rf.getNextRecord(4096).getControlHeader();
						blockNumber++;
					} catch (SeedFormatException e) {
						if(e.getMessage().equals("Found a control header in a miniseed file")){
							lg.debug("isMSEED: found control header in miniseed file");
						} else {
							lg.debug("isMSEED: " + e.getMessage());
							return false;
						}	
				    }
				}
				*/	
			} catch (EOFException ex) {
System.out.format("==     [file:%s] Caught EOFException:%s\n", file.getName(), ex.toString());
				return true;
			} catch (FileNotFoundException e) {
				return false;
			} catch (IOException e) {
				return false;
			} catch (SeedFormatException e) {
System.out.format("==     [file:%s] Caught SeedFormatException:%s\n", file.getName(), e.toString());
				return false;
            } catch (RuntimeException e) {
System.out.format("==     [file:%s] Caught RuntimeException:%s\n", file.getName(), e.toString());
                return false;
			} finally {
				try {
					dis.close();
				} catch (IOException e) {
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
			return false;
		} finally {
			try {
				ras.close();
			} catch (IOException e) {
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
		} catch (Exception e) {
			return false;
		} finally {
			try{
				inputStream.close();
			} catch (Exception ex){
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
			return false;
		} finally {
			try{
				importDirector.close();
				fileInputStream.close();
			} catch (Exception ex){
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
			return false;
		} finally {
			try{
				input.close();
			} catch (Exception ex){
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
