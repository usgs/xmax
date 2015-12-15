package com.isti.traceview.gui;

import java.awt.Color;

/**
 *Gray color mode, all traces drawn in gray.
 * 
 */

public class ColorModeGray implements IColorModeState {

	public Color getSegmentColor(int segmentNumber, int rdpNumber, int continueAreaNumber, Color manualColor) {
		return Color.GRAY;
	}
}
