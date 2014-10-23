package com.isti.traceview.gui;

import java.awt.Color;

/**
 * Black and white color mode, all traces drawn in black.
 * 
 * @author Max Kokoulin
 */

public class ColorModeBW implements IColorModeState {

	public Color getSegmentColor(int segmentNumber, int rdpNumber, int continueAreaNumber, Color manualColor) {
		return Color.BLACK;
	}
}
