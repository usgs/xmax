package com.isti.traceview.data;

import static org.junit.Assert.*;

import com.isti.traceview.TraceViewException;
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
import org.junit.Test;

public class RawDataProviderTest {

  private void saveAndReloadDataMseed(String filename) {
    // TODO -- and will likely need to change method signature
    // this will save data and load in the new saved data
  }

  @Test
  public void dumpMseed_trim() throws IOException, SeedFormatException, TraceViewException {

    DataModule dm = XMAXDataModule.getInstance();

    // first, we need to load in the data (may be moved to set-up method)
    File fileToModify = new File("src/test/resources/00_LHZ.512.mseed");
    List<ISource> dataFiles = SourceFile.getDataFiles(Collections.singletonList(fileToModify));
    dm.addDataSources(dataFiles);
    dm.loadData();


    RawDataProvider data = new ArrayList<>(dm.getAllChannels()).get(0);
    data.setStation(DataModule.getOrAddStation(data.getStation().getName()));
    TimeInterval initial = data.getTimeRange();

    // trim data by an hour
    long start = initial.getStart();
    long end = initial.getEnd();
    final long HOUR_IN_MS = 60 * 60 * 1000; // milliseconds in one hour
    start += HOUR_IN_MS; // trim the first hour
    end -= HOUR_IN_MS; // trim the last hour
    TimeInterval cut = new TimeInterval(start, end);

    String filename2 = "src/test/resources/trimmed_00_LHZ.512.mseed";

    File outputFile = new File(filename2);
    if (outputFile.exists()) {
      outputFile.delete();
    }

    DataOutputStream ds = new DataOutputStream(new FileOutputStream(outputFile));
    data.dumpMseed(ds, cut, null, null);
    ds.close();

    String filenameTest = "src/test/resources/trimmed.mseed";

    PlotDataProvider trimmedData = PlotDataProvider.load(filename2);
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