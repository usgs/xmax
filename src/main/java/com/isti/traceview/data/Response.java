package com.isti.traceview.data;

import static asl.utils.input.InstrumentResponse.getRespFileEpochs;
import static java.nio.charset.StandardCharsets.US_ASCII;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.RunEvalResp;
import edu.iris.dmc.fdsn.station.model.Network;
import edu.iris.dmc.fdsn.station.model.Station;
import edu.iris.dmc.service.ServiceUtil;
import edu.iris.dmc.ws.util.RespUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

/**
 * Class to represent response
 *
 * @author Max Kokoulin
 */
public class Response {

  private static final Logger logger = Logger.getLogger(Response.class);
  private static final boolean verboseDebug = false;

  String network;
  String station;
  String location;
  String channel;
  String fileName;
  Map<Date, Double> azimuthMap;
  Map<Date, edu.iris.Fissures.IfNetwork.Response> parsedEpochs;

  /**
   * @param network network code
   * @param station station code
   * @param location location code
   * @param channel channel code
   * @param content String with content of evalresp response file
   * @param fileName name of file from which response was loaded
   */
  public Response(String network, String station, String location, String channel, String content,
      String fileName) {
    this.network = network;
    this.station = station;
    this.location = location;
    this.channel = channel;
    this.fileName = fileName;
    this.azimuthMap = new HashMap<>();
    parsedEpochs = populateResponseMap(content);
  }

  /**
   * Construct response object from parameters taken from stationXML parse/conversion
   *
   * @param network network code
   * @param station station code
   * @param location location code
   * @param channel channel code
   * @param content String with content of evalresp response file
   * @param fileName name of file from which response was loaded
   * @param azimuthMap Azimuths keyed by epoch date as parsed in from stationXML
   */
  public Response(String network, String station, String location, String channel, String content,
      String fileName, Map<Date, Double> azimuthMap) {
    this.network = network;
    this.station = station;
    this.location = location;
    this.channel = channel;
    this.fileName = fileName;
    this.azimuthMap = azimuthMap;
    parsedEpochs = populateResponseMap(content);
  }

  public Double getEpochStartAzimuth(Date date) {
    if (azimuthMap == null) {
      return null;
    }
    return azimuthMap.get(date);
  }

  /**
   * Find the azimuth during latest epoch that starts before the given date.
   * This gives the best guess for azimuth value for the date based on metadata.
   * @param date Starting date for the active trace of data assoc. with this response
   * @return Azimuth value for the channel in question if it exists, or null if no azimuth data
   * exists or the date comes before any specified epochs in the parsed metadata
   */
  public Double getEnclosingEpochAzimuth(Date date) {
    List<Date> epochStartDates = new ArrayList<>(azimuthMap.keySet());
    Collections.sort(epochStartDates);
    int location = Collections.binarySearch(epochStartDates, date);
    if (location >= 0) {
      return azimuthMap.get(epochStartDates.get(location));
    } else if (location < -1) {
      // if location = -1, then insertion point is 0, so date is before even first epoch in list
      // location = -(insertion point) -1, so invert to get insertion point
      // then insertion point is point of first epoch AFTER date, so go back by 1 to get epoch
      location = ((location + 1) * -1) -1;
      return azimuthMap.get(epochStartDates.get(location));
    }
    return null;
  }

  public edu.iris.Fissures.IfNetwork.Response getEnclosingEpochResponse(Date date) {
    List<Date> epochStartDates = new ArrayList<>(parsedEpochs.keySet());
    Collections.sort(epochStartDates);
    int location = Collections.binarySearch(epochStartDates, date);
    if (location >= 0) {
      return parsedEpochs.get(epochStartDates.get(location));
    } else if (location < -1) {
      // if location = -1, then insertion point is 0, so date is before even first epoch in list
      // location = -(insertion point) -1, so invert to get insertion point
      // then insertion point is point of first epoch AFTER date, so go back by 1 to get epoch
      location = ((location + 1) * -1) -1;
      return parsedEpochs.get(epochStartDates.get(location));
    }
    return null;
  }

  public Map<Date, edu.iris.Fissures.IfNetwork.Response> getResponseEpochMap() {
    return parsedEpochs;
  }

  public Map<Date, Double> getAzimuthMap() {
    return azimuthMap;
  }

  public String getNetwork() {
    return network;
  }

  public String getStation() {
    return station;
  }

  public String getLocation() {
    return location;
  }

  public String getChannel() {
    return channel;
  }

  public String getFileName() {
    return fileName;
  }

  /**
   * Get default store file name for response
   */
  public String getLocalFileName() {
    return "RESP." + getNetwork() + "." + getStation() + "." + getLocation() + "." + getChannel();
  }

