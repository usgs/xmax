package com.isti.traceview.gui;

/**
 * State pattern realization for mean mode, meaning disabled
 * 
 * @author Max Kokoulin
 */
public class MeanModeDisabled implements IMeanState {

	public double getValue(double value, double mean) {
		return value;
	}
}
