package com.isti.traceview.source;

import asl.utils.timeseries.*;
import com.isti.traceview.TraceView;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.FDSNStationQuerier;
import edu.sc.seis.seisFile.fdsnws.FDSNStationQueryParams;
import edu.sc.seis.seisFile.fdsnws.FDSNWSException;
import edu.sc.seis.seisFile.fdsnws.stationxml.FDSNStationXML;
import edu.sc.seis.seisFile.fdsnws.stationxml.Network;
import edu.sc.seis.seisFile.fdsnws.stationxml.NetworkIterator;
import edu.sc.seis.seisFile.fdsnws.stationxml.Station;
import edu.sc.seis.seisFile.fdsnws.stationxml.StationIterator;
import edu.sc.seis.seisFile.fdsnws.stationxml.StationXMLException;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

public class SourceSocketFDSN extends SourceSocket {

  private static final Logger logger = Logger.getLogger(SourceSocketFDSN.class);
  private Map<String, DataBlock> cachedData;
  private List<String> dataNames;
  private Map<String, Long> intervals;

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
      cachedData = new HashMap<>();
      List<Pair<String, String>> networkAndStations = getStationsViaMetadata(
          network, station, location, channel, startTime, scheme, host, port, path
      );

      // get data for each station separately to prevent timeouts on particularly long queries
      for (Pair<String, String> networkAndStation : networkAndStations) {
        String net = networkAndStation.getFirst();
        String sta = networkAndStation.getSecond();
        cachedData.putAll(TimeSeriesUtils.getDataFromFDSNQuery(scheme, host, port, path,
            net, sta, location, channel, startTime, endTime));
      }
      dataNames = new ArrayList<>(cachedData.keySet());
      intervals = new HashMap<>();
      // here the index into datanames list is used as start offset
      // snclData has format {network [0], station [1], location [2], channel [3]}
      IntStream.range(0, dataNames.size()).parallel().forEach(i -> {
        String key = dataNames.get(i);
        String[] snclData = key.split("_"); // split on occurence of character '_'
        logger.debug(Arrays.toString(snclData));
        DataBlock block = cachedData.get(key);
        PlotDataProvider pdp = new PlotDataProvider(snclData[3],
            DataModule.getOrAddStation(snclData[1]), snclData[0], snclData[2]);
        intervals.put(key, block.getInterval());
        logger.debug("Expected sample count: " + cachedData.size());
        ret.add(pdp);
        Map<Long, double[]> dataMap = block.getDataMap();
        for (Long startTime : dataMap.keySet()) {
          double[] timeSeries = dataMap.get(startTime);
          int numberSamples = timeSeries.length;
          Segment segment = new Segment(this, i, Instant.ofEpochMilli(startTime),
              (double) block.getInterval(), numberSamples, 0);
          pdp.addSegment(segment);
        }
      });
    } catch(SeisFileException | IOException | CodecException | XMLStreamException e) {
      logger.error(e);
    }
    return ret;
  }

  private List<Pair<String, String>> getStationsViaMetadata(String network, String station,
      String location, String channel, long startTime, String scheme, String host, int port,
      String path) throws FDSNWSException, XMLStreamException, StationXMLException {

    List<Pair<String, String>> stationNetworkPairs = new ArrayList<>();

    Instant epochInstant = Instant.ofEpochMilli(startTime);

    // TODO: implement as a 'getQuerier' method in asl-java-utils?
    FDSNStationQueryParams params = new FDSNStationQueryParams();
    params.setScheme(scheme);
    params.setPort(port);
    params.setHost(host);
    params.setFdsnwsPath(path);
    params.setLevel(FDSNStationQueryParams.LEVEL_RESPONSE);
    params.setStartBefore(epochInstant).setEndAfter(epochInstant).appendToNetwork(network)
        .appendToStation(station).appendToLocation(location).appendToChannel(channel);
    FDSNStationQuerier querier = new FDSNStationQuerier(params);
    FDSNStationXML xml = querier.getFDSNStationXML();

    NetworkIterator nIt = xml.getNetworks();
    while (nIt.hasNext()) {
      Network n = nIt.next();
      StationIterator sIt = n.getStations();
      while (sIt.hasNext()) {
        Station s = sIt.next();
        stationNetworkPairs.add(new Pair<>(n.getCode(), s.getCode()));
      }
    }
    querier.close();

    return stationNetworkPairs;
  }

  @Override
  public void load(Segment segment) {
    logger.debug("SAMPLE COUNT: " + segment.getSampleCount());
    int cachedDataOffset = (int)
        ((segment.getStartTime().toEpochMilli() - startTime) / segment.getSampleIntervalMillis());
    logger.debug("CACHED DATA OFFSET? " + cachedDataOffset);
    long startTime = segment.getStartTime().toEpochMilli();
    int offset = (int) segment.getStartOffset();
    double[] dataRange = cachedData.get(dataNames.get(offset)).getDataMap().get(startTime);
    for (int i = 0; i < segment.getSampleCount(); ++i) {
      segment.addDataPoint((int) dataRange[i]);
    }
  }


}
