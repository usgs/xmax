package com.isti.traceview.data;

import static org.junit.Assert.*;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.TimeInterval;
import com.isti.xmax.data.XMAXDataModule;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.transform.Source;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class RawDataProviderTest {

  private void saveAndReloadDataMseed(String filename) {
    // TODO -- and will likely need to change method signature
    // this will save data and load in the new saved data
  }

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
  public void dumpMseed_trim() throws IOException, SeedFormatException, TraceViewException {


    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File("src/test/resources/00_LHZ.512.seed");

    dm.loadNewDataFromSources(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadNewDataFromSources(fileToModify);
    // dm.loadData();

    RawDataProvider data = dm.getAllChannels().get(0);
    System.out.println("DATA: " + data);
    List<Segment> segments = data.getRawData();
    assertEquals(1, segments.size());
    for (Segment segment : segments) {
      assertEquals(86400, segment.getSampleCount());
      assertNotNull(segment.getData().data);
    }
    TimeInterval initial = data.getTimeRange();

    long offset = data.getRawData().get(0).getStartOffset();

    // trim data by an hour
    long start = initial.getStart();
    long end = initial.getEnd();
    System.out.println(start + ", " + end);
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

    DataOutputStream ds = new DataOutputStream(new FileOutputStream(outputFile));
    data.dumpMseed(ds, cut, null, null);
    ds.close();
    dm = new DataModule();
    dm.loadNewDataFromSources(outputFile);
    long secondStart = dm.getAllChannels().get(0).getTimeRange().getStart();
    long secondEnd = dm.getAllChannels().get(0).getTimeRange().getEnd();
    int[] dataFromSecondFile = dm.getAllChannels().get(0).getRawData().get(0).getData().data;

    assertEquals(trimmedFromFirstFile.length, dataFromSecondFile.length);
    assertArrayEquals(trimmedFromFirstFile, dataFromSecondFile);
    assertEquals(end-start, secondEnd-secondStart);
    assertTrue(start-secondStart < 125); // small correction for leap-seconds?

    /*
    String filenameTest = "src/test/resources/trimmed.mseed";
    PlotDataProvider testAgainstThis = PlotDataProvider.load(filenameTest);

    List<Segment> rawTrimmed = trimmedData.getRawData();
    List<Segment> rawTestAgainst = testAgainstThis.getRawData();
    for (int i = 0; i < rawTrimmed.size(); ++i) {
      assertArrayEquals(rawTrimmed.get(i).getData().data, rawTestAgainst.get(i).getData().data);
    }

    // save data as miniseed and reload (see method above)
    // compare to trimmed.mseed file:
    // - are the start times the same?
    // - are the sample rates the same?
    // - are the data lengths the same?
    // - does the data match?
    */
    outputFile = new File(filename2);
    outputFile.delete();
  }

  @Test
  public void dumpMseed_filter() throws FileNotFoundException {
    // first, we need to load in the data (may be moved to set-up method)
    String filename = "src/test/resources/00_LHZ.512.seed";
    PlotDataProvider data = PlotDataProvider.load(filename);


    // then we may need to convert it (perform LPF)
    // save this converted data as miniseed
    // re-load that data

  }

  @Test
  public void dumpMseed_unchanged() throws IOException {
    String filename = "src/test/resources/00_LHZ.512.seed";
    PlotDataProvider data = PlotDataProvider.load(filename);

    String filename2 = "src/test/resources/unchanged_00_LHZ.512.seed";

    File outputFile = new File(filename2);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    DataOutputStream ds = new DataOutputStream(new FileOutputStream(outputFile));
    data.dumpMseed(ds, data.getTimeRange(), null, null);
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