package com.isti.traceview.data;

import static org.junit.Assert.assertEquals;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.data.ISource.FormatType;
import com.isti.traceview.source.SourceFileMseed;
import java.io.File;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;

public class SourceFileMseedTest {

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
  public void getFormatType() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File file = new File(filename);

    SourceFileMseed sourceFileMseed = new SourceFileMseed(file);

    assertEquals(sourceFileMseed.getFormatType(), FormatType.MSEED);
  }

  @Test
  public void parse_finds_valid_channel() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File file = new File(filename);

    SourceFileMseed sourceFileMseed = new SourceFileMseed(file);

    Set<PlotDataProvider> dataProviders = sourceFileMseed.parse();

    assertEquals(1, dataProviders.size());
  }
  @Test
  public void parse_skips_invalid_channel() {
    String filename = "src/test/resources/93_OCF_NO_SAMPLES.512.seed";
    File file = new File(filename);

    SourceFileMseed sourceFileMseed = new SourceFileMseed(file);

    Set<PlotDataProvider> dataProviders = sourceFileMseed.parse();

    assertEquals(0, dataProviders.size());
  }


  @Test
  public void toString_returns_expected() {

    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File file = new File(filename);

    SourceFileMseed sourceFileMseed = new SourceFileMseed(file);

    assertEquals("MseedRawDataProvider: file ANMO_00_LHZ_GAP.512.seed", sourceFileMseed.toString());

    sourceFileMseed = new SourceFileMseed(null);

    assertEquals("MseedRawDataProvider: file absent", sourceFileMseed.toString());
  }
}