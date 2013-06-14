package com.isti.xmax;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.gui.ColorModeBySegment;
import com.isti.xmax.XMAXException;

/**
 * <p>
 * This class holds configuration data. It reads configuration file and uses default values for
 * missed parameters.
 * </p>
 * <p>
 * It overwrites configuration parameters with command-line parameters.
 * </p>
 * <p>
 * Realize singleton pattern, i.e we can have only one configuration in the program.
 * </p>
 * 
 * @author Max Kokoulin
 */
public class XMAXconfiguration extends Configuration {

	private Map<String, String> userDirectories = null;

	/**
	 * Full pathname for quality control data file
	 * 
	 * @uml.property name="qCdataFileName"
	 */
	private String qCdataFileName = "";

	/**
	 * Location of xml picks database
	 * 
	 * @uml.property name="PickPath"
	 */
	private String pickPath = "";

	/**
	 * Wildcarded mask for earthquakes definition files
	 * 
	 * @uml.property name="earthquakeFileMask"
	 */
	private String earthquakeFileMask;

	/**
	 * @uml.property name="outputPath"
	 */
	private String outputPath;

	/**
	 * Time interval to load channels, correspond -b and -e command line options.
	 * 
	 * @uml.property name="ti"
	 */
	private TimeInterval ti;

	/**
	 * Major version number
	 * 
	 * @uml.property name="version_major"
	 */
	private int version_major;

	/**
	 * Minor version number
	 * 
	 * @uml.property name="version_minor"
	 */
	private int version_minor;

	private boolean showStatusBar = true;

	private boolean showCommandButtons = true;

	private boolean showCommandButtonsTop = false;

	int frameState = Frame.MAXIMIZED_BOTH;

	int[] framePos = new int[2];

	int[] frameSize = new int[2];

	private String logFile = null;

	private static XMAXconfiguration instance = null;

	private XMLConfiguration config = null;

	private static Logger lg = Logger.getLogger(Configuration.class);

	private XMAXconfiguration() throws ConfigurationException, TraceViewException, XMAXException {
		setDefaultHTMLPattern("<html><head><title>XMAX report</title></head><body><h1>XMAX report</h1> </body></html>");
		config = new XMLConfiguration(confFileName);
		userDirectories = new HashMap<String, String>();
		ti = null;
		try {
			setUseTempData(false);
			setLogFile(config.getString("Configuration.LogFile", "XMAX.log"));
			setPanelCountUnit(PanelCountUnit.values()[config.getInt("Configuration.PanelCountUnit", 0)]);
			setUnitsInFrame(config.getInt("Configuration.UnitsInFrame", 1));
			setMergeLocations(config.getBoolean("Configuration.MergeLocations", false));
			setPanelOrder(ChannelSortType.values()[config.getInt("Configuration.PanelOrder", 0)]);
			setDataPath(config.getString("Configuration.Data.DataMask", "!"));
			setDataTempPath(config.getString("Configuration.Data.TempPath"));
			setQCdataFileName(config.getString("Configuration.Data.QCdataFile", "qc.xml"));
			setPickPath(config.getString("Configuration.Data.PickPath", "./Picks"));
			setStationInfoFileName(config.getString("Configuration.Data.StationInfoFile"));
			setEarthquakeFileMask(config.getString("Configuration.Data.EventFileMask"));
			setResponsePath(config.getString("Configuration.Data.ResponsePath", "/Responses"));
			// setAllowMultiplexedData(config.getBoolean("Configuration.Data.AllowMultiplexedData"));
			setOutputPath(config.getString("Configuration.OutputPath"));
			String startTimeStr = config.getString("Configuration.StartTime");
			if (startTimeStr != null) {
				setStartTime(TimeInterval.parseDate(startTimeStr, TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE));
			}
			String endTimeStr = config.getString("Configuration.EndTime");
			if (endTimeStr != null) {
				setEndTime(TimeInterval.parseDate(endTimeStr, TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE));
			}
			version_major = config.getInt("Version.Major");
			version_minor = config.getInt("Version.Minor");
			setScaleMode(config.getString("Configuration.View.ScaleMode", "AUTO"));
			setColorModeState(config.getString("Configuration.View.ColorMode", "SEGMENT"), false);
			setShowBigCursor(config.getBoolean("Configuration.View.ShowBigCursor", false));
			setShowStatusBar(config.getBoolean("Configuration.View.ShowStatusBar", true));
			setShowCommandButtons(config.getBoolean("Configuration.View.ShowCommandButtons", true));
			setShowCommandButtonsTop(config.getBoolean("Configuration.View.ShowCommandButtonsTop", false));
			Iterator<String> it = config.getKeys("SessionData.UserDir");
			while (it.hasNext()) {
				String key = it.next();
				StringTokenizer st = new StringTokenizer(key, ".");
				if (st.countTokens() == 3) {
					userDirectories.put(key.substring(key.lastIndexOf('.') + 1), config.getString(key));
				}
			}
			String state = config.getString("SessionData.Frame.State", "MAXIMIZED_BOTH");
			if (state.equals("NORMAL")) {
				frameState = Frame.NORMAL;
			} else if (state.equals("ICONIFIED")) {
				frameState = Frame.ICONIFIED;
			} else if (state.equals("MAXIMIZED_HORIZ")) {
				frameState = Frame.MAXIMIZED_HORIZ;
			} else if (state.equals("MAXIMIZED_VERT")) {
				frameState = Frame.MAXIMIZED_VERT;
			} else if (state.equals("MAXIMIZED_BOTH")) {
				frameState = Frame.MAXIMIZED_BOTH;
			}
			framePos[0] = config.getInt("SessionData.Frame.PosX", 0);
			framePos[1] = config.getInt("SessionData.Frame.PosY", 0);
			frameSize[0] = config.getInt("SessionData.Frame.Width", 800);
			frameSize[1] = config.getInt("SessionData.Frame.Heigth", 600);
			setFilterStation(config.getString("Configuration.Filters.Station"));
			setFilterNetwork(config.getString("Configuration.Filters.Network"));
			setFilterChannel(config.getString("Configuration.Filters.Channel"));
			setFilterLocation(config.getString("Configuration.Filters.Location"));
		} catch (NoSuchElementException e) {
			// do nothing, use defaults
		}
	}

