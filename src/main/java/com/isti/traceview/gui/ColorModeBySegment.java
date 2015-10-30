package com.isti.traceview.gui;

import java.awt.Color;

/**
 * Color mode to color traces by segment, color changes at gaps and overlaps
 * 
 * @author Max Kokoulin
 */

public class ColorModeBySegment implements IColorModeState {

	public Color getSegmentColor(int segmentNumber, int rdpNumber, int continueAreaNumber, Color manualColor) {
		return colors[segmentNumber % colors.length];
	}
}
