package com.isti.traceview.data;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.gui.IColorModeState;
import com.isti.traceview.source.SourceFile;
import com.isti.traceview.source.SourceSocketFDSN;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 * This class holds collections of events, stations, traces loaded from configured data sources. It
 * provides a simplified interface to access data module functions.
 *
 * @author Max Kokoulin
 */
public class DataModule extends Observable {

  private static final Logger logger = Logger.getLogger(DataModule.class);

  private IChannelFactory channelFactory = new DefaultChannelFactory();

  /**
   * List of found channels
   */
  private final List<PlotDataProvider> channels;

  /**
   * Map of affected stations
   */
  private static Map<String, Station> stations = new HashMap<>();

  /**
   * List of found files with trace data
   */
  private List<ISource> dataSources;

  private List<Response> responses;

  // Information about current channel set
  private int markerPosition;
  private int windowSize;
  private int from = 0;
  private int to = 0;

  /**
   * Time interval including ALL found channels
   */
  private TimeInterval allChannelsTI = null;

  protected static TemporaryStorage storage = null;

  /**
   * Constructor
   */
  public DataModule() {
    allChannelsTI = new TimeInterval();
    channels = Collections.synchronizedList(new ArrayList<>());
    markerPosition = 0;
    dataSources = new ArrayList<>();
    responses = new ArrayList<>();
  }

  /**
   * Sets channel factory. Customers can set their own channel factory and get their customized data
   * providers during sources parsing
   *
   * @param factory instance of class implementing IChannelFactory interface
   */
  public void setChannelFactory(IChannelFactory factory) {
    this.channelFactory = factory;
  }

  /**
   * Class to load in data from a list of files (taken from config file, -d parameter in cmd, or
   * built as part of the setup in a test case)
   *
   * @param files List of file objects to be loaded in
   */
  public File[] loadNewDataFromSources(File... files) {
    // TODO: probably need to find a way to parallelize this?
    List<File> filesProducingError = new ArrayList<>(files.length);
    for (File file : files) {
      try {
        logger.info("Loading in " + file.getCanonicalPath());
      } catch (IOException e) {
        filesProducingError.add(file);
        logger.error(e);
        continue;
      }
      ISource fileParser = SourceFile.getDataFile(file);
      addDataSource(fileParser);
      Set<PlotDataProvider> dataSet = fileParser.parse();
      for (PlotDataProvider channel : dataSet) {
        String station = channel.getStation().getName();
        getOrAddStation(station);
        // merge in the new data into any channel that may already exist
        getOrAddChannel(channel.getChannelName(), channel.getStation(),
            channel.getNetworkName(), channel.getLocationName()).mergeData(channel);
      }
    }

    channels.sort(Channel.getComparator(TraceView.getConfiguration().getPanelOrder()));
    for (RawDataProvider channel : channels) {
      channel.sort(); // properly numerate segments, gaps, etc. for channel plot coloration
    }
    return filesProducingError.toArray(new File[]{});
  }

  /**
   * Load in data from data path given in configuration and then ensure it is fully parsed in.
   * This allows data to be loaded in from a single call for tests
   */
  public void loadAndParseDataForTesting() throws TraceViewException {
    loadNewDataFromSources();
    channels.forEach(RawDataProvider::load);
  }

  /**
   * Load in data from a list of files and then ensure it is parsed in.
   * This allows data to be loaded in from a single call for tests
   */
  public void loadAndParseDataForTesting(File... files) {
    loadNewDataFromSources(files);
    channels.forEach(RawDataProvider::load);
  }

  public void loadNewDataFromSources() throws TraceViewException {
    String mask = TraceView.getConfiguration().getDataPath();
    File[] files = SourceFile.getDataFiles(mask).toArray(new File[] {});
    loadNewDataFromSources(files);
  }

  public void reLoadData() throws TraceViewException {
    markerPosition = 0;
    windowSize = 0;
    from = 0;
    to = 0;
  }

