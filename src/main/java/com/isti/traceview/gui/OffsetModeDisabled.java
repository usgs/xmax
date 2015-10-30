package com.isti.traceview.gui;

/**
 * State pattern realization for offset mode, offset disabled
 * 
 * @author Max Kokoulin
 */
public class OffsetModeDisabled implements IOffsetState {

	public void setShift(double shift){}
	
	public void increaseStep(){}

	public double getValue(double value, int segmentNumber) {
		return value;
	}
}
