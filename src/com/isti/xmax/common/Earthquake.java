package com.isti.xmax.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.AbstractEvent;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.Wildcard;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.xmax.XMAX;
import com.isti.xmax.XMAXconfiguration;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.fissuresUtil.bag.DistAz;

/**
 * Class holds information about particular earthquake. Also contain initialization logic from "ndk"
 * files and can compute arrivals for this earthquake.
 * 
 * @author Max Kokoulin
 */
public class Earthquake extends AbstractEvent implements IEvent {
	private static Logger lg = Logger.getLogger(Earthquake.class);
	private static SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	/**
	 * Time lag to load earthquakes to see phase in time interval, in milliseconds
	 */
	private static long maxPhaseDelay = 7200000; // Two hours

	public static String[] phases = { "p", "s", "P", "S", "Pn", "Sn", "PcP", "ScS", "Pdiff", "Sdiff", "PKP", "SKS", "PKiKP", "SKiKS", "PKIKP",
			"SKIKS" };

	static {
		df.setTimeZone(XMAX.timeZone);
	}

	/**
	 * @param time
	 *            earthquake time
	 * @param code
	 *            directory code
	 * @param latitude
	 *            latitude
	 * @param longitude
	 *            longitude
	 * @param depth
	 *            depth
	 * @param magnitude_mb
	 *            magnitude
	 * @param magnitude_MS
	 *            magnitude
	 * @param location
	 *            region description
	 */
	public Earthquake(Date time, String code, Double latitude, Double longitude, Double depth, Double magnitude_mb, Double magnitude_MS,
			String location) {
		super(time, 0);
		setParameter("SOURCECODE", code);
		setParameter("LATITUDE", latitude);
		setParameter("LONGITUDE", longitude);
		setParameter("DEPTH", depth);
		setParameter("MAGNITUDE_MB", magnitude_mb);
		setParameter("MAGNITUDE_MS", magnitude_MS);
		setParameter("LOCATION", location);
		lg.debug("Created " + this);
	}

	public String getType() {
		return "EARTHQUAKE";
	}

	public String getSourceCode() {
		return (String) getParameterValue("SOURCECODE");
	}

	public Double getLatitude() {
		return (Double) getParameterValue("LATITUDE");
	}

	public Double getLongitude() {
		return (Double) getParameterValue("LONGITUDE");
	}

	public Double getDepth() {
		return (Double) getParameterValue("DEPTH");
	}

	public Double getMagnitude_mb() {
		return (Double) getParameterValue("MAGNITUDE_MB");
	}

	public Double getMagnitude_MS() {
		return (Double) getParameterValue("MAGNITUDE_MS");
	}

	public String getLocation() {
		return (String) getParameterValue("LOCATION");
	}

	public String toString() {
		return "Earthquake: SourceCode " + getSourceCode() + ", latitude " + getLatitude() + ", longitude " + getLongitude() + ", depth "
				+ getDepth() + ", magnitude mb " + getMagnitude_mb() + ", magnitude MS " + getMagnitude_MS() + ", location " + getLocation()
				+ ", time " + getStartTime();
	}

	/**
	 * Reads earthquakes list from Global Centroid-Moment-Tensor (CMT) catalog NDK file format
	 */
	public static List<IEvent> getEarthquakes(TimeInterval ti) throws TraceViewException {
		List<IEvent> ret = new ArrayList<IEvent>();
		List<File> files = new Wildcard().getFilesByMask(XMAXconfiguration.getInstance().getEarthquakeFileMask());
		for (File file: files) {
			lg.debug("Processing event file: " + file.getName());
			try {
				LineNumberReader r = new LineNumberReader(new FileReader(file));
				String[] rawData = null;
				while ((rawData = readRawData(r))[0] != null) {
					try {
						// First line: Hypocenter line

						// [1-4] Hypocenter reference catalog (e.g., PDE for USGS location, ISC for
						// ISC catalog, SWE for surface-wave location, [Ekstrom, BSSA, 2006])
						String catalog = rawData[0].substring(0, 4).trim();
						// [6-15] Date of reference event
						// [17-26] Time of reference event
						Date date = df.parse(rawData[0].substring(5, 26).trim());
						// [28-33] Latitude
						Double latitude = new Double(rawData[0].substring(27, 33).trim());
						// [35-41] Longitude
						Double longitude = new Double(rawData[0].substring(34, 41).trim());
						// [43-47] Depth
						Double depth = new Double(rawData[0].substring(42, 47).trim());
						// [49-55] Reported magnitudes, usually mb and MS
						Double magnitude_mb = new Double(rawData[0].substring(48, 51).trim());
						Double magnitude_MS = new Double(rawData[0].substring(52, 55).trim());
						// [57-80] Geographical location (24 characters)
						String location = rawData[0].substring(56, 80).trim();

						// Second line: CMT info (1)

						// [1-16] CMT event name. This string is a unique CMT-event identifier.
						// Older
						// events have 8-character names, current ones have 14-character names.
						String name = rawData[1].substring(0, 16).trim();
						if ((date.getTime() > ti.getStart() - maxPhaseDelay) && (date.getTime() < ti.getEnd())) {
							Earthquake earthquake = new Earthquake(date, name, latitude, longitude, depth, magnitude_mb, magnitude_MS, location);
							ret.add(earthquake);
						}
					} catch (NumberFormatException e) {
						lg.error("Can't parse earthquake, line " + (r.getLineNumber() - rawData.length) + "; " + e);
					} catch (ParseException e) {
						lg.error("Can't parse earthquake, line " + (r.getLineNumber() - rawData.length) + "; " + e);
					}
				}
			} catch (FileNotFoundException e) {
				lg.error("Can't open earthquake file " + "; " + e);
			}
		}
		Collections.sort(ret);
		return ret;
	}

	/**
	 * Reads data for one earthquake from .NDK file. Return String[5] array contains five lines of
	 * earquake's data, or null if exception occured
	 */
	private static String[] readRawData(LineNumberReader r) {
		String[] data = new String[5];
		try {
			for (int i = 0; i < data.length; i++) {
				data[i] = r.readLine();
			}
		} catch (IOException e) {
			return null;
		}
		return data;
	}

	/**
	 * Compute arrivals for this earthquake
	 * 
	 * @param channel
	 *            channel describes station and time range of interested arrivals
	 * @return set of arrivals
	 */
	public SortedSet<IEvent> computeArrivals(PlotDataProvider channel) {
		TreeSet ret = new TreeSet<IEvent>();
		Station station = channel.getStation();
		try {
			TauP_Time timeTool = new TauP_Time("iasp91");
			timeTool.setPhaseNames(phases);
			for (IEvent earthquake: XMAX.getDataModule().getEarthquakes()) {
				timeTool.depthCorrect((Double) earthquake.getParameterValue("DEPTH"));
				DistAz da = new DistAz((Double) earthquake.getParameterValue("LATITUDE"), (Double) earthquake.getParameterValue("LONGITUDE"), station
						.getLatitude(), station.getLongitude());
				double angle = da.getDelta();
				timeTool.calculate(angle);
				edu.sc.seis.TauP.Arrival[] arrivals = timeTool.getArrivals();
				for (int i = 0; i < arrivals.length; i++) {
					ret.add(new Arrival(new Date(earthquake.getStartTime().getTime() + new Double(arrivals[i].getTime() * 1000).longValue()),
							(Earthquake) earthquake, arrivals[i].getName(), angle, da.getAz(), da.getBaz(), DistAz.degreesToKilometers(angle)));
				}
			}
		} catch (TauModelException e) {
			lg.error("Can't load TauP earth model: " + e);
		}
		return ret;
	}
}
