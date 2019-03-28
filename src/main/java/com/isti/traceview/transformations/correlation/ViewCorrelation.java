package com.isti.traceview.transformations.correlation;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import com.isti.traceview.processing.IstiUtilsMath;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Dialog to view correlation results. Also performs correlation itself and
 * apply selected hanning window
 * 
 * @author Max Kokoulin
 */
class ViewCorrelation extends JDialog implements PropertyChangeListener, ItemListener {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ViewCorrelation.class);
	private static DecimalFormat dFormat = new DecimalFormat("###.###");

	private List<double[]> data = null;
	private String seriesName = null;
	private double sampleRate = 0.0;
	private JOptionPane optionPane;
	private XYPlot plot = null;
	private TraceViewChartPanel chartPanel = null;
	private JPanel optionP;
	private JLabel ampMaxL;
	private JLabel lagTimeL;
	private JLabel taperL;
	private JComboBox<Object> taperCB;

	/**
	 * @param owner
	 *            parent frame
	 * @param data
	 *            list of arrays - double raw data for selected traces and time
	 *            ranges
	 * @param channelNames
	 *            list of trace names
	 * @param sampleRate
	 *            sample rate of all traces (should be same)
	 */
	ViewCorrelation(Frame owner, List<double[]> data, List<String> channelNames, double sampleRate,
			TimeInterval timeInterval) {
		super(owner, "Correlation", true);
		this.data = data;
		Object[] options = { "Close", "Print" };
		if (data.size() == 2) {
			seriesName = "Correlation " + channelNames.get(0) + "-" + channelNames.get(1);
		} else {
			seriesName = "Autocorrelation " + channelNames.get(0);
		}
		this.sampleRate = sampleRate;
		// Create the JOptionPane.
		optionPane = new JOptionPane(createChartPanel(filterData(data), timeInterval), JOptionPane.PLAIN_MESSAGE,
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
			}
		}
	}

	private JPanel createChartPanel(XYDataset dataset, TimeInterval timeInterval) {
		JPanel ret = new JPanel();
		BoxLayout retLayout = new BoxLayout(ret, javax.swing.BoxLayout.Y_AXIS);
		ret.setLayout(retLayout);
		JFreeChart chart = ChartFactory.createXYLineChart(null, // title
				"Delay, seconds", "Correlation", dataset, // dataset
				PlotOrientation.VERTICAL, // orientation
				true, // legend
				true, // tooltips
				false// include URLs
		);

		TextTitle title = new TextTitle("Start time: "
				+ TimeInterval.formatDate(timeInterval.getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
				+ ", Duration: " + timeInterval.convert(), ret.getFont());
		chart.setTitle(title);
		plot = (XYPlot) chart.getPlot();
		chartPanel = new TraceViewChartPanel(chart, true);
		ret.add(chartPanel);
		ret.add(getOptionP());
		return ret;
	}

	/** Listens to the check box. */
	@Override
	public void itemStateChanged(ItemEvent e) {
		plot.setDataset(filterData(data));
	}

	private XYDataset filterData(List<double[]> ds) {
		this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		double[] correlation = null;
		double[] dblData1 = applyWindow(ds.get(0), (String) getTaperCB().getSelectedItem());
		try {
			if (ds.size() == 1) {
				correlation = IstiUtilsMath.correlate(dblData1, dblData1);
			} else {
				double[] dblData2 = applyWindow(ds.get(1), (String) taperCB.getSelectedItem());
				correlation = IstiUtilsMath.correlate(dblData1, dblData2);
			}
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException:", e);
		}
		logger.debug("correlation computed, size = " + correlation.length);
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries series = new XYSeries(seriesName);
		double ampMax = 0;
		int ampMaxPoint = -1;
		for (int i = 0; i < correlation.length; i++) {
			series.add(sampleRate * (i - correlation.length / 2) / 1000, correlation[i]);
			if (Math.abs(correlation[i]) > ampMax) {
				ampMax = Math.abs(correlation[i]);
				ampMaxPoint = i;
			}
		}
		dataset.addSeries(series);
		getAmpMaxL().setText("Max Amplitude: " + dFormat.format(ampMax));
		getLagTimeL().setText("Lag time: " + sampleRate * (ampMaxPoint - correlation.length / 2) / 1000 + " s");
		this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		return dataset;
	}

	private double[] applyWindow(double[] dataToProcess, String windowName) {
		if (windowName.equals("Hanning")) {
			return IstiUtilsMath.windowHanning(dataToProcess);
		} else if (windowName.equals("Hamming")) {
			return IstiUtilsMath.windowHamming(dataToProcess);
		} else if (windowName.equals("Cosine")) {
			return IstiUtilsMath.windowCosine(dataToProcess);
		} else if (windowName.equals("Triangular")) {
			return IstiUtilsMath.windowTriangular(dataToProcess);
		} else if (windowName.equals("Bartlett")) {
			return IstiUtilsMath.windowBartlett(dataToProcess);
		} else if (windowName.equals("Gauss")) {
			return IstiUtilsMath.windowGauss(dataToProcess);
		} else if (windowName.equals("Blackman")) {
			return IstiUtilsMath.windowBlackman(dataToProcess);
		} else {
			return dataToProcess;
		}
	}

	private JPanel getOptionP() {
		if (optionP == null) {
			optionP = new JPanel();
			optionP.setMaximumSize(new java.awt.Dimension(32767, 32));
			optionP.add(getAmpMaxL());
			optionP.add(getLagTimeL());
			optionP.add(getTaperL());
			optionP.add(getTaperCB());
		}
		return optionP;
	}

	private JLabel getAmpMaxL() {
		if (ampMaxL == null) {
			Dimension size = new Dimension(160, 22);
			ampMaxL = new JLabel();
			ampMaxL.setPreferredSize(size);
			ampMaxL.setText("Max Amplitude:");
		}
		return ampMaxL;
	}

	private JLabel getLagTimeL() {
		if (lagTimeL == null) {
			Dimension size = new Dimension(200, 22);
			lagTimeL = new JLabel();
			lagTimeL.setPreferredSize(size);
			lagTimeL.setText("Lag time:");
		}
		return lagTimeL;
	}

	private JLabel getTaperL() {
		if (taperL == null) {
			taperL = new JLabel();
			taperL.setText("Window taper:");
		}
		return taperL;
	}

	private JComboBox<Object> getTaperCB() {
		if (taperCB == null) {
			List<String> options = new ArrayList<>();
			options.add("None");
			options.add("Hanning");
			options.add("Hamming");
			options.add("Cosine");
			options.add("Triangular");
			options.add("Bartlett");
			options.add("Gauss");
			options.add("Blackman");
			ComboBoxModel<Object> convolveCBModel = new DefaultComboBoxModel<>(options.toArray());
			taperCB = new JComboBox<>();
			taperCB.setModel(convolveCBModel);
			taperCB.setPreferredSize(new java.awt.Dimension(100, 22));
			taperCB.setEnabled(true);
			taperCB.setSelectedIndex(0);
			taperCB.addItemListener(this);
		}
		return taperCB;
	}
}
