package com.isti.traceview.common;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Date;

/**
 * Abstract class implements {@link IEvent} interface, it contains all common behaviors of events.
 * All concrete events are based on this class.
 * 
 * @author Max Kokoulin
 */
public abstract class AbstractEvent implements IEvent {

	/**
	 * @param date
	 *            event start time
	 * @param duration
	 *            event duration
	 */
	public AbstractEvent(Date date, long duration) {
		this.startTime = date;
		this.duration = duration;
		parameters = new HashMap<String, Object>();
	}

	public abstract String getType();


	private Date startTime;


	private long duration = 0;


	private Map<String, Object> parameters = null;

	/**
	 * Getter of the property <tt>duration</tt>
	 * 
	 * @return Returns the duration.
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Setter of the property <tt>duration</tt>
	 * 
	 * @param duration
	 *            The duration to set.
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	public Color getColor() {
		return Color.BLACK;
	}

	public Set<String> getParameters() {
		return parameters.keySet();
	}

	public Object getParameterValue(String parameterName) {
		return parameters.get(parameterName);
	}

	public void setParameter(String parameterName, Object value) {
		parameters.put(parameterName, value);
	}

	/**
	 * Getter of the property <tt>startTime</tt>
	 * 
	 * @return Returns the startTime.
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * Setter of the property <tt>startTime</tt>
	 * 
	 * @param startTime
	 *            The startTime to set.
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * Standard comparator - by start time
	 */
	public int compareTo(Object o) {
		if ((o instanceof IEvent)) {
			IEvent ae = (IEvent) o;
			return this.getStartTime().compareTo(ae.getStartTime());
		} else {
			return -1;
		}
	}

}
