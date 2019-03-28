package com.isti.traceview.commands;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.OffsetModeDisabled;
import com.isti.traceview.gui.OffsetModeEnabled;

/**
 * This command changes offset mode
 * 
 * @author Max Kokoulin
 */
public class OffsetCommand extends AbstractUndoableCommand {

	private GraphPanel graphPanel = null;

	/**
	 * @param gp
	 *            target graph panel
	 * @param gain
	 *            gain to remove
	 */
	public OffsetCommand(GraphPanel gp) {
		this.graphPanel = gp;
	}

	public void run() {
		super.run();
		graphPanel.setOffsetState(new OffsetModeEnabled());
		graphPanel.getOffsetState().increaseStep();
	}
	
	public void undo() {
		super.undo();
		graphPanel.setOffsetState(new OffsetModeDisabled());
	}
	
	public boolean canUndo() {
		// TODO Auto-generated method stub
		return true;
	}

}
