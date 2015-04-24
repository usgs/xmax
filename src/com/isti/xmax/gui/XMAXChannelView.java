package com.isti.xmax.gui;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.isti.traceview.ExecuteCommand;
import com.isti.traceview.CommandHandler;
import com.isti.traceview.commands.SelectTimeCommand;
import com.isti.traceview.commands.SelectValueCommand;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.ChannelView;
import com.isti.traceview.gui.GraphPanel;
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
		GraphPanel graphPanel = cv.getGraphPanel();	
		long clickedTime = graphPanel.getTime(x);
		logger.debug("ChannelView clicked: " + x + ":" + y + ", time "
				+ TimeInterval.formatDate(new Date(clickedTime), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + "(" + clickedTime + ")"
				+ ", value " + graphPanel.getScaleMode().getValue(y));
		double pointAmp = Double.NEGATIVE_INFINITY; // Graph amplitude in the clicked point
		if (cv.getLastClickedY() != Integer.MIN_VALUE) {
			pointAmp = graphPanel.getScaleMode().getValue(y) - graphPanel.getScaleMode().getValue(cv.getLastClickedY());
		}
		String amp = "";
		if (pointAmp < 0) {
			amp = "-";
			pointAmp = -pointAmp;
		} else {
			amp = "+";
		}
		amp = pointAmp == Double.NEGATIVE_INFINITY ? "" : ":" + amp + new Double(pointAmp).intValue();
		long lastClickedTime = graphPanel.getLastClickedTime();
		String diff = lastClickedTime == Long.MAX_VALUE ? "" : " diff " + new TimeInterval(lastClickedTime, clickedTime).convert();
		XMAXframe.getInstance().getStatusBar().setMessage(
				TimeInterval.formatDate(new Date(clickedTime), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + ":"
						+ new Double(graphPanel.getScaleMode().getValue(y)).intValue() + diff + amp);

		if (graphPanel.getPickState()) {
			PlotDataProvider channel = cv.getPlotDataProviders().get(0);
			channel.addEvent(new Pick(new Date(clickedTime), channel));
			cv.repaint();
		}
	}

	public void mouseClickedButton2(int x, int y, JPanel clickedAt) {
	}

	public void mouseClickedButton3(int x, int y, JPanel clickedAt) {
		ChannelView cv = (ChannelView) clickedAt;
		GraphPanel graphPanel = cv.getGraphPanel();
		if (graphPanel.getPickState()) {
			long clickedTime = graphPanel.getTime(x);
			PlotDataProvider channel = cv.getPlotDataProviders().get(0);
			SortedSet<IEvent> events = channel.getEvents(new Date(clickedTime), graphPanel.getTimeRange().getDuration()
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
		GraphPanel graphPanel = cv.getGraphPanel();
		long selectionTime = graphPanel.getSelectionTime();
		String diff = selectionTime == Long.MAX_VALUE ? "" : " diff " + new TimeInterval(selectionTime, graphPanel.getTime(x)).convert();
		XMAXframe.getInstance().getStatusBar().setMessage(
				TimeInterval.formatDate(new Date(graphPanel.getTime(cv.getMousePressX())), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
						+ ":" + graphPanel.getScaleMode().getValue(cv.getMousePressY()) + diff);

	}

	public void mouseReleasedButton1(int x, int y, JPanel clickedAt) {
		Date from;
		Date to;
		ChannelView cv = (ChannelView) clickedAt;
		GraphPanel graphPanel = cv.getGraphPanel();
		if (cv.getMousePressX() > x) {
			to = new Date(graphPanel.getTime(cv.getMousePressX()));
			from = new Date(graphPanel.getTime(x));
		} else {
			from = new Date(graphPanel.getTime(cv.getMousePressX()));
			to = new Date(graphPanel.getTime(x));
		}
		if (Math.abs(cv.getMousePressX() - x) > 1) {
			// to avoid mouse bounce
			if (to.getTime() > from.getTime()) {
				// Create Runnable SelectTimeCommand object 
				System.out.println("XMAXChannelView --> Execute SelectTimeCommand()");
				SelectTimeCommand timeTask = new SelectTimeCommand(graphPanel, new TimeInterval(from, to));
				
				// Create ExecuteCommand object for executing Runnable
				ExecuteCommand executor = new ExecuteCommand(timeTask);
				executor.initialize();
				executor.start();
				executor.shutdown();	
			} else {
				JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Max zoom reached", "Alert", JOptionPane.WARNING_MESSAGE);
			}
		}
		XMAXframe.getInstance().getStatusBar().setMessage("");
	}

	public void mouseReleasedButton3(int x, int y, JPanel clickedAt) {
		ChannelView cv = (ChannelView) clickedAt;
		GraphPanel graphPanel = cv.getGraphPanel();
		IScaleModeState scaleMode = graphPanel.getScaleMode();
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
					// Create Runnable SelectValueCommand object
					System.out.println("XMAXChannelView --> Execute SelectValueCommand()");
					SelectValueCommand valueTask = new SelectValueCommand(graphPanel, from, to);

					// Create ExecuteCommand object for executing Runnable
					ExecuteCommand executor = new ExecuteCommand(valueTask);
					executor.initialize();
					executor.start();
					executor.shutdown();
				} else {
					JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Please select non-null Y range", "Warning", JOptionPane.WARNING_MESSAGE);
				}
			}
		}
		XMAXframe.getInstance().getStatusBar().setMessage("");
	}
}
