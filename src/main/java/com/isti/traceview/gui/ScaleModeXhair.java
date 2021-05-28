package com.isti.traceview.gui;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import java.text.DecimalFormat;
import java.util.List;

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
					maxValue = Double.parseDouble(df.format(dataMaxValue));
				}
				double dataMinValue = meanState.getValue(data.getMinValue(), data.getMeanValue());
				if (dataMinValue < minValue) {
					minValue = Double.parseDouble(df.format(dataMinValue));
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

	// override super method in order to prevent range argument exception if max is somehow < min
	// since this is entirely user-specified scaling based on GUI, this is clearly preferable
	public double getMaxValue() {
		return Math.max(super.getMaxValue(), super.getMinValue());
	}

	// same as above, prevents issue where assigned min value is somehow > max
	public double getMinValue() {
		return Math.min(super.getMaxValue(), super.getMinValue());
	}

	public String getStateName() {
		return "XHAIR";
	}
}
