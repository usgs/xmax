package com.isti.traceview.data;

import static org.junit.Assert.*;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.TimeInterval;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;

public class RawDataProviderTest {

  // full filepath of seed file used as basis for tests
  public static String startFileSeedPath = "src/test/resources/2018-270.00_LHZ.512.seed";

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
  public void testLoad() throws IOException {
    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File(startFileSeedPath);

    dm.loadNewDataFromSources(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadNewDataFromSources(fileToModify);
    // dm.loadData();

    RawDataProvider data = dm.getAllChannels().get(0);
    List<Segment> segments = data.getRawData();
    assertEquals(1, segments.size());
    for (Segment segment : segments) {
      assertEquals(86400, segment.getSampleCount());
      assertNotNull(segment.getData().data);
    }
    assertEquals(1538006400069L, data.getTimeRange().getStart());
  }

  @Test
  public void dumpMseed_trim() throws IOException, SeedFormatException, TraceViewException {

    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File(startFileSeedPath);

    dm.loadNewDataFromSources(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadNewDataFromSources(fileToModify);
    // dm.loadData();

    RawDataProvider data = dm.getAllChannels().get(0);
    TimeInterval initial = data.getTimeRange();

    // trim data by an hour
    long start = initial.getStart();
    long end = initial.getEnd();
    final long HOUR_IN_MS = 60 * 60 * 1000; // milliseconds in one hour
    start += HOUR_IN_MS; // trim the first hour
    end -= HOUR_IN_MS; // trim the last hour
    assertTrue(start < initial.getEnd());
    assertTrue(end > initial.getStart());
    TimeInterval cut = new TimeInterval(start, end);
    int[] trimmedFromFirstFile = data.getRawData().get(0).getData(cut).data;

    String filename2 = "src/test/resources/trimmed_00_LHZ.512.mseed";
    // make sure data from a previous test isn't lingering
    File outputFile = new File(filename2);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    for (int i = 0; i < 10; ++i) {
      DataOutputStream ds = new DataOutputStream(new FileOutputStream(outputFile));
      data.dumpMseed(ds, cut, null, null);
      ds.close();
      dm = new DataModule();
      dm.loadNewDataFromSources(outputFile);
      data = dm.getAllChannels().get(0);
      cut = data.getTimeRange();
    }

    long secondStart = dm.getAllChannels().get(0).getTimeRange().getStart();
    long secondEnd = dm.getAllChannels().get(0).getTimeRange().getEnd();
    int[] dataFromSecondFile = dm.getAllChannels().get(0).getUncutSegmentData(0);

    // clean up written file
    outputFile = new File(filename2);
    outputFile.delete();


    assertEquals(trimmedFromFirstFile.length, dataFromSecondFile.length);
    assertArrayEquals(trimmedFromFirstFile, dataFromSecondFile);
    assertEquals(end-start, secondEnd-secondStart);
    assertEquals(start, secondStart); // small correction for leap-seconds?

  }

  @Test
  public void dumpMseed_filter() throws FileNotFoundException {
    // first, we need to load in the data (may be moved to set-up method)
    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File(startFileSeedPath);

    dm.loadNewDataFromSources(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadNewDataFromSources(fileToModify);
    // dm.loadData();

    RawDataProvider data = dm.getAllChannels().get(0);
    List<Segment> segments = data.getRawData();
    assertEquals(1, segments.size());
    for (Segment segment : segments) {
      assertEquals(86400, segment.getSampleCount());
      assertNotNull(segment.getData().data);
    }


    // then we may need to convert it (perform LPF)
    // save this converted data as miniseed
    // re-load that data

  }

  @Test
  public void dumpMseed_unchanged() throws IOException {
    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File(startFileSeedPath);

    dm.loadNewDataFromSources(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadNewDataFromSources(fileToModify);
    // dm.loadData();

    RawDataProvider data = dm.getAllChannels().get(0);
    int[] dataFromFirstFile = dm.getAllChannels().get(0).getUncutSegmentData(0);

    TimeInterval initial = data.getTimeRange();
    long start = initial.getStart();
    long end = initial.getEnd();

    String filename2 = "src/test/resources/unchanged_00_LHZ.512.mseed";
    // make sure data from a previous test isn't lingering
    File outputFile = new File(filename2);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    for (int i = 0; i < 10; ++i) {
      DataOutputStream ds = new DataOutputStream(new FileOutputStream(outputFile));
      data.dumpMseed(ds, data.getTimeRange(), null, null);
      dm = new DataModule();
      dm.loadNewDataFromSources(outputFile);
      data = dm.getAllChannels().get(0);
    }

    long secondStart = dm.getAllChannels().get(0).getTimeRange().getStart();
    long secondEnd = dm.getAllChannels().get(0).getTimeRange().getEnd();
    int[] dataFromSecondFile = dm.getAllChannels().get(0).getUncutSegmentData(0);

    // clean up the written in file
    outputFile = new File(filename2);
    outputFile.delete();

    assertEquals(dataFromFirstFile.length, dataFromSecondFile.length);
    assertArrayEquals(dataFromFirstFile, dataFromSecondFile);
    assertEquals(end-start, secondEnd-secondStart);
    assertEquals(start, secondStart); // small correction for leap-seconds?
  }

  @Test
  public void dumpASCII() {
    // TODO
  }

  @Test
  public void dumpXML() {
    // TODO
  }

  @Test
  public void dumpSacAscii() {
    // TODO
  }
}