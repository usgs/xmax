package com.asl.traceview.transformations.coherence;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import com.isti.traceview.gui.GraphUtil;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.xmax.XMAX;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Dialog to view coherence results. Also performs smoothing.
 * 
 * @author Nick Falco
 */
class ViewCoherence extends JDialog implements PropertyChangeListener, ItemListener {

	private static final long serialVersionUID = 1L;

	// private static SimpleDateFormat df = new SimpleDateFormat("yyyy,DDD");
	private JOptionPane optionPane;
	private ButtonGroup SmoothButtonGroup;
	private JRadioButton RawRB;
	private JRadioButton SmoothRB;
	private JRadioButton RawAndSmoothRB;
	private JPanel optionPanel;
	private XYSeriesCollection data = null;
	private XYPlot plot = null;
	private TimeInterval timeInterval = null;
	private TraceViewChartPanel chartPanel = null;

	ViewCoherence(Frame owner, XYSeriesCollection data, TimeInterval timeInterval) {
		super(owner, "Coherence", true);
		this.data = data;
		this.timeInterval = timeInterval;
		Object[] options = { "Close", "Print", "Export GRAPH" };
		// Create the JOptionPane.
		optionPane = new JOptionPane(createChartPanel(filterData(this.data)), JOptionPane.PLAIN_MESSAGE,
				JOptionPane.CLOSED_OPTION, null, options, options[0]);
		// Make this dialog display it.
		setContentPane(optionPane);
		optionPane.addPropertyChangeListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property.
				 */
				optionPane.setValue("Close");
			}
		});
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if (isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
			Object value = optionPane.getValue();
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			// If you were going to check something
			// before closing the window, you'd do
			// it here.
			if (value.equals("Close")) {
				setVisible(false);
				dispose();
			} else if (value.equals("Print")) {
				chartPanel.createChartPrintJob();
			} else if (value.equals("Export GRAPH")) {
				File exportFile = GraphUtil.saveGraphics(chartPanel, XMAX.getConfiguration().getUserDir("GRAPH"));
				if (exportFile != null) {
					XMAX.getConfiguration().setUserDir("GRAPH", exportFile.getParent());
				}
			}
		}
	}

	/** Listens to the check box. */
	@Override
	public void itemStateChanged(ItemEvent e) {
		// the only possible event source is going to be the radio buttons choosing smooth vs. not
		// which is checked in filterData method
		plot.setDataset(filterData(data));
	}

	private JPanel createChartPanel(XYDataset dataset) {
		JPanel ret = new JPanel();
		BoxLayout retLayout = new BoxLayout(ret, javax.swing.BoxLayout.Y_AXIS);
		ret.setLayout(retLayout);
		JFreeChart chart = ChartFactory.createXYLineChart(null, // title
				"Period, s", // x-axis label
				"Coherence", // y-axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
		);
		chart.setBackgroundPaint(Color.white);
		TextTitle title = new TextTitle("Start time: "
				+ TimeInterval.formatDate(timeInterval.getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
				+ ", Duration: " + timeInterval.convert(), ret.getFont());
		chart.setTitle(title);
		plot = chart.getXYPlot();
		NumberAxis domainAxis = new LogarithmicAxis("Period, s");
		plot.setDomainAxis(domainAxis);
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setLowerBound(0);
		rangeAxis.setUpperBound(1);
		rangeAxis.setAutoRangeIncludesZero(true);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		chartPanel = new TraceViewChartPanel(chart, true);
		ret.add(chartPanel);
		ret.add(getOptionP());
		return ret;
	}

	private XYDataset filterData(XYSeriesCollection series) {
		XYSeriesCollection ret = new XYSeriesCollection();
		if (getSmoothRB().isSelected()) {
			for (int i = 0; i < series.getSeriesCount(); ++i) {
				String key = series.getSeriesKey(i).toString();
				if (key.contains("smoothed")) {
					ret.addSeries(series.getSeries(i));
				}
			}
		}
		else if (getRawRB().isSelected()){
			for (int i = 0; i < series.getSeriesCount(); ++i) {
				String key = series.getSeriesKey(i).toString();
				if (!key.contains("smoothed")) {
					ret.addSeries(series.getSeries(i));
				}
			}
		}
		else if (getRawAndSmoothRB().isSelected()){
			ret = series;
		}
		return ret;
	}

	private JRadioButton getRawRB() {
		if (RawRB == null) {
			RawRB = new JRadioButton();
			RawRB.setText("Raw");
			RawRB.setSelected(true);
			RawRB.addItemListener(this);
		}
		return RawRB;
	}
	
	private JRadioButton getRawAndSmoothRB() {
		if (RawAndSmoothRB == null) {
			RawAndSmoothRB = new JRadioButton();
			RawAndSmoothRB.setText("Raw & Smooth");
			RawAndSmoothRB.addItemListener(this);
		}
		return RawAndSmoothRB;
	}

	private JRadioButton getSmoothRB() {
		if (SmoothRB == null) {
			SmoothRB = new JRadioButton();
			SmoothRB.setText("Smooth");
			SmoothRB.addItemListener(this);
		}
		return SmoothRB;
	}

	private JPanel getOptionP() {
		if (optionPanel == null && SmoothButtonGroup == null) {
			optionPanel = new JPanel();
			optionPanel.setMaximumSize(new java.awt.Dimension(32767, 32));
			SmoothButtonGroup = new ButtonGroup();
			SmoothButtonGroup.add(getRawRB());
			SmoothButtonGroup.add(getSmoothRB());
			SmoothButtonGroup.add(getRawAndSmoothRB());
			optionPanel.add(getRawRB());
			optionPanel.add(getSmoothRB());
			optionPanel.add(getRawAndSmoothRB());
		}
		return optionPanel;
	}

}