  public static Map<Date, edu.iris.Fissures.IfNetwork.Response>
  populateResponseMap(String content) {
    Map<Date, edu.iris.Fissures.IfNetwork.Response> parsedEpochs = new HashMap<>();
    // sometimes we create a response with empty content to check the cache against
    // in that case we don't need to populate this map -- it's almost a dummy object
    if (content == null) return null;

    BufferedReader contentReader = new BufferedReader(new StringReader(content));
    try {
      List<Pair<Instant, Instant>> epochMap = getRespFileEpochs(contentReader);
      for (Pair<Instant, Instant> startAndEnd : epochMap) {
        // sometimes evalResp has issues with getting the wrong response when epochs have the
        // same start and end times. We can correct for this by offsetting the start time by
        // a small amount. We don't expect epochs to last less than a second, so this
        // should prevent collision errors.
        Instant startInstant = startAndEnd.getFirst().plusMillis(500);
        Date date = Date.from(startInstant);
        RunEvalResp evalResp = new RunEvalResp(false, verboseDebug);
        edu.iris.Fissures.IfNetwork.Response resp = evalResp.getResponseFromFile(date, content);
        parsedEpochs.put(date, resp);
      }
    } catch (IOException | TraceViewException e) {
      logger.error(e);
    }

    return parsedEpochs;
  }

  /**
   * Generate response as complex spectra
   *
   * @param date Time stamp to search response
   * @param minFreqValue minimum requested frequency
   * @param maxFreqValue maximum requested frequency
   * @param len length of generated response array
   * @return response as array of complex numbers
   * @throws TraceViewException if thrown in {@link
   * com.isti.traceview.processing.RunEvalResp#generateResponse(double, double, int, Date, String)}
   */
  public synchronized Complex[] getResp(Date date, double minFreqValue, double maxFreqValue,
      int len) throws TraceViewException {
    RunEvalResp evalResp = new RunEvalResp(false, verboseDebug);
    // find closest epoch to the date
    edu.iris.Fissures.IfNetwork.Response closestResp = getEnclosingEpochResponse(date);
    return evalResp.generateResponse(minFreqValue, maxFreqValue, len, closestResp);
  }

  public double[] getRespAmp(Date date, double minFreqValue, double maxFreqValue, int len)
      throws TraceViewException {
    RunEvalResp evalResp = new RunEvalResp(false, verboseDebug);
    edu.iris.Fissures.IfNetwork.Response closestResp = getEnclosingEpochResponse(date);
    double[] respAmp = IstiUtilsMath.getSpectraAmplitude(
        evalResp.generateResponse(minFreqValue, maxFreqValue, len, closestResp));
    if (respAmp.length != len) {
      throw new TraceViewException(
          getLocalFileName() + ": The length of the RESPONSE AMPLITUDE (" + respAmp.length
              + ") does not match the number of frequencies ("
              + len + ")");
    }
    // Calper = 1/calibration frequency (Frequency of sensitivity)
    final double calper = Math.pow(evalResp.frequencyOfSensitivity, -1.0);
    // Calval = 1/overal sensitivity (Sensitivity)
    final double calib = Math.pow(evalResp.sensitivity, -1.0);
    if (IstiUtilsMath.calibAmpResp(respAmp, calper, calib, minFreqValue, maxFreqValue, len)
        != IstiUtilsMath.ISTI_UTIL_SUCCESS) {
      throw new TraceViewException(
          getLocalFileName() + ": Calibration frequency is " + calper + " outside range of <"
              + minFreqValue + " : " + maxFreqValue
              + ">: continue without proper calibration");
    }
    return respAmp;
  }

  public boolean equals(Object o) {
    if (o instanceof Response) {
      Response r = (Response) o;
      return (getNetwork().equals(r.getNetwork()) && getStation().equals(r.getStation())
          && getChannel().equals(r.getChannel()) && getLocation()
          .equals(r.getLocation()));
    } else {
      return false;
    }
  }

  public void setAzimuthMap(Map<Date, Double> azimuthMap) {
    this.azimuthMap = azimuthMap;
  }

  /**
   * Computes frequency parameters which should be used to compute proper response function out of
   * RESP file
   *
   * @param numSamples the number of samples.
   * @param sampRate the sample rate.
   * @return the frequency parameters.
   */
	/*
	public static FreqParameters getFreqParameters(int numSamples, double sampRate) {
		final double endFreq = sampRate / 2.0;
		final double startFreq = 1.e-30;
		final int numFreq = (int) (numSamples / 2.0 + 1.0 + 0.5); // 0.5 for double to int conversion
		final double sampFreq = (endFreq - startFreq) / ((double) (numFreq - 1.0));
		return new FreqParameters(startFreq, endFreq, sampFreq, numFreq);
	}
	*/
  public static FreqParameters getFreqParameters(int numSamples, double sampRate) {
    final double endFreq = sampRate / 2.0;
    final int numFreq = (int) (numSamples / 2.0 + 1.0 + 0.5); // 0.5 causes int cast to round up
    final double startFreq = endFreq / numFreq;
    //(double)
    final double sampFreq = (endFreq - startFreq) / (numFreq - 1.0);
    return new FreqParameters(startFreq, endFreq, sampFreq, numFreq);
  }

