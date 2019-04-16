package com.isti.traceview.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.filters.FilterLP;
import com.isti.traceview.processing.LPFilterException;
import com.isti.traceview.processing.Rotation;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
  public void testSegmentCacheComparator() {
    int[] dataArray = new int[]{1,2,3,4,5};
    double sampleRate = 1000L; // 1Hz data -- 1000 ms sampling interval
    long startTime = (long) sampleRate * dataArray.length;
    List<SegmentCache> cache = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      cache.add(new SegmentCache(new Segment(dataArray, i * startTime, sampleRate)));
    }
    Collections.sort(cache);
    for (int i = 1; i < cache.size(); ++i) {
      // assert that new segment starts when previous segment ends
      // and that segments are strictly increasing by start time
      // (so technically this also tests that segment end times are calculated correctly)
      assertEquals(cache.get(i).getSegment().getStartTime().getTime(),
          cache.get(i-1).getSegment().getEndTime().getTime());
      assertTrue(cache.get(i).getSegment().getStartTime().getTime() >
          cache.get(i-1).getSegment().getStartTime().getTime());
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
      assertEquals(segment.getSampleRate(), 1000., 0.1);
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
  public void dumpMseed_filter() throws IOException, LPFilterException {
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
    TimeInterval ti = data.getTimeRange();
    int sampleCount = segments.get(0).getSampleCount();

    FilterLP lowPass = new FilterLP();
    lowPass.init(data);

    double[] unfilteredExpected = new double[sampleCount];
    for (int i = 0; i < unfilteredExpected.length; ++i) {
      unfilteredExpected[i] = segments.get(0).getData().data[i];
    }
    double[] filteredExpected = lowPass.filter(unfilteredExpected, sampleCount);
    System.out.println("How much filtered data? " + filteredExpected.length);
    System.out.println("What's the sample rate? " + data.getSampleRate());
    for (int i = 0; i < filteredExpected.length; ++i) {
      assertNotEquals(segments.get(0).getData().data[i], filteredExpected[i]);
    }

    String filename2 = "src/test/resources/filtered_00_LHZ.512.mseed";
    // make sure data from a previous test isn't lingering
    File outputFile = new File(filename2);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    // write out filtered data
    DataOutputStream ds = new DataOutputStream(new FileOutputStream(outputFile));
    data.dumpMseed(ds, data.getTimeRange(), lowPass, null);
    dm = new DataModule();
    dm.loadNewDataFromSources(outputFile);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    data = dm.getAllChannels().get(0);
    segments = data.getRawData();
    assertEquals(1, segments.size());
    for (Segment segment : segments) {
      assertEquals(86400, segment.getSampleCount());
      assertNotNull(segment.getData().data);
    }
    double[] filteredLoaded = new double[segments.get(0).getData().data.length];
    for (int i = 0; i < filteredLoaded.length; ++i) {
      filteredLoaded[i] = segments.get(0).getData().data[i];
    }

    assertArrayEquals(filteredExpected, filteredLoaded, 1.);


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
  public void dumpMseed_rotated() throws IOException {

    String folderStructure = "src/test/resources/rotation/";

    DataModule dm = new DataModule();
    String originalNorthFilename = folderStructure + "unrot_10_BH1.512.seed";
    String originalEastFilename = folderStructure + "unrot_10_BH2.512.seed";
    String originalVertFilename = folderStructure + "unrot_10_BHZ.512.seed";

    // first, we need to load in the data (may be moved to set-up method)
    File originalNorthFile = new File(originalNorthFilename);
    File originalEastFile = new File(originalEastFilename);
    File originalVertFile = new File(originalVertFilename);
    assertTrue(originalEastFile.exists() &&
        originalNorthFile.exists() && originalVertFile.exists());

    dm.loadNewDataFromSources(originalNorthFile, originalEastFile, originalVertFile);
    // now that we have the data, rotate it
    Rotation twentyDegreesRotation = new Rotation(20.);
    for (PlotDataProvider dataProvider : dm.getAllChannels().subList(0, 2)) {
      dataProvider.setRotation(twentyDegreesRotation);
    }

    String filenameNorth = folderStructure + "rotated_north_output.mseed";
    String filenameEast = folderStructure + "rotated_east_output.mseed";
    // make sure data from a previous test isn't lingering
    File outputFileNorth = new File(filenameNorth);
    File outputFileEast = new File(filenameEast);
    if (outputFileNorth.exists()) {
      outputFileNorth.delete();
    }
    if (outputFileEast.exists()) {
      outputFileEast.delete();
    }

    TraceView.setDataModule(dm); // step used to get matching pairs for horiz. rotation

    RawDataProvider dataNorth = dm.getAllChannels().get(0);
    TimeInterval ti = dataNorth.getTimeRange();
    assertEquals("BH1", dataNorth.getChannelName());
    DataOutputStream dsNorth = new DataOutputStream(new FileOutputStream(outputFileNorth));
    dataNorth.dumpMseed(dsNorth, ti, null, twentyDegreesRotation);

    RawDataProvider dataEast = dm.getAllChannels().get(1);
    assertEquals("BH2", dataEast.getChannelName());
    DataOutputStream dsEast = new DataOutputStream(new FileOutputStream(outputFileEast));
    dataEast.dumpMseed(dsEast, ti, null, twentyDegreesRotation);

    dm = new DataModule();
    dm.loadNewDataFromSources(outputFileNorth, outputFileEast);
    // now the data in dataNorth, dataEast is rotated data (by 20 degrees)
    dataNorth = dm.getAllChannels().get(0);
    dataEast = dm.getAllChannels().get(1);

    int[] rawDataNorth = dataNorth.getRawData(ti).get(0).getData().data;
    int[] rawDataEast = dataEast.getRawData(ti).get(0).getData().data;

    // delete the files now that we've read them in
    outputFileNorth = new File(filenameNorth);
    outputFileEast = new File(filenameEast);
    if (outputFileNorth.exists()) {
      outputFileNorth.delete();
    }
    if (outputFileEast.exists()) {
      outputFileEast.delete();
    }

    String rotatedNorthFilename = folderStructure + "rotated-20-deg_10.BH1.512.seed";
    String rotatedEastFilename = folderStructure + "rotated-20-deg_10.BH2.512.seed";
    // first, we need to load in the data (may be moved to set-up method)
    File expectedDataNorthFile = new File(rotatedNorthFilename);
    File expectedDataEastFile = new File(rotatedEastFilename);
    assertTrue(expectedDataEastFile.exists());
    assertTrue(expectedDataNorthFile.exists());
    DataModule holdsExpectedData = new DataModule();
    holdsExpectedData.loadNewDataFromSources(expectedDataNorthFile, expectedDataEastFile);

    int[] expectedDataNorth =
        holdsExpectedData.getAllChannels().get(0).getRawData(ti).get(0).getData().data;
    assertEquals("BH1", holdsExpectedData.getAllChannels().get(0).getChannelName());
    int[] expectedDataEast =
        holdsExpectedData.getAllChannels().get(1).getRawData(ti).get(0).getData().data;

    // we'll skip over every 40 points
    for (int i = 0; i < expectedDataNorth.length; i+=40) {
      assertEquals("Discrepancy found at north data index " + i + "(of " +
          expectedDataNorth.length + ")", (double) expectedDataNorth[i],
          (double) rawDataNorth[i], 220);
      assertEquals("Discrepancy found at east data index " + i + "(of " +
          expectedDataNorth.length + ")", (double) expectedDataEast[i],
          (double) rawDataEast[i], 220);
    }

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