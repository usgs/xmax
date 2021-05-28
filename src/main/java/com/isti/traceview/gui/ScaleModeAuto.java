package com.isti.traceview.gui;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import java.text.DecimalFormat;
import java.util.List;


/**
 * State pattern realization for scale mode, AUTO scaling. It means that value axis in each channel view scales to
 * to maximum and minimum values of traces loaded in this channel view on the visible time range
 * 
 * @author Max Kokoulin
 */
public class ScaleModeAuto extends ScaleModeAbstract implements IScaleModeState {

	public void init(List<PlotData> graphs, List<ChannelView> allViews, TimeInterval timeRange, IMeanState meanState, int height) {
		maxValue = Double.NEGATIVE_INFINITY;
		double minValue = Double.POSITIVE_INFINITY;
		DecimalFormat df = new DecimalFormat("#.#####E0");
		for (PlotData data: graphs) {
			double dataMaxValue = meanState.getValue(data.getMaxValue(), data.getMeanValue());
			if (dataMaxValue > maxValue) {
				maxValue = Double.parseDouble(df.format(dataMaxValue));
			}
			double dataMinValue = meanState.getValue(data.getMinValue(), data.getMeanValue());
			if (dataMinValue < minValue) {
				minValue = Double.parseDouble(df.format(dataMinValue));
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
		return "AUTO";
	}
}
