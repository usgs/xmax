package com.isti.traceview.common;

import static edu.sc.seis.seisFile.fdsnws.AbstractQueryParams.DEFAULT_HOST;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.gui.ColorModeByGap;
import com.isti.traceview.gui.IColorModeState;
import com.isti.traceview.gui.IScaleModeState;
import com.isti.traceview.gui.ScaleModeAuto;
import com.isti.traceview.gui.ScaleModeCom;
import com.isti.traceview.gui.ScaleModeXhair;
import edu.iris.dmc.seedcodec.B1000Types;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * <p>
 * This class holds configuration data. Concrete realizations can add parameters and should define
 * method of initialization (reading configuration file, defaults, by command line options etc)
 * </p>
 * <p>
 * </p>
 * 
 * @author Max Kokoulin
 */
public class Configuration {
	private static final Logger logger = Logger.getLogger(Configuration.class);

	private String default_pattern_html = "<html><head><title>HTML report</title></head><body><h1>HTML report</h1> </body></html>";
	protected static String listSeparator = ",";

	private PropertyChangeSupport listenerHelper;

	public Configuration() throws TraceViewException {
		setPanelCountUnit(PanelCountUnit.TRACE);
		setUnitsInFrame(20);
		setPanelOrder(ChannelSortType.TRACENAME);
		listenerHelper = new PropertyChangeSupport(this);
	}

	/**
	 * Enumeration for panel count units, we can define how many of this units we want to see on
	 * graph panel
	 */
	public enum PanelCountUnit {
		/**
		 * unit is one trace
		 */
		TRACE,

		/**
		 * unit is one station
		 */
		STATION,

		/**
		 * unit is one channel, with all locations
		 */
		CHANNEL,

		/**
		 * Channel type is last character of channel.
		 */
		CHANNEL_TYPE,

		/**
		 * All available data in one screen
		 */
		ALL
	}

	/**
	 * Enumeration for channel sort type. Note that not all combinations of show
	 * units and sorting options are permitted. For example, we can't show
	 * stations and have a list sorted by channels
	 */
	public enum ChannelSortType {
		/**
		 * Trace name is what you see on a plot, i.e
		 * network/station/location/channel. See
		 * {@link com.isti.traceview.data.Channel.NameComparator} for details
		 */
		TRACENAME,

		/**
		 * Network - station - sample rate - location code -channel type order
		 */
		NETWORK_STATION_SAMPLERATE,

		/**
		 * Really Channel - network - station - location order See
		 * {@link com.isti.traceview.data.Channel.ChannelComparator} for details
		 */
		CHANNEL,

		/**
		 * Channel type is last character of channel name. Channel type -
		 * channel - network - station order.
		 * @see com.isti.traceview.data.Channel.ChannelTypeComparator
		 */
		CHANNEL_TYPE
	}

	/**
	 * Name of configuration file
	 */
	public static String confFileName = "config.xml";

	/**
	 * Wildcarded mask of datafiles to search on startup
	 */
	private String dataPath = "";

	/**
	 * Folder in which stationXML files of format NET.STA.LOC.CHA.xml can be found
	 */
	private String stationXMLPath = null;
	private String dataServiceProtocol = "http";
	private String dataServiceURL = DEFAULT_HOST;
	private String dataServicePath = "fdsnws";
	private String metadataServicePath = "/fdsnws/station/1/query";
	private int dataServicePort = 80;

	/**
	 * True when station XML should be loaded with priority over response files
	 */
	private boolean stationXMLPreferred = false;

	private PanelCountUnit panelCountUnit;

	// parameters for default values for filter cut bands in
	private double lowPassCutoff = 0.05;
	private double highPassCutoff = 1.0;
	private double bandLowCutoff = 0.1;
	private double bandHighCutoff = 0.5;

	/**
	 * Quantity of visible units on the screen, see {@link PanelCountUnit}. Correspond -f command
	 * line option.
	 */
	private int unitsInFrame;

