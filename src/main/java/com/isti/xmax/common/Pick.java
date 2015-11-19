package com.isti.xmax.common;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.isti.traceview.common.AbstractEvent;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.xmax.XMAX;
import com.isti.xmax.XMAXException;
import com.isti.xmax.XMAXconfiguration;

/**
 * Pick mark, manually entered time label which can be shown on the trace and stored in special xml
 * database.
 * 
 * @author Max Kokoulin
 */
public class Pick extends AbstractEvent implements IEvent {
	private static final Logger logger = Logger.getLogger(Pick.class);
	private static String thisSessionStart = TimeInterval.formatDate(new Date(), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE);
	private static Pick lastPick = null;

	private String sessionLabel = null;
	private transient PlotDataProvider channel = null;
	private Pick previousPick = null;
	private Pick nextPick = null;

	public enum PickType {
		P, S, PS, SP
	}

	private PickType pickType;

	/**
	 * Default constructor
	 */
	public Pick(Date time) {
		this(time, null, null);
	}

	/**
	 * Constructor to create pick from current session *
	 * 
	 * @param time
	 *            pick time
	 * @param channel
	 *            trace to attach pick
	 */
	public Pick(Date time, PlotDataProvider channel) {
		this(time, channel, thisSessionStart);
		addToXML();
	}

