package com.isti.traceview.commands;

import org.apache.log4j.Logger;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.gui.GraphPanel;

/**
 * This command sets the desired time range to graph panel
 * these methods do not extend the Java Thread class
 * (**NOTE: This is based on SelectTimeCommand.java)
 * 
 * @author Alejandro Gonzales
 */

public class TimeRangeCommand {
	private static final Logger logger = Logger.getLogger(TimeRangeCommand.class);

	private GraphPanel graphPanel = null;
	private TimeInterval previousRange = null;

	/**
	 * @param gp
	 * 		target graph panel
	 */
	public TimeRangeCommand(GraphPanel gp) {
		this.graphPanel = gp;
		this.previousRange = graphPanel.getTimeRange();
		System.out.println("TimeRangeCommand: Prv Time Range: " + 
			previousRange.toString() + "\n");
	}

	/**
	 * @param ti
	 * 		desired time range for zoom
	 */
	public void setRange(TimeInterval ti) { 
		try {	
			System.out.println("TimeRangeCommand.setRange(): TI: " + ti.toString() + 
				" --> graphPanel.setTimeRange()\n");
			graphPanel.setTimeRange(ti);
		} catch (Exception e) {
			logger.error("Exception:", e);
		}
	}
}
