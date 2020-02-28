package com.isti.traceview.transformations.psd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.processing.Rotation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.XMAXconfiguration;
import edu.sc.seis.seisFile.segd.Trace;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jfree.data.xy.XYSeries;
import org.junit.Before;
import org.junit.Test;

public class TransPSDTest {



  @Before
  public void setUp() throws TraceViewException {
    // NOTE: currently not matching anything as resources here are not included anywhere
    String mask = "src/test/resources/psd/*.seed";
    String resp = "src/test/resources/psd";
    XMAXconfiguration config = XMAXconfiguration.getInstance();
    config.setStationXMLPreferred(false);
    TraceView.setConfiguration(config);
    TraceView.getConfiguration().setDataPath(mask);
    TraceView.getConfiguration().setResponsePath(resp);
    TraceView.setDataModule(new DataModule());
    TraceView.getDataModule().loadAndParseDataForTesting();

  }

  @Test
  public void testRotate() throws IOException, TraceViewException, XMAXException {

    DataModule dm = TraceView.getDataModule();
    // we will test to see if the data matches up with PSD on the LH1 orientation
    int firstIndex = -1;
    int secondIndex = -1;

    List<PlotDataProvider> pdpList = dm.getAllChannels();
    Rotation firstRotation = new Rotation(-41);
    Rotation secondRotation = new Rotation(-281);
    for (int i = 0; i < pdpList.size(); ++i) {
      PlotDataProvider pdp = pdpList.get(i);
      if (pdp.getName().contains("00/LH1")) {
        firstIndex = i;
        pdp.setRotation(firstRotation);
      } else if (pdp.getName().contains("10/LH1")) {
        secondIndex = i;
        pdp.setRotation(secondRotation);
      }/* else if (pdp.getName().contains("00/LH2")) {
        pdp.setRotation(firstRotation);
      } else if(pdp.getName().contains("10/LH2")) {
        pdp.setRotation(new Rotation(-281.));
      }*/
    }

    long startMs = TimeInterval.getTime(2019, 60, 5, 42, 0, 0);
    long endMs = TimeInterval.getTime(2019, 60, 6, 42, 0, 0);

    TimeInterval ti = new TimeInterval(startMs, endMs);
    List<PlotDataProvider> toPSD = new ArrayList<>();
    toPSD.add(pdpList.get(firstIndex));
    toPSD.add(pdpList.get(secondIndex));

    for (PlotDataProvider pdp : toPSD) {
      assertTrue(pdp.isRotated());
    }

    TransPSD psd = new TransPSD();
    List<XYSeries> psdData = psd.createData(toPSD, null, ti, null);

    // first two entries are going to be the smoothed data for these entries rather than raw
    XYSeries firstSeries = psdData.get(2);
    XYSeries secondSeries = psdData.get(3);

    for (int i = 0; i < firstSeries.getItemCount(); ++i) {
      double x1 = (Double) firstSeries.getX(i);
      double x2 = (Double) secondSeries.getX(i);
      assertEquals(x1, x2, 1E-10);
      double y1 = (Double) firstSeries.getY(i);
      double y2 = (Double) secondSeries.getY(i);

      if (x1 <= 10 && x1 >= 5) {
        assertEquals("Discrepancy between values found at period: " + x1, y1, y2, 1.1);
      }

    }
  }

  @Test
  public void testSmoothingValid() throws XMAXException {
    DataModule dm = TraceView.getDataModule();
    List<PlotDataProvider> pdpList = dm.getAllChannels();
    pdpList = pdpList.subList(0, 1);
    TimeInterval ti = pdpList.get(0).getTimeRange();
    TransPSD psd = new TransPSD();
    List<XYSeries> psdData = psd.createData(pdpList, null, ti, null);
    assertEquals(2, psdData.size());
    assertEquals("IU/COLA/00/LH1 smoothed", psdData.get(0).getKey().toString());
    double[] yValues = psdData.get(1).toArray()[1];
    boolean yValuesAllNonzeroNumeric = true;
    for (double y : yValues) {
      if (y != 0 && !Double.isNaN(y)) {
        yValuesAllNonzeroNumeric = false;
        break;
      }
    }
    assertFalse(yValuesAllNonzeroNumeric);
  }

  @Test
  public void testTimingOfPSDSmoothing() throws XMAXException, TraceViewException {
    TraceView.getDataModule().deleteChannels(TraceView.getDataModule().getAllChannels());
    TraceView.getConfiguration().setDataPath("src/test/resources/rotation/unrot*.seed");
    TraceView.getConfiguration().setResponsePath("src/test/resources/rotation");
    TraceView.getDataModule().loadAndParseDataForTesting();
    DataModule dm = TraceView.getDataModule();
    List<PlotDataProvider> pdpList = dm.getAllChannels();
    TimeInterval ti = pdpList.get(0).getTimeRange();

    TransPSD.setPerformSmoothing(false);
    TransPSD psd = new TransPSD();
    long timeUnsmoothedStart = System.currentTimeMillis();
    List<XYSeries> psdData = psd.createData(pdpList, null, ti, null);
    long timeUnsmoothedEnd = System.currentTimeMillis();
    for (XYSeries psdDatum : psdData) {
      assertFalse(psdDatum.getKey().toString().contains("Smoothed"));
    }

    TransPSD.setPerformSmoothing(true);
    long timeSmoothedStart = System.currentTimeMillis();
    psdData = psd.createData(pdpList, null, ti, null);
    long timeSmoothedEnd = System.currentTimeMillis();
    assertEquals(pdpList.size() * 2, psdData.size());
    System.out.println(timeUnsmoothedEnd - timeUnsmoothedStart);
    System.out.println(timeSmoothedEnd - timeSmoothedStart);
  }

}