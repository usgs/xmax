package com.asl.traceview.transformations.coherence;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.ChannelView;
import com.isti.traceview.gui.ColorModeFixed;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.GraphUtil;
import com.isti.traceview.gui.IChannelViewFactory;
import com.isti.traceview.gui.ScaleModeAuto;
import com.isti.xmax.XMAX;

/**
 * Dialog to view coherence plot results. 
 * 
 * @author Nick Falco
 */
class ViewCoherence extends JDialog implements PropertyChangeListener, ChartProgressListener {

	private static final long serialVersionUID = 1L;

	private static DecimalFormat screenDataFormat = new DecimalFormat("#####.####");
	private JLabel crosshairPositionL = null;
	private JFreeChart chart = null;

	private JOptionPane optionPane;
	private MyOptionPane chartPanel = null;
	private XYSeriesCollection dataset = null;
	private TimeInterval ti = null;
	private boolean showWaves = false;

	ViewCoherence(Frame owner, XYSeriesCollection dataset, TimeInterval ti,
			List<PlotDataProvider> input) {
		super(owner, "Coherence", true);
		this.dataset = dataset;
		this.ti = ti;
		Object[] options = { "Close", "Print", "Export GRAPH"};
		// Create the JOptionPane.
		optionPane = new JOptionPane();
		optionPane.setVisible(false);
		optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
		optionPane.setOptionType(JOptionPane.CLOSED_OPTION);
		optionPane.setIcon(null);
		optionPane.setOptions(options);
		optionPane.setInitialValue(options[0]);
		chartPanel = createChartPanel(dataset, input);
		optionPane.setMessage(chartPanel);
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
		optionPane.setVisible(true);
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
				PrinterJob job = PrinterJob.getPrinterJob();
				PageFormat pf = job.defaultPage();
				pf.setOrientation(PageFormat.LANDSCAPE);
				PageFormat pf2 = job.pageDialog(pf);
				if (pf2 != pf) {
					job.setPrintable(chartPanel, pf2);
					if (job.printDialog()) {
						try {
							job.print();
						} catch (PrinterException e1) {
							JOptionPane.showMessageDialog(this, e1);
						}
					}
				}
			} else if (value.equals("Export GRAPH")) {
				File exportFile = GraphUtil.saveGraphics((JPanel) optionPane.getMessage(),
						XMAX.getConfiguration().getUserDir("GRAPH"));
				if (exportFile != null) {
					XMAX.getConfiguration().setUserDir("GRAPH", exportFile.getParent());
				}
			}
		}
	}

	private MyOptionPane createChartPanel(XYDataset dataset, List<PlotDataProvider> input) {
		chart = ChartFactory.createXYLineChart(null, // title
				"Period, s", // x-axis label
				"Coherence", // y-axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
		);
		chart.setBackgroundPaint(Color.white);
		chart.addProgressListener(this);
		TraceViewChartPanel cp = new TraceViewChartPanel(chart, true);
		TextTitle title = new TextTitle("Start time: "
				+ TimeInterval.formatDate(ti.getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
				+ ", Duration: " + ti.convert(), getFont());
		chart.setTitle(title);
		XYPlot plot = chart.getXYPlot();
		NumberAxis domainAxis = new LogarithmicAxis("Period, s");
		domainAxis.setRange(new Range(0.01, 1000.0));
		plot.setDomainAxis(domainAxis);
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setAutoRange(true);
		rangeAxis.setAutoRangeIncludesZero(false);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainCrosshairLockedOnData(true);
		plot.setRenderer(new CoherenceItemRenderer());
		return new MyOptionPane(cp, input);
	}

	/**
	 * Handles a chart progress event.
	 * 
	 * @param event
	 *            the event.
	 */
	@Override
	public void chartProgress(ChartProgressEvent event) {
		if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
			return;
		}
		if (chart != null) {
			XYPlot plot = (XYPlot) chart.getPlot();
			// XYDataset dataset = plot.getDataset();
			// Comparable seriesKey = dataset.getSeriesKey(0);
			double xx = plot.getDomainCrosshairValue();
			double yy = plot.getRangeCrosshairValue();
			// update the screen...
			if (xx != 0.0 && yy != 0.0) {
				getCrosshairPositionL().setText(
						"  Period: " + screenDataFormat.format(xx) + " s, Coherence: " + screenDataFormat.format(yy));
			}
			// lg.debug("X: " + xx + ", Y: " + yy);
		}
	}

	private JLabel getCrosshairPositionL() {
		if (crosshairPositionL == null) {
			crosshairPositionL = new JLabel();
			crosshairPositionL.setText(" ");
		}
		return crosshairPositionL;
	}

	private class CoherenceItemRenderer extends StandardXYItemRenderer {

		private static final long serialVersionUID = 1L;

		public CoherenceItemRenderer() {
			super();
			setSeriesPaint(dataset.getSeriesCount() - 1, Color.BLACK);
		}

		@Override
		public LegendItem getLegendItem(int datasetIndex, int series) {
			if (series > dataset.getSeriesCount() - 3) {
				return null;
			} else
				return super.getLegendItem(datasetIndex, series);
		}
	}

	private class CoherenceChannelViewFactory implements IChannelViewFactory {
		@Override
		public int getInfoAreaWidth() {
			return 0;
		}

		@Override
		public ChannelView getChannelView(List<PlotDataProvider> channels) {
			return new ChannelView(channels, getInfoAreaWidth(), false, Color.WHITE, Color.WHITE);
		}

		@Override
		public ChannelView getChannelView(PlotDataProvider channel) {
			return new ChannelView(channel, getInfoAreaWidth(), false, Color.WHITE, Color.WHITE);
		}
	}

	private class MyOptionPane extends JPanel implements Printable {

		private static final long serialVersionUID = 1L;
		private TraceViewChartPanel cp = null;
		private JPanel waveP = null;
		private List<PlotDataProvider> input;

		private MyOptionPane(TraceViewChartPanel cp, List<PlotDataProvider> input) {
			this.cp = cp;
			this.input = input;
			BoxLayout mLayout = new BoxLayout(this, javax.swing.BoxLayout.Y_AXIS);
			setLayout(mLayout);
			add(cp);
			if (showWaves) {
				add(getWavePanel());
			}
			add(getCrosshairPositionL());
		}

		public JPanel getWavePanel() {
			if (waveP == null) {
				waveP = new JPanel();
				BoxLayout oLayout = new BoxLayout(waveP, javax.swing.BoxLayout.Y_AXIS);
				waveP.setLayout(oLayout);
				GraphPanel gp = new GraphPanel(false);
				gp.setChannelViewFactory(new CoherenceChannelViewFactory());
				gp.setShowBigCursor(false);
				gp.setColorMode(new ColorModeFixed());
				gp.setScaleMode(new ScaleModeAuto());
				gp.setBackground(Color.WHITE);
				gp.setChannelShowSet(input);
				gp.setTimeRange(ti);
				CoherenceItemRenderer ir = (CoherenceItemRenderer) cp.getChart().getXYPlot().getRenderer();
				for (int i = 0; i < input.size(); i++) {
					PlotDataProvider channel = input.get(i);
					channel.setColor((Color) ir.lookupSeriesPaint(i));
				}
				waveP.add(gp);
				waveP.setMaximumSize(new java.awt.Dimension(32767, 100));
				waveP.setPreferredSize(new java.awt.Dimension(this.getWidth(), 100));
			}
			return waveP;
		}

		@Override
		public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
			if (pageIndex != 0) {
				return NO_SUCH_PAGE;
			}
			double factor;
			if (showWaves) {
				factor = new Double(cp.getHeight()) / (waveP.getHeight() + cp.getHeight());
			} else {
				factor = 1.0;
			}
			Graphics2D g2 = (Graphics2D) g;
			double x = pf.getImageableX();
			double y = pf.getImageableY();
			double w = pf.getImageableWidth();
			double h = pf.getImageableHeight() * factor;
			cp.getChart().draw(g2, new Rectangle2D.Double(x, y, w, h), null, null);
			if (showWaves) {
				g2.translate(x, y + h);
				g2.scale(w / waveP.getWidth(), (pf.getImageableHeight() * (1 - factor)) / waveP.getHeight());
				waveP.paint(g2);
			}
			// double imageHeight = pf.getImageableHeight() * (1-factor);
			// BufferedImage image = new BufferedImage(new Double(w).intValue(),
			// new Double(imageHeight).intValue(), BufferedImage.TYPE_INT_RGB);
			// Graphics2D g2image = image.createGraphics();
			// g2image.scale(w / new Double(optionP.getWidth()), imageHeight /
			// new Double(optionP.getHeight()));
			// optionP.paint(g2image);
			// g2.drawImage(image, new Double(x).intValue(), new
			// Double(y).intValue(), null, null);
			return PAGE_EXISTS;
		}
	}
}
