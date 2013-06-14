package com.isti.traceview.commands;

import org.apache.log4j.Logger;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.processing.Rotation;

/**
 * This command performs rotation
 * 
 * @author Max Kokoulin
 */
public class RotateCommand extends AbstractUndoableCommand {
	private static Logger lg = Logger.getLogger(SelectCommand.class);
	private GraphPanel graphPanel = null;
	private Rotation previous = null;
	private Rotation rotation = null;

	/**
	 * @param gp
	 *            target graph panel
	 * @param rotation
	 *            rotation to perform
	 */
	public RotateCommand(GraphPanel gp, Rotation rotation) {
		this.graphPanel = gp;
		this.rotation = rotation;
		this.previous = graphPanel.getRotation();
	}

	public void run() {
		super.run();
		lg.debug("Rotation command: " + rotation);
		graphPanel.setRotation(rotation);
	}

	public void undo() {
		super.undo();
		graphPanel.setRotation(previous);
	}

	public boolean canUndo() {
		return true;
	}
}
