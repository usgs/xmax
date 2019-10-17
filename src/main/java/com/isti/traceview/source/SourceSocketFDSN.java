package com.isti.traceview.source;

import asl.utils.TimeSeriesUtils;
import asl.utils.input.DataBlock;
import com.isti.traceview.TraceView;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.seisFile.SeisFileException;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import javax.swing.SwingWorker;
import org.apache.log4j.Logger;

public class SourceSocketFDSN extends SourceSocket {

  private static final Logger logger = Logger.getLogger(SourceSocketFDSN.class);
  private Map<String, DataBlock> cachedData;
  private List<String> dataNames;
  private long interval;

  /**
   * Connect to the FDSN and receive data from one trace over a given time range.
   * Currently, wildcards are not supported.
   * FDSN connection parameters such as port, url, etc. are taken from config.xml file
   * @param network Name of network to get data from
   * @param station Name of station
   * @param location Name of location (i.e., "00")
   * @param channel Name of channel (i.e., "LHZ")
   * @param startTime Start time, in epoch ms
   * @param endTime End time, in epoch ms
   */
  public SourceSocketFDSN(String network, String station, String location, String channel,
      long startTime, long endTime) {
    super(network, station, location, channel, startTime, endTime);
  }

  @Override
  public FormatType getFormatType() {
    return FormatType.MSEED;
  }

  @Override
  public Set<PlotDataProvider> parse() {
    Set<PlotDataProvider> ret = Collections.synchronizedSet(new HashSet<>());
    Configuration config = TraceView.getConfiguration();
    String scheme = config.getDataServiceProtocol();
    String host = config.getDataServiceHost();
    String path = config.getDataServicePath();
    int port = config.getDataServicePort();
    try {
      cachedData =  TimeSeriesUtils.getDataFromFDSNQuery(scheme, host, port, path,
          network, station, location, channel, startTime, endTime);

      dataNames = new ArrayList<>(cachedData.keySet());
      // here the index into datanames list is used as start offset
      // snclData has format {network [0], station [1], location [2], channel [3]}
      IntStream.range(0, dataNames.size()).parallel().forEach(i -> {
        String key = dataNames.get(i);
        String[] snclData = key.split("_"); // split on occurence of character '_'
        logger.debug(Arrays.toString(snclData));
        DataBlock block = cachedData.get(key);
        PlotDataProvider pdp = new PlotDataProvider(snclData[3],
            DataModule.getOrAddStation(snclData[1]), snclData[0], snclData[2]);
        interval = block.getInterval();
        logger.debug("Expected sample count: " + cachedData.size());
        ret.add(pdp);
        Map<Long, double[]> dataMap = block.getDataMap();
        for (Long startTime : dataMap.keySet()) {
          double[] timeSeries = dataMap.get(startTime);
          int numberSamples = timeSeries.length;
          Segment segment = new Segment(this, i,
              Date.from(Instant.ofEpochMilli(startTime)), (double) interval, numberSamples, 0);
          pdp.addSegment(segment);
        }
      });
    } catch(SeisFileException | IOException | CodecException e) {
      logger.error(e);
    }
    return ret;
  }

  @Override
  public void load(Segment segment) {
    logger.debug("SAMPLE COUNT: " + segment.getSampleCount());
    int cachedDataOffset = (int)
        ((segment.getStartTime().toInstant().toEpochMilli() - startTime) / interval);
    logger.debug("CACHED DATA OFFSET? " + cachedDataOffset);
    long startTime = segment.getStartTime().toInstant().toEpochMilli();
    int offset = (int) segment.getStartOffset();
    double[] dataRange = cachedData.get(dataNames.get(offset)).getDataMap().get(startTime);
    for (int i = 0; i < segment.getSampleCount(); ++i) {
      segment.addDataPoint((int) dataRange[i]);
    }
  }


}
