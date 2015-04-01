package com.isti.xmax.gui;

import java.awt.event.MouseEvent;
import java.util.SortedSet;
import java.util.TreeSet;

import com.isti.traceview.common.IEvent;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.ScaleModeXhair;
import com.isti.xmax.XMAXTimeRangeAdapter;
import com.isti.xmax.data.XMAXChannel;

/**
 * Customized {@link GraphPanel}
 * 
 * @author Max Kokoulin
 */
public class XMAXGraphPanel extends GraphPanel {

	private static final long serialVersionUID = 1L;

	public XMAXGraphPanel() {
		super();
		setMouseAdapter(new XMAXGraphPanelMouseAdapter());
		setChannelViewFactory(new XMAXChannelViewFactory());
		setTimeRangeAdapter(new XMAXTimeRangeAdapter());
	}

	/**
	 * @return Returns array of available earthquakes which phases we can see in loaded channels in
	 *         given time range.
	 */
	public Object[] getAvailableEarthquakes() {
		SortedSet<IEvent> ret = new TreeSet<IEvent>();
		for (PlotDataProvider channel: getChannelSet()) {
			XMAXChannel ch = (XMAXChannel) channel;
			for (IEvent earthquake: ch.getAvailableEarthquakes(getTimeRange())) {
				ret.add(earthquake);
			}
		}
		return ret.toArray();
	}

	/**
	 * @return Returns array of available phases for all channels loaded for given set of
	 *         earthquakes.
	 */
	public Object[] getAvailablePhases(Object[] earthquakes) {
		SortedSet<String> ret = new TreeSet<String>();
		for (PlotDataProvider channel: getChannelSet()) {
			XMAXChannel ch = (XMAXChannel) channel;
			for (String phase: ch.getAvailablePhases(getTimeRange(), earthquakes)) {
				ret.add(phase);
			}
		}
		return ret.toArray();
	}

	// selection behavior
	public void mouseDragged(MouseEvent e) {
		// lg.debug("XMAXGraphPanel.mouseDragged");
		if (button == MouseEvent.BUTTON1) {
			setSelectionX(getSelectionTime(), getTime(e.getX() - channelViewFactory.getInfoAreaWidth() - getInsets().left));
		} else if (button == MouseEvent.BUTTON3 && getScaleMode() instanceof ScaleModeXhair) {
			setSelectionY(getScaleMode().getValue(mousePressY), getScaleMode().getValue(e.getY()));
		}
		super.mouseDragged(e);
	}

	public void mouseClicked(MouseEvent e) {
		// lg.debug("XMAXGraphPanel.mouseClicked");
		setSelectionX(Long.MAX_VALUE, Long.MIN_VALUE);
		setSelectionY(Double.NaN, Double.NaN);
		super.mouseClicked(e);
	}

	public void mousePressed(MouseEvent e) {
		// lg.debug("XMAXGraphPanel.mousePressed");
		setSelectionX(Long.MAX_VALUE, Long.MIN_VALUE);
		setSelectionY(Double.NaN, Double.NaN);
		super.mousePressed(e);
	}

	public void mouseReleased(MouseEvent e) {
		// lg.debug("XMAXGraphPanel.mouseReleased");
		setSelectionX(Long.MAX_VALUE, Long.MIN_VALUE);
		setSelectionY(Double.NaN, Double.NaN);
		super.mouseReleased(e);
	}

}
