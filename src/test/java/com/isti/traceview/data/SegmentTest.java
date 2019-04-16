package com.isti.traceview.data;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SegmentTest {

  @Test
  public void testBasicSegmentCreatedCorrectly() {

    int[] dataArray = new int[]{1,2,3,4,5};
    double sampleRate = 1.;
    long startTime = (long) sampleRate * dataArray.length;
    Segment test = new Segment(dataArray, 1000L * startTime, sampleRate);

    assertEquals(5000L, test.getStartTime().getTime());
    assertEquals(10000L, test.getEndTime().getTime());
    assertArrayEquals(dataArray, test.getData().data);

  }

}
