package com.isti.traceview.transformations.modal;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Spectra;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;

/**
 * Power spectra density transformation with normal mode overlays. Prepares data for presentation in
 * {@link ViewModal}
 *
 * @author Max Kokoulin
 */
public class TransModal implements ITransformation {

  private static final Logger logger = Logger.getLogger(TransModal.class);
  public static final String NAME = "Normal mode PSD overlay";
  private static final boolean verboseDebug = false;

  private int effectiveLength = 0;

  @Override
  public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration,
      JFrame parentFrame) {
    if (input.size() == 0) {
      JOptionPane.showMessageDialog(parentFrame, "Please select channels", "PSD computation warning",
          JOptionPane.WARNING_MESSAGE);
    } else if (input.get(0).getDataLength(ti) < 32) {
      JOptionPane.showMessageDialog(parentFrame, "One or more of the traces you selected does not contain enough datapoints (<32). "
              + "Please select a longer dataset.", "PSD computation warning",
          JOptionPane.WARNING_MESSAGE);
    } else {
      try {
        List<Spectra> spList = createData(input, filter, ti, parentFrame);
        TimeInterval effectiveInterval = new TimeInterval(ti.getStart(),
            ti.getStart() + (long) (input.get(0).getSampleRate() * effectiveLength));
        @SuppressWarnings("unused")
        ViewModal vp = new ViewModal(parentFrame, spList, effectiveInterval, input);
      } catch (RuntimeException e) {
        if (!e.getMessage().equals("Operation cancelled")) {
          String message = "Mode plots encountered the following critical error:\n" +
              e.getMessage();
          JOptionPane.showMessageDialog(parentFrame, message, "Warning", JOptionPane.WARNING_MESSAGE);
        }
      }
    }
    ((XMAXframe) parentFrame).getGraphPanel().forceRepaint();
  }

  /**
   * Identical to createData function in TransSpectra.java; get amplitude of FFT calculations
   * @param input
   *            List of traces to process
   * @param filter
   *            Filter applied to traces before correlation
   * @param ti
   *            Time interval to define processed range
   * @param parentFrame
   *            parent frame
   * @return list of spectra for selected traces and time ranges
   */
  private List<Spectra> createData(List<PlotDataProvider> input, IFilter filter,
      TimeInterval ti, JFrame parentFrame) {

    List<Spectra> dataset = new ArrayList<>();

    input.forEach(channel -> {
      try {
        int[] intData = channel.getContinuousGaplessDataOverRange(ti);
        int dataSize;
        if (intData.length > maxDataLength) {
          dataSize = (int) Math.pow(2, (int) IstiUtilsMath.log2(maxDataLength));
          ((XMAXframe) parentFrame).getStatusBar().setMessage(
              "Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
        } else {
          dataSize = (int) Math.pow(2, (int) IstiUtilsMath.log2(intData.length));
        }
        effectiveLength = dataSize;

        logger.debug("data size = " + dataSize);
        int[] data = new int[dataSize];
        System.arraycopy(intData, 0, data, 0, dataSize);
        if (filter != null) {
          data = new FilterFacade(filter, channel).filter(data);
        }
        try {
          Spectra spectra = IstiUtilsMath.getNoiseSpectra(data, channel.getResponse(),
              ti.getStartTime(), channel, verboseDebug);
          dataset.add(spectra);
        } catch (TraceViewException e) {
          logger.error("Caught exception while iterating through transformation: ", e);
          throw new RuntimeException(e.getMessage(), e);
        }
      } catch (XMAXException e) {
        logger.error("Caught exception while iterating through transformation: ", e);
        throw new RuntimeException("Could not get gapless data for trace under analysis.", e);
      }
    });

    return dataset;
  }

  @Override
  public String getName() {
    return TransModal.NAME;
  }

}