	static public XMAXconfiguration getInstance() {
		AbstractConfiguration.setDefaultListDelimiter(';');
		try {
			if (instance == null) {
				instance = new XMAXconfiguration();
			}
		} catch (ConfigurationException e) {
			lg.error(e);
			e.printStackTrace();
		} catch (XMAXException e) {
			lg.error(e);
			e.printStackTrace();
		} catch (TraceViewException e) {
			lg.error(e);
			e.printStackTrace();
		}
		return instance;
	}

	/**
	 * @return configuration file version
	 */
	String getVersion() {
		return version_major + "." + version_minor;
	}

	/**
	 * Getter of the property <tt>OutputPath</tt>, see also <OutputPath> tag in config.xml
	 * 
	 * @return Returns the directory to store generated files.
	 */
	public String getOutputPath() {
		return outputPath.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of the property <tt>outputPath</tt>, see also <OutputPath> tag in config.xml
	 * 
	 * @param outputPath
	 *            directory to store generated files.
	 */
	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	/**
	 * Getter of the property <tt>Time interval</tt>
	 * 
	 * @return time interval to set after initialization and for filtering of source data during
	 *         parsing.
	 */
	public TimeInterval getTimeInterval() {
		return ti;
	}
	
	public void setColorModeState(String colorMode, boolean needsave) throws XMAXException{
		if(colorMode.toUpperCase().equals("SEGMENT")){
			setColorModeState(new ColorModeBySegment());
		} else if(colorMode.toUpperCase().equals("GAP")){
			setColorModeState(new ColorModeBySegment());
		} else if(colorMode.toUpperCase().equals("BW")){
			setColorModeState(new ColorModeBySegment());
		} else {
			throw new XMAXException("Illegal color mode marker");
		}
		if(needsave)
		config.setProperty("Configuration.View.ColorMode", colorMode);
	}

	/**
	 * Setter of the property <tt>startTime</tt>
	 * 
	 * @param startTime
	 *            start time of time interval
	 * @see XMAXconfiguration#getTimeInterval()
	 */
	public void setStartTime(Date startTime) throws XMAXException {
		if (ti == null) {
			ti = new TimeInterval(startTime, new Date(startTime.getTime() + 24 * 60 * 60 * 1000));
		} else {
			if (startTime.getTime() < ti.getEnd()) {
				ti = new TimeInterval(startTime, ti.getEndTime());
			} else {
				throw new XMAXException("Start time (" + startTime + ") more then end time (" + ti.getEndTime() + ")");
			}
		}
	}

	/**
	 * Setter of the property <tt>endTime</tt>
	 * 
	 * @param endTime
	 *            start time of time interval
	 * @see XMAXconfiguration#getTimeInterval()
	 */
	public void setEndTime(Date endTime) throws XMAXException {
		if (ti == null) {
			ti = new TimeInterval(new Date(endTime.getTime() - 24 * 60 * 60 * 1000), endTime);
		} else {
			if (endTime.getTime() > ti.getStart()) {
				ti = new TimeInterval(ti.getStartTime(), endTime);
			} else {
				throw new XMAXException("End time (" + endTime + ") less then start time (" + ti.getStartTime() + ")");
			}
		}
	}

	/**
	 * Getter of the property <tt>qCdataFileName</tt>, see also <QCDataFile> tag in config.xml and
	 * -q command line option.
	 * 
	 * @return quality control data file name.
	 */
	public String getQCdataFileName() {
		return qCdataFileName.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of the property <tt>qCdataFileName</tt>, see also <QCDataFile> tag in config.xml and
	 * -q command line option.
	 * 
	 * @param qCdataFileName
	 *            quality control data file name.
	 */
	public void setQCdataFileName(String qCdataFileName) {
		this.qCdataFileName = qCdataFileName;
	}

	/**
	 * Getter of the property <tt>pickPath</tt>, see also <PickPath> tag in config.xml and -p
	 * command line option.
	 * 
	 * @return directory name to store picks database.
	 */
	public String getPickPath() {
		return pickPath.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Setter of the property <tt>pickPath</tt>, see also <PickPath> tag in config.xml and -p
	 * command line option.
	 * 
	 * @param pickPath
	 *            directory name to store picks database.
	 */
	public void setPickPath(String pickPath) {
		this.pickPath = pickPath;
	}

	/**
	 * Getter of the property <tt>earthquakeFileMask</tt>, see also <EventFileMask> tag in
	 * config.xml and -k command line option.
	 * 
	 * @return wildcarded mask to search earthquake description files.
	 */
	public String getEarthquakeFileMask() {
		return earthquakeFileMask.replace("." + File.separator, getConfigFileDir());
	}

	/**
	 * Getter of the property <tt>earthquakeFileMask</tt>, see also <EventFileMask> tag in
	 * config.xml and -k command line option.
	 * 
	 * @param earthquakeFileMask
	 *            wildcarded mask to search earthquake description files.
	 */
	public void setEarthquakeFileMask(String earthquakeFileMask) {
		this.earthquakeFileMask = earthquakeFileMask;
	}

	/**
	 * @return last selected in the file chooser directory, see also <ExportDir> tag in config.xml
	 */
	public String getUserDir(String type) {
		return userDirectories.get(type);
	}

	/**
	 * @param dir
	 *            last selected in the file chooser directory, see also <ExportDir> tag in
	 *            config.xml
	 */
	public void setUserDir(String type, String dir) {
		if ((getUserDir(type) == null) || !getUserDir(type).equals(dir)) {
			config.clearProperty("SessionData.UserDir." + type);
			if (dir != null) {
				userDirectories.put(type, dir);
				config.addProperty("SessionData.UserDir." + type, dir);
			}
			save();
		}
	}

	public Point getFramePos() {
		return new Point(framePos[0], framePos[1]);
	}

	public Dimension getFrameSize() {
		return new Dimension(frameSize[0], frameSize[1]);
	}

	public int getFrameExtendedState() {
		return frameState;
	}

	public void setFrameState(int extendedState, int posX, int posY, int width, int heigth) {
		boolean needsave = false;
		String state = "";
		if (extendedState != frameState) {
			frameState = extendedState;
			switch (frameState) {
			case Frame.NORMAL:
				state = "NORMAL";
				break;
			case Frame.ICONIFIED:
				state = "ICONIFIED";
				break;
			case Frame.MAXIMIZED_HORIZ:
				state = "MAXIMIZED_HORIZ";
				break;
			case Frame.MAXIMIZED_VERT:
				state = "MAXIMIZED_VERT";
				break;
			case Frame.MAXIMIZED_BOTH:
				state = "MAXIMIZED_BOTH";
				break;
			default:
				state = "MAXIMIZED_BOTH";
			}
			config.setProperty("SessionData.Frame.State", state);
			needsave = true;
		}
		if (posX != framePos[0]) {
			if (posX < 0) {
				framePos[0] = 0;
			} else {
				framePos[0] = posX;
			}
			config.setProperty("SessionData.Frame.PosX", framePos[0]);
			needsave = true;
		}
		if (posY != framePos[1]) {
			if (posY < 0) {
				framePos[1] = 0;
			} else {
				framePos[1] = posY;
			}
			config.setProperty("SessionData.Frame.PosY", framePos[1]);
			needsave = true;
		}
		if (width != frameSize[0]) {
			frameSize[0] = width;
			config.setProperty("SessionData.Frame.Width", width);
			needsave = true;
		}
		if (heigth != frameSize[1]) {
			frameSize[1] = heigth;
			config.setProperty("SessionData.Frame.Heigth", heigth);
			needsave = true;
		}
		if (needsave) {
			save();
		}
	}

	/**
	 * @return flag if we see status bar at the bottom of main frame
	 */
	public boolean getShowStatusBar() {
		return showStatusBar;
	}

	/**
	 * @param ssb
	 *            flag if we see status bar at the bottom of main frame
	 */
	public void setShowStatusBar(boolean ssb) {
		this.showStatusBar = ssb;
	}

	/**
	 * @return flag if we see command buttons panel in the main frame
	 */
	public boolean getShowCommandButtons() {
		return showCommandButtons;
	}

	/**
	 * @param scb
	 *            flag if we see command buttons panel in the main frame
	 */
	public void setShowCommandButtons(boolean scb) {
		this.showCommandButtons = scb;
	}

	/**
	 * @return if true, we see command buttons panel at the top of main frame, otherwise we see it
	 *         at the bottom.
	 */
	public boolean getShowCommandButtonsTop() {
		return showCommandButtonsTop;
	}

	/**
	 * @param scbt
	 *            if true, we see command buttons panel at the top of main frame, otherwise we see
	 *            it at the bottom.
	 */
	public void setShowCommandButtonsTop(boolean scbt) {
		this.showCommandButtonsTop = scbt;
	}

	/**
	 * Getter of the property <tt>logFile</tt>
	 * 
	 * @return the log file name.
	 */
	public String getLogFile() {
		return logFile;
	}

	/**
	 * Setter of the property <tt>logFile</tt>
	 * 
	 * @param logFile
	 *            the log file name.
	 */
	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	/**
	 * Get HTML pattern to generate blank HTML report. First looks in the "pattern.html" file, if
	 * not found returns default pattern
	 */
	public String getHTMLpattern() {
		File file = new File("pattern.html");
		if (file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				FileChannel fch = fis.getChannel();
				MappedByteBuffer mbf = fch.map(FileChannel.MapMode.READ_ONLY, 0, fch.size());
				byte[] barray = new byte[(int) (fch.size())];
				mbf.get(barray);
				return new String(barray);
			} catch (FileNotFoundException e) {
				lg.error("Can't get html pattern: " + e);
				return getDefaultHTMLPattern();
			} catch (IOException e) {
				lg.error("Can't get html pattern: " + e);
				return getDefaultHTMLPattern();
			}
		} else {
			return getDefaultHTMLPattern();
		}
	}
	
	public  SubnodeConfiguration getConfigurationAt(String node){
		return config.configurationAt(node);
	}
	
	public void save(){
		try {
			config.save(confFileName);
		} catch (ConfigurationException e) {
			lg.error("can't save configuration to file " + confFileName + ": " + e);
		}
	}
}
