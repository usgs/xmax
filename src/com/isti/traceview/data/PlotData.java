package com.isti.traceview.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents list of {@link PlotDataPoint}s prepared to render whole graph
 * 
 * @author Max Kokoulin
 */

public class PlotData implements Comparable<Object> {

	/**
	 * List of arrays of data points. PlotDataPoint[] contains one data points of several overlayed segments.
	 * 
	 * @uml.property name="plotDataPoints" multiplicity="(0 -1)" dimension="1"
	 */
	private List<PlotDataPoint[]> pixels;

	/*
	 * Max and min boundaries of data range - to not iterate twice during repaint
	 */
	private double max = Double.NEGATIVE_INFINITY;
	private double min = Double.POSITIVE_INFINITY;
	// To calculate mean
	private double sum = 0;
	// To calculate mean amplitude
	private double ampSum = 0;
	private int filledPointCount = 0;
	private String label = null;
	private Color labelColor = null;
	private Color traceColor = null;

	/**
	 * @param label
	 *            This label will be printed on graph
	 */
	public PlotData(String label, Color traceColor) {
		pixels = new ArrayList<PlotDataPoint[]>();
		this.label = label;
		this.traceColor = traceColor;
	}

	/**
	 * Getter of the property <tt>points</tt>
	 * 
	 * @return Returns list of points.
	 * @uml.property name="points"
	 */
	public List<PlotDataPoint[]> getPixels() {
		return pixels;
	}

	// PlotDataPoint is filled when top and bottom differs from init values.
	// mean - mean value on interval corresponding this data point

	/**
	 * Add data point to this list
	 */
	public void addPixel(PlotDataPoint[] pixelPoints) {
		pixels.add(pixelPoints);
		for(PlotDataPoint point:pixelPoints){
			if (point.getMean() != Double.POSITIVE_INFINITY) {
				sum = sum + point.getMean();
				ampSum = ampSum + (point.getTop() - point.getBottom());
				filledPointCount++;
			}
			if (point.getTop() > max) {
				max = point.getTop();
			}
			if (point.getBottom() < min) {
				min = point.getBottom();
			}
		}
	}

	/**
	 * @return max data value in whole data set
	 */
	public double getMaxValue() {
		return max;
	}

	/**
	 * @return min data value in whole data set
	 */
	public double getMinValue() {
		return min;
	}

	/**
	 * @return mean data value in whole data set
	 */
	public double getMeanValue() {
		if (filledPointCount == 0) {
			return Double.POSITIVE_INFINITY;
		} else {
			return sum / filledPointCount;
		}
	}

	/**
	 * @return mean amplitude (max-min on each plot data point) in whole data set
	 */
	public double getMeanAmpValue() {
		if (filledPointCount == 0) {
			return Double.POSITIVE_INFINITY;
		} else {
			return ampSum / filledPointCount;
		}
	}

	/**
	 * @return count of points
	 */
	public int getPointCount() {
		return pixels.size();
	}

	/**
	 * @return label to print on graph
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return color to draw label
	 */
	public Color getLabelColor() {
		return labelColor;
	}
	
	/**
	 * @param color
	 *            to draw label
	 */
	public void setLabelColor(Color color) {
		labelColor = color;
	}

	/**
	 * @param color
	 *            to draw trace in manual mode
	 */
	public void setTraceColor(Color color) {
		traceColor = color;
	}
	
	/**
	 * @return color to draw trace in manual mode
	 */
	public Color getTraceColor() {
		return traceColor;
	}

	public String toString() {
		return "PlotData: name " + getLabel() + ", max " + getMaxValue() + ", min " + getMinValue() + ", mean " + getMeanValue();
	}

	/**
	 * Comparator to sort collection of PlotData by mean amplitude
	 */
	public int compareTo(Object o) {
		if ((o instanceof PlotData)) {
			PlotData pd = (PlotData) o;
			if (this.getMeanAmpValue() == pd.getMeanAmpValue()) {
				return 0;
			} else {
				if (this.getMeanAmpValue() < pd.getMeanAmpValue()) {
					return 1;
				} else {
					return -1;
				}
			}
		} else {
			return -1;
		}
	}
}