	/**
	 * Order to sort traces to show, see {@link ChannelSortType} for options list.
	 * Correspond -o command line option.
	 */
	private ChannelSortType panelOrder;

	/**
	 * Flag to indicate if enabled mode for placing all locations for given channel in one graph
	 * panel
	 */
	private boolean merge_locations = false;

	/**
	 * Location of temporary data storage
	 */
	private String dataTempPath = "";

	/**
	 * Full pathname for stations definition file
	 */
	private String stationInfoFileName = "";

	/**
	 * Flag if we expect several channels in raw data provider.
	 */
	private boolean allowMultiplexedData = false;

	/**
	 * Location of responses storage.
	 */
	private String responsePath;

	/**
	 * Current scale mode
	 */
	private IScaleModeState scaleMode = new ScaleModeAuto();

	/**
	 * flag if we use color to draw graphs
	 */
	private IColorModeState colorModeState = new ColorModeByGap();

	/**
	 * flag if we show big crosshair cursor or use ordinary cursor
	 */
	private boolean showBigCursor = false;

	/**
	 * this values we use in mseed decompression the case of absence of blockette 1000
	 */
	private int defaultCompression = B1000Types.STEIM1;

	/**
	 * this values we use in mseed decompression the case of absence of blockette 1000
	 */
	private int defaultBlockLength = 4096;

	private boolean useTempData = false;

	private boolean useDataPath = false;

    /**
    * If dumpData = true then we are in -T mode
    **/
	private boolean dumpData = false;

	private Set<String> filterStation = null;

	private Set<String> filterNetwork = null;

	private Set<String> filterChannel = null;

	private Set<String> filterLocation = null;

	/**
	 * Sets whether to use station XML or RESP metadata file formats
	 * @param preferred True to default to station XML files
	 */
	public void setStationXMLPreferred(boolean preferred) {
		this.stationXMLPreferred = preferred;
	}

	/**
	 * Get current state of preference betweeen RESP and stationXML.
	 * If this is set to true and FDSN metadata is enabled, lacking stationXML for a given trace
	 * will cause a call to the FDSN metadata service to run to get the relevant data.
	 * @return true if preferred to use station XML metadata if it exists
	 */
	public boolean stationXMLPreferred() {
		return stationXMLPreferred;
	}

	/**
	 * Getter of the property <tt>dataPath</tt>
	 * 
	 * @return wildcarded mask of datafiles to search on startup.
	 */
	public String getDataPath() {
        // MTH: The line below will take xmax -d '../xs0/seed/..' and turn it into path="./Users/mth/mth/../xs0/seed/.." !
		//String ret = dataPath.replace("." + File.separator, getConfigFileDir());
		//lg.debug("Configuration.getDataPath(): " + ret);
		//return ret;
		return dataPath;
	}

	/**
	 * Setter of the property <tt>dataPath</tt>
	 * 
	 * @param dataPath
	 *            The dataPath to set.
	 */
	public void setDataPath(String dataPath) {
		logger.debug("== dataPath: " + dataPath);
		this.dataPath = dataPath;
	}

