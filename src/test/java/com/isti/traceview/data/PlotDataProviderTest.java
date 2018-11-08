package com.isti.traceview.data;

import static org.junit.Assert.*;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.xmax.data.XMAXDataModule;
import java.io.File;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;

public class PlotDataProviderTest {

  @Before
  public void setUp() {
    LogManager.getRootLogger().setLevel(Level.DEBUG);
    try {
      Configuration defaultConfig = new Configuration();
      TraceView.setConfiguration(defaultConfig);
    } catch (TraceViewException e) {
      System.out.println("Error in creating default config file");
    }
  }

  @Test
  public void findsGapCorrectly() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File fileWithGaps = new File(filename);
    assertTrue(fileWithGaps.getAbsolutePath(), fileWithGaps.getAbsoluteFile().exists());

    DataModule dm = new DataModule();
    dm.loadNewDataFromSources(fileWithGaps);

    RawDataProvider dataProvider = dm.getAllChannels().get(0);
    List<Segment> segments = dataProvider.getRawData();
    Segment firstSegment = segments.get(0);
    Segment lastSegment = segments.get(segments.size() - 1);
    assertTrue(Segment.isDataBreak(
        firstSegment.getEndTime().toInstant().toEpochMilli(),
        lastSegment.getStartTime().toInstant().toEpochMilli(),
        dataProvider.getSampleRate()
    ));
  }
}
