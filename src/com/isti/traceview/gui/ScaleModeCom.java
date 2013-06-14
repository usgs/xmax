package com.isti.traceview.gui;

import java.util.List;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import com.isti.traceview.gui.ScaleModeAbstract;

/**
 * State pattern realization for scale mode, COM scaling. It means that value axis in all channel
 * views scales to the same maximum and minimum values. Maximum and minimum searches among all
 * loaded in graph panel traces (in all channel views) in visible time range.
 * 
 * @author Max Kokoulin
 */
public class ScaleModeCom extends ScaleModeAbstract implements IScaleModeState {

	public void init(List<PlotData> graphs, List<ChannelView> allViews, TimeInterval timeRange, IMeanState meanState, int height) {
		maxValue = Double.NEGATIVE_INFINITY;
		double minValue = Double.POSITIVE_INFINITY;
		for (ChannelView view: allViews) {
			for (PlotData data: view.getPlotData()) {
				if (maxValue < data.getMaxValue() || maxValue == Double.NEGATIVE_INFINITY) {
					maxValue = data.getMaxValue();
				}
				if (minValue > data.getMinValue() || minValue == Double.POSITIVE_INFINITY) {
					minValue = data.getMinValue();
				}
			}
		}
		if (maxValue == minValue) {
			amp = 100.0;
		} else {
			amp = maxValue - minValue;
		}
		this.height = height;
	}

	public String getStateName() {
		return "COM";
	}
}
