package com.isti.traceview.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import java.io.File;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;

public class ChannelTest {

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
  public void channelNameCorrect() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File fileWithGaps = new File(filename);
    assertTrue(fileWithGaps.getAbsolutePath(), fileWithGaps.getAbsoluteFile().exists());

    DataModule dm = new DataModule();
    dm.loadAndParseDataForTesting(fileWithGaps);

    RawDataProvider dataProvider = dm.getAllChannels().get(0);
    assertEquals("LHZ", dataProvider.getChannelName());
  }

  @Test
  public void channelFullNameCorrect() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File fileWithGaps = new File(filename);
    assertTrue(fileWithGaps.getAbsolutePath(), fileWithGaps.getAbsoluteFile().exists());

    DataModule dm = new DataModule();
    dm.loadAndParseDataForTesting(fileWithGaps);

    RawDataProvider dataProvider = dm.getAllChannels().get(0);
    assertEquals("IU/ANMO/00/LHZ", dataProvider.getName());
  }

  @Test
  public void channelSampleRateCorrect() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File fileWithGaps = new File(filename);
    assertTrue(fileWithGaps.getAbsolutePath(), fileWithGaps.getAbsoluteFile().exists());

    DataModule dm = new DataModule();
    dm.loadAndParseDataForTesting(fileWithGaps);

    RawDataProvider dataProvider = dm.getAllChannels().get(0);
    // LHZ = 1Hz data, or 1 sample every 1000 ms
    assertEquals(1000., dataProvider.getSampleInterval(), 1E-5);
  }

  @Test
  public void channelTypeCorrect() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File fileWithGaps = new File(filename);
    assertTrue(fileWithGaps.getAbsolutePath(), fileWithGaps.getAbsoluteFile().exists());

    DataModule dm = new DataModule();
    dm.loadAndParseDataForTesting(fileWithGaps);

    RawDataProvider dataProvider = dm.getAllChannels().get(0);
    // LHZ = 1Hz data, or 1 sample every 1000 ms
    assertEquals('Z', dataProvider.getType());
  }

}
