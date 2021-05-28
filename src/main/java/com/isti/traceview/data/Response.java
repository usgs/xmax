package com.isti.traceview.data;

import static asl.utils.response.ResponseParser.getAllEpochsFromXML;
import static asl.utils.response.ResponseParser.getResponseFromXMLInput;
import static asl.utils.response.ResponseParser.listEpochsForSelection;
import static asl.utils.response.ResponseParser.parseResponse;
import static asl.utils.response.ResponseParser.parseXMLForEpochs;
import static com.isti.traceview.processing.IstiUtilsMath.generateFreqArray;

import asl.utils.response.ChannelMetadata;
import asl.utils.response.ResponseParser.EpochIdentifier;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.processing.IstiUtilsMath;
import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.stationxml.FDSNStationXML;
import edu.sc.seis.seisFile.fdsnws.stationxml.GainSensitivity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import javax.xml.stream.XMLStreamException;
import org.apache.commons.math3.complex.Complex;
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
  Map<Date, ChannelMetadata> parsedEpochs;

  /**
   * @param network network code
   * @param station station code
   * @param location location code
   * @param channel channel code
   * @param filename name of file from which response was loaded
   */
  public Response(String network, String station, String location, String channel,
      String filename) {
    this.network = network;
    this.station = station;
    this.location = location;
    this.channel = channel;
    this.fileName = filename;
    parsedEpochs = populateResponseMap(filename, network, station, location, channel);
  }

  /**
   * Create a dummy (empty) response to test for existence in cached response lists
   * @param network network code
   * @param station station code
   * @param location location code
   * @param channel channel code
   */
  public Response(String network, String station, String location, String channel) {
    // creates a dummy response
    this.network = network;
    this.station = station;
    this.location = location;
    this.channel = channel;
  }

  /**
   * Construct a response from FDSN Station XML data
   * @param network network code
   * @param station station code
   * @param location location code
   * @param channel channel code
   * @param filename name of file from which response was loaded
   * @param xml xml data
   */
  private Response(String network, String station, String location, String channel, String filename,
      FDSNStationXML xml) {
    this.network = network;
    this.station = station;
    this.location = location;
    this.channel = channel;
    this.fileName = filename;
    parsedEpochs = populateResponseMapXML(network, station, location, channel, filename);
  }

  /**
   * Construct a response from FDSN Station XML data from an inputsream and not a file
   * @param network network code
   * @param station station code
   * @param location location code
   * @param channel channel code
   * @param filename name of file from which response was loaded
   * @param stream data stream for xml (i.e., from FDSN query)
   */
  private Response(String network, String station, String location, String channel, String filename,
      InputStream stream) {
    this.network = network;
    this.station = station;
    this.location = location;
    this.channel = channel;
    this.fileName = filename;
    parsedEpochs = populateResponseMapXML(network, station, location, channel, stream);
  }

  public Double getEpochStartAzimuth(Date date) {
    if (parsedEpochs == null || parsedEpochs.get(date) == null) {
      return null;
    }
    return parsedEpochs.get(date).getAzimuth();
  }

  /**
   * Find the azimuth during latest epoch that starts before the given date.
   * This gives the best guess for azimuth value for the date based on metadata.
   * @param date Starting date for the active trace of data assoc. with this response
   * @return Azimuth value for the channel in question if it exists, or null if no azimuth data
   * exists or the date comes before any specified epochs in the parsed metadata
   */
  public Double getEnclosingEpochAzimuth(Date date) {
    ChannelMetadata metadata = getEnclosingEpochResponse(date);
    return metadata == null ? null : metadata.getAzimuth();
  }

  public ChannelMetadata getEnclosingEpochResponse(Date date) {
    List<Date> epochStartDates = new ArrayList<>(parsedEpochs.keySet());
    // TODO: see if this fixes the issue
    /*
    if (epochStartDates.size() == 1) {
      return parsedEpochs.get(epochStartDates.get(0));
    }
     */
    Collections.sort(epochStartDates);
    int location = Collections.binarySearch(epochStartDates, date);
    if (location < -1) {
      // if location = -1, then insertion point is 0, so date is before even first epoch in list
      // location = -(insertion point) -1, so invert to get insertion point
      // then insertion point is point of first epoch AFTER date, so go back by 1 to get epoch
      location = ((location + 1) * -1) -1;
    } else if (location == -1) {
      // location = -1 in which case we will just return the first epoch available
      location = 0;
      logger.warn("Data predates any epochs in loaded response; choosing first available.");
    }

    return parsedEpochs.get(epochStartDates.get(location));
  }

  public Map<Date, ChannelMetadata> getResponseEpochMap() {
    return parsedEpochs;
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

  public static Map<Date, ChannelMetadata> populateResponseMap(String filename,
      String network, String station, String location, String channel) {
    Map<Date, ChannelMetadata> parsedEpochs = new HashMap<>();
    File file = new File(filename);
    // sometimes we create a response with empty content to check the cache against
    // in that case we don't need to populate this map -- it's almost a dummy object
    if (!file.exists()) return null;

    try {
      List<EpochIdentifier> epochList = listEpochsForSelection(filename);
      for (EpochIdentifier epoch : epochList) {
        if (location.length() == 0) {
          location = " ";
        }
        String compareAgainst = network + "." + station + "." + location + "." + channel;
        // second conditional where parsed epochs size is 1 is used to handle cases
        // where we want to load in a response that is obviously artificial and may not match
        // the SNCL data we would seen in a properly-formed resp; artificial resps will surely
        // never contain more than a single epoch anyway.
        if (epoch.channelIdentifier.equals(compareAgainst) || epochList.size() == 1) {
          ChannelMetadata resp = parseResponse(filename, epoch.filePointer);
          if (epochList.size() == 1) {
            logger.warn("Found only one resp in file, but it had mismatched SNCL of " +
                epoch.channelIdentifier);
          }
          parsedEpochs.put(Date.from(epoch.startInstant), resp);
        }
      }
    } catch (IOException e) {
      logger.error(e);
    }

    return parsedEpochs;
  }

  public static Map<Date, ChannelMetadata> populateResponseMapXML(String network, String station,
      String location, String channel, String filename) {
    Map<Date, ChannelMetadata> parsedEpochs = new HashMap<>();

    try {

      List<EpochIdentifier> epochList = parseXMLForEpochs(FDSNStationXML.loadStationXML(filename));
      for (EpochIdentifier epoch : epochList) {
        if (location.length() == 0) {
          location = " ";
        }
        String compareAgainst = network + "." + station + "." + location + "." + channel;
        // add 1 ms just to ensure that current time is set *inside* the given epoch
        Instant startInstant = Instant.ofEpochMilli(epoch.startInstant.toEpochMilli() + 1);
        if (epoch.channelIdentifier.equals(compareAgainst)) {
          ChannelMetadata resp = getResponseFromXMLInput(FDSNStationXML.loadStationXML(filename),
              network, station, location, channel, startInstant);
          parsedEpochs.put(Date.from(epoch.startInstant), resp);
        }
      }
    } catch (IOException | XMLStreamException | SeisFileException e) {
      logger.error("Encountered an error:", e);
    }

    return parsedEpochs;
  }

  public static Map<Date, ChannelMetadata> populateResponseMapXML(String network, String station,
        String location, String channel, InputStream stream) {
    Map<Date, ChannelMetadata> parsedEpochs = new HashMap<>();

    try {
      FDSNStationXML xml = FDSNStationXML.loadStationXML(stream);
      Map<Instant, ChannelMetadata> receivedEpochs = getAllEpochsFromXML(xml, network, station,
          location, channel);
      for (Instant instant : receivedEpochs.keySet()) {
        parsedEpochs.put(Date.from(instant), receivedEpochs.get(instant));
      }
    } catch (IOException | XMLStreamException | SeisFileException e) {
      logger.error("Encountered an error:", e);
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
   */
  public synchronized Complex[] getResp(Date date, double minFreqValue, double maxFreqValue,
      int len) {
    // find closest epoch to the date
    ChannelMetadata closestResp = getEnclosingEpochResponse(date);
    double[] frequencies = generateFreqArray(minFreqValue, maxFreqValue, len);
    return closestResp.applyResponseToInput(frequencies);
  }

  public double[] getRespAmp(Date date, double minFreqValue, double maxFreqValue, int len)
      throws TraceViewException {
    ChannelMetadata closestResp = getEnclosingEpochResponse(date);
    double[] frequencies = generateFreqArray(minFreqValue, maxFreqValue, len);
    double[] respAmp = IstiUtilsMath.getSpectraAmplitude(
        closestResp.applyResponseToInput(frequencies));
    if (respAmp.length != len) {
      throw new TraceViewException(
          getLocalFileName() + ": The length of the RESPONSE AMPLITUDE (" + respAmp.length
              + ") does not match the number of frequencies ("
              + len + ")");
    }
    GainSensitivity sensitivity = closestResp.getResponse()
        .getResponseStageList().get(0).getStageSensitivity();
    // Calper = 1/calibration frequency (Frequency of sensitivity)
    final double calper = Math.pow(sensitivity.getFrequency(), -1.0);
    // Calval = 1/overal sensitivity (Sensitivity)
    final double calib = Math.pow(sensitivity.getSensitivityValue(), -1.0);
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

  /**
   * Computes frequency parameters which should be used to compute proper response function out of
   * RESP file
   *
   * @param numSamples the number of samples.
   * @param sampRate the sample rate.
   * @return the frequency parameters.
   */
  public static FreqParameters getFreqParameters(int numSamples, double sampRate) {
    final double endFreq = sampRate / 2.0;
    final int numFreq = (int) (numSamples / 2.0 + 1.0 + 0.5); // 0.5 causes int cast to round up
    final double startFreq = endFreq / numFreq;
    final double sampFreq = (endFreq - startFreq) / (numFreq - 1.0);
    return new FreqParameters(startFreq, endFreq, sampFreq, numFreq);
  }

  /**
   * Initialize response from file
   */
  public static Response getResponse(File file) {
    Response resp = null;
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
      resp = new Response(network, station, location, channel, file.getCanonicalPath());
    } catch (Exception ex) {
      logger.error(("Could not open file: " + file.getName()), ex);
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
      FDSNStationXML xml = FDSNStationXML.loadStationXML(xmlFilename);
      return getResponseFromXML(network, station, location, channel, xmlFilename, xml);
    } catch (MalformedURLException e) {
      logger.error("Problem with constructing URL for " + xmlFilename + ": ", e);
    } catch (IOException e) {
      logger.error("Problem with accessing FDSN web services for " + xmlFilename + ": ", e);
    } catch (XMLStreamException | SeisFileException e) {
      logger.error("Problem with parsing XML data for " + xmlFilename + ": ", e);
    }
    return null;
  }

  private static Response getResponseFromXML(String network, String station, String location,
      String channel, String xmlFilename, FDSNStationXML xml) {
      return new Response(network, station, location, channel, xmlFilename, xml);
  }

  private static Response getResponseFromXML(String network, String station, String location,
      String channel, String xmlFilename, InputStream stream) {
    return new Response(network, station, location, channel, xmlFilename, stream);
  }

  public static Response getResponseFromWeb(String network, String station, String location,
      String channel, String queryURL) {
    String snclString = network + "." + station + "." + location + "." + channel;
    String webServicesURL = queryURL + "?net=" +
        network + "&sta=" + station + "&loc=" + location + "&cha=" + channel +
        "&level=response&format=xml&includecomments=false&nodata=404";
    try {
      URL xmlWeb = new URL(webServicesURL);
      URLConnection xmlGrabber = xmlWeb.openConnection();
      InputStream stream = xmlGrabber.getInputStream();
      return getResponseFromXML(network, station, location, channel, snclString, stream);
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
