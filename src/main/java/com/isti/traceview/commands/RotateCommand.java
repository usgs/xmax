package com.isti.traceview.commands;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.processing.Rotation;
import java.util.List;

/**
 * This command performs rotation
 */
public class RotateCommand extends AbstractUndoableCommand {
	private List<PlotDataProvider> plotDataProviders;
	private GraphPanel graphPanel; //in order to notify the graph panel to repaint since the data provider was modified.
	private Rotation rotation;

	/**
	 * @param pdpsToRotate
	 * 						traces that will be rotated
	 * @param graphPanel
	 *            target graph panel
	 * @param rotation
	 *            rotation to perform
	 */
	public RotateCommand(List<PlotDataProvider> pdpsToRotate, GraphPanel graphPanel, Rotation rotation) {
		this.plotDataProviders = pdpsToRotate;
		this.graphPanel = graphPanel;
		this.rotation = rotation;
	}

	public void run() {
		for(PlotDataProvider pdp : plotDataProviders) {
			pdp.setRotation(rotation);
			graphPanel.getListener().firePropertyChange("rotate command", null, this);
			// graphPanel.getListener().notifyObservers("ROT");
			graphPanel.forceRepaint();
		}
	}
	
	public void undo() {
	}

	public boolean canUndo() {
		return false; //undo using toggle button feature
	}
}
