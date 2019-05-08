package com.isti.traceview.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import org.apache.log4j.Logger;

public class SourceFileASCII extends SourceFile implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(SourceFileSAC.class);
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
      while ((line = file.readLine()) != null) {
        System.out.println("Reading line " + (++count));
        String header = line.substring(0, 4);
        if (header.equals("NET ")) {
          network = line.substring(4);
        } else if (header.equals("STA ")) {
          station = line.substring(4);
        } else if (header.equals("LOC ")) {
          location = line.substring(4);
        } else if (header.equals("COMP")) {
          channel = line.substring(4);
        } else if (header.equals("RATE")) {
          sampleInterval = 1000. / Double.parseDouble(line.substring(4));
        } else if (header.equals("TIME")) {
          start = ZonedDateTime.parse(line.substring(4), TIME_FMT);
        } else if (header.equals("NSAM")) {
          // there are probably spaces between NSAM and the actual value to parse in
          // so let's trim off the front part of this substring
          numberSamples = Integer.parseInt(line.substring(4).trim());
        } else if (header.equals("DATA")) {
          PlotDataProvider pdp = new PlotDataProvider(network,
                DataModule.getOrAddStation(station), location, channel);
          ret.add(pdp);
          Segment segment = new Segment(this, file.getFilePointer(),
              Date.from(start.toInstant()), sampleInterval, numberSamples, 0);
          pdp.addSegment(segment);
          break;
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
      for (int i = 0; i < segment.getSampleCount(); ++i) {
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
