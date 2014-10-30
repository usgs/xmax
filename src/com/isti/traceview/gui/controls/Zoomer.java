package com.isti.traceview.gui.controls;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceView;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.GraphUtil;

/**
 * Set of buttons to zoom in time axis traces into GraphPanel. Has "Zoom", "Unzoom" and auxilary
 * "All data" buttons.
 * 
 * @author Max Kokoulin
 */
public class Zoomer extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Zoomer.class);
	
	private static Insets inset = new Insets(0, 0, 0, 0);
	private static final Dimension maxSize = new Dimension(130, 30);
	private double factor = 1.5;
	private GraphPanel graphPanel = null;
	private JButton zoomInButton = null;
	private JButton zoomOutButton = null;
	private JButton allData = null;

	/**
	 * @param graphPanel
	 *            Graph panel which this element controls
	 * @param factor
	 *            multiplier to zoom/unzoom window
	 * @param showAllData
	 *            flag if we show "All data" button
	 */
	public Zoomer(GraphPanel graphPanel, double factor, boolean showAllData) {
		super();
		setPreferredSize(maxSize);
		setMaximumSize(maxSize);
		if (showAllData) {
			setLayout(new GridLayout(0, 3));
		} else {
			setLayout(new GridLayout(0, 2));
		}
		this.graphPanel = graphPanel;
		if (factor > 1.0) {
			this.factor = factor;
		} else {
			logger.error("Zoom factor = " + factor + " must be greater than 1; Forced to " + this.factor);
		}
		zoomInButton = GraphUtil.createGraphicalButton("zoom_in.gif", "Zoom In");
		zoomInButton.setToolTipText("Zoom In");
		zoomInButton.addActionListener(this);
		zoomInButton.setBorderPainted(true);
		zoomInButton.setMargin(inset);

		zoomOutButton = GraphUtil.createGraphicalButton("zoom_out.gif", "Zoom Out");
		zoomOutButton.setToolTipText("Zoom Out");
		zoomOutButton.setBorderPainted(true);
		zoomOutButton.addActionListener(this);
		zoomOutButton.setMargin(inset);

		add(zoomInButton);
		add(zoomOutButton);
		if (showAllData) {
			allData = GraphUtil.createGraphicalButton("all_data.gif", "All");
			allData.setToolTipText("Show all data");
			allData.setBorderPainted(true);
			allData.addActionListener(this);
			allData.setMargin(inset);
			add(allData);
		}
	}

	/**
	 * Simplified constructor
	 * 
	 * @param graphPanel
	 *            Graph panel which this element controls
	 */
	public Zoomer(GraphPanel graphPanel) {
		this(graphPanel, 1.5, false);
	}

	public void actionPerformed(ActionEvent e) {
		long centerTime = (graphPanel.getTimeRange().getStart() + graphPanel.getTimeRange().getEnd()) / 2;
		long duration = graphPanel.getTimeRange().getDuration();
		if (e.getSource().equals(zoomInButton)) {
			graphPanel.setTimeRange(new TimeInterval(centerTime - new Double(duration / (2 * factor)).longValue(), centerTime
					+ new Double(duration / (2 * factor)).longValue()));
		} else if (e.getSource().equals(zoomOutButton)) {
			graphPanel.setTimeRange(new TimeInterval(centerTime - new Double(duration * factor / 2).longValue(), centerTime
					+ new Double(duration * factor / 2).longValue()));
		} else if (e.getSource().equals(allData)) {
			graphPanel.setTimeRange(TraceView.getDataModule().getAllDataTimeInterval());
		}
	}
}
