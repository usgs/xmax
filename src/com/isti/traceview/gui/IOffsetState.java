package com.isti.traceview.gui;

/**
 * State pattern realization for offset mode. Offset mode defines if we need to draw traces as is or
 * draw each segments with vertical shift to highlight gaps.
 */
public interface IOffsetState {
	/**
	 * Sets vertical shift size for one step
	 * 
	 * @param shift
	 *            Shift in pixels
	 */
	public void setShift(double shift);
	
	/**
	 * increase count of steps. Resulting segments offset will be shift*step.
	 * @return
	 */
	public void increaseStep();

	/**
	 * Computes trace value to place on graph
	 * 
	 * @param value
	 *            trace value
	 * @param segmentNumber
	 *            segment's ordinary number
	 * @return trace value to draw
	 */
	public double getValue(double value, int segmentNumber);
}
