package com.isti.traceview.gui;

/**
 * State pattern realization for mean mode. Mean mode defines if we need to draw traces as is or
 * demean its.
 */
public interface IMeanState {

	/**
	 * Computes trace value to place on graph
	 * 
	 * @param value
	 *            trace value
	 * @param mean
	 *            trace mean
	 * @return trace value to draw
	 */
	public abstract double getValue(double value, double mean);

}
