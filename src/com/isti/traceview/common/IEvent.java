package com.isti.traceview.common;

import java.awt.Color;
import java.util.Date;
import java.util.Set;

/**
 * This base interface represents event, i.e some occasion with time and duration which can be
 * plotted on channel's graph. Event here is as an abstract object that is defined by a start time,
 * duration, and series of other parameters and needs to be displayed in the channel panels. Event
 * has set of parameter-value pairs, so every type of event can be processed in the same manner.
 * This interface extends "Comparable" in order to define events sorting order. Note that we use the
 * word "Event" in a software sense, not seismological.
 * 
 * @author Max Kokoulin
 */
public interface IEvent extends Comparable {

	/**
	 * @return event's type
	 */
	public String getType();

	/**
	 * @return event's starting time
	 */
	public Date getStartTime();

	/**
	 * @return event's duration in milliseconds, or 0 if it's one-moment event
	 */
	public long getDuration();

	/**
	 * @return color to draw event on channel's graph
	 */
	public Color getColor();

	/**
	 * @return set of available parameters for this event
	 */
	public Set<String> getParameters();

	/**
	 * @param parameterName
	 *            name of parameter
	 * @return value of given parameter
	 */
	public Object getParameterValue(String parameterName);

}
