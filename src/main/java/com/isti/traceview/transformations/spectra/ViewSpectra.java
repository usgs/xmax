package com.isti.traceview.transformations.spectra;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

import com.isti.traceview.TraceView;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import com.isti.traceview.data.Response;
import com.isti.traceview.gui.GraphUtil;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Spectra;
import com.isti.xmax.XMAX;

/**
 * Dialog to view Spectra results. Also performs smoothing.
 * 
 * @author Max Kokoulin
 */
class ViewSpectra extends JDialog implements PropertyChangeListener, ItemListener {

	private static final long serialVersionUID = 1L;
	// private static final Logger logger = Logger.getLogger(ViewSpectra.class);

	// private static SimpleDateFormat df = new SimpleDateFormat("yyyy,DDD");
	private JOptionPane optionPane;
	private JCheckBox SmoothCB;
	private JCheckBox LogScaleCB;
	private JPanel selectionP;
	private JLabel convolveL;
	private JPanel optionPanel;
	private JComboBox<Object> convolveCB;
	private JCheckBox deconvolveCB;
	private JCheckBox showDiffCB;
	private List<Spectra> data = null;
	private XYPlot plot = null;
	private TimeInterval timeInterval = null;
	private TraceViewChartPanel chartPanel = null;
	private NumberAxis origRangeAxis = null;

