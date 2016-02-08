package com.isti.traceview.gui;

import java.text.DecimalFormat;
import java.util.List;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import com.isti.traceview.gui.ScaleModeAbstract;

/**
 * State pattern realization for scale mode, XHAIR scaling. It means that maximum and minimum values
 * for value axis set up manually, and they are the same for all channel views.
 * 
 * @author Max Kokoulin
 */
public class ScaleModeXhair extends ScaleModeAbstract implements IScaleModeState {

	public void init(List<PlotData> graphs, List<ChannelView> allViews, TimeInterval timeRange, IMeanState meanState, int height) {
		maxValue = Double.NEGATIVE_INFINITY;
		double minValue = Double.POSITIVE_INFINITY;
		DecimalFormat df = new DecimalFormat("#.###E0");
		for (PlotData data: graphs) {
			if (data.getMeanValue() == Double.POSITIVE_INFINITY || data.getMeanValue() == Double.NEGATIVE_INFINITY) {
				maxValue = Double.POSITIVE_INFINITY;
				minValue = Double.NEGATIVE_INFINITY;
			} else {
				double dataMaxValue = meanState.getValue(data.getMaxValue(), data.getMeanValue());
				if (dataMaxValue > maxValue) {
					maxValue = Double.valueOf(df.format(dataMaxValue));
				}
				double dataMinValue = meanState.getValue(data.getMinValue(), data.getMeanValue());
				if (dataMinValue < minValue) {
					minValue = Double.valueOf(df.format(dataMinValue));
				}
			}
		}
		if ((getManualValueMax() != Double.NEGATIVE_INFINITY) && (getManualValueMin() != Double.POSITIVE_INFINITY)) {
			maxValue = getManualValueMax();
			minValue = getManualValueMin();
		}
		if (maxValue == minValue) {
			amp = 100.0;
		} else {
			amp = maxValue - minValue;
		}
		this.height = height;
	}

	public String getStateName() {
		return "XHAIR";
	}
}
