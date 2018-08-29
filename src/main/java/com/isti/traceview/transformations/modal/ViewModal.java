package com.isti.traceview.transformations.modal;

import com.isti.jevalresp.OutputGenerator;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.SacTimeSeriesASCII;
import com.isti.traceview.gui.GraphUtil;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Spectra;
import com.isti.xmax.XMAX;
import com.isti.xmax.XMAXconfiguration;
import com.isti.xmax.gui.XMAXframe;
import java.awt.BasicStroke;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

public class ViewModal extends JDialog implements PropertyChangeListener, ChangeListener {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(ViewModal.class);
  private static final int INIT_MIN_PERIOD = 900;
  private static final int INIT_MAX_PERIOD = 3500;
  private static final int MIN_DISTANCE = 100;

  private JOptionPane optionPane;
  private TimeInterval ti;
  private List<Spectra> data;
  private TraceViewChartPanel chartPanel = null; // set as a side effect of createChartPanel
  private JSpinner minSpinner, maxSpinner;

  private static final double[] MODE_VALUES = new double[] {
      0.8143, 0.30945, 0.46850, 0.64682, 0.68020, 0.84002, 0.93997, 0.94435, 0.37970, 0.58630,
      0.76593, 0.98245};
  private static final String[] MODE_NAMES = new String[] {
      "0S0", "0S2", "0S3", "0S4", "1S2", "0S5", "1S3", "3S1", "0T2", "0T3", "0T4", "0T5"};
  private static final DecimalFormat psnFormat1 = new DecimalFormat("0.00000E00");
  private static final DecimalFormat psnFormat2 = new DecimalFormat("#####.####");