	ViewSpectra(Frame owner, List<Spectra> data, TimeInterval timeInterval) {
		super(owner, "Spectra", true);
		this.data = data;
		this.timeInterval = timeInterval;
		Object[] options = { "Close", "Print", "Export GRAPH" };
		// Create the JOptionPane.
		optionPane = new JOptionPane(createChartPanel(filterData(data)), JOptionPane.PLAIN_MESSAGE,
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
		if (e.getSource().equals(getSmoothCB())) {
		} else if (e.getSource().equals(getDeconvolveCB())) {
			getConvolveCB().setEnabled(e.getStateChange() == ItemEvent.SELECTED);
		} else if (e.getSource().equals(getConvolveCB())) {

		} else if (e.getSource().equals(getLogScaleCB())) {
			if(getLogScaleCB().isSelected()) {
				NumberAxis rangeAxis = (NumberAxis) new LogarithmicAxis("Spectra");
				rangeAxis.setAutoRange(true);
				plot.setRangeAxis(rangeAxis);
			} else {
				plot.setRangeAxis(origRangeAxis);
			}
		} else if (e.getSource().equals(getShowDiffCB())) {
			if (getShowDiffCB().isSelected()) {
				// Component[] ca = selectionP.getComponents();
				int countSelected = 0;
				for (int i = 1; i < selectionP.getComponentCount(); i++) {
					JCheckBox cb = (JCheckBox) selectionP.getComponent(i);
					if (cb.isSelected()) {
						countSelected++;
					}
				}
				if (countSelected == 2) {

				} else {
					getShowDiffCB().setSelected(false);
					JOptionPane.showMessageDialog(this, "You should select 2 channels to show difference", "Warning",
							JOptionPane.WARNING_MESSAGE);
				}
			} else {

			}
		}
		plot.setDataset(filterData(data));
	}

	private JPanel createChartPanel(XYDataset dataset) {
		JPanel ret = new JPanel();
		BoxLayout retLayout = new BoxLayout(ret, javax.swing.BoxLayout.Y_AXIS);
		ret.setLayout(retLayout);
		JFreeChart chart = ChartFactory.createXYLineChart(null, // title
				"Period, s", // x-axis label
				"Spectra", // y-axis label
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
		rangeAxis.setAutoRange(true);
		rangeAxis.setAutoRangeIncludesZero(false);
		origRangeAxis = (NumberAxis) plot.getRangeAxis();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		chartPanel = new TraceViewChartPanel(chart, true);
		ret.add(chartPanel);
		ret.add(getOptionP());
		if (dataset.getSeriesCount() > 1) {
			ret.add(getSelectionP());
		}
		return ret;
	}

	private XYDataset filterData(List<Spectra> ds) {
		XYSeriesCollection ret = new XYSeriesCollection();
		for (Spectra spectra : ds) {
			ret.addSeries(spectra.getSpectraSeries(getDeconvolveCB().isSelected(),
					getConvolveCB().getSelectedItem().toString()));
		}

		if (getSmoothCB().isSelected()) {
			ret = IstiUtilsMath.varismooth(ret);
		}

		if (getShowDiffCB().isSelected()) {
			XYSeries[] series = new XYSeries[2];
			// Component[] ca = selectionP.getComponents();
			int seriesFound = 0;
			int i = 1;
			while (i < selectionP.getComponentCount() && seriesFound < 2) {
				JCheckBox cb = (JCheckBox) selectionP.getComponent(i);
				if (cb.isSelected()) {
					series[seriesFound] = ret.getSeries(i - 1);
					seriesFound++;
				}
				i++;
			}
			double mean1 = getSeriesMean(series[0]);
			double mean2 = getSeriesMean(series[1]);
			XYSeries subtract = null;
			if (mean1 > mean2) {
				subtract = subtractSeries(series[0], series[1]);
			} else {
				subtract = subtractSeries(series[1], series[0]);
			}
			ret = new XYSeriesCollection();
			ret.addSeries(subtract);
		}
		return ret;
	}

	/**
	 * TODO: This method is too vague. Not sure what it does. Analysis and
	 * refactoring required.
	 * 
	 * @param series
	 *            the XYZSeries
	 * @param arg
	 *            a double
	 * @return either a Double.NaN or a computed value.
	 */

	private double getValue(XYSeries series, double arg) {
		for (int i = 0; i < series.getItemCount(); i++) {
			if (arg < series.getX(i).doubleValue()) {
				return series.getY(i - 1).doubleValue()
						+ ((series.getY(i).doubleValue() - series.getY(i - 1).doubleValue())
								* (arg - series.getX(i - 1).doubleValue())
								/ (series.getX(i).doubleValue() - series.getX(i - 1).doubleValue()));
			}
		}
		return Double.NaN;
	}

	/**
	 * @param ser
	 *            the series
	 * @return Series mean
	 */

	private double getSeriesMean(XYSeries ser) {
		double ret = 0;
		for (int i = 0; i < ser.getItemCount(); i++) {
			ret = ret + ser.getY(i).doubleValue();
		}
		return ret / ser.getItemCount();
	}

	private XYSeries subtractSeries(XYSeries series1, XYSeries series2) {
		XYSeries ret = new XYSeries(series1.getKey() + "-" + series2.getKey());
		for (int i = 0; i < series1.getItemCount(); i++) {
			double val = getValue(series2, series1.getX(i).doubleValue());
			if (val != Double.NaN) {
				ret.add(series1.getX(i), series1.getY(i).doubleValue() - val);
			}
		}
		return ret;
	}

	private JCheckBox getLogScaleCB() {
		if (LogScaleCB == null) {
			LogScaleCB = new JCheckBox();
			LogScaleCB.setText("Log Scale");
			LogScaleCB.addItemListener(this);
		}
		return LogScaleCB;
	}
	
	private JCheckBox getSmoothCB() {
		if (SmoothCB == null) {
			SmoothCB = new JCheckBox();
			SmoothCB.setText("Smooth");
			SmoothCB.addItemListener(this);
		}
		return SmoothCB;
	}
	
	private JCheckBox getShowDiffCB() {
		if (showDiffCB == null) {
			showDiffCB = new JCheckBox();
			showDiffCB.setText("Show difference");
			showDiffCB.setPreferredSize(new java.awt.Dimension(246, 20));
			showDiffCB.addItemListener(this);
		}
		return showDiffCB;
	}

	private JCheckBox getDeconvolveCB() {
		if (deconvolveCB == null) {
			deconvolveCB = new JCheckBox();
			deconvolveCB.setText("Deconvolve");
			deconvolveCB.addItemListener(this);
		}
		return deconvolveCB;
	}

	private JComboBox<Object> getConvolveCB() {
		if (convolveCB == null) {
			List<String> options = new ArrayList<String>();
			options.add("None");
			for (Response resp : TraceView.getDataModule().getLoadedResponses()) {
				options.add(resp.getLocalFileName());
			}
			ComboBoxModel<Object> convolveCBModel = new DefaultComboBoxModel<Object>(options.toArray());
			convolveCB = new JComboBox<Object>();
			convolveCB.setModel(convolveCBModel);
			convolveCB.setPreferredSize(new java.awt.Dimension(128, 22));
			convolveCB.setEnabled(false);
			convolveCB.addItemListener(this);
		}
		return convolveCB;
	}

	private JPanel getOptionP() {
		if (optionPanel == null) {
			optionPanel = new JPanel();
			optionPanel.setMaximumSize(new java.awt.Dimension(32767, 32));
			optionPanel.add(getLogScaleCB());
			optionPanel.add(getSmoothCB());
			optionPanel.add(getDeconvolveCB());
			optionPanel.add(getConvolveL());
			optionPanel.add(getConvolveCB());
		}
		return optionPanel;
	}

	private JLabel getConvolveL() {
		if (convolveL == null) {
			convolveL = new JLabel();
			convolveL.setText("Convolve:");
		}
		return convolveL;
	}

	private JPanel getSelectionP() {
		if (selectionP == null) {
			selectionP = new JPanel();
			FlowLayout selectionPLayout = new FlowLayout();
			selectionP.setLayout(selectionPLayout);
			selectionP.setMaximumSize(new java.awt.Dimension(32767, 32));
			selectionP.add(getShowDiffCB());
			for (Spectra spectra : data) {
				JCheckBox seriesCB = new JCheckBox(spectra.getName(), false);
				seriesCB.addItemListener(this);
				selectionP.add(seriesCB);
			}
		}
		return selectionP;
	}
}