	/**
	 * Getter of the property <tt>dataTempPath</tt>
	 * 
	 * @return location of temporary data storage.
	 */
	public String getDataTempPath() {
		return dataTempPath.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of data service protocol -- connection protocol for FDSN data service.
	 * This is expected to be a value like http or https
	 * @param dataServiceProtocol Protocol for FDSN data service connections
	 */
	public void setDataServiceProtocol(String dataServiceProtocol) {
		this.dataServiceProtocol = dataServiceProtocol;
	}

	/**
	 * Gets the data service connection protocol
	 * @return FDSN connection protocol, i.e., http or https
	 */
	public String getDataServiceProtocol() {
		return dataServiceProtocol;
	}

	/**
	 * Expected location to find stationXML data if existed and preferred metadata source
	 * @param stationXMLPath Path to search for station XML metadata from
	 */
	public void setStationXMLPath(String stationXMLPath) {
		this.stationXMLPath = stationXMLPath;
	}

	/**
	 * Returns location to search for stationXML files from
	 * @return String representing stationXML data root path
	 */
	public String getStationXMLPath() {
		if (stationXMLPath == null) {
			return null;
		}
		return stationXMLPath.replace("." + File.separator, getConfigFileDir());

	}

	/**
	 * Set the domain to use in calling FDSN data/metadata requests.
	 * This is a string like "service.iris.edu"
	 * @param dataServiceURL Base URL for FDSN requests
	 */
	public void setDataServiceHost(String dataServiceURL) {
		this.dataServiceURL = dataServiceURL;
	}

	/**
	 * Get the domain for FDSN data service requests
	 * @return Base URL for FDSN requests
	 */
	public String getDataServiceHost() {
		return dataServiceURL;
	}

	/**
	 * Set the port to use for FDSN services (default value is 80)
	 * @param dataServicePort Port to connect to for FDSN service requests
	 */
	public void setDataServicePort(int dataServicePort) {
		this.dataServicePort = dataServicePort;
	}

	/**
	 * Get the port used for FDSN services
	 * @return port value for FDSN connections
	 */
	public int getDataServicePort() {
		return dataServicePort;
	}

	/**
	 * Setter of the property <tt>dataTempPath</tt>
	 * 
	 * @param dataTempPath
	 *            location of temporary data storage
	 */
	public void setDataTempPath(String dataTempPath) {
		this.dataTempPath = dataTempPath;
	}

	/**
	 * Set path to use for stationXML queries from FDSN metadata services
	 * Full URL will be protocol + "://" + dataServiceURL + this value
	 * @param metadataServicePath subdirectory (i.e., "/metadatairis/fdsnws/station/1/query")
	 */
	public void setMetadataServicePath(String metadataServicePath) {
		this.metadataServicePath = metadataServicePath;
	}

	/**
	 * Get subpath of metadata services for constructing metadata requests
	 * Full URL will be dataServiceProtocol + "://" + dataServiceURL + this value
	 * @return subpath of metadata services (i.e., "/metadatairis/fdsnws/station/1/query")
	 */
	public String getMetadataServicePath() {
		return metadataServicePath;
	}

	/**
	 * Set path for data services to be used in FDSN data queries
	 * FDSN data queries will use protocol, url, port, and this value
	 * @param dataServicePath path of FDSN data query (i.e., "metadatairis/fdsnws")
	 */
	public void setDataServicePath(String dataServicePath) {
		this.dataServicePath = dataServicePath;
	}

	/**
	 * Get path for data services to be used in FDSN data queries
	 * FDSN data queries will use protocol, url, port, and this value
	 * @return path under domain for FDSN data queries
	 */
	public String getDataServicePath() {
		return dataServicePath;
	}

	/**
	 * Setter of the property <tt>allowMultiplexedData</tt>
	 * 
	 * @param allowMultiplexedData
	 *            flag if we expect several channels in raw data provider.
	 */
	public void setAllowMultiplexedData(boolean allowMultiplexedData) {
		this.allowMultiplexedData = allowMultiplexedData;
	}

	/**
	 * Getter of the property <tt>allowMultiplexedData</tt>
	 * 
	 * @return flag if we expect several channels in raw data provider.
	 */
	public boolean isAllowMultiplexedData() {
		return allowMultiplexedData;
	}

	/**
	 * Getter of the property <tt>stationInfoFileName</tt>
	 * 
	 * @return full pathname for stations definition file.
	 */
	public String getStationInfoFileName() {
		return stationInfoFileName.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of the property <tt>stationInfoFileName</tt>
	 * 
	 * @param stationInfoFileName
	 *            The stationInfoFileName to set.
	 */
	public void setStationInfoFileName(String stationInfoFileName) {
		if (stationInfoFileName == null) {
			stationInfoFileName = "";
		}
		this.stationInfoFileName = stationInfoFileName;
	}

	/**
	 * Getter of the property <tt>unitsInFrame</tt>
	 * 
	 * @return quantity of visible units on the screen, see {@link PanelCountUnit}. Correspond -f
	 *         command line option.
	 */
	public int getUnitsInFrame() {
		return unitsInFrame;
	}

	/**
	 * Setter of the property <tt>unitsInFrame</tt>
	 * 
	 * @param unitsInFrame
	 *            The unitsInFrame to set.
	 */
	public void setUnitsInFrame(int unitsInFrame) {
		this.unitsInFrame = unitsInFrame;
	}

	/**
	 * Getter of the property <tt>panelCountUnit</tt>
	 * 
	 * @return current panel count unit,
	 * @see PanelCountUnit to reference.
	 */
	public PanelCountUnit getPanelCountUnit() {
		return panelCountUnit;
	}

	/**
	 * Setter of the property <tt>panelCountUnit</tt>
	 * 
	 * @param panelCountUnit
	 *            The panelCountUnit to set.
	 */
	public void setPanelCountUnit(PanelCountUnit panelCountUnit) {
		this.panelCountUnit = panelCountUnit;

		try {
			if (panelCountUnit == PanelCountUnit.STATION) {
				setPanelOrder(ChannelSortType.NETWORK_STATION_SAMPLERATE);
			} else if (panelCountUnit == PanelCountUnit.CHANNEL_TYPE) {
				setPanelOrder(ChannelSortType.CHANNEL_TYPE);
			} else if (panelCountUnit == PanelCountUnit.CHANNEL) {
				setPanelOrder(ChannelSortType.CHANNEL);
			}
		} catch (TraceViewException e) {
			// do nothing, all should be correct
			logger.error("TraceViewException:", e);	
		}

	}

	/**
	 * If true, we will load channel with the same location code in the one graph panel
	 * 
	 * @return flag to indicate if enabled mode for placing all locations for given channel in one
	 *         graph panel
	 */
	public boolean getMergeLocations() {
		return merge_locations;
	}

	public void setMergeLocations(boolean merge) {
		this.merge_locations = merge;
	}

	/**
	 * Getter of the property panelOrder
	 * 
	 * @return order to sort traces to show, see {@link ChannelSortType} for options list.
	 */
	public ChannelSortType getPanelOrder() {
		return panelOrder;
	}

	/**
	 * Setter of the property <tt>panelOrder</tt>
	 * 
	 * @param po
	 *            The panelOrder to set.
	 */
	public void setPanelOrder(ChannelSortType po) throws TraceViewException {
		if (panelCountUnit == PanelCountUnit.STATION && !((po == ChannelSortType.TRACENAME) || (po == ChannelSortType.NETWORK_STATION_SAMPLERATE))) {
			throw new TraceViewException("Wrong display sorting option for display unit STATION");
		} else if (panelCountUnit == PanelCountUnit.CHANNEL_TYPE && (po != ChannelSortType.CHANNEL_TYPE)) {
			throw new TraceViewException("Wrong display sorting option for display unit CHANNEL_TYPE");
		} else if (panelCountUnit == PanelCountUnit.CHANNEL && (po != ChannelSortType.CHANNEL)) {
			throw new TraceViewException("Wrong display sorting option for display unit CHANNEL");
		}
		if (this.getMergeLocations() && po != ChannelSortType.CHANNEL) {
			throw new TraceViewException("Wrong sorting option for locations merge mode, should be CHANNEL");
		}
		this.panelOrder = po;
	}

	/*
	 * public Set<String> getStationNames() { return stations.keySet(); } public Set<String>
	 * getChannelNames(String stationName) { return stations.get(stationName).getChannelNames(); }
	 * public List<String> getSelectedChannelNames(String stationName) { return null; } public Date
	 * getChannelBegin(String channelName) { return null; } public Date getChannelEnd(String
	 * channelName) { return null; }
	 */

	/**
	 * @return current scale mode
	 */
	public IScaleModeState getScaleMode() {
		return scaleMode;
	}

	/**
	 * Setter of scale mode
	 * 
	 * @param sm
	 *            scale mode to set
	 */
	public void setScaleMode(IScaleModeState sm) {
		IScaleModeState oldScale = this.scaleMode;
		this.scaleMode = sm;
		listenerHelper.firePropertyChange("scale state", oldScale, sm);
	}

	/**
	 * Setter of scale mode
	 * 
	 * @param str
	 *            scale mode to set - as string
	 */
	public void setScaleMode(String str) {
		IScaleModeState oldScale = this.scaleMode;
		if (str != null) {
			switch (str) {
				case "AUTO":
					this.scaleMode = new ScaleModeAuto();
					break;
				case "COM":
					this.scaleMode = new ScaleModeCom();
					break;
				case "XHAIR":
					this.scaleMode = new ScaleModeXhair();
					break;
			}
			if (!oldScale.equals(this.scaleMode)) {
				listenerHelper.firePropertyChange("scale state", oldScale, this.scaleMode);
			}
		}
	}

	/**
	 * Getter of the property <tt>useColor</tt>
	 * 
	 * @return flag if we use color to draw graphs
	 */
	public IColorModeState getColorModeState() {
		return colorModeState;
	}

	public void setColorModeState(IColorModeState uc) {
		this.colorModeState = uc;

	}

	/**
	 * Getter of the property <tt>responsePath</tt>
	 * 
	 * @return location of responses storage.
	 */
	public String getResponsePath() {
		return responsePath.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of the property <tt>responsePath</tt>
	 * 
	 * @param responsePath
	 *            The ResponsePath to set.
	 */
	public void setResponsePath(String responsePath) {
		this.responsePath = responsePath;
	}

	/**
	 * Getter of the property <tt>showBigCursor</tt>
	 * 
	 * @return flag if we show big crosshair cursor or use ordinary cursor
	 */
	public boolean getShowBigCursor() {
		return showBigCursor;
	}

	public void setShowBigCursor(boolean sbc) {
		this.showBigCursor = sbc;
	}

	/**
	 * Setter of property defaultCompression.
	 * 
	 * @param defaultCompressionStr
	 *            string name of compression type
	 * @throws TraceViewException
	 *             in case of unsupported compression name
	 */
	public void setDefaultCompression(String defaultCompressionStr) throws TraceViewException {
		switch (defaultCompressionStr) {
			case "ASCII":
				setDefaultCompression(B1000Types.ASCII);
				break;
			case "SHORT":
				setDefaultCompression(B1000Types.SHORT);
				break;
			case "INT24":
				setDefaultCompression(B1000Types.INT24);
				break;
			case "INT32":
				setDefaultCompression(B1000Types.INTEGER);
				break;
			case "FLOAT":
				setDefaultCompression(B1000Types.FLOAT);
				break;
			case "DOUBLE":
				setDefaultCompression(B1000Types.DOUBLE);
				break;
			case "STEIM1":
				setDefaultCompression(B1000Types.STEIM1);
				break;
			case "STEIM2":
				setDefaultCompression(B1000Types.STEIM2);
				break;
			case "CDSN":
			case "RSTN":
				setDefaultCompression(B1000Types.CDSN);
				break;
			case "DWW":
				setDefaultCompression(B1000Types.DWWSSN);
				break;
			case "SRO":
			case "ASRO":
			case "HGLP":
				setDefaultCompression(B1000Types.SRO);
				break;
			default:
				throw new TraceViewException(
						"Unsupported compression type: '" + defaultCompressionStr + "'");
		}

	}

	/**
	 * Setter of property defaultCompression. For compression types list see {@link B1000Types}
	 */
	public void setDefaultCompression(int defaultCompression) {
		this.defaultCompression = defaultCompression;
	}

	/**
	 * For compression types list see {@link B1000Types}
	 * 
	 * @return compression type used in mseed decompression the case of absence of blockette 1000
	 */
	public int getDefaultCompression() {
		return defaultCompression;
	}

	public void setDefaultBlockLength(int defaultBlockLength) {
		this.defaultBlockLength = defaultBlockLength;
	}

	/**
	 * Getter of property defaultBlockLength.
	 * 
	 * @return block length used in mseed decompression the case of absence of blockette 1000
	 */
	public int getDefaultBlockLength() {
		return defaultBlockLength;
	}

	/**
	 * Getter of the property <tt>useTempData</tt>
	 * 
	 * @return Returns flag if we should load content of temporary storage
	 */
	public boolean getUseTempData() {
		return useTempData;
	}

	/**
	 * Setter of the property <tt>useTempData</tt>
	 * 
	 * @param useTempData
	 *            flag if we should load content of temporary storage
	 */
	public void setUseTempData(boolean useTempData) {
		this.useTempData = useTempData;
	}
    /**
    * MTH: Added useDataPath so we can know if user entered "-d dataPath"
    *      on the command line in.
    *      If -t was also entered, then we want to try to load data 
    *      from both locations (dataPath + TempData)
    */
	public boolean getUseDataPath() {
		return useDataPath;
	}
	public void setUseDataPath(boolean useDataPath) {
		this.useDataPath = useDataPath;
	}
	public boolean getDumpData() {
		return dumpData;
	}
	public void setDumpData(boolean dumpData) {
		this.dumpData = dumpData;
	}

	/**
	 * Setter of station filter
	 * 
	 * @param filtStr
	 *            comma-separated list of stations
	 */
	public void setFilterStation(String filtStr) {
		if (filtStr == null || filtStr.equals("")) {
			filterStation = null;
		} else {
			filterStation = new HashSet<>();
			fillFilter(filterStation, filtStr);
		}
	}

	/**
	 * Getter of station filter
	 * 
	 * @return set of station names
	 */
	public Set<String> getFilterStation() {
		return filterStation;
	}

	/**
	 * Setter of network filter
	 * 
	 * @param filtStr
	 *            comma-separated list of networks
	 */
	public void setFilterNetwork(String filtStr) {
		if (filtStr == null || filtStr.equals("")) {
			filterNetwork = null;
		} else {
			filterNetwork = new HashSet<>();
			fillFilter(filterNetwork, filtStr);
		}
	}

	/**
	 * Getter of network filter
	 * 
	 * @return set of network names
	 */
	public Set<String> getFilterNetwork() {
		return filterNetwork;
	}

	/**
	 * Setter of channel filter
	 * 
	 * @param filtStr
	 *            comma-separated list of channels
	 */
	public void setFilterChannel(String filtStr) {
		if (filtStr == null || filtStr.equals("")) {
			filterChannel = null;
		} else {
			filterChannel = new HashSet<>();
			fillFilter(filterChannel, filtStr);
		}
	}

	/**
	 * Getter of channel filter
	 * 
	 * @return set of channel names
	 */
	public Set<String> getFilterChannel() {
		return filterChannel;
	}

	/**
	 * Setter of location filter
	 * 
	 * @param filtStr
	 *            comma-separated list of locations
	 */
	public void setFilterLocation(String filtStr) {
		if (filtStr == null || filtStr.equals("")) {
			filterLocation = null;
		} else {
			filterLocation = new HashSet<>();
			fillFilter(filterLocation, filtStr);
		}
	}

	/**
	 * Getter of location filter
	 * 
	 * @return set of location names
	 */
	public Set<String> getFilterLocation() {
		return filterLocation;
	}

	private static void fillFilter(Set<String> filter, String filtStr) {
		StringTokenizer st = new StringTokenizer(filtStr, listSeparator);
		while (st.hasMoreTokens()) {
			filter.add(st.nextToken());
		}
	}

	public void setDefaultHTMLPattern(String pattern) {
		default_pattern_html = pattern;
	}

	/**
	 * @return html code which placed in the beginning of every printed report
	 */
	public String getDefaultHTMLPattern() {
		return default_pattern_html;
	}

	/**
	 * @return directory where current configuration file placed
	 */
	public String getConfigFileDir() {
		File confFile = new File(confFileName);
		String ret = confFile.getAbsolutePath().substring(0, confFile.getAbsolutePath().
				lastIndexOf(confFile.getName()));
		logger.debug("== fileDir: " + ret);
		return ret;
	}

	/**
	 * Return configuration-derived value for corner frequency in low-pass filter operations.
	 * This value is taken from field "Configuration.FreqLimits.LowPass" in config.xml
	 * Default value if not defined in config is 0.05 (Hz).
	 * @return value to apply to low-pass filter for corner frequency, in Hz
	 */
	public double getLowPassCutoff() {
		return lowPassCutoff;
	}

	/**
	 * Set the value for corner frequency in low-pass filter operations
	 * @param lowPassCutoff Frequency value to use as corner, in Hz
	 */
	public void setLowPassCutoff(double lowPassCutoff) {
		this.lowPassCutoff = lowPassCutoff;
	}

	/**
	 * Return configuration-derived value for corner frequency in high-pass filter operations.
	 * This value is taken from field "Configuration.FreqLimits.HighPass" in config.xml
	 * Default value if not defined in config is 1.0 (Hz).
	 * @return value to apply to high-pass filter for corner frequency, in Hz
	 */
	public double getHighPassCutoff() {
		return highPassCutoff;
	}

	/**
	 * Set the value for corner frequency in high-pass filter operations
	 * @param highPassCutoff Frequency value to use as corner, in Hz
	 */
	public void setHighPassCutoff(double highPassCutoff) {
		this.highPassCutoff = highPassCutoff;
	}

	/**
	 * Return configuration-derived value for low corner frequency in band-pass filter operations.
	 * This value is taken from field "Configuration.FreqLimits.BandPassLow" in config.xml and is
	 * designed to pair with "Configuration.FreqLimits.BandPassHigh" parameter.
	 * Default value if not defined in config is 0.1 (Hz).
	 * If both bandPassLow and bandPassHigh fields are defined, this is the lower of the two values.
	 * If this value was not defined in the configuration but BandPassHigh was using a value
	 * below 0.1, this will be the value assigned to BandPassHigh and BandPassHigh will be at 0.1
	 * @return value to apply to band-pass filter for lower corner frequency, in Hz
	 */
	public double getBandLowCutoff() {
		return bandLowCutoff;
	}

	/**
	 * Return configuration-derived value for high corner frequency in band-pass filter operations.
	 * This value is taken from field "Configuration.FreqLimits.BandPassHigh" in config.xml
	 * Default value if not defined in config is 0.5 (Hz).
	 * If both bandPassLow and bandPassHigh fields are defined, this is the higher of the two values.
	 * If this value was not defined in the configuration but BandPassLow was using a value
	 * above 0.5, this will be the value assigned to BandPassLow and BandPassLow will be at 0.5
	 * @return value to apply to band-pass filter for higher corner frequency, in Hz
	 */
	public double getBandHighCutoff() {
		return bandHighCutoff;
	}

	/**
	 * Return configuration-derived values for upper and lower corners for band-pass filter
	 * operations.
	 * These values are taken from fields "Configuration.FreqLimits.BandPassLow" and
	 * "Configuration.FreqLimits.BandPassHigh" with some caveats.
	 * In particular, values are only assigned if lowBand and highBand are different values.
	 * The values assigned will always be taken as the max and min values passed in,
	 * rather than which field is tagged as the lower and higher corner.
	 * @param lowBand low-frequency corner value to use, in Hz
	 * @param highBand high-frequency corner value to use, in Hz
	 */
	public void setBandPassCutoffs(double lowBand, double highBand) {
		if (lowBand != highBand) {
			bandLowCutoff = Math.min(lowBand, highBand);
			bandHighCutoff = Math.max(lowBand, highBand);
		}
	}
}
