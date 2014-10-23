package com.isti.traceview.data;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
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

	private static Logger lg = Logger.getLogger(SourceFileMseed.class);

	// used during parsing
	private int segmentSampleCount = 0;
	private long segmentStartTime = 0;
	private long segmentOffset = 0;

	// -----
	public SourceFileMseed(File file) {
		super(file);
		lg.debug("Created: " + this);
	}

	public FormatType getFormatType() {
		return FormatType.MSEED;
	}

	public synchronized Set<RawDataProvider> parse(DataModule dataModule) {
		lg.debug("SourceFileMseed.parse begin " + this);
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
			double correction = 0.0;
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
								lg.debug("Skipping 0-length block #" + blockNumber);
							}
						} else {
							lg.error("Block # " + blockNumber + " is not a data record");
						}
					}
				} else {
					lg.error("File " + getFile().getCanonicalPath() + " has null length");
				}
			} catch (EOFException ex) {
				if (!skipChannel) {
					addSegment(currentChannel, null, 0, sampleRate, currentChannel.getSegmentCount());
				}
				lg.debug("Read " + blockNumber + " blocks");
			}
		} catch (FileNotFoundException e) {
			lg.error("File not found: " + e);
		} catch (IOException e) {
			lg.error("IO error: " + e);
		} catch (SeedFormatException e) {
			lg.error("Wrong mseed file format: " + e);
		} finally {
			try {
				endPointer = dis.getFilePointer();
				dis.close();
			} catch (IOException e) {
			}
		}
		lg.debug("SourceFileMseed.parse end " + this + " end position " + endPointer);
		setParsed(true);
		return ret;
	}

	public synchronized void load(Segment segment) {
		lg.debug("SourceFileMSEED.load(): " + this + " " + segment);
		long filePointer = 0;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
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
            lg.debug("== SourceFileMSEED.load() segment from source=" + getFile().getCanonicalPath() );
			dis = new BufferedRandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
			dis.seek(segment.getStartOffset());
			lg.debug(this + " " + segment + " Beginning position:" + dis.getFilePointer());
			while (currentSampleCount < segment.getSampleCount()) {
				int blockSampleCount = 0;
				long blockStartOffset = dis.getFilePointer();
				SeedRecord sr = SynchronizedSeedRecord.read(dis, TraceView.getConfiguration().getDefaultBlockLength());
				blockNumber++;
				if (sr instanceof DataRecord) {
					DataRecord dr = (DataRecord) sr;
					headerSampleCount+=dr.getHeader().getNumSamples();
					segment.addBlockDescription(getBlockStartTime(dr.getHeader()),blockStartOffset);
					// lg.debug("Size: data " + dr.getDataSize() + ", header " + dr.getHeader().getSize());
					if (dr.getHeader().getNumSamples() > 0) {
						LocalSeismogramImpl lsi = null;
						int intData[] = new int[dr.getHeader().getNumSamples()];
						try {
							if (dr.getBlockettes(1000).length == 0) {
								DataRecord dra[] = new DataRecord[1];
								dra[0] = dr;
								lsi = FissuresConvert.toFissures(dra, (byte) TraceView.getConfiguration().getDefaultCompression(), (byte) 1);
							} else {
								lsi = FissuresConvert.toFissures(dr);
							}
							intData = lsi.get_as_longs();
						} catch (FissuresException fe) {
							lg.error("File " + getFile().getName() + ": Can't decompress data of block " + dr.getHeader().getSequenceNum() + ", setting block data to 0: " + fe);
							for (int i = 0; i < intData.length; i++) {
								intData[i] = 0;
							}
						}
//						if(intData.length != dr.getHeader().getNumSamples()){
//							lg.warn("data array length mismatch!!! block " + sr.getControlHeader().getSequenceNum() + ", header: " + dr.getHeader().getNumSamples() + ", array length " + intData.length);
//						}
						for (int sample: intData) {
							if (currentSampleCount < segment.getSampleCount()) {
								// lg.debug("sample" + sample);
								data[currentSampleCount++] = sample;
								blockSampleCount++;
							} else {
								lg.warn("currentSampleCount > segment.getSampleCount(): " + currentSampleCount + ", " + segment.getSampleCount() + "block " + sr.getControlHeader().getSequenceNum());
							}
						}
//						if(blockSampleCount != intData.length){
//							lg.warn("Sample count mismatch!!! block " + sr.getControlHeader().getSequenceNum() + ", headers: " + dr.getHeader().getNumSamples() + "; real "+ blockSampleCount);
//						}
					} else {
						lg.warn("File " + getFile().getName() + ": Skipping block " + dr.getHeader().getSequenceNum() + " due to absence of data");
					}
				} else {
					lg.warn("File " + getFile().getName() + ": Skipping block " + sr.getControlHeader().getSequenceNum() + " so as no-data record");

				}
			}
			// TraceViewCore.dumpMemory();

		} catch (FileNotFoundException e) {
			lg.error("Can't find file: " + e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			try{
				e.printStackTrace();
				lg.debug("ERROR:" + this + " " + segment + " Ending position " + dis.getFilePointer() + ", sampleCount read" + currentSampleCount + ", samples from headers " + headerSampleCount + ", blocks read " + blockNumber);
				}  catch (IOException ex) {
				}

			throw new RuntimeException(e);
		} catch (SeedFormatException e) {
			lg.error("Wrong seed format: " + e);
			throw new RuntimeException(e);
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
			}
		}
		for (int value: data) {
			segment.addDataPoint(value);
		}

		lg.debug("Loaded " + this + " " + segment + ", sampleCount read" + currentSampleCount + ", samples from headers " + headerSampleCount + ", blocks read " + blockNumber);
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
			FileInputStream d = null;
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
			ret = ret + "<br>Header block text is unavailable";
		} catch (SeedFormatException e) {
			ret = ret + "<br>Header block text is unavailable";
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
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

	private void addSegment(RawDataProvider channel, DataHeader dh, long currentOffset, double sampleRate, int serialNumber) {
		if (segmentSampleCount != 0) {
			// lg.debug("Adding segment: offset " + segmentOffset);
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