  public ViewModal(Frame owner, List<Spectra> data, TimeInterval ti,
      List<PlotDataProvider> input) {
    super(owner, "Normal Mode PSD Comparison", true);
    this.ti = ti;
    this.data = data;

    minSpinner = new JSpinner(
        new SpinnerNumberModel(INIT_MIN_PERIOD, INIT_MIN_PERIOD, (INIT_MAX_PERIOD - MIN_DISTANCE), MIN_DISTANCE)
    );
    maxSpinner = new JSpinner(
        new SpinnerNumberModel(INIT_MAX_PERIOD, (INIT_MIN_PERIOD + MIN_DISTANCE), INIT_MAX_PERIOD, MIN_DISTANCE)
    );

    // create parent panel holding (subpanel of) spinners and the chart panel
    // (create a subpanel of the spinners)
    JPanel spinnerPanel = new JPanel();
    spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));
    spinnerPanel.add(minSpinner);
    spinnerPanel.add(maxSpinner);

    JPanel chartAndSpinner = new JPanel();
    chartAndSpinner.setLayout(new BoxLayout(chartAndSpinner, BoxLayout.Y_AXIS));
    chartAndSpinner.add(createChartPanel(createDataset(), ti));
    chartAndSpinner.add(spinnerPanel);

    Object[] options = {"Close", "Print", "Export PSD", "Export SAC", "Export GRAPH"};
    // Create the JOptionPane.
    optionPane = new JOptionPane(chartAndSpinner, JOptionPane.PLAIN_MESSAGE,
        JOptionPane.CLOSED_OPTION, null, options, options[0]);
    setContentPane(optionPane);
    optionPane.addPropertyChangeListener(this);
    minSpinner.addChangeListener(this);
    maxSpinner.addChangeListener(this);
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

  private JFreeChart createChart(XYSeriesCollection dataset, TimeInterval timeInterval) {
    double min = 0.;
    double max = 0.;
    double midpoint;
    for (int i = 0; i < dataset.getSeriesCount(); ++i) {
      XYSeries xys = dataset.getSeries(i);
      min = Math.min(min, xys.getMinY());
      max = Math.max(max, xys.getMaxY());
    }
    midpoint = (max - min) / 2.;
    JFreeChart chart = ChartFactory.createXYLineChart(null, // title
        "Period (s)", "Spectra",
        dataset, // dataset
        PlotOrientation.VERTICAL, // orientation
        true, // legend
        true, // tooltips
        false// include URLs
    );
    TextTitle title = new TextTitle("Start time: "
        + TimeInterval.formatDate(timeInterval.getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
        + ", Duration: " + timeInterval.convert());
    chart.setTitle(title);
    XYPlot plot = (XYPlot) chart.getPlot();
    for (int i = 0; i < MODE_VALUES.length; ++i) {
      // divide by 1000 and take the reciprocal to go from milliHz to seconds period
      double period = 1000. / MODE_VALUES[i];
      Marker marker = new ValueMarker(period);
      marker.setStroke(new BasicStroke((float) 1.5));
      XYTextAnnotation label = new XYTextAnnotation(MODE_NAMES[i], period, midpoint);
      label.setRotationAnchor(TextAnchor.BASELINE_CENTER);
      label.setTextAnchor(TextAnchor.BASELINE_CENTER);
      label.setRotationAngle(-3.14 / 2);
      plot.addDomainMarker(marker);
      plot.addAnnotation(label);
    }
    return chart;
  }

  private JPanel createChartPanel(XYSeriesCollection dataset, TimeInterval timeInterval) {
    JPanel ret = new JPanel();
    BoxLayout retLayout = new BoxLayout(ret, javax.swing.BoxLayout.Y_AXIS);
    ret.setLayout(retLayout);
    JFreeChart chart = createChart(dataset, timeInterval);
    chartPanel = new TraceViewChartPanel(chart, true);
    ret.add(chartPanel);
    return ret;
  }

  private XYSeriesCollection createDataset() {
    int minPeriod = (int) minSpinner.getValue();
    int maxPeriod = (int) maxSpinner.getValue();
    XYSeriesCollection ret = new XYSeriesCollection();
    for (Spectra spectra : data) {
      ret.addSeries( spectra.getSpectraSeriesTruncated(true, minPeriod, maxPeriod));
    }
    return ret;
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
      } else if (value.equals("Export PSD")) {
        XYSeriesCollection dataset = createDataset();
        for (int i = 0; i < dataset.getSeriesCount(); ++i) {
          BufferedOutputStream stream = null;
          String fileName = null;
          try {
            String seriesName = (String) dataset.getSeriesKey(i);
            fileName = XMAXconfiguration.getInstance().getOutputPath() + File.separator + "PSD_"
                + seriesName.replace("/", "_");
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
              stream.close();
            } catch (IOException e1) {
              // do nothing
              logger.error("Can't close buffered output stream");
            }
          }
        }
        JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data exported to PSD ascii", "Message",
            JOptionPane.INFORMATION_MESSAGE);
      } else if (value.equals("Export SAC")) {
        DataOutputStream ds = null;
        String fileName = null;
        try {
          XYSeriesCollection dataset = createDataset();
          for (int i = 0; i < dataset.getSeriesCount(); ++i) {
            // note that we do not have the noise models here so iterate through entire dataset
            String seriesName = (String) dataset.getSeriesKey(i);
            fileName = XMAXconfiguration.getInstance().getOutputPath() + File.separator + "PSD_"
                + seriesName.replace("/", "_") + ".SAC";
            ds = new DataOutputStream(new FileOutputStream(new File(fileName)));
            float ydata[] = new float[dataset.getItemCount(i)];
            float xdata[] = new float[dataset.getItemCount(i)];
            for (int j = 0; j < dataset.getItemCount(i); j++) {
              xdata[j] = new Double(dataset.getXValue(i, j)).floatValue();
              ydata[j] = new Double(dataset.getYValue(i, j)).floatValue();
            }
            SacTimeSeriesASCII sacAscii = SacTimeSeriesASCII.getSAC(data.get(i).getChannel(),
                ti.getStartTime(), xdata, ydata);
            sacAscii.writeHeader(ds);
            sacAscii.writeData(ds);
          }
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(XMAXframe.getInstance(),
              "Can't write file " + fileName + "; " + e1, "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
          try {
            ds.close();
          } catch (Exception e1) {
            // do nothing
            logger.error("Can't close data output stream");
          }
        }
        JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Data exported to SAC ascii", "Message",
            JOptionPane.INFORMATION_MESSAGE);
      } else if (value.equals("Export GRAPH")) {
        File exportFile = GraphUtil.saveGraphics((JPanel) optionPane.getMessage(),
            XMAX.getConfiguration().getUserDir("GRAPH"));
        if (exportFile != null) {
          XMAX.getConfiguration().setUserDir("GRAPH", exportFile.getParent());
        }
      }
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    // we remove change listeners for the spinner panels temporarily; we may need to set them
    // manually -- and make sure we have extra step at the bottom for cleanup
    boolean sourceWasSpinner = false;
    if (e.getSource() == minSpinner) {
      sourceWasSpinner = true;
      minSpinner.removeChangeListener(this);
      maxSpinner.removeChangeListener(this);
      int minValue = (int) minSpinner.getValue();
      int maxValue = (int) maxSpinner.getValue();
      // is there at least a 100 second gap between the two spinners now
      if (maxValue - minValue < MIN_DISTANCE) {
        // if there isn't, then move the one we didn't touch
        // note that min value here
        maxSpinner.setValue(Math.min(minValue + MIN_DISTANCE, INIT_MAX_PERIOD));
      }
    } else if (e.getSource() == maxSpinner) {
      sourceWasSpinner = true;
      minSpinner.removeChangeListener(this);
      maxSpinner.removeChangeListener(this);
      int minValue = (int) minSpinner.getValue();
      int maxValue = (int) maxSpinner.getValue();
      // is there at least a 100 second gap between the two spinners now
      if (maxValue - minValue < MIN_DISTANCE) {
        // if there isn't, then move the one we didn't touch
        // note that min value here
        minSpinner.setValue(Math.max(maxValue - MIN_DISTANCE, INIT_MIN_PERIOD));
      }
    }

    if (sourceWasSpinner) {
      // redraw chart with new values, and reset the spinners
      chartPanel.setChart(createChart(createDataset(), ti));
      minSpinner.addChangeListener(this);
      maxSpinner.addChangeListener(this);
    }
  }
}
