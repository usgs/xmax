package com.isti.traceview.source;

import com.isti.traceview.TraceView;
import com.isti.traceview.data.ISource;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import java.util.Set;
import org.apache.log4j.Logger;

public abstract class SourceSocket implements ISource {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(SourceSocket.class);

  private boolean parsed = false;

  protected String network;
  protected String station;
  protected String location;
  protected String channel;
  protected long startTime;
  protected long endTime;

  /**
   * Constructor
   */
  public SourceSocket(String network, String station, String location, String channel,
      long startTime, long endTime) {
    this.network = network;
    this.station = station;
    this.location = location;
    this.channel = channel;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public abstract Set<PlotDataProvider> parse();

  public abstract void load(Segment segment);


  public String getName() {
    return network + "." + station + "." + location + "." + channel + "."
        + startTime + "." + endTime;
  }

  public SourceType getSourceType() {
    return SourceType.SOCKET;
  }

  public synchronized boolean isParsed() {
    return parsed;
  }

  public synchronized void setParsed(boolean parsed) {
    this.parsed = parsed;
  }

  public boolean equals(Object o) {
    if (o instanceof SourceSocket) {
      SourceSocket sock = (SourceSocket) o;
      return (getName().equals(sock.getName()) && getFormatType().equals(sock.getFormatType()));
    } else {
      return false;
    }
  }

  /**
   * @return flag if url for data services defined in the configuration
   */
  public static boolean isWebservicesPathDefined() {
    return !TraceView.getConfiguration().getDataServiceHost().equals("!");
  }


  public String getBlockHeaderText(long blockStartOffset){
    return "<html><i>File type:</i>" + getFormatType() + "<br>Header block text is unavailable</html>";
  }
}
