package com.isti.xmax;

import com.isti.traceview.TraceView;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.gui.ColorModeBySegment;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.data.XMAXDataModule;
import com.isti.xmax.gui.XMAXframe;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;
import javax.swing.JOptionPane;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.reflections.Reflections;

/**
 * Main class for XMAX. Keeps command line parsing logic, handles with plugins, initialize data and
 * graphics.
 *
 * @author Max Kokoulin
 */
public class XMAX extends TraceView {
	private static final Logger logger = Logger.getLogger(XMAX.class);
	public static final String version = XMAX.class.getPackage().getImplementationVersion();
	public static final String releaseDate = getReleaseDate();

	/**
	 * Parsed command line
	 */
	private static CommandLine cmd;
	private static Options options;
	private static Set<Class<? extends IFilter>> filters;
	private static Set<Class<? extends ITransformation>> transformations;

	public static String getReleaseDate() {
		URLClassLoader urlLoader = (URLClassLoader) XMAX.class.getClassLoader();
		try {
			URL url = urlLoader.findResource("META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest(url.openStream());
			return manifest.getMainAttributes().getValue("Build-Timestamp");
		} catch (IOException e) {
			return "RELEASE DATE UNKNOWN";
		}
	}

	public XMAX() {
		super();
		setUndoAdapter(new XMAXUndoAdapter());
		try {
			boolean dump = false;
			System.out.println("  XMAX ver." + getVersionMessage() );

			System.out.println("===============");
			if (cmd.getOptions().length == 0) {
				System.out.println("[ Quick Examples ]\n");
				System.out.println("* Read from -d 'data/path':");
				System.out.println(" >java -Xms512M -Xmx512M -jar xmax.jar -d '/xs0/seed/IU_PTGA/2014_1{93,94}/00_LHZ*seed'\n");
				System.out.println("* Read from BOTH -d 'data/path' AND existing serialized data in DATA_TEMP:");
				System.out.println(" >java -Xms512M -Xmx512M -jar xmax.jar -t -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'\n");
				System.out.println("* Overwrite Serialized data in DATA_TEMP:");
				System.out.println(" >java -Xms512M -Xmx512M -jar xmax.jar -T -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'\n");
				System.out.println("* Append to Serialized data in DATA_TEMP:");
				System.out.println(" >java -Xms512M -Xmx512M -jar xmax.jar -T -t -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'");
			}
			System.out.println("===============");
			if (cmd.hasOption("h")) {
				if (cmd.getOptions().length > 1) {
					throw new XMAXException("It isn't allowed to use any other options with -h");
				}
				HelpFormatter formatter = new HelpFormatter();
				formatter
						.printHelp(
								"xmax [-h | -v | -T] {-t -u<units> -o<order>} [-c<config file> -d<data mask> -s<station file> -k<earthquakes mask> -q<QC file> -b<begin time> -e<end time> -f<units count>]",
								options);
			} else if (cmd.hasOption("v")) {
				if (cmd.getOptions().length > 1) {
					throw new XMAXException("It isn't allowed to use any other options with -v");
				}
				System.out.println("XMAX version " + getVersionMessage() + ". Instrumental Software Technologies, " + getReleaseDateMessage());
			} else {
				if (cmd.hasOption("g")) {
					XMAXconfiguration.confFileName = cmd.getOptionValue("g").trim();
				}
				setConfiguration(XMAXconfiguration.getInstance());	// if -g not given load current config.xml
				if (cmd.hasOption("T")) {
					dump = true;
					getConfiguration().setDumpData(true);
/** MTH: This has changed
 if (cmd.hasOption("t")) {
 throw new XMAXException("It isn't allowed to use -T and -t options together");
 }
 **/
				}
				if (cmd.hasOption("t")) {
					getConfiguration().setUseTempData(true);
/**
 if (cmd.hasOption("T")) {
 throw new XMAXException("It isn't allowed to use -T and -t options together");
 }
 **/
				}
				if (cmd.hasOption("d")) {
					getConfiguration().setUseDataPath(true);
					getConfiguration().setDataPath(dequote(cmd.getOptionValue("d")).trim());
				}
				if (cmd.hasOption("i")) {
					getConfiguration().setStationInfoFileName(cmd.getOptionValue("i").trim());
				}
				if (cmd.hasOption("q")) {
					getConfiguration().setQCdataFileName(cmd.getOptionValue("q").trim());
				}
				if (cmd.hasOption("p")) {
					getConfiguration().setPickPath(dequote(cmd.getOptionValue("p")).trim());
				}
				if (cmd.hasOption("u")) {
					getConfiguration().setPanelCountUnit(XMAXconfiguration.PanelCountUnit.values()[new Integer(cmd.getOptionValue("u").trim())]);
				}
				if (cmd.hasOption("o")) {
					getConfiguration().setPanelOrder(XMAXconfiguration.ChannelSortType.values()[new Integer(cmd.getOptionValue("o").trim())]);
				}
				if (cmd.hasOption("f")) {
					getConfiguration().setUnitsInFrame(new Integer(cmd.getOptionValue("f").trim()));
				}
				if (cmd.hasOption("F")) {
					getConfiguration().setDefaultCompression(cmd.getOptionValue("F").trim());
				}
				if (cmd.hasOption("k")) {
					getConfiguration().setEarthquakeFileMask(dequote(cmd.getOptionValue("k")));
				}
				if (cmd.hasOption("b")) {
					getConfiguration().setStartTime(
							TimeInterval.parseDate(cmd.getOptionValue("b").trim(), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE));
				}
				if (cmd.hasOption("e")) {
					getConfiguration().setEndTime(
							TimeInterval.parseDate(cmd.getOptionValue("e").trim(), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE));
				}
				if (cmd.hasOption("m")) {
					getConfiguration().setMergeLocations(true);
				}
				if (cmd.hasOption("s")) {
					getConfiguration().setFilterStation(cmd.getOptionValue("s").trim());
				}
				if (cmd.hasOption("n")) {
					getConfiguration().setFilterNetwork(cmd.getOptionValue("n").trim());
				}
				if (cmd.hasOption("c")) {
					getConfiguration().setFilterChannel(cmd.getOptionValue("c").trim());
				}
				if (cmd.hasOption("l")) {
					getConfiguration().setFilterLocation(cmd.getOptionValue("l").trim());
				}
				if (cmd.hasOption("L")) {
					getConfiguration().setDefaultBlockLength(new Integer(cmd.getOptionValue("L").trim()));
				}
				if (dump) {
					// -T option in command line, make dump
					setConfiguration(XMAXconfiguration.getInstance());
					setDataModule(XMAXDataModule.getInstance());
					getDataModule().dumpData(new ColorModeBySegment());
				} else {
					// Find all classes that implement IFilter and ITransformation.
					Reflections reflect = new Reflections("com.isti");
					filters = reflect.getSubTypesOf(IFilter.class);
					transformations = reflect.getSubTypesOf(ITransformation.class);

					setDataModule(XMAXDataModule.getInstance());

					getDataModule().loadData();

					if (getDataModule().getAllChannels().size() > 0) {
						setFrame(XMAXframe.getInstance());
						if (XMAXconfiguration.getInstance().getTimeInterval() != null) {
							getFrame().setShouldManageTimeRange(false);
							getFrame().setTimeRange(XMAXconfiguration.getInstance().getTimeInterval());
						}
						try {
							// Wait while frame will be created to correct repaint
							Thread.sleep(200);
						} catch (InterruptedException e) {
							logger.error("InterruptedException:", e);
						}
						getFrame().setVisible(true);
						getFrame().setShouldManageTimeRange(true);
					} else {
						JOptionPane.showMessageDialog(null, "No data found at path " + XMAXconfiguration.getInstance().getDataPath(), "Alert",
								JOptionPane.WARNING_MESSAGE);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception:", e);
			System.exit(0);
		}
	}

	/**
	 * Getter for configuration.
	 */
	public static XMAXconfiguration getConfiguration() {
		return (XMAXconfiguration) TraceView.getConfiguration();
	}

	/**
	 * Getter for data module.
	 */
	public static XMAXDataModule getDataModule() {
		return (XMAXDataModule) TraceView.getDataModule();
	}

	/**
	 * Getter for main frame
	 */
	public static XMAXframe getFrame() {
		return (XMAXframe) TraceView.getFrame();
	}

	/**
	 * Get all plugins-filters
	 */
	public static Set<Class<? extends IFilter>> getFilters() {
		return filters;
	}

	/**
	 * Get plugin-filter by id
	 */
	public static IFilter getFilter(String id)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		for (Class<? extends IFilter> curClass : filters) {

			try {
				if (Objects.equals(curClass.getField("NAME").get(null), id)) {
					IFilter filter = curClass.newInstance();
					return filter;
				}
			} catch (NoSuchFieldException | SecurityException e) {
				// Field doesn't exist, move to next
			}
		}
		return null;
	}

	/**
	 * Get all transformations
	 */
	public static Set<Class<? extends ITransformation>> getTransformations() {
		return transformations;
	}

	/**
	 * Get transformation by id.
	 *
	 * @return the matching ITransformation or null if none matches
	 */
	public static ITransformation getTransformation(String id)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		for (Class<? extends ITransformation> curClass : transformations) {
			try {
				if (Objects.equals(curClass.getField("NAME").get(null), id)) {
					ITransformation transform = curClass.newInstance();
					return transform;
				}
			} catch (IllegalArgumentException | NoSuchFieldException | SecurityException e) {
				// Field has issues, move to next field
			}
		}
		return null;
	}

	/**
	 * @return filled and initialized CLI options object
	 */
	private static Options getOptions() {
		Options opt = new Options();
		opt.addOption(new Option("h", "help", false, "print this message"));
		opt.addOption(new Option("v", "version", false, "print xmax version"));
		opt.addOption(new Option("g", "config", true, "configuration file"));
		opt.addOption(new Option("d", "data", true, "wildcarded mask of data files to load"));
		opt.addOption(new Option("T", "make_dump", false, "dumps temporary file storage"));
		opt.addOption(new Option("t", "use_dump", false, "adds temporary file storage content to data found by wildcarded mask (see -d)"));
		opt.addOption(new Option("i", "stations", true, "stations description file"));
		opt.addOption(new Option("k", "earthquakes", true, "wildcarded mask of earthquekes files"));
		opt.addOption(new Option("q", "qcdata", true, "QC data file name"));
		opt.addOption(new Option("b", "bdate", true, "begin date at yyyy,DDD,HH:mm:ss format"));
		opt.addOption(new Option("e", "edate", true, "end date at yyyy,DDD,HH:mm:ss format"));
		opt.addOption(new Option("u", "unit", true, "panel count unit: 0 - trace, 1 - station, 2 - channel, 3 - channel type, 4 - all"));
		opt.addOption(new Option("o", "order", true,
				"panel order: 0 - trace name, 1 - network/station/samplerate, 2 - channel, 3 - channel type,  4 - event"));
		opt.addOption(new Option("f", "unitsframe", true, "units count (from -u option) in frame to display"));
		opt.addOption(new Option("p", "picks", true, "picks database path"));
		opt.addOption(new Option("m", "merge", false, "merge different locations of  channel into one graphical panel"));
		opt
				.addOption(new Option("F", "Format", true,
						"default block compression format, possible values are SHORT, INT24, INT32, FLOAT, DOUBLE, STEIM1, STEIM2, CDSN, RSTN, DWW, SRO, ASRO, HGLP"));
		opt.addOption(new Option("L", "Length", true, "default block length"));

		opt.addOption(new Option("n", "flt_network", true, "semicolon-separated wildcarded filter by network"));
		opt.addOption(new Option("s", "flt_station", true, "semicolon-separated wildcarded filter by station"));
		opt.addOption(new Option("l", "flt_location", true, "semicolon-separated wildcarded filter by location"));
		opt.addOption(new Option("c", "flt_channel", true, "semicolon-separated wildcarded filter by channel"));
		return opt;
	}

	/**
	 * Dequote string, i.e remove wrapping ' and ".
	 */
	public static String dequote(String str) {
		if ((str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"') || (str.charAt(0) == '\'' && str.charAt(str.length() - 1) == '\'')) {
			return str.substring(1, str.length() - 1);
		} else {
			return str;
		}
	}

	/**
	 * Get version message
	 */
	public static String getVersionMessage() {
		return version;
	}

	/**
	 * Get release date
	 */
	public static String getReleaseDateMessage() {
		return releaseDate;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		options = getOptions();
		try {
			CommandLineParser parser = new PosixParser();
			cmd = parser.parse(options, args);
			XMAX xyz = new XMAX();
		} catch (ParseException e) {
			//System.err.println("Command line parsing failed.  Reason: " + e.getMessage());
			String message = "Command line parsing failed. Reason:";
			logger.error(message, e);
		}
	}

	/**
	 * Set configuration
	 */
	public static void setConfiguration(XMAXconfiguration cn) {
		RollingFileAppender apd = new RollingFileAppender();
		apd.setName("FILELOG");
		apd.setFile(cn.getLogFile());
		apd.setMaxFileSize("1000KB");
		apd.setMaxBackupIndex(10);
		apd.setLayout(new PatternLayout("%d %5p %m%n"));
		apd.setAppend(false);
		apd.activateOptions();
		Logger.getRootLogger().addAppender(apd);
		Runtime.getRuntime().addShutdownHook(new ClearLogShutDownHook());
		TraceView.setConfiguration(cn);
	}
}

/**
 * Clears logs after program shutdown.
 */
class ClearLogShutDownHook extends Thread {
	public void run() {
		RollingFileAppender apd = (RollingFileAppender) (Logger.getRootLogger().getAppender("FILELOG"));
		apd.close();
		File f = new File(XMAXconfiguration.getInstance().getLogFile());
		if (f.length() == 0) {
			f.deleteOnExit();
		}
	}
}
