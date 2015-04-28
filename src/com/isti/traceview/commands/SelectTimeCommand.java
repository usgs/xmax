package com.isti.traceview.commands;

import org.apache.log4j.Logger;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.UndoException;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.gui.GraphPanel;

/**
 * This command selects time range, i.e sets desired time range to graph panel
 * 
 * @author Max Kokoulin
 */
public class SelectTimeCommand extends AbstractUndoableCommand {
	private static final Logger logger = Logger.getLogger(SelectTimeCommand.class); // @jve:decl-index=0:

	private GraphPanel graphPanel = null;
	private TimeInterval previousRange = null;
	private TimeInterval ti = null;

	/**
	 * @param gp
	 *            target graph panel
	 * @param ti -
	 *            desired time range
	 */
	public SelectTimeCommand(GraphPanel gp, TimeInterval ti) {
		this.ti = ti;
		this.graphPanel = gp;
		this.previousRange = graphPanel.getTimeRange();
	}

	public void run() {
		try {
			super.run();
			graphPanel.setTimeRange(ti);
		} catch (Exception e) {
			logger.error("Exception:", e);
		}
	}

	public void undo() {
		try {
			super.undo();
			if (previousRange.getStartTime().getTime() != Long.MAX_VALUE || previousRange.getEndTime().getTime() != Long.MIN_VALUE) {
				//logger.info("== undo(): Previous range: " + previousRange.toString() + "\n");	
				graphPanel.setTimeRange(previousRange);
			}
		} catch (UndoException e) {
			// do nothing
			logger.error("UndoException:", e);	
		}
	}

	public boolean canUndo() {
		if (previousRange == null)
			return false;
		else
			return true;
	}
}
