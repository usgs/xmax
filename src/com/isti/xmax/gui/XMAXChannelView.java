package com.isti.xmax.gui;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.isti.traceview.CommandExecutor;
import com.isti.traceview.commands.SelectTimeCommand;
import com.isti.traceview.commands.SelectValueCommand;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.ChannelView;
import com.isti.traceview.gui.IMouseAdapter;
import com.isti.traceview.gui.IScaleModeState;
import com.isti.traceview.gui.ScaleModeXhair;
import com.isti.xmax.common.Earthquake;
import com.isti.xmax.common.Pick;

/**
 * Customized {@link ChannelView}
 * 
 * @author Max Kokoulin
 */
public class XMAXChannelView extends ChannelView {

	private static final long serialVersionUID = 1L;

	public XMAXChannelView(List<PlotDataProvider> channels, int infoPanelWidth, boolean isDrawSelectionCheckBox, Color graphAreaBgColor, Color infoAreaBgColor) {
		super(channels, infoPanelWidth, isDrawSelectionCheckBox, graphAreaBgColor, infoAreaBgColor);
		setMouseAdapter(new XMAXChannelViewMouseAdapter());
	}

	public XMAXChannelView(PlotDataProvider channel, int infoPanelWidth, boolean isDrawSelectionCheckBox, Color graphAreaBgColor, Color infoAreaBgColor) {
		super(channel, infoPanelWidth, isDrawSelectionCheckBox, graphAreaBgColor, infoAreaBgColor);
		setMouseAdapter(new XMAXChannelViewMouseAdapter());
	}
}

/**
 * Special mouse adapter to set mouse behavior
 */
class XMAXChannelViewMouseAdapter implements IMouseAdapter {
	private static final Logger logger = Logger.getLogger(XMAXChannelViewMouseAdapter.class);
	public static final DecimalFormat df = new DecimalFormat("#####.##");

