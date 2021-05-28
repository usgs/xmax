package com.isti.traceview.gui;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import java.text.DecimalFormat;
import java.util.List;

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
		DecimalFormat df = new DecimalFormat("#.###E0");
		for (ChannelView view: allViews) {
			for (PlotData data: view.getPlotData()) {
				double meanMaxValue = meanState.getValue(data.getMaxValue(), data.getMeanValue());
				double meanMinValue = meanState.getValue(data.getMinValue(), data.getMeanValue());
				if (maxValue < meanMaxValue || maxValue == Double.NEGATIVE_INFINITY) {
					maxValue = Double.parseDouble(df.format(meanMaxValue));
				}
				if (minValue > meanMinValue || minValue == Double.POSITIVE_INFINITY) {
					minValue = Double.parseDouble(df.format(meanMinValue));
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