  /**
   * Getter of allChannelsTI property
   *
   * @return time interval including ALL found channels
   */
  public TimeInterval getAllDataTimeInterval() {
    return allChannelsTI;
  }

  /**
   * Cleanup temp storage and dump all found data to temp storage
   */
  public void dumpData(IColorModeState colorMode) throws TraceViewException {

    System.out.format("     -T: Serialize data to temp storage ");
    if (storage == null) {
      logger.debug("storage == null --> new TemporaryStorage()");
      String dataTempPath = TraceView.getConfiguration().getDataTempPath();
      storage = new TemporaryStorage(dataTempPath);

      // MTH: Check if dataTempPath exists and if not, try to create it:
      File dir = new File(dataTempPath);
      if (dir.exists()) {
        if (!dir.isDirectory()) {
          logger.error(String.format("== dataTempPath=[%s] is NOT a directory!\n", dataTempPath));
          System.exit(1);
        }
      } else {
        boolean success = dir.mkdirs();
        if (!success) {
          logger
              .error(String.format("unable to create directory dataTempPath=[%s]\n", dataTempPath));
          System.exit(1);
        }
      }
    }

    // MTH: if -t was given on command line then keep the files in /DATA_TEMP, else clear them out
    if (TraceView.getConfiguration().getUseTempData()) {
      System.out.format(" [-t: Don't wipe out existing data in temp storage]\n");
    } else {
      System.out.format(" [First wipe out existing data in temp storage]\n");
      storage.delAllTempFiles();
    }

    Iterator<PlotDataProvider> it = getAllChannels().iterator();
    while (it.hasNext()) {
      PlotDataProvider channel = it.next();
      logger.debug("== call channel.load() for channel=" + channel);
      channel.load();
      logger.debug("== call channel.load() DONE for channel=" + channel);
      channel.initPointCache(colorMode);
      System.out.format("\tSerialize to file:%s\n", storage.getSerialFileName(channel));
      channel.dump(storage.getSerialFileName(channel));
      it.remove();

      //MTH: not sure why this is needed
      //channel.drop();
      //channel = null;
    }
    //printAllChannels();
  }

  /**
   * Getter of the property <tt>dataFiles</tt>
   *
   * @return Returns the list of found data files.
   */
  public List<ISource> getAllSources() {
    return dataSources;
  }

  /**
   * Add source of data to data module
   *
   * @param datafile file to add
   * @return list of {@link RawDataProvider}s found in the data file
   */
  public void addDataSource(ISource datafile) {

    // Parse seed file into trace segments based on times and/or gaps
    if (!isSourceLoaded(datafile)) {
      //dataSources.add(datafile);	// why is it adding twice?
      logger.debug("Parsing file " + datafile.getName());
      dataSources.add(datafile);

    }
  }

