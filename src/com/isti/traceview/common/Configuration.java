package com.isti.traceview.common;

import java.io.File;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.gui.ColorModeBySegment;
import com.isti.traceview.gui.IColorModeState;
import com.isti.traceview.gui.IScaleModeState;
import com.isti.traceview.gui.ScaleModeAuto;
import com.isti.traceview.gui.ScaleModeCom;
import com.isti.traceview.gui.ScaleModeXhair;

import edu.iris.dmc.seedcodec.B1000Types;

/**
 * <p>
 * This class holds configuration data. Concrete realizations can add parameters and should define
 * method of initialization (reading configuration file, defaults, by command line options etc)
 * </p>
 * <p>
 * Extends {@link Observable} interface, i.e this class can report to listeners about configuration
 * changes
 * </p>
 * 
 * @author Max Kokoulin
 */
public class Configuration extends Observable {
	private static final Logger logger = Logger.getLogger(Configuration.class);

	private String default_pattern_html = "<html><head><title>HTML report</title></head><body><h1>HTML report</h1> </body></html>";
	protected static String listSeparator = ",";
	
	public Configuration() throws TraceViewException {
		setPanelCountUnit(PanelCountUnit.TRACE);
		setUnitsInFrame(20);
		setPanelOrder(ChannelSortType.TRACENAME);
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
	 * Enumeration for channel sort type. Note that not all combinations of show units and sorting
	 * options are permitted. For example, we can't show stations and have a list sorted by channels
	 */
	public enum ChannelSortType {
		/**
		 * Trace name is what you see on a plot, i.e network/station/location/channel. See
		 * {@link com.isti.traceview.data.NameComparator} for details
		 */
		TRACENAME,

		/**
		 * Network - station - sample rate - location code -channel type order
		 */
		NETWORK_STATION_SAMPLERATE,

		/**
		 * Really Channel - network - station - location order See {@link com.isti.traceview.data.ChannelComparator}
		 * for details
		 */
		CHANNEL,

		/**
		 * Channel type is last character of channel name. Channel type - channel - network -
		 * station order. See {@link com.isti.traceview.data.ChannelTypeComparator} for details
		 */
		CHANNEL_TYPE,

		/**
		 * Sorting by channel's events See {@link com.isti.traceview.data.EventComparator} for details
		 */
		EVENT
	}

	/**
	 * Name of configuration file
	 */
	public static String confFileName = "config.xml";

	/**
	 * Wildcarded mask of datafiles to search on startup
	 * 
	 * @uml.property name="dataPath"
	 */
	private String dataPath = "";

	/**
	 * @uml.property name="panelCountUnit"
	 */
	private PanelCountUnit panelCountUnit;

	/**
	 * Quantity of visible units on the screen, see {@link PanelCountUnit}. Correspond -f command
	 * line option.
	 * 
	 * @uml.property name="unitsInFrame"
	 */
	private int unitsInFrame;

	/**
	 * Order to sort traces to show, see <code>ChannelSortType<code> for options list.
	 * Correspond -o command line option.
	 * 
	 * @uml.property name="panelOrder
	 */
	private ChannelSortType panelOrder;

	/**
	 * Flag to indicate if enabled mode for placing all locations for given channel in one graph
	 * panel
	 */
	private boolean merge_locations = false;

	/**
	 * Location of temporary data storage
	 * 
	 * @uml.property name="dataTempPath"
	 */
	private String dataTempPath = "";

	/**
	 * Full pathname for stations definition file
	 * 
	 * @uml.property name="stationInfoFileName"
	 */
	private String stationInfoFileName = "";

	/**
	 * Flag if we expect several channels in raw data provider.
	 */
	private boolean allowMultiplexedData = false;

	/**
	 * Location of responses storage.
	 * 
	 * @uml.property name="responsePath"
	 */
	private String responsePath;

	/**
	 * Current scale mode
	 */
	private IScaleModeState scaleMode = new ScaleModeAuto();

	/**
	 * flag if we use color to draw graphs
	 */
	private IColorModeState colorModeState = new ColorModeBySegment();

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
	 * Getter of the property <tt>dataPath</tt>
	 * 
	 * @return wildcarded mask of datafiles to search on startup.
	 * @uml.property name="dataPath"
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
	 * @uml.property name="dataPath"
	 */
	public void setDataPath(String dataPath) {
		logger.debug("== dataPath: " + dataPath);
		this.dataPath = dataPath;
	}

	/**
	 * Getter of the property <tt>dataTempPath</tt>
	 * 
	 * @return location of temporary data storage.
	 * @uml.property name="dataTempPath"
	 */
	public String getDataTempPath() {
		return dataTempPath.replace("." + File.separator, getConfigFileDir());
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
	 * @uml.property name="allowMultiplexedData"
	 */
	public boolean isAllowMultiplexedData() {
		return allowMultiplexedData;
	}

	/**
	 * Getter of the property <tt>stationInfoFileName</tt>
	 * 
	 * @return full pathname for stations definition file.
	 * @uml.property name="stationInfoFileName"
	 */
	public String getStationInfoFileName() {
		return stationInfoFileName.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of the property <tt>stationInfoFileName</tt>
	 * 
	 * @param stationInfoFileName
	 *            The stationInfoFileName to set.
	 * @uml.property name="stationInfoFileName"
	 */
	public void setStationInfoFileName(String stationInfoFileName) {
		this.stationInfoFileName = stationInfoFileName;
	}

	/**
	 * Getter of the property <tt>unitsInFrame</tt>
	 * 
	 * @return quantity of visible units on the screen, see {@link PanelCountUnit}. Correspond -f
	 *         command line option.
	 * @uml.property name="unitsInFrame"
	 */
	public int getUnitsInFrame() {
		return unitsInFrame;
	}

	/**
	 * Setter of the property <tt>unitsInFrame</tt>
	 * 
	 * @param unitsInFrame
	 *            The unitsInFrame to set.
	 * @uml.property name="unitsInFrame"
	 */
	public void setUnitsInFrame(int unitsInFrame) {
		this.unitsInFrame = unitsInFrame;
	}

	/**
	 * Getter of the property <tt>panelCountUnit</tt>
	 * 
	 * @return current panel count unit,
	 * @see PanelCountUnit to reference.
	 * @uml.property name="panelCountUnit"
	 */
	public PanelCountUnit getPanelCountUnit() {
		return panelCountUnit;
	}

	/**
	 * Setter of the property <tt>panelCountUnit</tt>
	 * 
	 * @param panelCountUnit
	 *            The panelCountUnit to set.
	 * @uml.property name="panelCountUnit"
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
	 * Getter of the property <tt>panelOrder</tt>
	 * 
	 * @return order to sort traces to show, see <code>ChannelSortType<code> for options list.
	 * @uml.property name="panelOrder"
	 */
	public ChannelSortType getPanelOrder() {
		return panelOrder;
	}

	/**
	 * Setter of the property <tt>panelOrder</tt>
	 * 
	 * @param po
	 *            The panelOrder to set.
	 * @uml.property name="panelOrder"
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
		this.scaleMode = sm;
		setChanged();
		notifyObservers(sm);
	}

	/**
	 * Setter of scale mode
	 * 
	 * @param str
	 *            scale mode to set - as string
	 */
	public void setScaleMode(String str) {
		if (str != null) {
			if (str.equals("AUTO")) {
				this.scaleMode = new ScaleModeAuto();
			} else if (str.equals("COM")) {
				this.scaleMode = new ScaleModeCom();
			} else if (str.equals("XHAIR")) {
				this.scaleMode = new ScaleModeXhair();
			}
			setChanged();
			notifyObservers(this.scaleMode);
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
	 * @uml.property name="responsePath"
	 */
	public String getResponsePath() {
		return responsePath.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of the property <tt>responsePath</tt>
	 * 
	 * @param responsePath
	 *            The ResponsePath to set.
	 * @uml.property name="earthquakeDirectory"
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
		if (defaultCompressionStr.equals("ASCII")) {
			setDefaultCompression(B1000Types.ASCII);
		} else if (defaultCompressionStr.equals("SHORT")) {
			setDefaultCompression(B1000Types.SHORT);
		} else if (defaultCompressionStr.equals("INT24")) {
			setDefaultCompression(B1000Types.INT24);
		} else if (defaultCompressionStr.equals("INT32")) {
			setDefaultCompression(B1000Types.INTEGER);
		} else if (defaultCompressionStr.equals("FLOAT")) {
			setDefaultCompression(B1000Types.FLOAT);
		} else if (defaultCompressionStr.equals("DOUBLE")) {
			setDefaultCompression(B1000Types.DOUBLE);
		} else if (defaultCompressionStr.equals("STEIM1")) {
			setDefaultCompression(B1000Types.STEIM1);
		} else if (defaultCompressionStr.equals("STEIM2")) {
			setDefaultCompression(B1000Types.STEIM2);
		} else if (defaultCompressionStr.equals("CDSN")) {
			setDefaultCompression(B1000Types.CDSN);
		} else if (defaultCompressionStr.equals("RSTN")) {
			setDefaultCompression(B1000Types.CDSN);
		} else if (defaultCompressionStr.equals("DWW")) {
			setDefaultCompression(B1000Types.DWWSSN);
		} else if (defaultCompressionStr.equals("SRO")) {
			setDefaultCompression(B1000Types.SRO);
		} else if (defaultCompressionStr.equals("ASRO")) {
			setDefaultCompression(B1000Types.SRO);
		} else if (defaultCompressionStr.equals("HGLP")) {
			setDefaultCompression(B1000Types.SRO);
		} else {
			throw new TraceViewException("Unsupported compression type: '" + defaultCompressionStr + "'");
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
			filterStation = new HashSet<String>();
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
			filterNetwork = new HashSet<String>();
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
			filterChannel = new HashSet<String>();
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
			filterLocation = new HashSet<String>();
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
		String ret = confFile.getAbsolutePath().substring(0, confFile.getAbsolutePath().lastIndexOf(confFile.getName()));
		logger.debug("== fileDir: " + ret);
		return ret;
	}
}
