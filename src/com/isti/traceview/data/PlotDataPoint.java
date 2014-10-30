package com.isti.traceview.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * represents one screen point of graph, contains all data needed to draw pixel. Each point on
 * screen graph represents some time range of raw trace data. Data point in this case is a vertical
 * line from minimal to maximal value for representing time range.
 * 
 * @author Max Kokoulin
 */
public class PlotDataPoint implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(PlotDataPoint.class);

	/**
	 * High value in representing section
	 * 
	 * @uml.property name="top"
	 */
	private double top = Double.NEGATIVE_INFINITY;

	/**
	 * Low value on representing section
	 * 
	 * @uml.property name="bottom"
	 */
	private double bottom = Double.POSITIVE_INFINITY;

	/**
	 * Sequential number of segment in trace, to which this point belongs 
	 */
	private int segmentNumber = 0;

	/**
	 * Sequential number of raw data provider in trace, to which this point belongs 
	 */
	private int rawDataProviderNumber = 0;
	
	/**
	 * Sequential number of continue data area in trace, to which this point belongs. 
	 * Similar to segmentNumber, but takes into account only gaps, not overlaps 
	 */
	private int continueAreaNumber = 0;

	private double mean = Double.POSITIVE_INFINITY;

	/**
	 * Event assosiated, if exist. Null if event absent.
	 * 
	 * @uml.property name="events"
	 * @uml.associationEnd inverse="plotDataPoint:com.isti.xmax.common.IEvent"
	 */
	private Set<EventWrapper> events = null;

	public PlotDataPoint(double top, double bottom, double mean, int segmentNumber, int continueAreaNumber, int rawDataProviderNumber, Set<EventWrapper> events) {
		logger.debug("Created:" + this);
		this.top = top;
		this.bottom = bottom;
		this.mean = mean;
		this.segmentNumber = segmentNumber;
		this.continueAreaNumber = continueAreaNumber;
		this.rawDataProviderNumber = rawDataProviderNumber;
		this.events = events;
	}

	/**
	 * Getter of the property <tt>bottom</tt>
	 * 
	 * @return minimum value in representing section
	 * @uml.property name="bottom"
	 */
	public double getBottom() {
		return bottom;
	}

	/**
	 * Getter of the property <tt>top</tt>
	 * 
	 * @return maximum value in representing section
	 * @uml.property name="top"
	 */
	public double getTop() {
		return top;
	}

	/**
	 * @return mean of raw trace data in representing section
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * Getter of the property <tt>segmentNumber</tt>
	 * 
	 * @return number of segment to which this range belongs.
	 * @uml.property name="segmentNumber"
	 */
	public int getSegmentNumber() {
		return segmentNumber;
	}

	/**
	 * Getter of the property <tt>rawDataProviderNumber</tt>
	 * 
	 * @return number of raw data provider to which this range belongs.
	 * @uml.property name="rawDataProviderNumber"
	 */
	public int getRawDataProviderNumber() {
		return rawDataProviderNumber;
	}
	
	/**
	 * Getter of the property <tt>continueAreaNumber</tt>
	 * 
	 * @return number of continue data area to which this range belongs.
	 * @uml.property name="continueAreaNumber"
	 */
	public int getContinueAreaNumber() {
		return continueAreaNumber;
	}

	/**
	 * Getter of the property <tt>events</tt>
	 * 
	 * @return Returns list of events found inside this time range.
	 * @uml.property name="events"
	 */
	public Set<EventWrapper> getEvents() {
		if (events == null) {
			return Collections.synchronizedSortedSet(new TreeSet<EventWrapper>());
		} else {
			return events;
		}
	}

	/**
	 * @return string representation of data point for debug purposes
	 */
	public String toString() {
		return "PlotDataPoint: top " + top + ", mean " + mean + ", bottom " + bottom + "; segment # " + segmentNumber + ", rdp number "
				+ rawDataProviderNumber;
	}
}