  /**
   * Returns flag if already loaded channels contain this source
   */
  public boolean isSourceLoaded(ISource ds) {
    if (getAllSources().contains(ds)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Check if we have found all sources for given channel
   */
  @SuppressWarnings("unused")
  private boolean channelHasAllSources(RawDataProvider channel) {
    List<ISource> sources = channel.getSources();
    for (Object o : sources) {
      if (o instanceof SourceFile) {
        if (!dataSources.contains(o)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * @return list of loaded stations
   */
  public static SortedSet<Station> getAllStations() {
    SortedSet<Station> loadedStations = new TreeSet<>();
    for (String key : stations.keySet()) {
      loadedStations.add(stations.get(key));
    }
    //return new TreeSet(stations.entrySet());
    return loadedStations;
  }

  /**
   * If we still have not this station, add it, if we have it, find it
   *
   * @param stationName name of station
   * @return Station as class
   */
  public static Station getOrAddStation(String stationName) {
    Station station = stations.get(stationName.trim());
    if (station == null) {
      station = addStation(stationName.trim());
    }
    return station;
  }

  /**
   * Find given station in the loaded list
   *
   * @param stationName name of station to find
   * @return Station as class, or null if not found
   */
  public static Station getStation(String stationName) {
    Station station = stations.get(stationName.trim());
    return station;
  }

  private static Station addStation(String stationName) {
    Station station = new Station(stationName);
    stations.put(station.getName(), station);
    logger.debug("Station added: " + stationName);
    return station;
  }

  /**
   * @return list of parsed traces
   */
  public List<PlotDataProvider> getAllChannels() {
    synchronized (channels) {
      logger.debug("getting channels");
      return channels;
    }
  }

  private void addChannel(PlotDataProvider channel) {
    synchronized (channels) {
      channels.add(channel);
      for (ISource src : channel.getSources()) {
        if (!isSourceLoaded(src)) {
          dataSources.add(src);
        }
      }
      logger.debug("Channel added: " + channel.toString());
    }
  }

  /**
   * Delete trace from internal data representation
   *
   * @param channel trace to delete
   */
  public void deleteChannel(PlotDataProvider channel) {
    synchronized (channels) {
      channels.remove(channel);
      if (!isChangedAllChannelsTI()) {
        setChanged();
        notifyObservers(channel);
      }
      logger.debug("Channel removed: " + channel.toString());
    }
  }

  /**
   * Delete list of traces from internal data representation
   *
   * @param toDelete list of traces to delete
   */
  public void deleteChannels(List<PlotDataProvider> toDelete) {
    synchronized (channels) {
      channels.removeAll(toDelete);
      if (!isChangedAllChannelsTI()) {
        setChanged();
        notifyObservers(toDelete);
      }
      logger.debug("Channels removed: list");
    }
  }

  /**
   * If we still have not trace with given SNCL, add it, if we have it, find it
   *
   * @param channelName name of channel
   * @param station station
   * @param networkName name of network
   * @param locationName name of location
   * @return initialized plot data provider for this trace
   */
  public PlotDataProvider getOrAddChannel(String channelName,
      Station station, String networkName, String locationName) {
    PlotDataProvider channel = channelFactory.getChannel(channelName.trim(),
        station, networkName.trim(), locationName.trim());
    synchronized (channels) {
      int i = channels.indexOf(channel);
      if (i >= 0) {
        // lg.debug("DataModule.getOrAddChannel() end");
        return channels.get(i);
      } else {
        addChannel(channel);
        // lg.debug("DataModule.getOrAddChannel() end");
        return channel;
      }
    }
  }

  /**
   * Find trace with given SNCL
   *
   * @param channelName name of channel
   * @param station station
   * @param networkName name of network
   * @param locationName name of location
   * @return plot data provider for this trace, or null if not found
   */
  public synchronized PlotDataProvider getChannel(String channelName,
      Station station, String networkName, String locationName) {
    PlotDataProvider channel = channelFactory.getChannel(channelName.trim(),
        station, networkName.trim(), locationName.trim());
    synchronized (channels) {
      int i = channels.indexOf(channel);
      if (i >= 0) {
        // lg.debug("DataModule.getChannel() end");
        return channels.get(i);
      } else {
        // lg.debug("DataModule.getChannel() end");
        return null;
      }
    }
  }

  /**
   * Computes display window position in traces list
   *
   * @return traces display window starting index
   * @see DataModule#getWindowSize(boolean)
   */
  public int getChannelSetStartIndex() {
    synchronized (channels) {
      // lg.debug("DataModule.getChannelSetStartIndex()");
      // don't allow the start index to be negative -- this might change when trying to get active
      // range of panel displays when a trace was deleted
      return Math.max(from, 0);
    }
  }

  /**
   * Computes display window position in traces list
   *
   * @return traces display window ending index
   * @see DataModule#getWindowSize(boolean)
   */
  public int getChannelSetEndIndex() {
    synchronized (channels) {
      // lg.debug("DataModule.getChannelSetEndIndex()");
      // we set the value of to here in the event a channel was deleted
      // otherwise our bounds will be messed up as we don't shift current active view range
      // i.e., if we are in the last show set, deleting a trace shrinks set size by 1
      // and so 'to' should be shrunk as well to reflect that
      to = Math.min(to, channels.size());
      return to;
    }
  }

  /**
   * Gets traces list for next window, see {@link DataModule#getWindowSize(boolean)}
   *
   * @return list of traces for next display window
   */
  public List<PlotDataProvider> getNextChannelSet() throws TraceViewException {
    synchronized (channels) {
      int newWindowSize = getWindowSize(true);
      if ((newWindowSize != 0)
          && ((markerPosition + newWindowSize) <= channels.size())) {
        from = markerPosition;
        to = Math.min(markerPosition + newWindowSize, channels.size());
        markerPosition = markerPosition + newWindowSize;
        windowSize = newWindowSize;
        logger.debug("END: from " + from
            + ", to " + to);
        return channels.subList(from, to);
      } else {
        throw new TraceViewException("This is the last set");
      }
    }
  }

  /**
   * Gets traces list for previous window, see {@link DataModule#getWindowSize(boolean)}
   *
   * @return list of traces for previous display window
   */
  public List<PlotDataProvider> getPreviousChannelSet()
      throws TraceViewException {
    synchronized (channels) {
      int newWindowSize = getWindowSize(false);
      if ((newWindowSize != 0) && (markerPosition > 1)) {
        markerPosition = markerPosition - windowSize - newWindowSize;
        from = markerPosition;
        to = Math.min(markerPosition + newWindowSize, channels.size());
        windowSize = 0;
        logger.debug("END: from " + from
            + ", to " + to);
        return channels.subList(from, to);
      } else {
        throw new TraceViewException("This is the first set");
      }
    }
  }

  public List<PlotDataProvider> getCurrentChannelSet(int frameUnits) {
    if (channels.size() == 0) {
      return new ArrayList<>();
    }
    int start = getChannelSetStartIndex();
    while (start >= channels.size()) {
      from -= frameUnits;
      start = getChannelSetStartIndex();
    }
    int end = getChannelSetEndIndex();
    return channels.subList(start, end);
  }

  /**
   * @param ws an unused parameter probably a window state?
   * @return flag if we have previous window (if no, this one is the first)
   * @deprecated This method appears to not be used by anything.
   */
  public boolean hasPreviousChannelSet(int ws) {
    return markerPosition > 1;
  }

  /**
   * <p>
   * We have list of trace, but it can be rather long and we can't display all traces in one screen.
   * So we should have some subset, or window, of traces list to show.
   * </p>
   * <p>
   * This function compute this window boundaries for next screen, based on given options: screen
   * counting units, quantity of units, sorting etc
   * <p>
   *
   * @param isForward flag if we go foreward/backward along traces list
   * @return window size in channels.
   */
  private int getWindowSize(boolean isForward) {
    Configuration.PanelCountUnit unit = TraceView.getConfiguration().getPanelCountUnit();
    int unitsInFrame = TraceView.getConfiguration().getUnitsInFrame();
    logger.debug("Units in frame: " + unitsInFrame);
    if (unit.equals(Configuration.PanelCountUnit.ALL)) {
      return channels.size();
    } else if (unit.equals(Configuration.PanelCountUnit.TRACE)) {
      if (isForward) {
        if (markerPosition + unitsInFrame < channels.size()) {

          // return markerPosition + windowSize +
          // unitsInFrame<channels.size() ?
          // unitsInFrame : channels.size() - markerPosition -
          return unitsInFrame;
        } else {
          return channels.size() - markerPosition;
        }
      } else {
        if (markerPosition - unitsInFrame >= 0) {
          return unitsInFrame;
        } else {
          return unitsInFrame - markerPosition;
        }
      }
    } else if (unit.equals(Configuration.PanelCountUnit.CHANNEL)) {
      int i = 0;
      int channelCount = 0;
      int ret = 0;
      String currentChannel = null;
      if (isForward) {
        for (i = markerPosition; i < channels.size(); i++) {
          String channel = channels.get(i).getName();
          if (!channel.equals(currentChannel)) {
            currentChannel = channel;
            channelCount++;
            if (channelCount > unitsInFrame) {
              return ret;
            }
          }
          ret++;
        }
        return ret;
      } else {
        for (i = markerPosition - windowSize - 1; i >= 0; i--) {
          String channel = channels.get(i).getName();
          if (!channel.equals(currentChannel)) {
            currentChannel = channel;
            channelCount++;
            if (channelCount > unitsInFrame) {
              return ret;
            }
          }
          ret++;
        }
        return ret;
      }
    } else if (unit.equals(Configuration.PanelCountUnit.CHANNEL_TYPE)) {
      int i = 0;
      int typeCount = 0;
      int ret = 0;
      String currentType = null;
      if (isForward) {
        for (i = markerPosition; i < channels.size(); i++) {
          String type = channels.get(i).getName().substring(
              channels.get(i).getName().length() - 1);
          if (!type.equals(currentType)) {
            currentType = type;
            typeCount++;
            if (typeCount > unitsInFrame) {
              return ret;
            }
          }
          ret++;
        }
        return ret;
      } else {
        for (i = markerPosition - windowSize - 1; i >= 0; i--) {
          String type = channels.get(i).getName().substring(
              channels.get(i).getName().length() - 1);
          if (!type.equals(currentType)) {
            currentType = type;
            typeCount++;
            if (typeCount > unitsInFrame) {
              return ret;
            }
          }
          ret++;
        }
        return ret;
      }
    } else if (unit.equals(Configuration.PanelCountUnit.STATION)) {
      int i = 0;
      int stationCount = 0;
      int ret = 0;
      Station currentStation = null;
      if (isForward) {
        for (i = markerPosition; i < channels.size(); i++) {
          Station station = channels.get(i).getStation();
          if (!station.equals(currentStation)) {
            currentStation = station;
            stationCount++;
            if (stationCount > unitsInFrame) {
              return ret;
            }
          }
          ret++;
        }
        return ret;
      } else {
        for (i = markerPosition - windowSize - 1; i >= 0; i--) {
          Station station = channels.get(i).getStation();
          if (!station.equals(currentStation)) {
            currentStation = station;
            stationCount++;
            if (stationCount > unitsInFrame) {
              return ret;
            }
          }
          ret++;
        }
        return ret;
      }
    } else {
      return -1;
    }
  }

  /**
   * @return reference to temporary storage area
   */
  public static TemporaryStorage getTemporaryStorage() {
    return storage;
  }

  /**
   * Fill up stations from station file Station file contains all info on station (lat/lon, elev,
   * time, depth, etc.)
   */
  protected void loadStations() {
    LineNumberReader r = null;
    try {
      r = new LineNumberReader(
          new FileReader(TraceView.getConfiguration().getStationInfoFileName()));
      String str = null;
      while ((str = r.readLine()) != null) {
        String name = str.substring(0, 7).trim();
        if (!name.equals("") && !name.equals("STAT")) {
          String network = str.substring(7, 11).trim();
          String longName = str.substring(11, 61).trim();
          String startDate = str.substring(61, 71).trim();
          String endDate = str.substring(71, 83).trim();
          String latitude = str.substring(83, 93).trim();
          String longitude = str.substring(93, 105).trim();
          String elevation = str.substring(105, 116).trim();
          String depth = str.substring(116, str.length()).trim();
          Station station = stations.get(name);
          if (station != null) {
            station.setNetwork(network);
            station.setLongName(longName);
            station.setLatitude(new Double(latitude));
            station.setLongitude(new Double(longitude));
            station.setElevation(new Double(elevation));
            station.setDepth(new Double(depth));
            logger.debug("Station loaded: name " + name + ", network "
                + network + ", longName " + longName
                + ", startDate " + startDate + ", endDate "
                + endDate + ", latitude " + latitude
                + ", longitude " + longitude + ", elevation "
                + elevation + ", depth " + depth);
          }
        }
      }
    } catch (FileNotFoundException e) {
      logger.error("Can't open station file: ", e);

    } catch (IOException e) {
      logger.error("Error during reading station file: ", e);
    } finally {
      try {
        r.close();
      } catch (IOException e) {
        logger.error("IOException:", e);
      }
    }
  }

  private boolean isChangedAllChannelsTI() {
    boolean ret = false;
    TimeInterval newTI = new TimeInterval();
    for (PlotDataProvider channel : channels) {
      newTI.setMinValue(channel.getTimeRange().getStartTime());
      newTI.setMaxValue(channel.getTimeRange().getEndTime());
    }
    if (!newTI.equals(allChannelsTI)) {
      allChannelsTI = newTI;
      ret = true;
      setChanged();
      notifyObservers(allChannelsTI);
    }
    return ret;
  }

  public static String getResponseFile(String network, String station,
      String location, String channel) throws TraceViewException {
    List<String> respFiles = new ArrayList<>();

    // these are the locations we intend to search -- first, the current working directory,
    // then the response path defined in config, then where the config is
    String[] pathsToSearch = new String[]{
        Paths.get(".").toAbsolutePath().toString(),
        TraceView.getConfiguration().getResponsePath(),
        TraceView.getConfiguration().getConfigFileDir()};

    for (String path : pathsToSearch) {
      addRespFiles(path, network, station, location, channel, respFiles);
      if (respFiles.size() > 0) {
        return respFiles.get(0);
      }
    }

    // now that we have done looking at root of all directories, let's do a sub-search if we
    // still haven't found a valid response
    for (String path : pathsToSearch) {
      addRespFilesSubdirFallback(path, network, station, location, channel, respFiles);
      if (respFiles.size() > 0) {
        return respFiles.get(0);
      }
    }

    // if there STILL isn't a valid response to find, then just return a null object instead
    return null;
  }

  public List<String> getAllResponseFiles() throws TraceViewException {
    List<String> respFiles = new ArrayList<>();
    addRespFiles(TraceView.getConfiguration().getConfigFileDir(), channels, respFiles);
    addRespFiles("./", channels, respFiles);
    addRespFiles(TraceView.getConfiguration().getResponsePath(), channels, respFiles);
    return respFiles;
  }

  private static void addRespFiles(String dirname, String network,
      String station, String location, String channel,
      List<String> whereToAdd) throws TraceViewException {

    File path = new File(dirname);
    if (!path.isDirectory()) {
      throw new TraceViewException("Loading responses from " + dirname
          + ": is not directory");
    }

    String respFileName = "RESP." + network + "." + station + "." + location + "." + channel;
    File file = new File(dirname + File.separator + respFileName);
    if (file.exists()) {
      whereToAdd.add(file.getAbsolutePath());
    }

  }

  private static void addRespFilesSubdirFallback(String dirname, String network,
      String station, String location, String channel,
      List<String> whereToAdd) throws TraceViewException {

    File path = new File(dirname);

    // our acceptance criterion is whether or not a file is a directory
    // this is slightly odd syntax for older Java developers; we're basically using a lambda here
    File[] subdirectories = path.listFiles(File::isDirectory);
    assert subdirectories != null;
    for (File subdirectory : subdirectories) {
      int size = whereToAdd.size();
      String subPath = subdirectory.getAbsolutePath() + "/";
      addRespFiles(subPath, network, station, location, channel, whereToAdd);
      if (whereToAdd.size() > size) {
        return;
      }
    }

  }

  private static void addRespFiles(String dirname, List<? extends Channel> channels,
      List<String> whereToAdd) throws TraceViewException {
    for (Channel channel : channels) {
      String network = channel.getNetworkName();
      String station = channel.getStation().getName();
      String location = channel.getLocationName();
      String channelName = channel.getChannelName();
      addRespFiles(dirname, network, station, location, channelName, whereToAdd);
    }
  }

  /**
   * Getter of responses property
   *
   * @return list of all loaded responses
   */
  public List<Response> getLoadedResponses() {
    return responses;
  }

  /**
   * Gets response for given SNCL
   *
   * @param channel name of channel
   * @param station station
   * @param network name of network
   * @param location name of location
   * @return response class
   */
  public Response getResponse(String network, String station, String location, String channel) {
    Response resp;

    if (TraceView.getConfiguration() != null) {
      Configuration config = TraceView.getConfiguration();
      if (config.stationXMLPreferred()) {
        logger.info("Attempting to get XML metadata based on user preferences.");

        // default to local copy to allow user to override data based on preferences
        if (config.getStationXMLPath() != null) {
          String xmlFilename = network + "." + station + "." + location + "." + channel + ".xml";
          logger.info("Attempting to read response from file: " + xmlFilename);
          resp = Response.getResponseFromXML(network, station, location, channel, xmlFilename);
          if (resp != null) {
            return resp;
          }
        }

        logger.info("Attempting to download response data from web services.");
        String webservicesURL = "http://" + config.getDataServiceHost() + config.getMetadataServicePath();
        // note that above should be non-null
        resp = Response.getResponseFromWeb(network, station, location, channel, webservicesURL);
        if (resp != null) {
          return resp;
        }

      }
    }

    // try to load from files
    try {
      String respFile = getResponseFile(network, station, location, channel);
      if (respFile != null) {
        File f = new File(respFile);
        resp = Response.getResponse(f);
        if (resp != null) {
          logger.info("Response loaded from file: " + respFile);
          return resp;
        }
      }
    } catch (TraceViewException e) {
      logger.error("TraceViewException:", e);
    }

    return null;

  }

  /**
   * Gets response for given SNCL, and cashes it in memory
   *
   * @param channel name of channel
   * @param station station
   * @param network name of network
   * @param location name of location
   * @return response class
   */
  public Response getResponseCashed(String network, String station, String location,
      String channel) {
    Response resp = new Response(network, station, location, channel, null, null);
    int i = responses.indexOf(resp);
    if (i >= 0) {
      return responses.get(i);
    } else {
      resp = getResponse(network, station, location, channel);
      responses.add(resp);
      return resp;
    }
  }

  // MTH:
  public void printAllChannels() {
    System.out
        .format("== DataModule: Number of channels(=PDP's) attached =[%d]:\n", channels.size());
    for (PlotDataProvider channel : channels) {
      System.out.format("\t[PDP: %s] [nsegs=%d] [isLoadingStarted=%s] [isLoaded=%s]\n",
          channel.toString(), channel.getSegmentCount(), channel.isLoadingStarted(),
          channel.isLoaded());
      List<Segment> segs = channel.getRawData();
      for (Segment seg : segs) {
        System.out.format("\t[%d][%d]:%s [Source:%s]\n", seg.getSourceSerialNumber(),
            seg.getChannelSerialNumber(), seg.toString(), seg.getDataSource().getName());
        System.out.println();
      }
    }
  }

  public void loadNewDataFromSocket(String net, String sta, String loc, String cha,
      long startMillis, long endMillis) {

    ISource socketSource = new SourceSocketFDSN(net, sta, loc, cha, startMillis, endMillis);
    addDataSource(socketSource);
    Set<PlotDataProvider> dataSet = socketSource.parse();
    for (PlotDataProvider channel : dataSet) {
      String station = channel.getStation().getName();
      getOrAddStation(station);
      // merge in the new data into any channel that may already exist
      getOrAddChannel(channel.getChannelName(), channel.getStation(),
          channel.getNetworkName(), channel.getLocationName()).mergeData(channel);
    }

    channels.sort(Channel.getComparator(TraceView.getConfiguration().getPanelOrder()));
    for (RawDataProvider channel : channels) {
      channel.sort(); // properly numerate segments, gaps, etc. for channel plot coloration
    }
  }

}
