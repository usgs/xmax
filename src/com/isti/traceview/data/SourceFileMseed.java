package com.isti.traceview.data;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

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

	public synchronized void load(Segment segment) {
		logger.debug(this + " " + segment);
		//long filePointer = 0;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error("InterruptedException:", e);	
		}
//		try{
//			throw new Exception();
//		} catch (Exception e){
//			String err = "";
//			for(StackTraceElement el: e.getStackTrace()){
//				err = err + el + "\n";
//			}
//			lg.debug(err);
//		}

		BufferedRandomAccessFile dis = null;
		int[] data = new int[segment.getSampleCount()];
		int currentSampleCount = 0; //Counter on the basis of data values
		int headerSampleCount = 0; //Counter on the basis of header information
		int blockNumber = 0;
		try {
			logger.debug("source = " + getFile().getCanonicalPath());	
			dis = new BufferedRandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
			dis.seek(segment.getStartOffset());
			logger.debug(this + " " + segment + " Beginning position:" + dis.getFilePointer());
			while (currentSampleCount < segment.getSampleCount()) {
				@SuppressWarnings("unused")	
				int blockSampleCount = 0;
				long blockStartOffset = dis.getFilePointer();
				SeedRecord sr = SynchronizedSeedRecord.read(dis, TraceView.getConfiguration().getDefaultBlockLength());
				blockNumber++;
				if (sr instanceof DataRecord) {
					DataRecord dr = (DataRecord) sr;
					headerSampleCount+=dr.getHeader().getNumSamples();
					segment.addBlockDescription(getBlockStartTime(dr.getHeader()),blockStartOffset);
					if (dr.getHeader().getNumSamples() > 0) {
						LocalSeismogramImpl lsi = null; // stores seed data as seis id, num samples, sample rate
														// channel id, and byte[] data (EncodedData)
						int intData[] = new int[dr.getHeader().getNumSamples()];
						try {
							if (dr.getBlockettes(1000).length == 0) {
								DataRecord dra[] = new DataRecord[1];
								dra[0] = dr;
								int defaultCompression = TraceView.getConfiguration().getDefaultCompression();
								byte dataCompression = (byte) defaultCompression;
								byte byteOrder = (byte) 1;	// big endian byte order
								byte byteData[] = dra[0].getData();
								lsi = FissuresConvert.toFissures(dra, dataCompression, byteOrder);
							} else {
								lsi = FissuresConvert.toFissures(dr);
							}
							intData = lsi.get_as_longs();	// gets Encoded byte[] data
						} catch (FissuresException fe) {
							StringBuilder message = new StringBuilder();
							message.append(String.format("File " + getFile().getName() + ": Can't decompress data of block " + dr.getHeader().getSequenceNum() + ", setting block data to 0: "));
							logger.error(message.toString(), fe);	
							for (int i = 0; i < intData.length; i++) {
								intData[i] = 0;
							}
						}
						for (int sample: intData) {
							if (currentSampleCount < segment.getSampleCount()) {
								data[currentSampleCount++] = sample;
								blockSampleCount++;
							} else {
								logger.warn("currentSampleCount > segment.getSampleCount(): " + currentSampleCount + ", " + segment.getSampleCount() + "block " + sr.getControlHeader().getSequenceNum());
							}
						}
						System.out.println("intData size = " + intData.length);
						System.out.println("currentSampleCount = " + currentSampleCount);
					} else {
						logger.warn("File " + getFile().getName() + ": Skipping block " + dr.getHeader().getSequenceNum() + " due to absence of data");
					}
				} else {
					logger.warn("File " + getFile().getName() + ": Skipping block " + sr.getControlHeader().getSequenceNum() + " so as no-data record");

				}
			}
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
		for (int value: data) {
			segment.addDataPoint(value);
		}

		logger.debug("Loaded " + this + " " + segment + ", sampleCount read" + currentSampleCount + ", samples from headers " + headerSampleCount + ", blocks read " + blockNumber);
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
