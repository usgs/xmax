package com.isti.traceview.gui;

import java.awt.Color;

/**
 * Interface to represent colouring mode, i.e. mean to colorize trace graphics
 * 
 * @author Max Kokoulin
 */
public interface IColorModeState {
	Color[] colors = new Color[]{ Color.RED, Color.BLACK, Color.CYAN, Color.GREEN, Color.GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK,
			Color.YELLOW, Color.BLUE };

	/**
	 * @param segmentNumber
	 *            segment ordinal number in trace
	 * @param sourceNumber
	 *            ordinal number of segment's data source in raw data provider
	 * @return Color to draw segment
	 */
  Color getSegmentColor(int segmentNumber, int sourceNumber, int continueAreaNumber,
      Color manualColor);
}
