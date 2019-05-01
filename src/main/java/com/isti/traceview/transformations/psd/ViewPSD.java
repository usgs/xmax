package com.isti.traceview.transformations.psd;

import com.isti.jevalresp.OutputGenerator;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import com.isti.traceview.data.FileOutputUtils;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.SacTimeSeriesASCII;
import com.isti.traceview.gui.ChannelView;
import com.isti.traceview.gui.ColorModeFixed;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.GraphUtil;
import com.isti.traceview.gui.IChannelViewFactory;
import com.isti.traceview.gui.ScaleModeAuto;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Spectra;
import com.isti.xmax.XMAX;
import com.isti.xmax.XMAXconfiguration;
import com.isti.xmax.gui.XMAXframe;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.GridArrangement;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;

/**
 * Dialog to view PSD results. Also performs deconvolution, convolution and smoothing.
 *
 * @author Max Kokoulin
 */
class ViewPSD extends JDialog implements PropertyChangeListener,
    ChartProgressListener, ItemListener {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(ViewPSD.class);
  private static final String huttFreqsFile = "hutt_freqs.txt";
  private static final String huttPeriodsKey = "HuttPeriods";
  private static final SimpleDateFormat df = new SimpleDateFormat("yyyy,DDD");
  private static final DecimalFormat huttFormat = new DecimalFormat("#####.##");
  private static final DecimalFormat psnFormat1 = new DecimalFormat("0.00000E00");
  private static final DecimalFormat psnFormat2 = new DecimalFormat("#####.####");
  private static final DecimalFormat screenDataFormat = new DecimalFormat("#####.####");
  private JLabel crosshairPositionL = null;
  private JFreeChart chart = null;

  private final JOptionPane optionPane;
  private final MyOptionPane chartPanel;
  private final XYSeriesCollection dataset;
  private final TimeInterval ti;
  private final List<Spectra> data;
  private final Configuration configuration;
  private boolean showWaves = false;

  private JRadioButton smoothRB, rawRB, rawAndSmoothRB;
  private ButtonGroup buttonGroup;

  ViewPSD(Frame owner, List<Spectra> data, TimeInterval ti, Configuration configuration,
      List<PlotDataProvider> input) {
    super(owner, "Power Spectra Density", true);
    this.ti = ti;
    this.data = data;
    this.configuration = configuration;

    Object[] options = {"Close", "Print", "Dump Freqs", "Export PSD (ASCII)", "Export SAC",
        "Export GRAPH",
        "Toggle waves"};
    // Create the JOptionPane.
    optionPane = new JOptionPane();
    optionPane.setVisible(false);
    optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
    optionPane.setOptionType(JOptionPane.DEFAULT_OPTION);
    optionPane.setIcon(null);
    optionPane.setOptions(options);
    optionPane.setInitialValue(options[0]);

    this.dataset = createDataset(data);
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

  private XYSeriesCollection filterData(XYSeriesCollection dataset) {
    // plot the smoothed data first so it shows up on top
    XYSeriesCollection ret = IstiUtilsMath.varismooth(dataset);
    // now add the unsmoothed data
    for (int i = 0; i < dataset.getSeriesCount(); ++i) {
      ret.addSeries(dataset.getSeries(i));
    }
    return ret;
  }

  private JRadioButton getRawRB() {
    if (rawRB == null) {
      rawRB = new JRadioButton();
      rawRB.setText("Raw");
      rawRB.addItemListener(this);
    }
    return rawRB;
  }

  private JRadioButton getRawAndSmoothRB() {
    if (rawAndSmoothRB == null) {
      rawAndSmoothRB = new JRadioButton();
      rawAndSmoothRB.setText("Raw & Smooth");
      rawAndSmoothRB.addItemListener(this);
    }
    return rawAndSmoothRB;
  }

  private JRadioButton getSmoothRB() {
    if (smoothRB == null) {
      smoothRB = new JRadioButton();
      smoothRB.setText("Smooth");
      smoothRB.setSelected(true); // default to smooth button being selected
      smoothRB.addItemListener(this);
    }
    return smoothRB;
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
      } else if (value.equals("Export PSD (ASCII)")) {
        for (int i = 0; i < dataset.getSeriesCount() - 2; i++) {
          BufferedOutputStream stream = null;
          String seriesName = (String) dataset.getSeriesKey(i);
          String fileName = "PSD_" + TimeInterval.formatDate(ti.getStartTime(),
              TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
              + seriesName.replace("/", "_") + ".txt";

          File outFile = FileOutputUtils.getOutputFromConfigOrUser(fileName, this);
          if (outFile == null) {
            JOptionPane.showMessageDialog(XMAXframe.getInstance(),
                "Output operation cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
          }
          try {
            stream = new BufferedOutputStream(new FileOutputStream(fileName, false));
            for (int j = 0; j < dataset.getItemCount(i); j++) {
              stream.write((psnFormat1.format(dataset.getXValue(i, j)) + "  "
                  + psnFormat2.format(dataset.getYValue(i, j)) + "\n").getBytes());
            }
          } catch (IOException e1) {
            JOptionPane.showMessageDialog(XMAXframe.getInstance(),
                "Can't write file " + fileName + "; " + e1, "Error", JOptionPane.ERROR_MESSAGE);
          } finally {
            try {
              if (stream != null)
                stream.close();
            } catch (IOException e1) {
              // do nothing
              logger.error("Can't close buffered output stream");
            }
          }
        }
        JOptionPane
            .showMessageDialog(XMAXframe.getInstance(), "Data exported to PSD ascii", "Message",
                JOptionPane.INFORMATION_MESSAGE);
      } else if (value.equals("Export SAC")) {
        for (int i = 0; i < dataset.getSeriesCount() - 2; i++) {
          DataOutputStream ds = null;

          String seriesName = (String) dataset.getSeriesKey(i);
          String fileName = "PSD_" + TimeInterval.formatDate(ti.getStartTime(),
              TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
              + seriesName.replace("/", "_") + ".SAC";

          File outFile = FileOutputUtils.getOutputFromConfigOrUser(fileName, this);
          if (outFile == null) {
            JOptionPane.showMessageDialog(XMAXframe.getInstance(),
                "Output operation cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
          }

          try {
            ds = new DataOutputStream(new FileOutputStream(new File(fileName)));
            float[] ydata = new float[dataset.getItemCount(i)];
            float[] xdata = new float[dataset.getItemCount(i)];
            for (int j = 0; j < dataset.getItemCount(i); j++) {
              xdata[j] = new Double(dataset.getXValue(i, j)).floatValue();
              ydata[j] = new Double(dataset.getYValue(i, j)).floatValue();
            }
            SacTimeSeriesASCII sacAscii = SacTimeSeriesASCII.getSAC(data.get(i).getChannel(),
                ti.getStartTime(), xdata, ydata);
            sacAscii.writeHeader(ds);
            sacAscii.writeData(ds);
          } catch (IOException e1) {
            JOptionPane.showMessageDialog(XMAXframe.getInstance(),
                "Can't write file " + fileName + "; " + e1, "Error", JOptionPane.ERROR_MESSAGE);
          } finally {
            try {
              if (ds != null)
                ds.close();
            } catch (Exception e1) {
              // do nothing
              logger.error("Can't close data output stream");
            }
          }
        }
        JOptionPane
            .showMessageDialog(XMAXframe.getInstance(), "Data exported to SAC ascii", "Message",
                JOptionPane.INFORMATION_MESSAGE);
      } else if (value.equals("Dump Freqs")) {
        BufferedOutputStream stream = null;

        String fileName = huttFreqsFile;

        File outFile = FileOutputUtils.getOutputFromConfigOrUser(fileName, this);
        if (outFile == null) {
          JOptionPane.showMessageDialog(XMAXframe.getInstance(),
              "Output operation cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
        }

        try {
          stream = new BufferedOutputStream(new FileOutputStream(outFile, true));
          for (int i = 0; i < dataset.getSeriesCount() - 2; i++) {
            String seriesName = (String) dataset.getSeriesKey(i);
            stream.write((seriesName + ":").getBytes());
            for (double huttPeriod : getHuttPeriods()) {
              stream.write((" " + huttFormat.format(getValue(i, huttPeriod)) + " @   "
                  + huttFormat.format(huttPeriod) + " s;").getBytes());
            }
            stream.write((" " + df.format(ti.getStartTime()) + "\n").getBytes());
          }
          JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Dump frequencies file updated",
              "Message",
              JOptionPane.INFORMATION_MESSAGE);
        } catch (FileNotFoundException e1) {
          JOptionPane
              .showMessageDialog(XMAXframe.getInstance(), "Cannot find the dump frequencies file",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
        } catch (IOException e2) {
          JOptionPane.showMessageDialog(XMAXframe.getInstance(),
              "Cannot write the dump frequencies: " + e2,
              "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
          try {
            if (stream != null)
              stream.close();
          } catch (IOException e1) {
            // do nothing
            logger.error("Cannot close the data stream");
          }
        }
      } else if (value.equals("Export GRAPH")) {
        File exportFile = GraphUtil.saveGraphics((JPanel) optionPane.getMessage(),
            XMAX.getConfiguration().getUserDir("GRAPH"));
        if (exportFile != null) {
          XMAX.getConfiguration().setUserDir("GRAPH", exportFile.getParent());
        }
      } else if (value.equals("Toggle waves")) {
        if (showWaves) {
          chartPanel.remove(chartPanel.getWavePanel());
          showWaves = false;
        } else {
          chartPanel.add(chartPanel.getWavePanel(), 1);
          showWaves = true;
        }
        invalidate();
        optionPane.invalidate();
        chartPanel.invalidate();
        synchronized (getTreeLock()) {
          validateTree();
        }
        doLayout();
        repaint();
      }
    }
  }

  private void setVisibleItems() {
    // controls whether to show or hide the smoothed/unsmoothed data
    XYItemRenderer renderer = chart.getXYPlot().getRenderer();
    for (int i = 0; i < dataset.getSeriesCount(); ++i) {
      boolean isSmoothed = ((String) dataset.getSeriesKey(i)).contains("smoothed");
      if (isSmoothed && rawRB.isSelected()) {
        renderer.setSeriesVisible(i, false);
      } else if (!isSmoothed && smoothRB.isSelected()) {
        renderer.setSeriesVisible(i, false);
      } else {
        renderer.setSeriesVisible(i, true);
      }

      if (dataset.getSeriesKey(i).equals("NHNM") ||
          dataset.getSeriesKey(i).equals("NLNM")) {
        renderer.setSeriesVisible(i, true);
        renderer.setSeriesPaint(i, Color.BLACK);
        renderer.setSeriesVisibleInLegend(i, false);
      }
    }
  }

  /**
   * get Hutt periods for PSD from plugin configuration, sec
   */
  private double[] getHuttPeriods() {
    String periods = configuration.getString(huttPeriodsKey);
    if (periods != null) {
      StringTokenizer st = new StringTokenizer(periods, ",");
      double[] huttPeriods = new double[st.countTokens()];
      int i = 0;
      while (st.hasMoreTokens()) {
        huttPeriods[i] = Double.parseDouble(st.nextToken());
        i++;
      }
      return huttPeriods;
    } else {
      return new double[]{0.2, 1.0, 20.5, 109.2};
    }
  }

  /**
   * Setter of the PSD export Hutt periods
   *
   * @param periods array of Hutt PSD export periods in seconds.
   */
  public void setHuttPeriods(double[] periods) {
    configuration.clearProperty(huttPeriodsKey);
    if (periods.length > 0) {
      StringBuilder toSave = new StringBuilder();
      for (int i = 0; i < periods.length; i++) {
        toSave.append(i == 0 ? "" : ",").append(new Double(periods[i]));
      }
      configuration.addProperty(huttPeriodsKey, toSave.toString());
    }
    XMAXconfiguration.getInstance().save();
  }

  private XYSeriesCollection createDataset(List<Spectra> ds) {
    XYSeriesCollection ret = new XYSeriesCollection();
    for (Spectra spectra : ds) {
      ret.addSeries(spectra.getPSDSeries(OutputGenerator.VELOCITY_UNIT_CONV));
    }

    XYSeries lowNoiseModelSeries = new XYSeries("NLNM");
    XYSeries highNoiseModelSeries = new XYSeries("NHNM");

    double[] freqArr = ds.get(0).getFrequencies();
    for (int i = 1; i < freqArr.length; i++) {
      double period = 1.0 / freqArr[i];
      double lowModel = NoiseModel.fnlnm(period);
      if (lowModel != 0.0) {
        lowNoiseModelSeries.add(period, lowModel);
      }
      double highModel = NoiseModel.fnhnm(period);
      if (highModel != 0) {
        highNoiseModelSeries.add(period, highModel);
      }
    }
    ret = filterData(ret);
    // Adding head of noise models, between 0.1 s and psd graph beginning
    int i = 2;
    double period;
    while ((period = 1 / (i * freqArr[freqArr.length - 1])) > 0.1) {
      double lowModel = NoiseModel.fnlnm(period);
      if (lowModel != 0.0) {
        lowNoiseModelSeries.add(/* Math.log10( */period/* ) */, lowModel);
      }
      double highModel = NoiseModel.fnhnm(period);
      if (highModel != 0) {
        highNoiseModelSeries.add(/* Math.log10( */period/* ) */, highModel);
      }
      i++;
    }
    // Adding tail of noise models, besides psd graph ending, and 1000 s
    // value
    i = 2;
    while ((period = i / (freqArr[1])) < 1000) {
      double lowModel = NoiseModel.fnlnm(period);
      if (lowModel != 0.0) {
        lowNoiseModelSeries.add(period, lowModel);
      }
      double highModel = NoiseModel.fnhnm(period);
      if (highModel != 0) {
        highNoiseModelSeries.add(period, highModel);
      }
      i++;
    }
    ret.addSeries(lowNoiseModelSeries);
    ret.addSeries(highNoiseModelSeries);
    return ret;
  }

  private MyOptionPane createChartPanel(XYDataset dataset, List<PlotDataProvider> input) {
    chart = ChartFactory.createXYLineChart(null, // title
        "Period, s", // x-axis label
        "Power spectra density (DB relative to 1m/s\u00B2)", // y-axis label
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
    domainAxis.setRange(new Range(0.01, 10000.0));
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
    return new MyOptionPane(cp, input);
  }

  /**
   * Handles a chart progress event.
   *
   * @param event the event.
   */
  @Override
  public void chartProgress(ChartProgressEvent event) {
    if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
      return;
    }
    if (chart != null) {
      XYPlot plot = (XYPlot) chart.getPlot();
      double xx = plot.getDomainCrosshairValue();
      double yy = plot.getRangeCrosshairValue();
      // update the screen...
      if (xx != 0.0 && yy != 0.0) {
        getCrosshairPositionL().setText(
            "  Period: " + screenDataFormat.format(xx) + " s, PSD: " + screenDataFormat.format(yy));
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

  private double getValue(int series, double arg) {
    for (int i = 0; i < dataset.getItemCount(series); i++) {
      if (arg < dataset.getXValue(series, i)) {
        if (i > 0) {
          return dataset.getYValue(series, i - 1)
              + ((dataset.getYValue(series, i) - dataset.getYValue(series, i - 1))
              * (arg - dataset.getXValue(series, i - 1))
              / (dataset.getXValue(series, i) - dataset.getXValue(series, i - 1)));
        }
      }
    }
    return Double.NaN;
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    setVisibleItems();
  }

  private class PSDChannelViewFactory implements IChannelViewFactory {

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

  private JPanel instantiateSmoothingButtons() {
    // instantiate the buttongroup here
    if (buttonGroup == null) {
      buttonGroup = new ButtonGroup();
      buttonGroup.add(getSmoothRB());
      buttonGroup.add(getRawRB());
      buttonGroup.add(getRawAndSmoothRB());
    }

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(getSmoothRB());
    buttonPanel.add(getRawRB());
    buttonPanel.add(getRawAndSmoothRB());

    JLabel smoothingDetail = new JLabel();
    smoothingDetail.setText("(Smoothing parameter is 1/" +
        (int) IstiUtilsMath.SMOOTHING_FACTOR +  " of octave per-point)");
    smoothingDetail.setMaximumSize(smoothingDetail.getMinimumSize());
    buttonPanel.add(smoothingDetail);


    buttonPanel.setMaximumSize(buttonPanel.getMinimumSize());
    buttonPanel.setPreferredSize(buttonPanel.getMinimumSize());

    return buttonPanel;
  }

  private class MyOptionPane extends JPanel implements Printable {

    private static final long serialVersionUID = 1L;
    private final TraceViewChartPanel cp;
    private JPanel waveP = null;
    private final List<PlotDataProvider> input;

    private MyOptionPane(TraceViewChartPanel cp, List<PlotDataProvider> input) {
      this.cp = cp;
      Dimension d = cp.getPreferredSize();
      int maxDim = (int) Math.max(d.getHeight(), d.getWidth());
      cp.setPreferredSize(new Dimension(maxDim, maxDim));
      this.input = input;
      BoxLayout mLayout = new BoxLayout(this, javax.swing.BoxLayout.Y_AXIS);
      setLayout(mLayout);
      add(cp);
      if (showWaves) {
        add(getWavePanel());
      }
      // add options for which (smooth vs. unsmooth) data should be shown
      add(instantiateSmoothingButtons());
      add(getCrosshairPositionL());
      setVisibleItems();
    }

    JPanel getWavePanel() {
      if (waveP == null) {
        waveP = new JPanel();
        BoxLayout oLayout = new BoxLayout(waveP, javax.swing.BoxLayout.Y_AXIS);
        waveP.setLayout(oLayout);
        GraphPanel gp = new GraphPanel(false);
        gp.setChannelViewFactory(new PSDChannelViewFactory());
        gp.setShowBigCursor(false);
        gp.setColorMode(new ColorModeFixed());
        gp.setScaleMode(new ScaleModeAuto());
        gp.setBackground(Color.WHITE);
        gp.setChannelShowSet(input);
        gp.setTimeRange(ti);
        waveP.add(gp);
        waveP.setMaximumSize(new java.awt.Dimension(32767, 100));
        waveP.setPreferredSize(new java.awt.Dimension(this.getWidth(), 100));
      }
      return waveP;
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) {
      if (pageIndex != 0) {
        return NO_SUCH_PAGE;
      }
      double factor;
      if (showWaves) {
        factor = (double) cp.getHeight() / (waveP.getHeight() + cp.getHeight());
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
        g2.scale(w / waveP.getWidth(),
            (pf.getImageableHeight() * (1 - factor)) / waveP.getHeight());
        waveP.paint(g2);
      }
      
      return PAGE_EXISTS;
    }
  }


}
