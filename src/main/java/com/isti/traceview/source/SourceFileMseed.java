package com.isti.traceview.source;

import com.isti.traceview.TraceView;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.data.SynchronizedSeedRecord;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.mseed.FissuresConvert;
import edu.sc.seis.seisFile.mseed.Btime;
import edu.sc.seis.seisFile.mseed.ControlHeader;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

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
	 * Parses *.mseed files and creates channel segments based on start times
	 * this includes gappy data (i.e. new segment for each gap)
	 * 
	 */
	public synchronized Set<PlotDataProvider> parse() {
		Set<PlotDataProvider> ret = new HashSet<>();
		long blockNumber = 0;
		long endPointer = 0;
		RandomAccessFile dis = null;
		try {
			dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
			PlotDataProvider currentChannel = new PlotDataProvider("", new Station(""), "", "");
			long blockEndTime = 0;
			double sampleRate = -1.0;
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

							// to exclude out of order blocks with events, which has 0 data length
							if (dh.getNumSamples() > 0) {
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
									currentChannel = new PlotDataProvider(dh.getChannelIdentifier(), DataModule.getOrAddStation(dh
											.getStationIdentifier()), dh.getNetworkCode(), dh.getLocationIdentifier());
									ret.add(currentChannel);
									skipChannel = false;
								} else {
									currentChannel = new PlotDataProvider(dh.getChannelIdentifier().trim(), new Station(dh.getStationIdentifier()
											.trim()), dh.getNetworkCode().trim(), dh.getLocationIdentifier().trim());
									skipChannel = true;
								}
							}
								if (sampleRate == 0.0) {
									sampleRate = 1000.0 / dh.calcSampleRateFromMultipilerFactor();
								}
								if (blockEndTime != 0) {
									if (Segment.isDataBreak(blockEndTime, getBlockStartTime(dh), sampleRate)) {
										// Gap detected, new segment starts
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
				if (dis != null) {
					endPointer = dis.getFilePointer();
					dis.close();
				}
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

		int segmentSampleCount = segment.getSampleCount();	// sample count of current segment
		int[] data = new int[segmentSampleCount];	// testing for memory usage
		RandomAccessFile dis = null;
		int currentSampleCount = 0; //Counter on the basis of data values
		int headerSampleCount = 0; //Counter on the basis of header information
		int drSampleCount;		//Counter on current DataRecord
		int blockNumber = 0;
		try {
			logger.debug("source = " + getFile().getCanonicalPath());	
			dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.seek(segment.getStartOffset());
			logger.debug(this + " " + segment + " Beginning position:" + dis.getFilePointer());
			while (currentSampleCount < segmentSampleCount) {
				long blockStartOffset = dis.getFilePointer();
				SeedRecord sr = SynchronizedSeedRecord.read(dis, TraceView.getConfiguration().getDefaultBlockLength());
				blockNumber++;
				if (sr instanceof DataRecord) {
					DataRecord dr = (DataRecord) sr;
					drSampleCount = dr.getHeader().getNumSamples();	// current DataRecord sample count
					headerSampleCount += drSampleCount;	// total sample count from all headers
					segment.addBlockDescription(getBlockStartTime(dr.getHeader()),blockStartOffset);
					if (drSampleCount > 0) {
						LocalSeismogramImpl lsi; // stores seed data as seis id, num samples, sample rate
														// channel id, and byte[] data (EncodedData)
						int[] intData = new int[drSampleCount];	// testing memory usage for normal array[]
						try {
							if (dr.getBlockettes(1000).length == 0) {
								DataRecord[] dra = new DataRecord[1];
								dra[0] = dr;
								int defaultCompression = TraceView.getConfiguration().getDefaultCompression();
								byte dataCompression = (byte) defaultCompression;
								byte byteOrder = (byte) 1;	// big endian byte order
										
								// Time Fissures, this is taking awhile to convert
								lsi = FissuresConvert.toFissures(dra, dataCompression, byteOrder);
							} else {
								// Time Fissures conversion	
								lsi = FissuresConvert.toFissures(dr);	// set LocalSeismogramImpl
							}
							//intData = lsi.get_as_longs();	// testing for memory leaks using array[]
							double[] tempData;
							tempData = lsi.get_as_doubles();
							for(int i = 0; i < intData.length; i++)
								intData[i] = (int) tempData[i];
						} catch (FissuresException fe) {
							logger.error(
									("File " + getFile().getName() + ": Can't decompress data of block " +
											dr.getHeader().getSequenceNum() + ", setting block data to 0: "),
									fe);
							Arrays.fill(intData, 0);
						}
						// Test int[] array for memory leaks
						for (int sample: intData) {
							if (currentSampleCount < segment.getSampleCount()) {
								data[currentSampleCount++] = sample;
							} else {
								logger.warn("currentSampleCount > segmentSampleCount: " + currentSampleCount + ", " + segmentSampleCount + "block " + sr.getControlHeader().getSequenceNum());
							}
						}
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
				message.append(this).append(" ").append(segment).append(" Ending position ")
						.append(dis != null ? dis.getFilePointer() : 0).append(", sampleCount read").append(currentSampleCount)
						.append(", samples from headers ").append(headerSampleCount).append(", blocks read ")
						.append(blockNumber);
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
				if (dis != null)
					dis.close();
			} catch (IOException e) {
				logger.error("IOException:", e);	
			}
		}
		// Test int[] array adding
		for (int value: data) 
			segment.addDataPoint(value);
		
		//logger.info("Loaded " + this + " " + segment + " [samples read = " + currentSampleCount + ", samples from headers = " + headerSampleCount + ", blocks read = " + blockNumber + "]");
	}

	public String toString() {
		return "MseedRawDataProvider: file " + (getFile() == null ? "absent" : getFile().getName());
	}

	public synchronized String getBlockHeaderText(long blockStartOffset) {
		RandomAccessFile dis = null;
		String ret = "<html><i>File type:</i>" + this.getFormatType();
		try {
			dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.seek(blockStartOffset);
			//FileInputStream d = null;
			SeedRecord sr = SynchronizedSeedRecord.read(dis, TraceView.getConfiguration().getDefaultBlockLength());
			ControlHeader ch;
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
				if (dis != null)
					dis.close();
			} catch (IOException e) {
				logger.error("IOException:", e);	
			}
		}

		return ret + "</html>";
	}

	private static long getBlockStartTime(DataHeader dh) {
		Btime startBtime = dh.getStartBtime();
		return startBtime.convertToCalendar().getTimeInMillis();
	}

	private static long getBlockEndTime(DataHeader dh, double sampleRate) {
		long time = (long) (sampleRate * (dh.getNumSamples() - 1));
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
