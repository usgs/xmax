package com.isti.traceview.commands;

import org.apache.log4j.Logger;
import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.gui.GraphPanel;

/**
 * This command changes overlay mode
 * 
 * @author Max Kokoulin
 */
public class OverlayCommand extends AbstractUndoableCommand {
	private static Logger lg = Logger.getLogger(OverlayCommand.class);
	private GraphPanel graphPanel = null;

	/**
	 * @param gp
	 *            target graph panel
	 */
	public OverlayCommand(GraphPanel gp) {
		this.graphPanel = gp;
	}

	public void run() {
		try {
			super.run();
			graphPanel.overlay();
		} catch (Exception e) {
			lg.error("OverlayCommand error: " + e);
		}
	}

	public void undo() {
		try {
			super.undo();
			if (graphPanel.getOverlayState()) {
				graphPanel.overlay();
			}
		} catch (Exception e) {
			// do nothing
		}
	}

	public boolean canUndo() {
		return true;
	}
}
