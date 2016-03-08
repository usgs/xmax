package com.isti.traceview.commands;

import java.util.ArrayList;
import java.util.List;

import com.isti.traceview.AbstractUndoableCommand;
import com.isti.traceview.UndoException;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.GraphPanel.GraphPanelObservable;
import com.isti.traceview.processing.Rotation;

/**
 * This command performs rotation
 */
public class RotateCommand extends AbstractUndoableCommand {
	private List<PlotDataProvider> plotDataProviders = new ArrayList<PlotDataProvider>();
	private GraphPanel graphPanel = null; //in order to notify the graph panel to repaint since the data provider was modified.
	private Rotation rotation = null;

	/**
	 * @param gp
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
			((GraphPanelObservable) graphPanel.getObservable()).setChanged();
			((GraphPanelObservable) graphPanel.getObservable()).notifyObservers("ROT");
			graphPanel.forceRepaint();
		}
	}
	
	public void undo() throws UndoException{
	}

	public boolean canUndo() {
		return false; //undo using toggle button feature
	}
}
