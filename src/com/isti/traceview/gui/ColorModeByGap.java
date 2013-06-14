package com.isti.traceview.gui;

import java.awt.Color;

/**
 * Color mode to color traces by gaps, color changes at gaps
 * 
 * @author Max Kokoulin
 */

public class ColorModeByGap implements IColorModeState {

	public Color getSegmentColor(int segmentNumber, int rdpNumber, int continueAreaNumber, Color manualColor) {
		return colors[continueAreaNumber % colors.length];
	}
}
