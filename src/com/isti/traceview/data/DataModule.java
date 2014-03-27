package com.isti.traceview.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.gui.IColorModeState;

/**
 * This class holds collections of events, stations, traces loaded from
 * configured data sources. It provides a simplified interface to access data
 * module functions.
 * 
 * @author Max Kokoulin
 */
public class DataModule extends Observable {
	private static Logger lg = Logger.getLogger(DataModule.class);

	/**
	 * List of found files with trace data
	 */
	private List<ISource> dataSources;

	/**
	 * Map of affected stations
	 */
	private static Map<String, Station> stations = new HashMap<String, Station>();

	private IChannelFactory channelFactory = new DefaultChannelFactory();

	/**
	 * List of found channels
	 */
	private List<PlotDataProvider> channels;

	List<Response> responses;

	// Information about current channel set
	private int markerPosition;
	private int windowSize;
	private int from = 0;
	private int to = 0;

	/**
	 * Time interval including ALL found channels
	 */
	private TimeInterval allChannelsTI = null;

	private static TemporaryStorage storage = null;

	/**
	 * Constructor
	 */
	public DataModule() {
		allChannelsTI = new TimeInterval();
		channels = Collections.synchronizedList(new ArrayList<PlotDataProvider>());
		markerPosition = 0;
		dataSources = new ArrayList<ISource>();
		responses = new ArrayList<Response>();
	}

	/**
	 * Sets channel factory. Customers can set their own channel factory and get
	 * their customized data providers during sources parsing
	 * 
	 * @param factory
	 *            instance of class implementing IChannelFactory interface
	 */
	public void setChannelFactory(IChannelFactory factory) {
		this.channelFactory = factory;
	}

	/**
	 * Loads data during startup. If useTempData flag set in the configuration,
	 * first looks in temporary storage, after looks in configured data
	 * directory and parse file data sources which absent in temp storage area
	 */
	public void loadData() throws TraceViewException {
        lg.debug("\n== Enter DataModule.loadData()\n");
     // -t: Read serialized PlotDataProviders from TEMP_DATA
		if (TraceView.getConfiguration().getUseTempData()) {
            lg.debug("-t: Read from temp storage\n");
            System.out.format("     -t: Read from temp storage\n");
			if (storage == null) {
				storage = new TemporaryStorage(TraceView.getConfiguration().getDataTempPath());
			}
			for (String tempFileName : storage.getAllTempFiles()) {
                lg.debug("PDP.load: tempFileName=" + tempFileName);
                System.out.format("         Read serialized file:%s\n", tempFileName);
				PlotDataProvider channel = PlotDataProvider.load(tempFileName);
             // MTH:
                if (TraceView.getConfiguration().getUseDataPath()) {
                    lg.debug("== loadData(): -t AND -d chosen --> null PlotDataProvider's pointsCache");
                    channel.nullPointsCache();
                }
				if ((channels.indexOf(channel) <= 0)) {
					addChannel(channel);
				}
			}
        // Should this be moved to after the -d read in case there are other channels ... ?
			Collections.sort(channels, Channel.getComparator(TraceView.getConfiguration().getPanelOrder()));
            lg.debug("-t: Read from temp storage DONE\n\n");
		}
     // -d: Read data from data path. Note that at this point we are merely *parsing* the
     //     data (e.g., mseed files) to construct PlotDataProviders, and the actual 
     //     traces (=Segments) won't be read in until just before they are displayed on the screen. 
		if (TraceView.getConfiguration().getUseDataPath()) {
            System.out.format("     -d: Read from data path\n");
            lg.debug("-d: Read from data path --> addDataSources()\n");
		    addDataSources(SourceFile.getDataFiles(TraceView.getConfiguration().getDataPath()));
            lg.debug("-d: Read from data path DONE\n\n");
        }
		else {
            if (!TraceView.getConfiguration().getUseTempData()) {
            lg.debug("-d + -t are both false: Read from data path --> addDataSources()\n");
		    addDataSources(SourceFile.getDataFiles(TraceView.getConfiguration().getDataPath()));
            lg.debug("-d + -t: Read from data path DONE\n\n");
            }
        }

		// Fill up stations from station file
		loadStations();
		setChanged();
		notifyObservers();

        //printAllChannels();
        lg.debug("== Exit DataModule.loadData()\n\n");
	}

