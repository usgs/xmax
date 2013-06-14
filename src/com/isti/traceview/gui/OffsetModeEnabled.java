package com.isti.traceview.gui;

/**
 * State pattern realization for offset mode, offset enabled
 * 
 * @author Max Kokoulin
 */

public class OffsetModeEnabled implements IOffsetState {
	private double shift = 0;
	private int steps = 1;
	
	public void setShift(double shift){
		this.shift = shift;		
	}

	public void increaseStep(){
		steps++;
	}
	
	public double getValue(double value, int segmentNumber) {
		int sig = segmentNumber % 2;
		if(sig==0)	return value + steps*shift;
		else return value - steps*shift;
	}
}
