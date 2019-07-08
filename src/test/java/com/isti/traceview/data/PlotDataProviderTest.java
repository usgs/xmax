package com.isti.traceview.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.Configuration;
import com.isti.traceview.common.Configuration.ChannelSortType;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.filters.FilterLP;
import com.isti.traceview.gui.ColorModeByGap;
import com.isti.traceview.gui.IColorModeState;
import com.isti.traceview.processing.LPFilterException;
import com.isti.traceview.processing.RemoveGainException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.jfree.chart.plot.Plot;
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
  public void filterWorksCorrectly() throws LPFilterException {
    String filename = "src/test/resources/ppm/00_BH1.512.seed";
    DataModule dm = new DataModule();
    File dataToFilter = new File(filename);
    dm.loadAndParseDataForTesting(dataToFilter);

    FilterLP defaultLPFilter = new FilterLP();

    PlotDataProvider pdp = dm.getAllChannels().get(0);
    defaultLPFilter.init(pdp);

    int sampleCount = pdp.getRawData().get(0).getSampleCount();
    double[] initData = new double[sampleCount];
    for (int i = 0; i < initData.length; ++i) {
      initData[i] = pdp.getRawData().get(0).getData().data[i];
    }

    double[] filteredCalculated = defaultLPFilter.filter(initData, sampleCount);
    double[] filteredExpected = new double[]{
        0.000000, 0.000000, 0.000001,
        0.000000, 0.000006, 0.000011,
        0.000017, 0.000023
    };

    for (int i = 0; i < filteredExpected.length; ++i) {
      assertEquals(filteredExpected[i], filteredCalculated[i], 1E-5);
    }
  }

  @Test
  public void dataIsRemovedCorrectly() throws IOException, TraceViewException {
    File dir = new File("src/test/resources/psd/");
    List<File> files = new ArrayList<>();
    Files.find(dir.toPath(), Integer.MAX_VALUE, (path, basicFileAttributes) ->
        path.toFile().getName().matches(".*.seed")).forEach(x -> files.add(x.toFile()));

    TraceView.getConfiguration().setPanelOrder(ChannelSortType.CHANNEL);

    DataModule dm = new DataModule();
    dm.loadAndParseDataForTesting(files.toArray(new File[]{}));

    TraceView.getConfiguration().setUnitsInFrame(3);

    // sets the initial set of data as if from reload (traces 0, 1, 2)
    dm.getNextChannelSet();
    // sets the next set of data (traces 3, 4, 5)
    dm.getNextChannelSet();

    assertEquals(6, dm.getAllChannels().size());
    assertEquals(dm.getChannelSetStartIndex(), 3);
    assertEquals(dm.getChannelSetEndIndex(), 6);

    dm.deleteChannel(dm.getAllChannels().get(4));
    assertEquals(5, dm.getAllChannels().size());
    List<PlotDataProvider> current = dm.getCurrentChannelSet(3);

    String[] expectedNames = new String[]{"IU/COLA/10/LH2", "IU/COLA/10/LHZ"};
    for (int i = 0; i < current.size(); ++i) {
      // System.out.println(current.get(i).getName());
      assertEquals(expectedNames[i], current.get(i).getName());
    }
    assertEquals(dm.getChannelSetStartIndex(), 3);
    assertEquals(dm.getChannelSetEndIndex(), 5);
  }

  @Test
  public void findsGapCorrectly() {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File fileWithGaps = new File(filename);
    assertTrue(fileWithGaps.getAbsolutePath(), fileWithGaps.getAbsoluteFile().exists());

    DataModule dm = new DataModule();
    dm.loadAndParseDataForTesting(fileWithGaps);

    RawDataProvider dataProvider = dm.getAllChannels().get(0);
    List<Segment> segments = dataProvider.getRawData();
    assertEquals(2, segments.size());
    Segment firstSegment = segments.get(0);
    Segment lastSegment = segments.get(segments.size() - 1);
    assertTrue(Segment.isDataBreak(
        firstSegment.getEndTime().toInstant().toEpochMilli(),
        lastSegment.getStartTime().toInstant().toEpochMilli(),
        dataProvider.getSampleRate()
    ));
  }

  @Test
  public void originalDataMethodTerminatesProperly() throws RemoveGainException, TraceViewException {
    String filename = "src/test/resources/ANMO_00_LHZ_GAP.512.seed";
    File fileWithGaps = new File(filename);
    assertTrue(fileWithGaps.getAbsolutePath(), fileWithGaps.getAbsoluteFile().exists());

    DataModule dm = new DataModule();
    dm.loadAndParseDataForTesting(fileWithGaps);

    PlotDataProvider dataProvider = dm.getAllChannels().get(0);
    dataProvider.load();

    List<Segment> segments = dataProvider.getRawData();
    Segment firstSegment = segments.get(0);

    TimeInterval ti = new TimeInterval(firstSegment.getStartTime(), firstSegment.getEndTime());
    int pointCount = firstSegment.getSampleCount();
    IColorModeState colorMode = new ColorModeByGap();
    PlotData data = dataProvider.getOriginalPlotData(ti, pointCount, null, null, colorMode);
    // if this doesn't terminate in a stack overflow error, we're good
  }
}
