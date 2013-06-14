package com.isti.traceview.gui;

import java.awt.Color;

/**
 * Color mode to color traces by source, color changes at sources boundaries
 * 
 * @author Max Kokoulin
 */
public class ColorModeBySource implements IColorModeState {

	public Color getSegmentColor(int segmentNumber, int sourceNumber, int continueAreaNumber, Color manualColor) {
		return colors[sourceNumber % colors.length];
	}

}
