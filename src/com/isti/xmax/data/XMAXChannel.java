package com.isti.xmax.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.isti.traceview.common.IEvent;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.xmax.XMAX;
import com.isti.xmax.common.Earthquake;

/**
 * Customized {@link com.isti.traceview.data.Channel}
 * 
 * @author Max Kokoulin
 */
public class XMAXChannel extends PlotDataProvider {
	private boolean isArrivalsComputed = false;

	public XMAXChannel(String channelName, Station station, String networkName, String locationName) {
		super(channelName, station, networkName, locationName);

	}

	/**
	 * Gets available earthquakes whose phases we can see in the given time range in this channel.
	 */

	public Set<IEvent> getAvailableEarthquakes(TimeInterval ti) {
		computeArrivals();
		Set<IEvent> ret = new HashSet<IEvent>();
		for (IEvent event: events) {
			if (event.getType().equals("ARRIVAL") && ti.isContain(event.getStartTime())) {
				IEvent earthquake = (IEvent) event.getParameterValue("EARTHQUAKE");
				ret.add(earthquake);
			}
		}
		return ret;
	}

	/**
	 * Set of available phases in this channel for given set of earthquakes.
	 * 
	 * @return Set of strings - phases names.
	 */

	public Set<String> getAvailablePhases(TimeInterval ti, Object[] earthquakes) {
		computeArrivals();
		Set<String> ret = new HashSet<String>();
		for (IEvent event: events) {
			if (event.getType().equals("ARRIVAL")) {
				Earthquake earthquake = (Earthquake) event.getParameterValue("EARTHQUAKE");
				int i = -1;
				if ((i = Arrays.binarySearch(earthquakes, earthquake)) >= 0) {
					String phaseName = (String) event.getParameterValue("PHASE");
					ret.add(phaseName);
				}
			}
		}
		return ret;
	}

	/**
	 * Computes and add to this channel arrivals
	 */
	private void computeArrivals() {
		if (!isArrivalsComputed) {
			for (IEvent event: XMAX.getDataModule().getEarthquakes()) {
				Earthquake eq = (Earthquake) event;
				addEvents(eq.computeArrivals(this));
			}
			isArrivalsComputed = true;
		}
	}

}
