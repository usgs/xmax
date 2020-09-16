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
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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

  public static void assertModCongruent(double expected, double result, double error, int modulus) {
    while (expected < 0) {
      expected += modulus;
    } while (expected >= modulus) {
      expected -= modulus;
    }

    while (result < 0) {
      result += modulus;
    } while (result >= modulus) {
      result -= modulus;
    }

    assertEquals(expected, result, error);
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

    XYSeriesCollection dataset = new XYSeriesCollection();
    XYSeries series = new XYSeries("series", false);
    series.add(45, 1);
    dataset.addSeries(series);

    TransPPM ppm = new TransPPM();
    double[][] data = ppm.createDataset(pdpList, null, ti);
    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);

    assertEquals(72.94367, bAzimuth, 1E-3);
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
    double[][] data = ppm.createDataset(pdpList, null, ti);
    for (int i = 0; i < data[0].length; ++i) {
      data[0][i] *= -1.;
    }
    XYSeriesCollection dataset = new XYSeriesCollection();
    XYSeries series = new XYSeries("series", false);
    series.add(120, 1);
    dataset.addSeries(series);

    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);
    assertModCongruent(-72.94367, bAzimuth, 1E-3, 180);
  }

  // some particularly basic tests to ensure that quadrant correction is unnecessary

  @Test
  public void testQuadrantCorrection0() {
    double[][] data = new double[][] {{0, 1}, {0, 0}};

    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);
    assertModCongruent(0, bAzimuth, 1E-3, 180);
  }

  @Test
  public void testQuadrantCorrection45() {
    double[][] data = new double[][] {{0, 1},{0, 1}};

    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);
    assertModCongruent(45, bAzimuth, 1E-3, 180);
  }

  @Test
  public void testQuadrantCorrection90() {
    double[][] data = new double[][] {{0, 0}, {0, 1}};

    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);
    assertModCongruent(90, bAzimuth, 1E-3, 180);
  }

  @Test
  public void testQuadrantCorrection135() {
    double[][] data = new double[][] {{1, 0}, {-1, 0}};

    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);
    assertModCongruent(135, bAzimuth, 1E-3, 180);
  }

  @Test
  public void testQuadrantCorrection180() {
    double[][] data = new double[][]{{0, -1}, {0, 0}};

    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);
    assertModCongruent(180, bAzimuth, 1E-3, 180);
  }

  @Test
  public void testQuadrantCorrection270() {
    double[][] data = new double[][]{{0, 0}, {0, -1}};

    double bAzimuth = TransPPM.estimateBackAzimuth(data[0], data[1]);
    assertModCongruent(270, bAzimuth, 1E-3, 180);
  }

}