	public void mouseClickedButton1(int x, int y, JPanel clickedAt) {
		ChannelView cv = (ChannelView) clickedAt;
		long clickedTime = cv.getGraphPanel().getTime(x);
		logger.debug("ChannelView clicked: " + x + ":" + y + ", time "
				+ TimeInterval.formatDate(new Date(clickedTime), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + "(" + clickedTime + ")"
				+ ", value " + cv.getGraphPanel().getScaleMode().getValue(y));
		double pointAmp = Double.NEGATIVE_INFINITY; // Graph amplitude in the clicked point
		if (cv.getLastClickedY() != Integer.MIN_VALUE) {
			pointAmp = cv.getGraphPanel().getScaleMode().getValue(y) - cv.getGraphPanel().getScaleMode().getValue(cv.getLastClickedY());
		}
		String amp = "";
		if (pointAmp < 0) {
			amp = "-";
			pointAmp = -pointAmp;
		} else {
			amp = "+";
		}
		amp = pointAmp == Double.NEGATIVE_INFINITY ? "" : ":" + amp + new Double(pointAmp).intValue();
		long lastClickedTime = cv.getGraphPanel().getLastClickedTime();
		String diff = lastClickedTime == Long.MAX_VALUE ? "" : " diff " + new TimeInterval(lastClickedTime, clickedTime).convert();
		XMAXframe.getInstance().getStatusBar().setMessage(
				TimeInterval.formatDate(new Date(clickedTime), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + ":"
						+ new Double(cv.getGraphPanel().getScaleMode().getValue(y)).intValue() + diff + amp);

		if (cv.getGraphPanel().getPickState()) {
			PlotDataProvider channel = cv.getPlotDataProviders().get(0);
			channel.addEvent(new Pick(new Date(clickedTime), channel));
			cv.repaint();
		}
	}

	public void mouseClickedButton2(int x, int y, JPanel clickedAt) {
	}

	public void mouseClickedButton3(int x, int y, JPanel clickedAt) {
		ChannelView cv = (ChannelView) clickedAt;
		if (cv.getGraphPanel().getPickState()) {
			long clickedTime = cv.getGraphPanel().getTime(x);
			PlotDataProvider channel = cv.getPlotDataProviders().get(0);
			SortedSet<IEvent> events = channel.getEvents(new Date(clickedTime), cv.getGraphPanel().getTimeRange().getDuration()
					/ cv.getGraphAreaWidth());
			for (IEvent event: events) {
				if (event.getType().equals("PICK")) {
					Pick pick = (Pick) event;
					pick.detach();
				}
			}
			cv.repaint();
		}
	}

	public void mouseMoved(int x, int y, JPanel clickedAt) {
		ChannelView cv = (ChannelView) clickedAt;
		// ToolBar message for event
		String message = null;
		if (cv.getEvents(x) != null) {
			Set<IEvent> events = cv.getEvents(x);
			if (events != null) {
				for (IEvent evt: events) {
					if (evt.getType().equals("ARRIVAL")) {
						message = ((Earthquake) evt.getParameterValue("EARTHQUAKE")).getSourceCode() + ";  Phase: "
								+ (String) evt.getParameterValue("PHASE") + ";  Azimuth: " + df.format((Double) evt.getParameterValue("AZIMUTH"))
								+ ";  Back azimuth: " + df.format((Double) evt.getParameterValue("AZIMUTH_BACK")) + ";  Distance: "
								+ df.format((Double) evt.getParameterValue("DISTANCE"));
					}
				}
			}

		}
		if (message != null) {
			XMAXframe.getInstance().getStatusBar().setMessage(message);
		}
	}

	public void mouseDragged(int x, int y, JPanel clickedAt) {
		ChannelView cv = (ChannelView) clickedAt;
		long selectionTime = cv.getGraphPanel().getSelectionTime();
		String diff = selectionTime == Long.MAX_VALUE ? "" : " diff " + new TimeInterval(selectionTime, cv.getGraphPanel().getTime(x)).convert();
		XMAXframe.getInstance().getStatusBar().setMessage(
				TimeInterval.formatDate(new Date(cv.getGraphPanel().getTime(cv.getMousePressX())), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
						+ ":" + cv.getGraphPanel().getScaleMode().getValue(cv.getMousePressY()) + diff);

	}

	public void mouseReleasedButton1(int x, int y, JPanel clickedAt) {
		Date from;
		Date to;
		ChannelView cv = (ChannelView) clickedAt;
		if (cv.getMousePressX() > x) {
			to = new Date(cv.getGraphPanel().getTime(cv.getMousePressX()));
			from = new Date(cv.getGraphPanel().getTime(x));
		} else {
			from = new Date(cv.getGraphPanel().getTime(cv.getMousePressX()));
			to = new Date(cv.getGraphPanel().getTime(x));
		}
		if (Math.abs(cv.getMousePressX() - x) > 1) {
			// to avoid mouse bounce
			if (to.getTime() > from.getTime()) {
				CommandExecutor.getInstance().execute(new SelectTimeCommand(cv.getGraphPanel(), new TimeInterval(from, to)));
			} else {
				JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Max zoom reached", "Alert", JOptionPane.WARNING_MESSAGE);
			}
		}
		XMAXframe.getInstance().getStatusBar().setMessage("");
	}

	public void mouseReleasedButton3(int x, int y, JPanel clickedAt) {
		ChannelView cv = (ChannelView) clickedAt;
		IScaleModeState scaleMode = cv.getGraphPanel().getScaleMode();
		if (scaleMode instanceof ScaleModeXhair) {
			double from;
			double to;
			if (y > cv.getMousePressY()) {
				to = scaleMode.getValue(cv.getMousePressY());
				from = scaleMode.getValue(y);
			} else {
				from = scaleMode.getValue(cv.getMousePressY());
				to = scaleMode.getValue(y);
			}
			if (Math.abs(cv.getMousePressY() - y) > 1) {
				// to avoid mouse bounce
				if (from != to) {
					CommandExecutor.getInstance().execute(new SelectValueCommand(cv.getGraphPanel(), from, to));
				} else {
					JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Please select non-null Y range", "Warning", JOptionPane.WARNING_MESSAGE);
				}
			}
		}
		XMAXframe.getInstance().getStatusBar().setMessage("");
	}
}
