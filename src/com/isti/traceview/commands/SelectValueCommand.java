package com.isti.traceview.commands;

import org.apache.log4j.Logger;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.UndoException;
import com.isti.traceview.gui.GraphPanel;

/**
 * This command selects values range, i.e sets desired range of Y values on axis
 * 
 * @author Max Kokoulin
 */
public class SelectValueCommand extends AbstractUndoableCommand {
	private static final Logger logger = Logger.getLogger(SelectValueCommand.class); // @jve:decl-index=0:

	private GraphPanel graphPanel = null;
	private double previousMax;
	private double previousMin;
	private double max;
	private double min;

	/**
	 * @param gp
	 *            target graph panel
	 * @param min
	 *            minimum Y axis value
	 * @param max
	 *            maximum Y axis value
	 */
	public SelectValueCommand(GraphPanel gp, double min, double max) {
		this.max = max;
		this.min = min;
		this.graphPanel = gp;
		this.previousMax = graphPanel.getManualValueMax();
		this.previousMin = graphPanel.getManualValueMin();
	}

	public void run() {
		try {
			super.run();
			graphPanel.setManualValueMax(max);
			graphPanel.setManualValueMin(min);
		} catch (Exception e) {
			logger.error("Exception:", e);
		}
	}

	public void undo() throws UndoException {
		try {
			super.undo();
			graphPanel.setManualValueMax(previousMax);
			graphPanel.setManualValueMin(previousMin);
		} catch (UndoException e) {
			// do nothing
			logger.error("UndoException:", e);	
		}
	}

	public boolean canUndo() {
		// if (previousMax == Integer.MIN_VALUE || previousMin == Integer.MAX_VALUE)
		// return false;
		// else
		return true;
	}
}
