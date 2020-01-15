package com.isti.traceview.transformations.ppm;

import static org.junit.Assert.assertEquals;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.xmax.XMAXException;
import com.isti.xmax.XMAXconfiguration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TransPPMTest {


  @Before
  public void setUp() throws TraceViewException {
    // NOTE: currently not matching anything as resources here are not included anywhere
    String mask = "src/test/resources/ppm/*.seed";
    XMAXconfiguration config = XMAXconfiguration.getInstance();
    TraceView.setConfiguration(config);
    TraceView.getConfiguration().setDataPath(mask);
    TraceView.setDataModule(new DataModule());
    TraceView.getDataModule().loadAndParseDataForTesting();
  }

  @Test
  public void testBackAzimCorrect() throws XMAXException {

    DataModule dm = TraceView.getDataModule();

    List<PlotDataProvider> pdpList = dm.getAllChannels();

    String startTimeString = "2019.108.14:51Z";
    String endTimeString = "2019.108.14:52Z";
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.DDD.HH:mmX").withZone(ZoneOffset.UTC);
    long startTime = ZonedDateTime.parse(startTimeString, dtf).toInstant().toEpochMilli();
    long endTime = ZonedDateTime.parse(endTimeString, dtf).toInstant().toEpochMilli();
    TimeInterval ti = new TimeInterval(startTime, endTime);

    TransPPM ppm = new TransPPM();
    int[][] data = ppm.createDataset(pdpList, null, ti);
    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);

    assertEquals(73.1489, bAzimuth, 1E-3);

  }

  @Test
  public void testBackAzimCorrectQuadrant() throws XMAXException {
    DataModule dm = TraceView.getDataModule();

    List<PlotDataProvider> pdpList = dm.getAllChannels();

    String startTimeString = "2019.108.14:51Z";
    String endTimeString = "2019.108.14:52Z";
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.DDD.HH:mmX").withZone(ZoneOffset.UTC);
    long startTime = ZonedDateTime.parse(startTimeString, dtf).toInstant().toEpochMilli();
    long endTime = ZonedDateTime.parse(endTimeString, dtf).toInstant().toEpochMilli();
    TimeInterval ti = new TimeInterval(startTime, endTime);

    TransPPM ppm = new TransPPM();
    int[][] data = ppm.createDataset(pdpList, null, ti);
    for (int i = 0; i < data[0].length; ++i) {
      data[0][i] *= -1.;
    }
    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);

    assertEquals(180 - 73.1489, bAzimuth, 1E-3);
  }

}