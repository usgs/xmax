package com.isti.traceview.transformations.psd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.isti.jevalresp.OutputGenerator;
import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.processing.Rotation;
import com.isti.traceview.processing.Spectra;
import com.isti.xmax.XMAXException;
import com.isti.xmax.XMAXconfiguration;
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
    TraceView.setConfiguration(config);
    TraceView.getConfiguration().setDataPath(mask);
    TraceView.getConfiguration().setResponsePath(resp);
    TraceView.setDataModule(new DataModule());
    TraceView.getDataModule().loadNewDataFromSources();

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
    List<Spectra> psdData = psd.createData(toPSD, null, ti, null);
    Spectra firstSpectra = psdData.get(0);
    Spectra secondSpectra = psdData.get(1);


    XYSeries firstSeries = firstSpectra.getPSDSeries(OutputGenerator.VELOCITY_UNIT_CONV);
    XYSeries secondSeries = secondSpectra.getPSDSeries(OutputGenerator.VELOCITY_UNIT_CONV);

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

}