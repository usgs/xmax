package com.isti.xmax.common;

import java.util.Date;

import org.apache.log4j.Logger;

import com.isti.traceview.common.AbstractEvent;
import com.isti.traceview.common.IEvent;

/**
 * Arrival event, i.e registration time of earthquake's wave on the given instrument. Collection of
 * Arrival(s) is a property of a trace and can be found in the it's events list.
 * 
 * @author Max Kokoulin
 */
public class Arrival extends AbstractEvent implements IEvent {
	private static Logger lg = Logger.getLogger(Arrival.class);

	/**
	 * @param localTime
	 *            time of registration
	 * @param eq
	 *            Earthquake to which this arrival belongs
	 * @param phase
	 *            Wave phase
	 * @param angle
	 *            angle distance between earthquake and registration point
	 * @param azimuth
	 *            azimuth from earthquake to registration point
	 * @param azimuth_back
	 *            azimuth from registration point to earthquake
	 * @param distance
	 *            distance between earthquake and registration point
	 */
	public Arrival(Date localTime, Earthquake eq, String phase, Double angle, Double azimuth, Double azimuth_back, Double distance) {
		super(localTime, 0);
		setParameter("PHASE", phase);
		setParameter("EARTHQUAKE", eq);
		setParameter("ANGLE", angle);
		setParameter("AZIMUTH", azimuth);
		setParameter("AZIMUTH_BACK", azimuth_back);
		setParameter("DISTANCE", distance);
		lg.debug("Created " + this);
	}

	@Override
	public String getType() {
		return "ARRIVAL";
	}

	public Earthquake getEarthquake() {
		return (Earthquake) getParameterValue("EARTHQUAKE");
	}

	public String getPhase() {
		return (String) getParameterValue("PHASE");
	}

	public Double getAngle() {
		return (Double) getParameterValue("ANGLE");
	}

	public Double getAzimuth() {
		return (Double) getParameterValue("AZIMUTH");
	}

	public Double getAzimuth_back() {
		return (Double) getParameterValue("AZIMUTH_BACK");
	}

	public Double getDistance() {
		return (Double) getParameterValue("DISTANCE");
	}

	public String toString() {
		return "Arrival: Earthquake " + getEarthquake().getSourceCode() + ", phase " + getPhase() + ", angle " + getAngle() + ", azimuth "
				+ getAzimuth() + ", back azimuth " + getAzimuth_back() + ", distance " + getDistance() + ", time " + getStartTime();
	}
}
