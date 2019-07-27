package com.isti.traceview.filters;

import asl.utils.TimeSeriesUtils;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.BPFilterException;
import com.isti.traceview.processing.HPFilterException;
import com.isti.traceview.processing.LPFilterException;
import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class AbstractFilter implements IFilter {

  abstract double[] filterBackend(double[] data, int length)
      throws LPFilterException, HPFilterException, BPFilterException;

  /**
   * number of filter sections
   */
  double sampleRate = 0.;

  @Override
  public int getMaxDataLength() {
    return Integer.MAX_VALUE;
  }

  @Override
  synchronized public void init(RawDataProvider channel) {
    sampleRate = 1000.0 / channel.getSampleRate();
  }

  @Override
  synchronized public double[] filter(double[] data, int length)
      throws BPFilterException, HPFilterException, LPFilterException {

    double predemean = data[0];
    data = TimeSeriesUtils.demean(Arrays.copyOfRange(data, 0, length));
    double offset = predemean - data[0]; // difference here is the removed mean
    double[] returnArray = filterBackend(data, length);
    IntStream.range(0, length).parallel().forEach(i -> returnArray[i] += offset);
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
}
