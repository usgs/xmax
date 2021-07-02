package com.isti.traceview.source;


import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.data.PlotDataProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class SourceSocketFDSNTest {

  @Test
  public void testSampleRatesCorrect() throws TraceViewException {
    TraceView.setConfiguration(new Configuration());
    // get data from 2021-001 for IU, ANMO, 00, *H* and make sure that we don't coercesamplerate
    long start = Instant.parse("2021-01-01T00:00:00.00Z").toEpochMilli();
    long end = Instant.parse("2021-01-02T00:00:00.00Z").toEpochMilli();
    SourceSocketFDSN fdsn = new SourceSocketFDSN("IU", "ANMO", "00",
        "*H*", start, end);
    List<PlotDataProvider> traces = new ArrayList<>(fdsn.parse());

    if (traces.isEmpty()){
      fail("Probable network error when contacting the IRIS FDSN server");
    }

    for (PlotDataProvider trace : traces) {
      String channel = trace.getChannelName();
      double sampleRate = trace.getRawData().get(0).getSampleRateHz();
      if (channel.startsWith("B")) {
        assertEquals(40, sampleRate, 0.);
      } else if (channel.startsWith("L")) {
        assertEquals(1, sampleRate, 0.);
      } else if (channel.startsWith("V")) {
        assertTrue(sampleRate < 1);
      } else {
        fail();
      }
    }

  }

}