	public void reLoadData() throws TraceViewException {
		markerPosition = 0;
		windowSize = 0;
		from = 0;
		to = 0;
		channels.clear();
		dataSources.clear();
		stations.clear();
		loadData();
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
        lg.debug("== DataModule.dumpData() ENTER");
        lg.debug("== DataModule.dumpData() call DataModule.loadData()");
        loadData();
        lg.debug("== DataModule.dumpData() call DataModule.loadData() DONE");

        System.out.format("     -T: Serialize data to temp storage ");

		if (storage == null) {
            lg.debug("== DataModule.dumpData() storage == null --> new TemporaryStorage()");
			storage = new TemporaryStorage(TraceView.getConfiguration().getDataTempPath());

    // MTH: Check if dataTempPath exists and if not, try to create it:
            String dataTempPath = TraceView.getConfiguration().getDataTempPath();
            File dir = new File(dataTempPath);
            if (dir.exists() ) {
                if (!dir.isDirectory()) {
                    System.out.format("== DataModule.dumpData(): ERROR: dataTempPath=[%s] is NOT a directory!\n", dataTempPath);
                    System.exit(1);
                }
                //System.out.format("== DataModule.dumpData(): dataTempPath=[%s] exists\n", dataTempPath);
            }
            else {
                Boolean success = dir.mkdirs();
                if (!success) {
                    System.out.format("== DataModule.dumpData(): ERROR: unable to create directory dataTempPath=[%s]\n", dataTempPath);
                    System.exit(1);
                }
                //System.out.format("== DataModule.dumpData(): successfully created dir dataTempPath=[%s]\n", dataTempPath);
            }
		}

    // MTH: if -t was given on command line then keep the files in /DATA_TEMP, else clear them out
        if (TraceView.getConfiguration().getUseTempData()) {
            System.out.format(" [-t: Don't wipe out existing data in temp storage]\n");
            // do nothing
        }
        else {
            System.out.format(" [First wipe out existing data in temp storage]\n");
		    storage.delAllTempFiles();
        }

		Iterator<PlotDataProvider> it = getAllChannels().iterator();
		while (it.hasNext()) {
			PlotDataProvider channel = it.next();
            lg.debug("== DataModule.dumpData() call channel.load() for channel=" + channel);
			channel.load();
            lg.debug("== DataModule.dumpData() call channel.load() DONE for channel=" + channel);
			channel.initPointCache(colorMode);
            System.out.format("         Serialize to file:%s\n", storage.getSerialFileName(channel));
			channel.dump(storage.getSerialFileName(channel));
			it.remove();
            //MTH: not sure why this is needed
			//channel.drop();
			//channel = null;
		}
        //printAllChannels();
        lg.debug("== DataModule.dumpData() EXIT");
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
	 * @param datafile
	 *            file to add
	 * @return list of {@link RawDataProvider}s found in the data file
	 */
	public Set<RawDataProvider> addDataSource(ISource datafile) {
		Set<RawDataProvider> changedChannels = null;
		lg.debug("DataModule.addDataSource() begin");
		if (!isSourceLoaded(datafile)) {
			dataSources.add(datafile);
			lg.debug("Parsing file " + datafile.getName());
			changedChannels = datafile.parse(this);
			dataSources.add(datafile);
			checkDataIntegrity(changedChannels);
			if (!isChangedAllChannelsTI()) {
				setChanged();
				notifyObservers(datafile);
			}
		}
		lg.debug("DataModule.addDataSource() end");
		return changedChannels;
	}

	/**
	 * Add list of data sources to data module
	 * 
	 * @param datafiles
	 *            sources list to add
	 * @return list of {@link RawDataProvider}s found in the sources
	 */
	public Set<RawDataProvider> addDataSources(List<ISource> datafiles) {
		boolean wasAdded = false;
		Set<RawDataProvider> changedChannels = new HashSet<RawDataProvider>();

		lg.debug("DataModule.addDataSources() begin");
		for (ISource datafile : datafiles) {
			if (!isSourceLoaded(datafile)) {
				lg.debug("Parsing " + datafile.getName());
				wasAdded = true;
				changedChannels.addAll(datafile.parse(this));
				dataSources.add(datafile);
			}
		}
		if (wasAdded) {
			checkDataIntegrity(changedChannels);
			if (!isChangedAllChannelsTI()) {
				setChanged();
				notifyObservers(datafiles);
			}
		}
		lg.debug("DataModule.addDataSources() end");

		return changedChannels;
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

	private void checkDataIntegrity(Set<RawDataProvider> changedChannels) {
		Iterator<RawDataProvider> it = changedChannels.iterator();
		while (it.hasNext()) {
			RawDataProvider channel = it.next();
			// lg.debug("Sorting " + channel.toString());
			if (channel.getRawData().size() == 0) {
				lg.warn("Deleting " + channel.toString()
						+ " due to absence of data");
				channels.remove(channel);
			} else {
				channel.sort();
			}
		}
		Collections.sort(channels, Channel.getComparator(TraceView.getConfiguration().getPanelOrder()));
	}

	/**
	 * Check if we have found all sources for given channel
	 */
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
		return new TreeSet(stations.entrySet());
	}

	/**
	 * If we still have not this station, add it, if we have it, find it
	 * 
	 * @param stationName
	 *            name of station
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
	 * @param stationName
	 *            name of station to find
	 * @return Station as class, or null if not found
	 */
	public static Station getStation(String stationName) {
		Station station = stations.get(stationName.trim());
		return station;
	}

	private static Station addStation(String stationName) {
		Station station = new Station(stationName);
		stations.put(station.getName(), station);
		lg.debug("Station added: name " + stationName);
		return station;
	}

	/**
	 * @return list of parsed traces
	 */
	public List<PlotDataProvider> getAllChannels() {
		synchronized (channels) {
			lg.debug("DataModule.getAllChannels() ");
			return channels;
		}
	}

	private void addChannel(PlotDataProvider channel) {
		synchronized (channels) {
			lg.debug("DataModule.addChannel() begin");
			channels.add(channel);
			for (ISource src : channel.getSources()) {
				if (!isSourceLoaded(src)) {
					dataSources.add(src);
				}
			}
			lg.debug("Channel added: " + channel.toString());
		}
	}

	/**
	 * Delete trace from internal data representation
	 * 
	 * @param channel
	 *            trace to delete
	 */
	public void deleteChannel(PlotDataProvider channel) {
		synchronized (channels) {
			lg.debug("DataModule.deleteChannel() begin");
			channels.remove(channel);
			if (!isChangedAllChannelsTI()) {
				setChanged();
				notifyObservers(channel);
			}
			lg.debug("Channel removed: " + channel.toString());
		}
	}

	/**
	 * Delete list of traces from internal data representation
	 * 
	 * @param toDelete
	 *            list of traces to delete
	 */
	public void deleteChannels(List<PlotDataProvider> toDelete) {
		synchronized (channels) {
			channels.removeAll(toDelete);
			if (!isChangedAllChannelsTI()) {
				setChanged();
				notifyObservers(toDelete);
			}
			lg.debug("Channels removed: list");
		}
	}

	/**
	 * If we still have not trace with given SNCL, add it, if we have it, find
	 * it
	 * 
	 * @param channelName
	 *            name of channel
	 * @param station
	 *            station
	 * @param networkName
	 *            name of network
	 * @param locationName
	 *            name of location
	 * @return initialized plot data provider for this trace
	 */
	public PlotDataProvider getOrAddChannel(String channelName,
			Station station, String networkName, String locationName) {
		// lg.debug("DataModule.getOrAddChannel() begin");
		PlotDataProvider channel = channelFactory.getChannel(
				channelName.trim(), station, networkName.trim(), locationName
						.trim());
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
	 * @param channelName
	 *            name of channel
	 * @param station
	 *            station
	 * @param networkName
	 *            name of network
	 * @param locationName
	 *            name of location
	 * @return plot data provider for this trace, or null if not found
	 */
	public synchronized PlotDataProvider getChannel(String channelName,
			Station station, String networkName, String locationName) {
		// lg.debug("DataModule.getChannel() begin");
		PlotDataProvider channel = channelFactory.getChannel(
				channelName.trim(), station, networkName.trim(), locationName
						.trim());
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
			return from;
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
			return to;
		}
	}

	/**
	 * Gets traces list for next window, see
	 * {@link DataModule#getWindowSize(boolean)}
	 * 
	 * @return list of traces for next display window
	 */
	public List<PlotDataProvider> getNextCnannelSet() throws TraceViewException {
		synchronized (channels) {
			lg.debug("DataModule.getNextCnannelSet() begin");
			int newWindowSize = getWindowSize(true);
			if ((newWindowSize != 0)
					&& ((markerPosition + newWindowSize) <= channels.size())) {
				from = markerPosition;
				to = Math.min(markerPosition + newWindowSize, channels.size());
				markerPosition = markerPosition + newWindowSize;
				windowSize = newWindowSize;
				lg.debug("DataModule.getNextCnannelSet() end: from " + from
						+ ", to " + to);
				return channels.subList(from, to);
			} else {
				throw new TraceViewException("This is the last set");
			}
		}
	}

	/**
	 * Gets traces list for previous window, see
	 * {@link DataModule#getWindowSize(boolean)}
	 * 
	 * @return list of traces for previous display window
	 */
	public List<PlotDataProvider> getPreviousCnannelSet()
			throws TraceViewException {
		synchronized (channels) {
			lg.debug("DataModule.getPreviousCnannelSet() begin");
			int newWindowSize = getWindowSize(false);
			if ((newWindowSize != 0) && (markerPosition > 1)) {
				markerPosition = markerPosition - windowSize - newWindowSize;
				from = markerPosition;
				to = Math.min(markerPosition + newWindowSize, channels.size());
				windowSize = 0;
				lg.debug("DataModule.getPreviousCnannelSet() end: from " + from
						+ ", to " + to);
				return channels.subList(from, to);
			} else {
				throw new TraceViewException("This is the first set");
			}
		}
	}

	/**
	 * @param ws
	 * @return flag if we have previous window (if no, this one is the first)
	 */
	public boolean hasPreviousChannelSet(int ws) {
		return markerPosition > 1;
	}

	/**
	 * <p>
	 * We have list of trace, but it can be rather long and we can't display all
	 * traces in one screen. So we should have some subset, or window, of traces
	 * list to show.
	 * </p>
	 * <p>
	 * This function compute this window boundaries for next screen, based on
	 * given options: screen counting units, quantity of units, sorting etc
	 * <p>
	 * 
	 * @param isForward
	 *            flag if we go foreward/backward along traces list
	 * @return window size in channels.
	 */
	private int getWindowSize(boolean isForward) {
		Configuration.PanelCountUnit unit = TraceView.getConfiguration().getPanelCountUnit();
		int unitsInFrame = TraceView.getConfiguration().getUnitsInFrame();
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
	 * Fill up stations from station file
	 */
	protected void loadStations() {
		LineNumberReader r = null;
		try {
			r = new LineNumberReader(new FileReader(TraceView.getConfiguration().getStationInfoFileName()));
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
						lg.debug("Station loaded: name " + name + ", network "
								+ network + ", longName " + longName
								+ ", startDate " + startDate + ", endDate "
								+ endDate + ", latitude " + latitude
								+ ", longitude " + longitude + ", elevation "
								+ elevation + ", depth " + depth);
					}

				}
			}
		} catch (FileNotFoundException e) {
			lg.error("Can't open station file: " + e);

		} catch (IOException e) {
			lg.error("Error during reading station file: " + e);
		} finally {
			try {
				r.close();
			} catch (IOException e) {

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
		List<String> respFiles = new ArrayList<String>();
		addRespFiles(TraceView.getConfiguration().getConfigFileDir(), network, station, location, channel, respFiles);
		if (respFiles.size() > 0)
			return respFiles.get(0);
		addRespFiles("./", network, station, location, channel, respFiles);
		if (respFiles.size() > 0)
			return respFiles.get(0);
		addRespFiles(TraceView.getConfiguration().getResponsePath(), network, station, location, channel, respFiles);
		return respFiles.size() == 0 ? null : respFiles.get(0);
	}

	public static List<String> getAllResponseFiles() throws TraceViewException {
		List<String> respFiles = new ArrayList<String>();
		addRespFiles(TraceView.getConfiguration().getConfigFileDir(), respFiles);
		addRespFiles("./", respFiles);
		addRespFiles(TraceView.getConfiguration().getResponsePath(), respFiles);
		return respFiles;
	}

	private static void addRespFiles(String dirname, String network,
			String station, String location, String channel,
			List<String> whereToAdd) throws TraceViewException {
		File f = new File(dirname);
		if (f.isDirectory()) {
			File[] dir = f.listFiles();
			if (dir.length > 0) {
				for (int i = 0; i < dir.length; i++) {
					if (!dir[i].isDirectory()
							&& dir[i].getName().matches(
									"^RESP\\." + network + "\\." + station
											+ "\\." + location + "\\."
											+ channel + "$")) {
						String absPath = dir[i].getAbsolutePath();
						// if (!whereToAdd.contains(absPath)) {
						whereToAdd.add(absPath);
						// ! System.out.println("Response file added: " +
						// dir[i].getAbsolutePath());
						// }
					}
				}
			}
		} else {
			throw new TraceViewException("Loading responses from " + dirname
					+ ": is not directory");
		}
	}

	private static void addRespFiles(String dirname, List<String> whereToAdd)
			throws TraceViewException {
		addRespFiles(dirname, ".*", ".*", ".*", ".*", whereToAdd);
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
	 * @param channel
	 *            name of channel
	 * @param station
	 *            station
	 * @param network
	 *            name of network
	 * @param location
	 *            name of location
	 * @return response class
	 */
	public Response getResponse(String network, String station,	String location, String channel) {
		Response resp = new Response(network, station, location, channel, null,	null);

		// try to load from files
		try {
			String respFile = getResponseFile(network, station, location, channel);
			if (respFile != null) {
				File f = new File(respFile);
				resp = Response.getResponse(f);
				if (resp != null) {
					System.out.println("Response loaded from file: " + respFile);
					return resp;
				}
			}
		} catch (TraceViewException e) {
			lg.error(e);
		}
		// try to load from network
		/*
		 * ByteArrayOutputStream stream = new ByteArrayOutputStream(); boolean
		 * multiOutFlag = false; // true for multi-output boolean headerFlag =
		 * false; // true for header in output file boolean verboseFlag = true;
		 * // true for verbose output boolean debugFlag = true; // true for
		 * debug messages boolean logSpacingFlag = false; // non-logarithmic
		 * spacing // load properites object with system properties: if
		 * (propsObj == null) { loadProperties(); } File outputDirectory = null;
		 * // output directory String[] staArr =
		 * getArray(getStation().getName()); String[] chaArr =
		 * getArray(getChannelName()); String[] netArr =
		 * getArray(getNetworkName()); String[] siteArr =
		 * getArray(getLocationName()); double[] freqArr = null; RespNetProc
		 * respNetProcObj = null; try { // generate array of frequency values:
		 * if ((freqArr = RespUtils.generateFreqArray(1, 10, 1000,
		 * logSpacingFlag)) == null) { throw new
		 * Exception("Error generating frequency array"); } respNetProcObj = new
		 * RespNetProc(propsObj, multiOutFlag, headerFlag, debugFlag,
		 * outputDirectory); if (respNetProcObj.getErrorFlag()) { throw new
		 * Exception(respNetProcObj.getErrorMessage()); } CallbackProcWrite
		 * callback = new CallbackProcWrite(OutputGenerator.DEFAULT_UNIT_CONV,
		 * freqArr, logSpacingFlag, verboseFlag, 0, 0, // all // stage //
		 * sequence // numbers // are // used 0, // response output type false,
		 * // file output new PrintStream(stream));
		 * callback.setRespProcObj(respNetProcObj); // set object to use // find
		 * responses if (!respNetProcObj.findNetResponses(staArr, chaArr,
		 * netArr, siteArr, getTimeRange().getStartTime(),
		 * getTimeRange().getEndTime(), verboseFlag, callback)) { throw new
		 * Exception(respNetProcObj.getErrorMessage()); } } catch (Exception e)
		 * { lg.warn("Can't retrieve response for " + toString() + " - " + e); }
		 * finally { respNetProcObj.destroyORB(); // deallocate ORB resources }
		 */
		return null;

	}

	/**
	 * Gets response for given SNCL, and cashes it in memory
	 * 
	 * @param channel
	 *            name of channel
	 * @param station
	 *            station
	 * @param network
	 *            name of network
	 * @param location
	 *            name of location
	 * @return response class
	 */
	public Response getResponseCashed(String network, String station, String location, String channel) {
		Response resp = new Response(network, station, location, channel, null,	null);
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
    /**
        System.out.format("== DataModule: Number of ISources=[%d]\n", dataSources.size() );
        for (ISource dataSource : dataSources) {
            System.out.format("==   [ISource: %s]\n", dataSource.getName() );
        }
    **/
        System.out.format("== DataModule: Number of channels(=PDP's) attached =[%d]:\n", channels.size());
        for (PlotDataProvider channel : channels) {
            System.out.format("   [PDP: %s] [nsegs=%d] [isLoadingStarted=%s] [isLoaded=%s]\n", 
                channel.toString(), channel.getSegmentCount(), channel.isLoadingStarted(), channel.isLoaded() ); 
            List<Segment> segs = channel.getRawData();
            //System.out.format("     [rdp#][chanSerial#]:Segment\n");
            for (Segment seg : segs) {
                System.out.format("         [%d][%d]:%s [Source:%s]\n", seg.getSourceSerialNumber(), seg.getChannelSerialNumber(),
                    seg.toString(), seg.getDataSource().getName() );
                System.out.println();
            }
        }
    }

}
