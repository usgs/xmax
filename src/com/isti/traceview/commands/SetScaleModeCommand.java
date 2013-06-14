package com.isti.traceview.commands;

import org.apache.log4j.Logger;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.IScaleModeState;

/**
 * This command sets desired scale mode for graph panel
 * 
 * @see IScaleModeState
 * @author Max Kokoulin
 */
public class SetScaleModeCommand extends AbstractUndoableCommand {
	private static Logger lg = Logger.getLogger(SetScaleModeCommand.class);
	GraphPanel graphPanel = null;
	IScaleModeState state = null;
	IScaleModeState prevState = null;

	/**
	 * @param gp
	 *            target graph panel
	 * @param state
	 *            desired scale mode
	 */

	public SetScaleModeCommand(GraphPanel gp, IScaleModeState state) {
		this.graphPanel = gp;
		prevState = graphPanel.getScaleMode();
		this.state = state;
	}

	public void run() {
		super.run();
		graphPanel.setScaleMode(state);
	}

	public void undo() {
		try {
			super.undo();
			graphPanel.setScaleMode(prevState);
		} catch (Exception e) {
			lg.error("SetScaleModeCommand error: " + e);
		}
	}

	public boolean canUndo() {
		return prevState != null;
	}
}