  /**
   * Initialize response from file
   */
  public static Response getResponse(File file) {
    Response resp = null;
    Reader respReader = null;
    if (!file.getName().startsWith("RESP.")) {
      logger.error("getResponse(" + file.getName() + "): response file should starts with RESP.");
      return null;
    }
    try {
      String[] split = file.getName().split("\\.");
      String network = split[1];
      String station = split[2];
      String location = split[3];
      String channel = split[4];
      respReader = new BufferedReader(new FileReader(file));
      int len = (int) file.length();
      char[] cbuf = new char[len];
      respReader.read(cbuf, 0, len);
      resp = new Response(network, station, location, channel, new String(cbuf),
          file.getCanonicalPath());
    } catch (Exception ex) {
      logger.error(("Could not open file: " + file.getName()), ex);
    } finally {
      try {
        respReader.close();
      } catch (IOException e) {
        logger.error("IOException:", e);
      }
    }
    return resp;
  }

  /**
   * Gets response data from a given FDSN StationXML file. Network, station, location, and channel
   * are all specified as paramters rather than extracted from the filename.
   *
   * @param network Network code (IU, CU, etc.)
   * @param station 4- or 5-letter station code
   * @param location Location for the relevant channel (i.e., 00, 10)
   * @param channel Channel name (i.e., LH1, BHZ)
   * @param xmlFilename Name of stationXML file to parse to get response data from. Ideally
   * @return New response object using the data taken from the stationXML
   */
  public static Response getResponseFromXML(String network, String station, String location,
      String channel, String xmlFilename) {
    try {
      FileInputStream fileInputStream = new FileInputStream(xmlFilename);
      return getResponseFromXML(network, station, location, channel, fileInputStream);
    } catch (FileNotFoundException e) {
      logger.error("FileNotFoundException:", e);
    }
    return null;
  }

  private static Response getResponseFromXML(String network, String station, String location,
      String channel, InputStream inputStream) {
    String snclString = network + "." + station + "." + location + "." + channel + ".xml";
    List<edu.iris.dmc.fdsn.station.model.Channel> foundChannels = new ArrayList<>();
    try {
      List<Network> networks = ServiceUtil
          .getInstance().getStationService().load(inputStream);
      for (Network foundNetwork : networks) {
        if (foundNetwork.getCode().equals(network)) {
          for (Station foundStation : foundNetwork.getStations()) {
            if (foundStation.getCode().equals(station)) {
              for (edu.iris.dmc.fdsn.station.model.Channel iterChan : foundStation.getChannels()) {
                if (iterChan.getLocationCode().equals(location) &&
                    iterChan.getCode().equals(channel)) {
                  foundChannels.add(iterChan);
                }
              }
            }
          }
        }
      }
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      Map<Date, Double> azimuthMap = new HashMap<>();
      // restricts the stream output to only include the channel of interest to reduce overhead
      for (edu.iris.dmc.fdsn.station.model.Channel foundChannel : foundChannels) {
        Date date = foundChannel.getStartDate().toGregorianCalendar().getTime();
        Double azimuthValue = foundChannel.getAzimuthValue();
        if (azimuthValue != null) {
          azimuthMap.put(date, azimuthValue);
        }
        RespUtil.write(new PrintWriter(output), network, station, foundChannel);
      }
      String respData = new String(output.toByteArray(), US_ASCII);
      output.close();
      return new Response(network, station, location, channel, respData, snclString, azimuthMap);
    } catch (IOException ex) {
      logger.error(("Could not load data from stream: " + snclString), ex);
    }

    return null;
  }

  public static Response getResponseFromWeb(String network, String station, String location,
      String channel, String queryURL) {
    String snclString = network + "." + station + "." + location + "." + channel;
    List<edu.iris.dmc.fdsn.station.model.Channel> foundChannels = new ArrayList<>();
    String webServicesURL = queryURL + "?net=" +
        network + "&sta=" + station + "&loc=" + location + "&cha=" + channel +
        "&level=response&format=xml&includecomments=false&nodata=404";
    try {
      URL xmlWeb = new URL(webServicesURL);
      URLConnection xmlGrabber = xmlWeb.openConnection();
      return getResponseFromXML(network, station, location, channel, xmlGrabber.getInputStream());
    } catch (MalformedURLException e) {
      logger.error("Problem with constructing URL for " + snclString + ": ", e);
    } catch (IOException e) {
      logger.error("Problem with accessing FDSN web services for " + snclString + ": ", e);
    }

    return null;
  }

  public String toString() {
    return "RESPONSE - " + network + "." + station + "." + location + "." + channel;
  }

  /**
   *
   *
   */
  public static class FreqParameters {

    public final double startFreq;
    public final double endFreq;
    public final double sampFreq;
    public final int numFreq;

    public FreqParameters(double startFreq, double endFreq, double sampFreq, int numFreq) {
      this.startFreq = startFreq;
      this.endFreq = endFreq;
      this.sampFreq = sampFreq;
      this.numFreq = numFreq;
    }
  }
}
