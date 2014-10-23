package com.isti.traceview.gui;

/**
 * State pattern realization for mean mode, meaning enabled
 * 
 * @author Max Kokoulin
 */
public class MeanModeEnabled implements IMeanState {

	public double getValue(double value, double mean) {
		if (mean == Double.POSITIVE_INFINITY || mean == Double.NEGATIVE_INFINITY)
			return value;
		if (value == Double.POSITIVE_INFINITY)
			return Double.POSITIVE_INFINITY;
		else if (value == Double.NEGATIVE_INFINITY)
			return Double.NEGATIVE_INFINITY;
		else
			return value - mean;
	}
}
