package com.isti.xmax;

import com.isti.traceview.ITimeRangeAdapter;
import com.isti.traceview.common.TimeInterval;
import com.isti.xmax.gui.PhasePanel;
import com.isti.xmax.gui.XMAXframe;

/**
 * Actions performed after time range settings in {@link XMAXGraphPanel}
 * 
 * @author Max Kokoulin
 */

public class XMAXTimeRangeAdapter implements ITimeRangeAdapter {

	/**
	 * Refresh phases after time range setting
	 */
	public void setTimeRange(TimeInterval timeRange) {
		PhasePanel pp = XMAXframe.getInstance().getPhasePanel();
		if (pp != null) {
			pp.refreshAvailableEarthQuakes();
		}
	}

}
