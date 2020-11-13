package com.isti.traceview.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.filters.FilterLP;
import com.isti.traceview.processing.BPFilterException;
import com.isti.traceview.processing.HPFilterException;
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

    dm.loadAndParseDataForTesting(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadAndParseDataForTesting(fileToModify);
    // dm.loadData();

    RawDataProvider data = dm.getAllChannels().get(0);
    List<Segment> segments = data.getRawData();
    assertEquals(279, segments.size());
    int dataPointCount = 0;
    for (Segment segment : segments) {
      assertEquals(segment.getSampleRate(), 1000., 0.1);
      dataPointCount += segment.getSampleCount();
      assertNotNull(segment.getData().data);
    }
    assertEquals(86400, dataPointCount);
    assertEquals(1538006400069L, data.getTimeRange().getStart());
  }

  @Test
  public void dumpMseed_trim() throws IOException, SeedFormatException, TraceViewException {

    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File(startFileSeedPath);

    dm.loadAndParseDataForTesting(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadAndParseDataForTesting(fileToModify);
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
    List<Segment> segs = data.getRawData(cut);
    List<Integer> points = new ArrayList<>();
    for (Segment seg : segs) {
      for (int point : seg.getData(cut).data) {
        points.add(point);
      }
    }

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
      dm.loadAndParseDataForTesting(outputFile);
      data = dm.getAllChannels().get(0);
      cut = data.getTimeRange();
    }

    long secondStart = dm.getAllChannels().get(0).getTimeRange().getStart();
    long secondEnd = dm.getAllChannels().get(0).getTimeRange().getEnd();
    List<Integer> dataFromSecondFile = new ArrayList<>();
    List<Segment> secondData = dm.getAllChannels().get(0).getRawData();
    for (Segment seg : secondData) {
      for (int point : seg.getData().data) {
        dataFromSecondFile.add(point);
      }
    }

    // clean up written file
    outputFile = new File(filename2);
    outputFile.delete();


    assertEquals(points.size(), dataFromSecondFile.size());
    assertEquals(points, dataFromSecondFile);
    assertEquals(end-start, secondEnd-secondStart);
    assertEquals(start, secondStart); // small correction for leap-seconds?

  }

  @Test
  public void dumpMseed_filter()
      throws IOException, LPFilterException, BPFilterException, HPFilterException {
    // first, we need to load in the data (may be moved to set-up method)
    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File(startFileSeedPath);

    dm.loadAndParseDataForTesting(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadAndParseDataForTesting(fileToModify);
    // dm.loadData();

    RawDataProvider data = dm.getAllChannels().get(0);
    List<Segment> segments = data.getRawData();
    assertEquals(279, segments.size());
    int sampleCount = 0;
    for (Segment segment : segments) {
      sampleCount += segment.getSampleCount();
      assertNotNull(segment.getData().data);
    }
    assertEquals(86400, sampleCount);
    TimeInterval ti = data.getTimeRange();

    FilterLP lowPass = new FilterLP();
    lowPass.init(data);

    int lastArrayPoint = 0;
    double[] unfilteredExpected = new double[sampleCount];
    for (Segment seg : segments) {
      for (int point : seg.getData(ti).data) {
        unfilteredExpected[lastArrayPoint++] = (double) point;
      }
    }

    double[] filteredExpected = lowPass.filter(unfilteredExpected, sampleCount);
    for (int i = 0; i < filteredExpected.length; ++i) {
      assertNotEquals(unfilteredExpected[i], filteredExpected[i]);
    }

    lowPass = new FilterLP();
    lowPass.init(data);
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
    dm.loadAndParseDataForTesting(outputFile);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    data = dm.getAllChannels().get(0);
    segments = data.getRawData();
    assertEquals(279, segments.size());
    sampleCount = 0;
    for (Segment segment : segments) {
      sampleCount += segment.getSampleCount();
      assertNotNull(segment.getData(ti).data);
    }
    assertEquals(86400, sampleCount);
    double[] filteredLoaded = new double[sampleCount];
    lastArrayPoint = 0;
    for (Segment seg : segments) {
      for (int point : seg.getData().data) {
        filteredLoaded[lastArrayPoint++] = (double) point;
      }
    }
    assertArrayEquals(filteredExpected, filteredLoaded, 1.);
  }

  @Test
  public void dumpMseed_unchanged() throws IOException {
    DataModule dm = new DataModule();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File(startFileSeedPath);

    dm.loadAndParseDataForTesting(fileToModify);

    assertTrue(fileToModify.getAbsolutePath(), fileToModify.getAbsoluteFile().exists());
    dm.loadAndParseDataForTesting(fileToModify);
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
      dm.loadAndParseDataForTesting(outputFile);
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
  public void mergeOverlappingData() {
    String folderStructure = "src/test/resources/overlaps/";

    DataModule dm = new DataModule();
    String day91FName = folderStructure + "91.00_LH1.512.seed";
    String day92FName = folderStructure + "92.00_LH1.512.seed";
    String day93FName = folderStructure + "93.00_LH1.512.seed";
    String day91And92Fname = folderStructure + "cat.00_LH1.512.seed";

    File day91File = new File(day91FName);
    File day92File = new File(day92FName);
    File day93File = new File(day93FName);
    File concattedFile = new File(day91And92Fname);

    // day 92's data will be loaded in from concatted file in gap between 91 and 93
    dm.loadAndParseDataForTesting(day91File, day93File, concattedFile, day92File);

    RawDataProvider data = dm.getAllChannels().get(0);

    List<Segment> segments = data.getRawData();
    double sampleInterval = segments.get(0).getSampleRate();
    for (int i = 1; i < segments.size(); ++i) {
      long previousEnd = segments.get(i-1).getEndTime().getTime();
      long thisStart = segments.get(i).getStartTime().getTime();
      assertFalse(Segment.isDataBreak(previousEnd, thisStart, sampleInterval));
    }

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

    dm.loadAndParseDataForTesting(originalNorthFile, originalEastFile, originalVertFile);
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
    // we'll compare over a couple segments only so this doesn't take a long time
    TimeInterval ti = new TimeInterval(
        dataNorth.getRawData().get(0).getStartTimeMillis(),
        dataNorth.getRawData().get(1).getEndTimeMillis()
    );
    assertEquals("BH1", dataNorth.getChannelName());
    DataOutputStream dsNorth = new DataOutputStream(new FileOutputStream(outputFileNorth));
    dataNorth.dumpMseed(dsNorth, ti, null, twentyDegreesRotation);

    RawDataProvider dataEast = dm.getAllChannels().get(1);
    assertEquals("BH2", dataEast.getChannelName());
    DataOutputStream dsEast = new DataOutputStream(new FileOutputStream(outputFileEast));
    dataEast.dumpMseed(dsEast, ti, null, twentyDegreesRotation);

    dm = new DataModule();
    dm.loadAndParseDataForTesting(outputFileNorth, outputFileEast);
    // now the data in dataNorth, dataEast is rotated data (by 20 degrees)
    dataNorth = dm.getAllChannels().get(0);
    dataEast = dm.getAllChannels().get(1);

    List<Segment> northSegmentsRaw = dataNorth.getRawData(ti);
    List<Segment> eastSegmentsRaw = dataEast.getRawData(ti);
    int sampleCount = 0;
    for (Segment segment : northSegmentsRaw) {
      sampleCount += segment.getSampleCount();
      assertNotNull(segment.getData(ti).data);
    }
    int[] rawDataNorth = new int[sampleCount];
    int lastArrayPoint = 0;
    for (Segment seg : northSegmentsRaw) {
      for (int point : seg.getData().data) {
        rawDataNorth[lastArrayPoint++] = point;
      }
    }
    int[] rawDataEast = new int[sampleCount];
    lastArrayPoint = 0;
    for (Segment seg : eastSegmentsRaw) {
      for (int point : seg.getData().data) {
        rawDataEast[lastArrayPoint++] = point;
      }
    }

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
    holdsExpectedData.loadAndParseDataForTesting(expectedDataNorthFile, expectedDataEastFile);


    RawDataProvider expectedDataNorth = dm.getAllChannels().get(0);
    List<Segment> northSegments = expectedDataNorth.getRawData(ti);
    sampleCount = 0;
    for (Segment segment : northSegments) {
      sampleCount += segment.getSampleCount();
      assertNotNull(segment.getData(ti).data);
    }
    double[] northRotated = new double[sampleCount];
    lastArrayPoint = 0;
    for (Segment seg : northSegments) {
      for (int point : seg.getData().data) {
        northRotated[lastArrayPoint++] = (double) point;
      }
    }

    assertEquals("BH1", holdsExpectedData.getAllChannels().get(0).getChannelName());

    double[] eastRotated = new double[sampleCount];
    lastArrayPoint = 0;
    RawDataProvider expectedDataEast = dm.getAllChannels().get(1);
    List<Segment> eastSegments = expectedDataEast.getRawData(ti);
    for (Segment seg : eastSegments) {
      for (int point : seg.getData().data) {
        eastRotated[lastArrayPoint++] = point;
      }
    }

    for (int i = 0; i < northRotated.length; ++i) {
      assertEquals("Discrepancy found at north data index " + i + "(of " +
          northRotated.length + ")", northRotated[i],
          rawDataNorth[i], 0);
      assertEquals("Discrepancy found at east data index " + i + "(of " +
          eastRotated.length + ")", eastRotated[i],
          rawDataEast[i], 0);
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