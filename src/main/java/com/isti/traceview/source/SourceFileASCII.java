package com.isti.traceview.source;

import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.sql.Date;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

public class SourceFileASCII extends SourceFile implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(SourceFileASCII.class);
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter
      .ofPattern("yyyy,DDD,HH:mm:ss.SSSS").withZone(ZoneOffset.UTC);

  /**
   * Constructor
   */
  public SourceFileASCII(File file) {
    super(file);
    logger.debug("Created: " + this);
  }

  @Override
  public Set<PlotDataProvider> parse() {
    Set<PlotDataProvider> ret = new HashSet<>();
    String network = "";
    String station = "";
    String location = "";
    String channel = "";
    double sampleInterval = 0.;
    ZonedDateTime start = null;
    int numberSamples = 0;
    try {
      RandomAccessFile file = new RandomAccessFile(getFile().getCanonicalPath(), "r");
      String line;
      int count = 0;
      parseLoop:
      while ((line = file.readLine()) != null) {
        String header = line.substring(0, 4);
        switch (header) {
          case "NET ":
            network = line.substring(4);
            break;
          case "STA ":
            station = line.substring(4);
            break;
          case "LOC ":
            location = line.substring(4);
            break;
          case "COMP":
            channel = line.substring(4);
            break;
          case "RATE":
            sampleInterval = 1000. / Double.parseDouble(line.substring(4));
            break;
          case "TIME":
            start = ZonedDateTime.parse(line.substring(4), TIME_FMT);
            break;
          case "NSAM":
            // there are probably spaces between NSAM and the actual value to parse in
            // so let's trim off the front part of this substring
            numberSamples = Integer.parseInt(line.substring(4).trim());
            break;
          case "DATA":
            PlotDataProvider pdp = new PlotDataProvider(network,
                DataModule.getOrAddStation(station), location, channel);
            ret.add(pdp);
            Segment segment = new Segment(this, file.getFilePointer(),
                Date.from(start.toInstant()), sampleInterval, numberSamples, 0);
            pdp.addSegment(segment);
            break parseLoop; // TODO: skip past [numberSamples] lines to get additional data
        }
      }
    } catch (IOException e) {
      logger.error("IO error: ", e);
    }
    return ret;
  }

  @Override
  public void load(Segment segment) {
    RandomAccessFile dis = null;
    try {
      dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
      dis.seek(segment.getStartOffset());

      String line = dis.readLine();
      segment.addDataPoint(Integer.parseInt(line.trim()));
      int charsToRead = line.length(); // assume all lines are of equal length
      // also, we've already read in the first point we need to get -- start index at 1
      for (int i = 1; i < segment.getSampleCount(); ++i) {
        // remove any whitespace from the start of the line
        segment.addDataPoint(Integer.parseInt(dis.readLine().trim()));
      }
    } catch (FileNotFoundException e) {
      logger.error("File not found: ", e);
    } catch (IOException e) {
      logger.error("IO error: ", e);
    }
  }

  @Override
  public FormatType getFormatType() {
    return FormatType.ASCII;
  }
}
