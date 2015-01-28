package com.isti.traceview.data;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.commons.lang.ArrayUtils;

import com.isti.traceview.TraceView;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.mseed.FissuresConvert;
import edu.sc.seis.seisFile.mseed.Btime;
import edu.sc.seis.seisFile.mseed.ControlHeader;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;

/**
 * File MSEED data source
 * 
 * @author Max Kokoulin
 */
public class SourceFileMseed extends SourceFile implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(SourceFileMseed.class);

	// used during parsing
	private int segmentSampleCount = 0;
	private long segmentStartTime = 0;
	private long segmentOffset = 0;

	// -----
	public SourceFileMseed(File file) {
		super(file);
		logger.debug("Created: " + this);
	}

	public FormatType getFormatType() {
		return FormatType.MSEED;
	}

	/**
	 * Method for converting 4 bytes to a 32bit Integer
	 *
	 * @param b
	 * 	array of 4 bytes (32 bits for integer)
	 * 
	 * @return val
	 * 	32bit integer
	 */
	public static int byteArrayToInt(byte[] b) {
		int val = 0;	// returned 32 bit int

		// Bit shifting using BIG ENDIAN
		if (b.length == 4) {
			val = b[3] & 0xFF |
				(b[2] & 0xFF) << 8 |
				(b[1] & 0xFF) << 16 |
				(b[0] & 0xFF) << 24;
			return val;
		} else {
			logger.error("Byte packet != 4bytes...");	
			return 0;
		}	
	}

	/**
	 * Method for converting byte[] array to int[] array 
	 *
	 * @param buff
	 * 	array of bytes (multiple of 4 bytes)
	 *
	 * @return data
	 * 	array of 32bit integers returned by byteArrayToInt()
	 */
	public static int[] byteArrayToIntArray(byte[] buff) {
		int len = buff.length;	
		int start = 0;	// start index for next string of bytes
		int end = 0;
		int numbytes = 4;	// will implement 8 for long later
		int tmpint = 0;		// current integer of 4 bytes	
		int[] data = new int[len/numbytes];	// output integers	
		byte[] tmpbytes = new byte[numbytes];	// current 4 byte packet	
	
		// Loop through buffer of bytes 
		for (int i = 0; i < (len/numbytes); i++) {
			start = i * numbytes;	// start index of 4 byte packet
			end = start + numbytes;
			tmpbytes = Arrays.copyOfRange(buff, start, end);	// 4 byte packet
			tmpint = byteArrayToInt(tmpbytes);	// convert to int
			data[i] = tmpint;
		}
		return data;	
	}
	
	public synchronized Set<RawDataProvider> parse(DataModule dataModule) {
		Set<RawDataProvider> ret = new HashSet<RawDataProvider>();
		long blockNumber = 0;
		long endPointer = 0;
		BufferedRandomAccessFile dis = null;
		try {
			dis = new BufferedRandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
			RawDataProvider currentChannel = new RawDataProvider("", new Station(""), "", "");
			long blockEndTime = 0;
			double sampleRate = -1.0;
			//double correction = 0.0;
			boolean skipChannel = true;

			segmentSampleCount = 0;
			segmentStartTime = 0;
			segmentOffset = 0;
			try {
				if (getFile().length() > 0) {
					while (true) {
						long currentOffset = dis.getFilePointer();
						SeedRecord sr = SynchronizedSeedRecord.read(dis, TraceView.getConfiguration().getDefaultBlockLength());
						blockNumber++;
						if (sr instanceof DataRecord) {
							DataHeader dh = (DataHeader)sr.getControlHeader();
							/*
							 * lg.debug("Block # " + blockNumber + " is a data record, seq num " +
							 * dh.getSequenceNum() + ", " + dh.getNetworkCode() + "/" +
							 * dh.getStationIdentifier() + "/" + dh.getLocationIdentifier() + "/" +
							 * dh.getChannelIdentifier() + ", starts " + GraphPanel.formatDate(new
							 * Date(getBlockStartTime(dh)),
							 * GraphPanel.DateFormatType.DATE_FORMAT_NORMAL) + ", ends " +
							 * GraphPanel.formatDate(new Date(getBlockEndTime(dh, 1000 /
							 * dh.getSampleRate())), GraphPanel.DateFormatType.DATE_FORMAT_NORMAL) +
							 * ", data length " + dh.getNumSamples());
							 */
							// correction = correction + (dh.getStartMilliSec() -
							// Math.round(dh.getStartMilliSec()));
							if ((!currentChannel.getStation().getName().equals(dh.getStationIdentifier().trim()))
									|| (!currentChannel.getChannelName().equals(dh.getChannelIdentifier().trim()))
									|| (!currentChannel.getNetworkName().equals(dh.getNetworkCode().trim()))
									|| (!currentChannel.getLocationName().equals(dh.getLocationIdentifier().trim()))) {
								// New channel detected
								if (!skipChannel) {
									// Add current segment to current channel and start new segment
									if (segmentSampleCount == 0) {
										segmentStartTime = getBlockStartTime(dh);
									} else {
										addSegment(currentChannel, dh, currentOffset, sampleRate, currentChannel.getSegmentCount());
									}

								}
								sampleRate = 0.0;
								// If new channels matches filters
								if (matchFilters(dh.getNetworkCode(), dh.getStationIdentifier(), dh.getLocationIdentifier(), dh
										.getChannelIdentifier())) {
									// Starts new channel
									// dataModule.addStation();
									currentChannel = dataModule.getOrAddChannel(dh.getChannelIdentifier(), DataModule.getOrAddStation(dh
											.getStationIdentifier()), dh.getNetworkCode(), dh.getLocationIdentifier());
									ret.add(currentChannel);
									skipChannel = false;
								} else {
									currentChannel = new RawDataProvider(dh.getChannelIdentifier().trim(), new Station(dh.getStationIdentifier()
											.trim()), dh.getNetworkCode().trim(), dh.getLocationIdentifier().trim());
									skipChannel = true;
								}
							}
							// to exclude out of order blocks with events, which has 0 data length
							if (dh.getNumSamples() > 0) {
								// try {
								if (sampleRate == 0.0) {
									sampleRate = 1000.0 / dh.getSampleRate();
								}
								/*
								 * } catch (Exception e) {
								 * lg.error("Can't get Mseed block sample rate. File: " +
								 * getFile().getName() + "; block #: " + blockNumber); }
								 */
								if (blockEndTime != 0) {
									if (Segment.isDataBreak(blockEndTime, getBlockStartTime(dh), sampleRate)) {
										// Gap detected, new segment starts
										// lg.debug("Correction " + correction);
										if (!skipChannel) {
											addSegment(currentChannel, dh, currentOffset, sampleRate, currentChannel.getSegmentCount());
										}
									}
								} else {
									segmentStartTime = getBlockStartTime(dh);
								}
								blockEndTime = getBlockEndTime(dh, sampleRate);
								segmentSampleCount = segmentSampleCount + dh.getNumSamples();
							} else {
								logger.debug("Skipping 0-length block #" + blockNumber);
							}
						} else {
							logger.error("Block # " + blockNumber + " is not a data record");
						}
					}
				} else {
					logger.error("File " + getFile().getCanonicalPath() + " has null length");
				}
			} catch (EOFException ex) {
				logger.debug("EOF: " + ex.getMessage());	
				if (!skipChannel) {
					addSegment(currentChannel, null, 0, sampleRate, currentChannel.getSegmentCount());
				}
				logger.debug("Read " + blockNumber + " blocks");
			}
		} catch (FileNotFoundException e) {
			logger.error("File not found: ", e);
		} catch (IOException e) {
			logger.error("IO error: ", e);
		} catch (SeedFormatException e) {
			logger.error("Wrong mseed file format: ", e);
		} finally {
			try {
				endPointer = dis.getFilePointer();
				dis.close();
			} catch (IOException e) {
				logger.error("IOException:", e);	
			}
		}
		logger.debug(this + " end position " + endPointer);
		setParsed(true);
		return ret;
	}

	// Loads current segment from RawDataProvider (this will be multithreaded)
	public synchronized void load(Segment segment) {
		logger.debug(this + " " + segment);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error("InterruptedException:", e);	
		}
		int segmentSampleCount = segment.getSampleCount();	// sample count of current segment
		List<Integer> data = new ArrayList<Integer>(segmentSampleCount);	// replace segment data[] Array with ArrayList
		//int[] data = new int[segmentSampleCount];	// testing for memory usage
		BufferedRandomAccessFile dis = null;
		int currentSampleCount = 0; //Counter on the basis of data values
		int headerSampleCount = 0; //Counter on the basis of header information
		int blockSampleCount = 0;	//Counter on current block 
		int drSampleCount = 0;		//Counter on current DataRecord
		int blockNumber = 0;
		try {
			logger.debug("source = " + getFile().getCanonicalPath());	
			dis = new BufferedRandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
			dis.seek(segment.getStartOffset());
			logger.debug(this + " " + segment + " Beginning position:" + dis.getFilePointer());
			while (currentSampleCount < segmentSampleCount) {
				blockSampleCount = 0;	// reset count for each DataRecord block
				drSampleCount = 0;	// number of samples in current DataRecord
				long blockStartOffset = dis.getFilePointer();
				SeedRecord sr = SynchronizedSeedRecord.read(dis, TraceView.getConfiguration().getDefaultBlockLength());
				blockNumber++;
				if (sr instanceof DataRecord) {
					DataRecord dr = (DataRecord) sr;
					drSampleCount = dr.getHeader().getNumSamples();	// current DataRecord sample count
					headerSampleCount += drSampleCount;	// total sample count from all headers
					segment.addBlockDescription(getBlockStartTime(dr.getHeader()),blockStartOffset);
					if (drSampleCount > 0) {
						LocalSeismogramImpl lsi = null; // stores seed data as seis id, num samples, sample rate
														// channel id, and byte[] data (EncodedData)
						List<Integer> intData = new ArrayList<Integer>(drSampleCount);	// SeedRecord data ArrayList
						//int[] intData = new int[drSampleCount];	// testing memory usage for normal array[]
						try {
							if (dr.getBlockettes(1000).length == 0) {
								DataRecord dra[] = new DataRecord[1];
								dra[0] = dr;
								int defaultCompression = TraceView.getConfiguration().getDefaultCompression();
								byte dataCompression = (byte) defaultCompression;
								byte byteOrder = (byte) 1;	// big endian byte order
										
								// Time Fissures, this is taking awhile to convert
								long startl = System.nanoTime();
								lsi = FissuresConvert.toFissures(dra, dataCompression, byteOrder);
								long endl = System.nanoTime() - startl;
								double end = endl * Math.pow(10, -9);
								System.out.println("Fissures(dra, byteOrder) conversion time = " + end + " sec");
							} else {
								// Get byte data directly (!Fissures) and convert to ArrayList<Int>
								byte[] byteData = dr.getData();	// will use this to get int[] data (faster than Fissures)
								
								System.out.println("byteData length = " + byteData.length);
								// Convert byte data to int[] array using bit shifting
								// **NOTE: May need to check if byte[] is an array of longs
								// 	   => if (byteData % 8 == 0) ==> array of longs
								// 	      if (byteData % 4 == 0) ==> array of ints
								int[] tmpData = byteArrayToIntArray(buff);	// converts byte data to int[] array

								long startl = System.nanoTime();
								lsi = FissuresConvert.toFissures(dr);
								long endl = System.nanoTime() - startl;
								double end = endl * Math.pow(10, -9);
								System.out.println("Fissures(dr) conversion time = " + end + " sec");
							}
							/**
							long startl = System.nanoTime();
							intData = lsi.get_as_longs();	// testing for memory leaks using array[]
							long endl = System.nanoTime() - startl;
							double end = endl * Math.pow(10, -9);
							System.out.println("lsi[] to int[] time = " + end);
							*/
							long startl = System.nanoTime();
							intData = Arrays.asList(ArrayUtils.toObject(lsi.get_as_longs()));	// gets Encoded byte[] data and converts to ArrayList<Integer>
							System.out.println("intData size = " + intData.size());
							long endl = System.nanoTime() - startl;
							double end = endl * Math.pow(10, -9);
							System.out.println("int[] to ArrayList<Integer> conversion time = " + end + " sec");
						} catch (FissuresException fe) {
							StringBuilder message = new StringBuilder();
							message.append(String.format("File " + getFile().getName() + ": Can't decompress data of block " + dr.getHeader().getSequenceNum() + ", setting block data to 0: "));
							logger.error(message.toString(), fe);	
							intData = Collections.nCopies(intData.size(), 0);	// file intData with 0s
						}
						// Append new intData[] to data[] ArrayList (i.e. current seg data to all seg data)
						if (currentSampleCount < segmentSampleCount) {
							data.addAll(intData);	// append current data to end of list
							currentSampleCount += drSampleCount;	// add DataRecord sample count to current sample count
							blockSampleCount += drSampleCount;		// add DataRecord sample count to block sample count
						} else {
							logger.warn("currentSampleCount > segmentSampleCount: " + currentSampleCount + ", " + segmentSampleCount + "block " + sr.getControlHeader().getSequenceNum());
						}
					} else {
						logger.warn("File " + getFile().getName() + ": Skipping block " + dr.getHeader().getSequenceNum() + " due to absence of data");
					}
				} else {
					logger.warn("File " + getFile().getName() + ": Skipping block " + sr.getControlHeader().getSequenceNum() + " so as no-data record");
				}
			}
			/**
			if ((currentSampleCount > segmentSampleCount) || (currentSampleCount == segmentSampleCount)) {
				System.out.println("Segment sample count = " + segmentSampleCount);
				System.out.println("Current sample count = " + currentSampleCount);
				System.out.println("Header sample count = " + headerSampleCount);
				System.out.println("Data record sample count = " + drSampleCount);
				System.out.println("Block sample count = " + blockSampleCount);
				System.out.println("Block number = " + blockNumber);
			}
			*/
		} catch (FileNotFoundException e) {
			logger.error("Can't find file: ", e);
			System.exit(0);	
		} catch (IOException e) {
			StringBuilder message = new StringBuilder();	
			try{
				message.append(String.format(this + " " + segment + " Ending position " + dis.getFilePointer() + ", sampleCount read" + currentSampleCount + ", samples from headers " + headerSampleCount + ", blocks read " + blockNumber));
				logger.error(message.toString(), e);			
			}  catch (IOException eIO) {
				logger.error("IOException:", eIO);
			}
			System.exit(0);	
		} catch (SeedFormatException e) {
			logger.error("Wrong seed format: ", e);
			System.exit(0);	
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
				logger.error("IOException:", e);	
			}
		}
		// Add all segment data to current segment
		segment.addDataPoints(data);
		
		//logger.debug("Loaded " + this + " " + segment + ", sampleCount read" + currentSampleCount + ", samples from headers " + headerSampleCount + ", blocks read " + blockNumber);
		System.out.println("Loaded " + this + " " + segment + " [samples read = " + currentSampleCount + ", samples from headers = " + headerSampleCount + ", blocks read = " + blockNumber + "]");
	}

	public String toString() {
		return "MseedRawDataProvider: file " + (getFile() == null ? "absent" : getFile().getName());
	}

	public synchronized String getBlockHeaderText(long blockStartOffset) {
		BufferedRandomAccessFile dis = null;
		String ret = "<html><i>File type:</i>" + this.getFormatType();
		try {
			dis = new BufferedRandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
			dis.seek(blockStartOffset);
			//FileInputStream d = null;
			SeedRecord sr = SynchronizedSeedRecord.read(dis, TraceView.getConfiguration().getDefaultBlockLength());
			ControlHeader ch = null;
			ch = sr.getControlHeader();
			//ret = ret + "<br><i>Query time: </i> " + TimeInterval.formatDate(new Date(time), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE);
			ret = ret + "<br><i>Seq number:</i> " + ch.getSequenceNum()
				+ "<br><i>Is continuation:</i> " + ch.isContinuation()
				+ "<br><i>Type:</i> " + ch.getTypeCode();			
			if (ch.getTypeCode() == (byte)'D' || ch.getTypeCode() == (byte)'R' || ch.getTypeCode() == (byte)'Q') {
			    DataHeader dh = (DataHeader)ch;
			    ret = ret + "<br><i>Size:</i> " + dh.getSize() 
			    //+ "<br><i>Channel:</i> " + dh.getNetworkCode() + "/" + dh.getStationIdentifier() + "/" + dh.getLocationIdentifier() + "/" + dh.getChannelIdentifier()
			    + "<br><i>Start time:</i> " + TimeInterval.formatDate(new Date(getBlockStartTime(dh)), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
			    + "<br><i>Num samples:</i> " + dh.getNumSamples()
			    + "<br><i>Sample rate:</i> " + dh.getSampleRate()
			    + "<br><i>Time correction:</i> " + dh.getTimeCorrection()
			    + "<br><i>Activity flags:</i> " + dh.getActivityFlags()
			    + "<br><i>IO clock flags:</i> " + dh.getIOClockFlags()
			    + "<br><i>Data quality flags:</i> " + dh.getDataQualityFlags()
			    + "<br><i>Num of blockettes:</i> " + dh.getNumBlockettes()
			    + "<br><i>Data blockette offset:</i> " + dh.getDataBlocketteOffset()
			    + "<br><i>Data offset:</i> " + dh.getDataOffset();
			} else {
				ret = ret + "<br><i>Size:</i> " + ch.getSize();
			}
		} catch (IOException e) {
			logger.error("IOException:", e);	
			ret = ret + "<br>Header block text is unavailable";
		} catch (SeedFormatException e) {
			logger.error("SeedFormatException:", e);	
			ret = ret + "<br>Header block text is unavailable";
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
				logger.error("IOException:", e);	
			}
		}

		return ret + "</html>";
	}

	private static long getBlockStartTime(DataHeader dh) {
		Btime startBtime = dh.getStartBtime();
		return TimeInterval.getTime(startBtime.year, startBtime.jday, startBtime.hour, startBtime.min, startBtime.sec, new Long(Math
				.round(startBtime.tenthMilli)).intValue() / 10);
	}

	private static long getBlockEndTime(DataHeader dh, double sampleRate) {
		long time = new Double((sampleRate * (dh.getNumSamples() - 1))).longValue();
		long blockStart = getBlockStartTime(dh);
		// lg.debug("getBlockEndTime: sampleRate " + sampleRate + ", numSamples " +
		// dh.getNumSamples() + ": return " + (blockStart + time));
		return blockStart + time;
	}

	// Is a segment a trace from the Seed/DataRecord?
	// Is a trace split into multiple segments depending on time and gaps?
	private void addSegment(RawDataProvider channel, DataHeader dh, long currentOffset, double sampleRate, int serialNumber) {
		if (segmentSampleCount != 0) {
			Segment segment = new Segment(this, segmentOffset, new Date(segmentStartTime), sampleRate, segmentSampleCount, serialNumber);
			channel.addSegment(segment);
			if (dh != null) {
				segmentSampleCount = 0;
				segmentStartTime = getBlockStartTime(dh);
				segmentOffset = currentOffset;
			}
		}
	}
}
