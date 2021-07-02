package com.isti.traceview.filters;

import com.isti.traceview.data.RawDataProvider;

public abstract class AbstractFilter {
  double sampleRate = 0.;

  public int getMaxDataLength() {
    return Integer.MAX_VALUE;
  }


  public void init(RawDataProvider channel) {
    sampleRate = channel.getSampleRate();
  }

  //Temporary until filters rewritten
  public void init2(double newsampleRateHz) {
    sampleRate = newsampleRateHz;
  }


  public String getName() {
    return null;
  }

}
