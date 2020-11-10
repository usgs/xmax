package com.isti.traceview.filters;

import asl.utils.NumericUtils;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.BPFilterException;
import com.isti.traceview.processing.HPFilterException;
import com.isti.traceview.processing.LPFilterException;
import java.util.Arrays;
import java.util.stream.IntStream;
import uk.me.berndporr.iirj.Butterworth;

public abstract class AbstractFilter implements IFilter {

  double[] filterBackend(double[] data, int length)
      throws LPFilterException, HPFilterException, BPFilterException {
    if (length > data.length)
      throw new BPFilterException("Requested filtering length exceeds provided array length");

    double[] dataToReturn = new double[data.length];
    Butterworth casc = getCurrentFilter();
    for (int i = 0; i < data.length; ++i) {
      dataToReturn[i] = casc.filter(data[i]);
    }
    return dataToReturn;
  }

  /**
   * Get the current filter for the given parameters for use on the active segment
   * @return
   */
  Butterworth getCurrentFilter() {
    return casc;
  }


  /**
   * Instantiate a new filter for the given parameters (after a gap, etc.)
   * @return
   */
  public abstract void reinitializeFilter();

  /**
   * number of filter sections
   */
  double sampleRate = 0.;
  Butterworth casc;
  // double offset = 0;
  boolean newPoint = false;
  private boolean initialized = false;

  @Override
  public int getMaxDataLength() {
    return Integer.MAX_VALUE;
  }

  @Override
  synchronized public void init(RawDataProvider channel) {
    sampleRate = 1000.0 / channel.getSampleRate();
    newPoint = true;
    reinitializeFilter();
    initialized = true;
  }

  public boolean isInitialized() {
    return initialized;
  }

  @Override
  synchronized public double[] filter(double[] data, int length)
      throws BPFilterException, HPFilterException, LPFilterException {

    /*
     */
    double[] returnArray = filterBackend(data, length);
    // IntStream.range(0, length).parallel().forEach(i -> returnArray[i] += offset);
    return returnArray;
  }

  @Override
  public boolean needProcessing() {
    return false;
  }

  @Override
  public String getName() {
    return null;
  }

  public enum FilterType {
    LOWPASS, HIGHPASS, BANDPASS;
  }
}
