package com.isti.traceview.data;

import static edu.sc.seis.seisFile.fdsnws.AbstractQueryParams.DEFAULT_HOST;

import asl.utils.TimeSeriesUtils;
import asl.utils.input.DataBlock;
import com.isti.traceview.TraceView;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.FDSNDataSelectQueryParams;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

public class SourceSocketFDSN extends SourceSocket {

  private static final Logger logger = Logger.getLogger(SourceSocketFDSN.class);
  private double[] cachedData;
  private long interval;

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
    Set<PlotDataProvider> ret = new HashSet<>();
    String host = TraceView.getConfiguration().getDataServiceHost();
    String path = TraceView.getConfiguration().getDataServicePath();
    try {
      String scheme = "http";
      DataBlock db = TimeSeriesUtils.getTimeSeriesFromFDSNQuery(scheme, host, path, network, station, location,
          channel, startTime, endTime);
      cachedData = db.getData();
      PlotDataProvider pdp = new PlotDataProvider(channel,
          DataModule.getOrAddStation(station), network, location);
      interval = db.getInterval();
      int numberSamples = cachedData.length;
      logger.debug("Expected sample count: " + numberSamples);
      ret.add(pdp);
      Segment segment = new Segment(this, 0,
          Date.from(Instant.ofEpochMilli(startTime)), (double) interval, numberSamples, 0);
      pdp.addSegment(segment);
    } catch (SeisFileException | IOException | CodecException e) {
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
    for (int i = 0; i < segment.getSampleCount(); ++i) {
      segment.addDataPoint((int) cachedData[i + cachedDataOffset]);
    }
  }


}
