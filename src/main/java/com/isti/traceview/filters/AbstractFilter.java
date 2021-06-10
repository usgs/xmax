package com.isti.traceview.filters;

import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.BPFilterException;
import uk.me.berndporr.iirj.Butterworth;

public abstract class AbstractFilter implements IFilter {
  double sampleRate = 0.;

  @Override
  public int getMaxDataLength() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void init(RawDataProvider channel) {
    sampleRate = channel.getSampleRate();
  }

  //Temporary until filters rewritten
  public void init2(double newsampleRateHz) {
    sampleRate = newsampleRateHz;
  }

  @Override
  public String getName() {
    return null;
  }

}