	/**
	 * Constructor to create old pick from closed session
	 * 
	 * @param time
	 *            pick time
	 * @param channel
	 *            trace to attach pick
	 * @param sessionLabel
	 *            session label
	 */
	public Pick(Date time, PlotDataProvider channel, String sessionLabel) {
		super(time, 0);
		this.channel = channel;
		this.sessionLabel = sessionLabel;
		if (channel != null && sessionLabel != null) {
			this.previousPick = lastPick;
			if (lastPick != null) {
				lastPick.nextPick = this;
			}
			lastPick = this;
			logger.debug("Pick created: channel " + channel + ", session " + sessionLabel + ", time "
					+ TimeInterval.formatDate(time, TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + "(" + time.getTime() + ")");
		}
	}

	@Override
	public String getType() {
		return "PICK";
	}

	public Color getColor() {
		return Color.BLUE;
	}

	/**
	 * Getter of the property <tt>pickType</tt>
	 * 
	 * @return type of pick.
	 */
	public PickType getPickType() {
		return pickType;
	}

	/**
	 * Setter of the property <tt>pickType</tt>
	 * 
	 * @param pickType
	 *            type of pick
	 */
	public void setPickType(PickType pickType) {
		this.pickType = pickType;
	}

	public PlotDataProvider getChannel() {
		return channel;
	}

	public void setChannel(PlotDataProvider channel) {
		this.channel = channel;
	}

	public String getSessionLabel() {
		return sessionLabel;
	}

	/**
	 * @return name of picks database file for current session
	 * @throws IOException if there is poblems creating a new file
	 */
	public File getSessionFile() throws IOException {
		String fName = sessionLabel.replace(",", "_").replace(":", "");
		File ret = new File(XMAXconfiguration.getInstance().getPickPath() + File.separator + fName + ".xml");
		RandomAccessFile raf = null;
		if (!ret.exists()) {
			// Create file here
			ret.createNewFile();
			raf = new RandomAccessFile(ret, "rw");
			raf.writeBytes("<?xml version=\"1.0\" ?>\n<Session time = \"" + sessionLabel + "\">\n</Session>");
			raf.close();
		}
		return ret;
	}

	/**
	 * Removes this pick from trace
	 */
	public void detach() {
		if (channel != null) {
			channel.removeEvent(this);
		}
		channel = null;
		if (previousPick != null) {
			previousPick.nextPick = this.nextPick;
		}
		if (nextPick != null) {
			nextPick.previousPick = this.previousPick;
		}
		deleteFromXML();
	}

	/**
	 * Removes this pick from xml database
	 */
	private void deleteFromXML() {
		long sleep = 1;
		long maxsleep = 4096;
		boolean success = false;
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(getSessionFile());
			//Element session = doc.getRootElement();
			String query = "//Pick[@time='" + TimeInterval.formatDate(getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + "']";
			XPath selectPickByTime = XPath.newInstance(query);
			List<?> picks = selectPickByTime.selectNodes(doc);
			for (Object o: picks) {
				Element pick = (Element) o;
				pick.detach();
			}
			XMLOutputter outputter = new XMLOutputter();
			FileWriter writer = null;
			// ugly hack to avoid Sun's error with unpredictable time of file's mapped buffer
			// releasing
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038
			while (!success) {
				try {
					writer = new FileWriter(getSessionFile());
					success = true;
				} catch (java.io.IOException e) {
					try {
						Thread.sleep(sleep);
					} catch (Exception ex) {
						logger.error("Exception:", ex);	
					}
					sleep *= 2;
					if (sleep > maxsleep) {
						throw e;
					}
					System.gc();
					System.runFinalization();
				}
				finally{
					//Close the writer before trying to instatiate another in next loop iteration.
					if(writer != null) writer.close();
				}
			}
			outputter.output(doc, writer);
			writer.close();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			logger.error("JDOMException:", e);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("IOException:", e);	
		}
	}

	/**
	 * Adds this pick to xml database
	 */
	private void addToXML() {
		RandomAccessFile raf = null;
		try {
			File file = getSessionFile();
			FileInputStream fis = new FileInputStream(file);
			FileChannel fch = fis.getChannel();
			fis.close();
			MappedByteBuffer mbf = fch.map(FileChannel.MapMode.READ_ONLY, 0, fch.size());
			byte[] barray = new byte[(int) (fch.size())];
			mbf.get(barray);
			String lines = new String(barray); // one big string
			int insertPlace = lines.lastIndexOf("\n</Session>");
			String insertText = "\n  <Pick network=\"" + getChannel().getNetworkName() + "\" station=\"" + getChannel().getStation().getName()
					+ "\" location=\"" + getChannel().getLocationName() + "\" channel=\"" + getChannel().getChannelName() + "\" time=\""
					+ TimeInterval.formatDate(getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + "\"/>";
			raf = new RandomAccessFile(file, "rw");
			raf.seek(insertPlace);
			raf.writeBytes(insertText + "\n</Session>");
			raf.setLength(insertPlace + insertText.length() + 11);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error("FileNotFoundException:", e);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("IOException:", e);	
		} finally {
			try {
				raf.close();
			} catch (Exception e) {
				// do nothing
				logger.error("Exception:", e);	
			}
		}
	}

	/**
	 * Deletes last added pick from xml database
	 */
	public static void geleteLastPick() {
		if (lastPick != null) {
			Pick lp = lastPick.previousPick;
			lastPick.detach();
			lastPick = lp;
		}
	}

	/**
	 * Loads all picks from xml database
	 * 
	 * @throws XMAXException if the Pick directory is not a directory
	 */
	public static void loadPicks() throws XMAXException {
		File[] dir;
		File f = new File(XMAXconfiguration.getInstance().getPickPath());
		if (f.isDirectory()) {
			dir = f.listFiles();
			if (dir.length > 0) {
				for (int i = 0; i < dir.length; i++) {
					if (!dir[i].isDirectory()) {
						try {
							SAXParserFactory spf = SAXParserFactory.newInstance();
							SAXParser sp = spf.newSAXParser();
							sp.parse(dir[i], new SAXHandler());
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							logger.error("ParseConfigurationException:", e);	
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							logger.error("SAXException:", e);	
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.error("IOException:", e);	
						}
					}
				}
			}
		} else {
			throw new XMAXException("Pick directory shouldn't be file");
		}
	}
}

/**
 * SAX handler to parse xml picks database file
 */
class SAXHandler extends DefaultHandler {
	private static final Logger logger = Logger.getLogger(SAXHandler.class);

	private Locator locator = null;
	private String sessionLabel = "";

	public SAXHandler() {
		super();
		setDocumentLocator(locator);
	}

	public void startElement(String namespaceURI, String localName, String rawName, Attributes attrs) {
		String network = null;
		Station station = null;
		String location = null;
		String channel = null;
		Date time = null;
		if (rawName.equals("Pick")) {
			int len = attrs.getLength();
			if (len != 5)
				error(new SAXParseException("Wrong Pick attributes count", locator));
			for (int i = 0; i < len; i++) {
				String attrName = attrs.getQName(i);
				if (attrName.equals("time")) {
					time = TimeInterval.parseDate(attrs.getValue(i), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL);
				} else if (attrName.equals("network")) {
					network = attrs.getValue(i);
				} else if (attrName.equals("station")) {
					station = DataModule.getStation(attrs.getValue(i));
				} else if (attrName.equals("location")) {
					location = attrs.getValue(i);
				} else if (attrName.equals("channel")) {
					channel = attrs.getValue(i);
				} else {
					error(new SAXParseException("Wrong Pick attributes name: " + attrName, locator));
				}
			}
			if (station != null) {
				PlotDataProvider ch = XMAX.getDataModule().getChannel(channel, station, network, location);
				if (ch != null && ch.getTimeRange().isContain(time)) {
					Pick pick = new Pick(time, ch, sessionLabel);
					ch.addEvent(pick);
				}
			}
		} else if (rawName.equals("Session")) {
			sessionLabel = attrs.getValue(0);
		}
	}

	public void endElement(String namespaceURI, String localName, String rawName) {

	}

	public void characters(char ch[], int start, int length) {

	}

	//
	// ErrorHandler methods
	//

	/** Warning. */
	public void warning(SAXParseException ex) {
		logger.warn(getLocationString(ex) + ": ", ex);
	}

	/** Error. */
	public void error(SAXParseException ex) {
		logger.error(getLocationString(ex) + ": ", ex);
	}

	/** Fatal error. */
	public void fatalError(SAXParseException ex) {
		logger.error(getLocationString(ex) + ": ", ex);
	}

	/** Returns a string of the location. */
	private String getLocationString(SAXParseException ex) {
		StringBuffer str = new StringBuffer();

		String systemId = ex.getSystemId();
		if (systemId != null) {
			int index = systemId.lastIndexOf('/');
			if (index != -1)
				systemId = systemId.substring(index + 1);
			str.append(systemId);
		}
		str.append(':');
		str.append(ex.getLineNumber());
		str.append(':');
		str.append(ex.getColumnNumber());

		return str.toString();
	}
}
