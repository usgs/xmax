package com.isti.traceview.gui;

import java.awt.Color;

/**
 * Fixed color mode, traces drawn in fixed color stored in each trace.
 * 
 * @author Max Kokoulin
 */

public class ColorModeFixed implements IColorModeState {
	
	public Color getSegmentColor(int segmentNumber, int rdpNumber, int continueAreaNumber, Color manualColor) {
		return manualColor;
	}
}
