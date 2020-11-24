package com.isti.xmax.common;

import com.isti.traceview.common.AbstractEvent;
import com.isti.traceview.common.IEvent;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * Arrival event, i.e registration time of earthquake's wave on the given instrument. Collection of
 * Arrival(s) is a property of a trace and can be found in the it's events list.
 * 
 * @author Max Kokoulin
 */
public class Arrival extends AbstractEvent implements IEvent {
	private static final Logger logger = Logger.getLogger(Arrival.class);

	/**
	 * @param localTime
	 *            time of registration
	 * @param eq
	 *            Earthquake to which this arrival belongs
	 * @param phase
	 *            Wave phase
	 * @param angle
	 *            angle (arc length) distance between earthquake and registration point
	 * @param azimuth
	 *            azimuth from earthquake to registration point
	 * @param distance
	 *            distance between points (angle scaled as fraction of earth's circumference)
	 */
	public Arrival(Date localTime, Earthquake eq, String phase, Double angle, Double azimuth, Double distance) {
		super(localTime, 0);
		setParameter("PHASE", phase);
		setParameter("EARTHQUAKE", eq);
		setParameter("ANGLE", angle);
		setParameter("AZIMUTH", azimuth);
		setParameter("DISTANCE", distance);
		logger.debug("Created " + this);
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

	public Double getDistance() {
		return (Double) getParameterValue("DISTANCE");
	}

	public String toString() {
		return "Arrival: Earthquake " + getEarthquake().getSourceCode() + ", phase " + getPhase() + ", angle " + getAngle() + ", azimuth "
				+ getAzimuth() + ", distance " + getDistance() + ", time " + getStartTime();
	}
}
