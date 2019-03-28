package com.isti.traceview.gui;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import java.util.List;

/**
 * State pattern realization for scale mode. Scale mode defines method of trace scaling inside each
 * ChannelView
 */
public interface IScaleModeState {
	/**
	 * Should be called before other methods using
	 * 
	 * @param graphs
	 *            List {@link PlotData} for current ChannelView
	 * @param allViews
	 *            List {@link ChannelView} All views for graph panel
	 * @param timeRange
	 *            TimeInterval set in graph panel
	 * @param meanState
	 *            current mean state
	 * @param height
	 *            height of current view, in pixels
	 */
  void init(List<PlotData> graphs, List<ChannelView> allViews, TimeInterval timeRange,
      IMeanState meanState, int height);

	/**
	 * @param value
	 *            trace value
	 * @return Y screen panel coordinate to draw
	 */
  int getY(double value);

	/**
	 * @param y
	 *            Y screen panel coordinate
	 * @return trace value
	 */
  double getValue(int y);

	/**
	 * @return name of state
	 */
  String getStateName();

	/**
	 * @return max value for all traces in current ChannelView
	 */
  double getMaxValue();

	/**
	 * @return min value for all traces in current ChannelView
	 */
  double getMinValue();
}
