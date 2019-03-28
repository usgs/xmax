package com.isti.traceview.gui;

/**
 * Abstract class for state pattern realization for scale mode. Each concrete scale mode should
 * extend this class ChannelView
 * 
 * @author Max Kokoulin
 */
public abstract class ScaleModeAbstract implements IScaleModeState {
	protected double maxValue = Double.NEGATIVE_INFINITY;
	protected double amp = 0.0;
	protected int height = 0;

	private static double manualValueMax = Double.NEGATIVE_INFINITY;
	private static double manualValueMin = Double.POSITIVE_INFINITY;

	/*
	 * Returns channel value Input: Y screen panel coordinate
	 */

	public double getValue(int y) {
		return maxValue - y * (amp / (double) height);
	}

	public int getY(double value) {
		return new Double(height * ((maxValue - value) / (amp))).intValue();
	}

	public double getMaxValue() {
		return maxValue;
	}

	public double getMinValue() {
		return maxValue - amp;
	}

	public static double getManualValueMax() {
		return manualValueMax;
	}

	public static double getManualValueMin() {
		return manualValueMin;
	}

	public static void setManualValueMax(double val) {
		manualValueMax = val;
	}

	public static void setManualValueMin(double val) {
		manualValueMin = val;
	}
}
