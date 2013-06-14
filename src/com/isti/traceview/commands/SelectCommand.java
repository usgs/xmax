package com.isti.traceview.commands;

import org.apache.log4j.Logger;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.gui.GraphPanel;

/**
 * This command selects choosed channels, i.e hides all unchoosed channels on graph panel
 * 
 * @author Max Kokoulin
 */
public class SelectCommand extends AbstractUndoableCommand {
	private static Logger lg = Logger.getLogger(SelectCommand.class);
	private GraphPanel graphPanel = null;

	/**
	 * @param gp
	 *            target graph panel
	 */
	public SelectCommand(GraphPanel gp) {
		this.graphPanel = gp;
	}

	public void run() {
		try {
			super.run();
			graphPanel.select();
		} catch (Exception e) {
			lg.error("SelectCommand error: " + e);
		}
	}

	public void undo() {
		try {
			super.undo();
			if (graphPanel.getSelectState()) {
				graphPanel.select();
			}
		} catch (Exception e) {
			// do nothing
		}
	}

	public boolean canUndo() {
		return true;
	}
}
