package com.isti.traceview;

import com.isti.traceview.common.Configuration;
import com.isti.traceview.data.DataModule;
import java.util.SimpleTimeZone;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * The main class of library, it should be created first to use lib. 
 * If library used in non-graphics environment, use TraceViewCore instead.
 * 
 * @author Max Kokoulin
 */
public class TraceView {

	/**
	 * TraceView is a graphics utility. This is reference to program frame.
	 * 
	 * @see JFrame
	 */
	
	private static final Logger logger = Logger.getLogger(TraceView.class);

	/**
	 * Library {@link Configuration} class
	 */
	private static Configuration conf = null;

	/**
	 * {@link DataModule} class - holds all informations about data
	 */
	private static DataModule dataModule = null;

	private static final String MIN_JAVA_VERSION_OSX = "1.7.0";
	private static final String MIN_JAVA_VERSION = "1.7.0";

	/**
	 * Global timezone used everywhere in library
	 */
	public static SimpleTimeZone timeZone = new SimpleTimeZone(12, "GMT");

	/**
	 * holds version of java virtual machine used to run library
	 */
	private static String javaVerString = "";

	/**
	 * holds operating system name used to run library
	 */
	public static String osNameString = null;
	private static JFrame frame = null;
	private static IUndoAdapter undoAdapter = null;

	static {
		String minJavaVersion = null;
		String javaVersionString = System.getProperty("java.version");
		osNameString = System.getProperty("os.name");
		if (osNameString.equals("Mac OS X")) {
			minJavaVersion = MIN_JAVA_VERSION_OSX;
		} else {
			minJavaVersion = MIN_JAVA_VERSION;
		}
		if (javaVersionString != null) {
			// Java version string fetched OK
			if (parseVersionNumbers(javaVersionString).length > 0 &&
					compareVersionStrings(javaVersionString, minJavaVersion) < 0) {
				// version string format OK and version is too low; build error msg
				javaVerString = "This program requires a newer version of " + "Java (Java \"" + javaVersionString + "\" in use, Java \""
						+ minJavaVersion + "\" or later required). OS " + osNameString;

			} else {
				javaVerString = "Java " + javaVersionString + " OS " + osNameString + ", version OK";
			}
		} else
			// unable to fetch Java version string
			javaVersionString = "(Unknown)"; // indicate unable to fetch
		logger.debug("" + javaVersionString);

	}

	public static int[] parseVersionNumbers(String versionString) {
		String[] versionComponents =
				versionString.replace("_",".").split("\\.");
		int[] components = new int[versionComponents.length];
		for (int i = 0; i < versionComponents.length; ++i) {
			components[i] = Integer.parseInt(versionComponents[i]);
		}
		return components;
	}

	public static int compareVersionStrings(String versionString, String minJavaVersionString) {
		int[] versionComponents = parseVersionNumbers(versionString);
		int[] minJavaVersion = parseVersionNumbers(minJavaVersionString);
		for (int i = 0; i < minJavaVersion.length; ++i) {
			if (versionComponents[i] == minJavaVersion[i]) {
				continue;
			}
			return Integer.compare(versionComponents[i], minJavaVersion[i]);
		}
		return 0;
	}

	public TraceView() {
	}

	public static JFrame getFrame() {
		return frame;
	}

	/**
	 * Program frame setter. Also checks java version correctness. If traceview used in non-graphics
	 * mode (for example, for responses calculations) this method isn't used and java version checks
	 * isn't happens.
	 * 
	 * @param fr
	 *            JFrame to set
	 * @see JFrame
	 */
	public static void setFrame(JFrame fr) {
		if (!getJavaVersionMessage().contains("version OK")) {
			// send warning to log
			logger.warn(getJavaVersionMessage());
			SwingUtilities.invokeLater(
					() -> JOptionPane.showMessageDialog(frame, getJavaVersionMessage(), "Warning", JOptionPane.WARNING_MESSAGE));
		}
		frame = fr;
	}
	
	public static DataModule getDataModule() {
		return dataModule;
	}

	public static void setDataModule(DataModule dm) {
		dataModule = dm;
	}

	public static Configuration getConfiguration() {
		return conf;
	}

	public static void setConfiguration(Configuration cn) {
		conf = cn;
	}

	public static String getJavaVersionMessage() {
		return javaVerString;
	}

	public static void setUndoEnabled(boolean ue) {
		if (undoAdapter != null) {
			undoAdapter.setUndoEnabled(ue);
		}
	}

	public static void setUndoAdapter(IUndoAdapter ul) {
		undoAdapter = ul;
	}

}
