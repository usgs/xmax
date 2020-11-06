package com.isti.traceview.source;

import com.isti.traceview.TraceView;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.Channel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

  // -----
  public SourceFileMseed(File file) {
    super(file);
    logger.debug("Created: " + this);
  }

  public FormatType getFormatType() {
    return FormatType.MSEED;
  }

  /**
   * Parses *.mseed files and creates channel segments based on start times this includes gappy data
   * (i.e. new segment for each gap)
   */
  public synchronized Set<PlotDataProvider> parse() {
    Map<Channel, PlotDataProvider> map = new HashMap<>();
    long blockNumber = 0;
    long endPointer = 0;
    RandomAccessFile dis = null;
    try {
      dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
      long segmentEndTime;
      double sampleRate;
      long segmentStartTime;
      try {
        if (getFile().length() > 0) {
          while (true) {
            // parsing until we get an exception isn't ideal but how the example seisfile case
            // was implemented
            long currentOffset = dis.getFilePointer();
            SeedRecord sr = SynchronizedSeedRecord
                .read(dis, TraceView.getConfiguration().getDefaultBlockLength());
            blockNumber++;
            if (sr instanceof DataRecord) {
              DataHeader dh = (DataHeader) sr.getControlHeader();

              // exclude blocks that don't actually have any timeseries data in them
              if (dh.getNumSamples() > 0) {
                // our goals here:
                // - if the channel does not pass filters, skip this record
                // - if the channel specified here doesn't exist, create a new channel
                // - add the current data segment to the channel
                if (!matchFilters(dh.getNetworkCode(), dh.getStationIdentifier(),
                    dh.getLocationIdentifier(), dh.getChannelIdentifier())) {
                  continue; // skip record -- it's not going to be loaded in
                }

                Channel key = new Channel(dh.getChannelIdentifier().trim(),
                    DataModule.getOrAddStation(dh.getStationIdentifier().trim()),
                    dh.getNetworkCode().trim(), dh.getLocationIdentifier().trim());
                if (!map.containsKey(key)) {
                  map.put(key, new PlotDataProvider(key.getChannelName(),
                      key.getStation(), key.getNetworkName(),
                      key.getLocationName()));
                }
                PlotDataProvider currentChannel = map.get(key);

                sampleRate = 1000.0 / dh.calcSampleRateFromMultipilerFactor();
                segmentStartTime = getBlockStartTime(dh);
                segmentEndTime = getBlockEndTime(dh, sampleRate);

                addSegment(currentChannel, currentOffset, sampleRate,
                    currentChannel.getSegmentCount(), dh.getNumSamples(),
                    segmentStartTime, segmentEndTime);
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
    return new HashSet<>(map.values());
  }

  // Loads current segment from RawDataProvider (this will be multithreaded)
  public synchronized void load(Segment segment) {
    logger.debug(this + " " + segment);

    int segmentSampleCount = segment.getSampleCount();  // sample count of current segment
    int[] data = new int[segmentSampleCount];  // testing for memory usage
    RandomAccessFile dis = null;
    int currentSampleCount = 0; //Counter on the basis of data values
    int headerSampleCount = 0; //Counter on the basis of header information
    int drSampleCount;    //Counter on current DataRecord
    int blockNumber = 0;
    try {
      logger.debug("source = " + getFile().getCanonicalPath());
      dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
      dis.seek(segment.getStartOffset());
      logger.debug(this + " " + segment + " Beginning position:" + dis.getFilePointer());
      while (currentSampleCount < segmentSampleCount) {
        long blockStartOffset = dis.getFilePointer();
        SeedRecord sr = SynchronizedSeedRecord
            .read(dis, TraceView.getConfiguration().getDefaultBlockLength());
        blockNumber++;
        if (sr instanceof DataRecord) {
          DataRecord dr = (DataRecord) sr;
          drSampleCount = dr.getHeader().getNumSamples();  // current DataRecord sample count
          headerSampleCount += drSampleCount;  // total sample count from all headers
          segment.addBlockDescription(getBlockStartTime(dr.getHeader()), blockStartOffset);
          if (drSampleCount > 0) {
            LocalSeismogramImpl lsi; // stores seed data as seis id, num samples, sample rate
            // channel id, and byte[] data (EncodedData)
            int[] intData = new int[drSampleCount];  // testing memory usage for normal array[]
            try {
              if (dr.getBlockettes(1000).length == 0) {
                DataRecord[] dra = new DataRecord[1];
                dra[0] = dr;
                int defaultCompression = TraceView.getConfiguration().getDefaultCompression();
                byte dataCompression = (byte) defaultCompression;
                byte byteOrder = (byte) 1;  // big endian byte order

                // Time Fissures, this is taking awhile to convert
                lsi = FissuresConvert.toFissures(dra, dataCompression, byteOrder);
              } else {
                // Time Fissures conversion
                lsi = FissuresConvert.toFissures(dr);  // set LocalSeismogramImpl
              }
              //intData = lsi.get_as_longs();	// testing for memory leaks using array[]
              double[] tempData;
              tempData = lsi.get_as_doubles();
							for (int i = 0; i < intData.length; i++) {
								intData[i] = (int) tempData[i];
							}
            } catch (FissuresException fe) {
              logger.error(
                  ("File " + getFile().getName() + ": Can't decompress data of block " +
                      dr.getHeader().getSequenceNum() + ", setting block data to 0: "),
                  fe);
              Arrays.fill(intData, 0);
            }
            // Test int[] array for memory leaks
            for (int sample : intData) {
              if (currentSampleCount < segment.getSampleCount()) {
                data[currentSampleCount++] = sample;
              } else {
                logger.warn("currentSampleCount > segmentSampleCount: " + currentSampleCount + ", "
                    + segmentSampleCount + "block " + sr.getControlHeader().getSequenceNum());
              }
            }
          } else {
            logger.warn("File " + getFile().getName() + ": Skipping block " + dr.getHeader()
                .getSequenceNum() + " due to absence of data");
          }
        } else {
          logger.warn("File " + getFile().getName() + ": Skipping block " + sr.getControlHeader()
              .getSequenceNum() + " so as no-data record");
        }
      }
    } catch (FileNotFoundException e) {
      logger.error("Can't find file: ", e);
      System.exit(0);
    } catch (IOException e) {
      StringBuilder message = new StringBuilder();
      try {
        message.append(this).append(" ").append(segment).append(" Ending position ")
            .append(dis != null ? dis.getFilePointer() : 0).append(", sampleCount read")
            .append(currentSampleCount)
            .append(", samples from headers ").append(headerSampleCount).append(", blocks read ")
            .append(blockNumber);
        logger.error(message.toString(), e);
      } catch (IOException eIO) {
        logger.error("IOException:", eIO);
      }
      System.exit(0);
    } catch (SeedFormatException e) {
      logger.error("Wrong seed format: ", e);
      System.exit(0);
    } finally {
      try {
				if (dis != null) {
					dis.close();
				}
      } catch (IOException e) {
        logger.error("IOException:", e);
      }
    }
    // Test int[] array adding
		for (int value : data) {
			segment.addDataPoint(value);
		}
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
      SeedRecord sr = SynchronizedSeedRecord
          .read(dis, TraceView.getConfiguration().getDefaultBlockLength());
      ControlHeader ch;
      ch = sr.getControlHeader();
      //ret = ret + "<br><i>Query time: </i> " + TimeInterval.formatDate(new Date(time), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE);
      ret = ret + "<br><i>Seq number:</i> " + ch.getSequenceNum()
          + "<br><i>Is continuation:</i> " + ch.isContinuation()
          + "<br><i>Type:</i> " + ch.getTypeCode();
      if (ch.getTypeCode() == (byte) 'D' || ch.getTypeCode() == (byte) 'R'
          || ch.getTypeCode() == (byte) 'Q') {
        DataHeader dh = (DataHeader) ch;
        // if there's a data header the record is a data record
        DataRecord dr = (DataRecord) sr;
        ret = ret + "<br><i>Size:</i> " + dh.getSize()
            // + "<br><i>Channel:</i> " + dh.getNetworkCode() + "/" + dh.getStationIdentifier() + "/" + dh.getLocationIdentifier() + "/" + dh.getChannelIdentifier()
            + "<br><i>Start time:</i> " + TimeInterval.formatDate(new Date(getBlockStartTime(dh)),
            TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
            + "<br><i>Num samples:</i> " + dh.getNumSamples()
            // get the sample rate from the record because the header method is deprecated
            + "<br><i>Sample rate:</i> " + dr.getSampleRate()
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
				if (dis != null) {
					dis.close();
				}
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
  private void addSegment(RawDataProvider channel, long currentOffset,
      double sampleRate, int serialNumber, int segmentSampleCount, long segmentStartTime, long segmentEndTime) {
    if (segmentSampleCount != 0) {
      Segment segment = new Segment(this, currentOffset, new Date(segmentStartTime), sampleRate,
          segmentSampleCount, serialNumber);
      channel.addSegment(segment);
    }
  }
}
