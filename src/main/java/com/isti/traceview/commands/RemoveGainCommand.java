package com.isti.traceview.commands;

import org.apache.log4j.Logger;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.UndoException;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.processing.RemoveGain;

/**
 * This command that removes the gain
 * 
 * @author Nick Falco
 */
public class RemoveGainCommand extends AbstractUndoableCommand {
	private static final Logger logger = Logger.getLogger(RemoveGainCommand.class);
	private GraphPanel graphPanel = null;
	private RemoveGain gain = null;

	/**
	 * @param gp
	 *            target graph panel
	 * @param gain
	 *            gain to remove
	 */
	public RemoveGainCommand(GraphPanel gp, RemoveGain gain) {
		this.graphPanel = gp;
		this.gain = gain;
	}

	public void run() {
		super.run();
		logger.debug("Remove gain command: " + gain);
		graphPanel.setRemoveGainState(gain);
	}

	public void undo() throws UndoException{
		super.undo();
		graphPanel.setRemoveGainState(new RemoveGain(false));
	}

	public boolean canUndo() {
		return false;
	}
}
