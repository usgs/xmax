package com.isti.traceview.processing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.data.Response;
import java.io.File;
import java.time.LocalDate;
import java.util.Date;
import org.junit.Test;

public class RunEvalRespTest {

  private static final String XML_SERVICE = "https://vmweb01.cr.usgs.gov/metadata/iris/fdsnws/station/1/query";

  @Test
  public void testLoadStationXMLToRespValues() throws TraceViewException {
    String folderName = "src/test/resources/";
    String xmlFilename = folderName + "IU.ANMO.00.LH1.xml";
    String respFilename = folderName + "RESP.IU.ANMO.00.LH1";
    LocalDate localDate = LocalDate.of(2014, 12, 18);

    Response respXML = Response.getResponseFromXML("IU", "ANMO", "00","LH1", xmlFilename);
    Response respExpected = Response.getResponse(new File(respFilename));
    Date date = java.sql.Date.valueOf(localDate);
    double[] testRespAmp = respXML.getRespAmp(date,0,100, 100);
    double[] expectRespAmp = respExpected.getRespAmp(date,0,100, 100);

    int exponent = (int) Math.log10(expectRespAmp[1]);
    assertArrayEquals(expectRespAmp, testRespAmp, 1E-5 * Math.pow(10, exponent));
  }

  @Test
  public void testLoadStationXMLToRespValuesOnline() throws TraceViewException {
    String folderName = "src/test/resources/";
    String respFilename = folderName + "RESP.IU.ANMO.00.LH1";
    LocalDate localDate = LocalDate.of(2014, 12, 18);

    Response respXML = Response.getResponseFromWeb("IU", "ANMO", "00","LH1", XML_SERVICE);
    Response respExpected = Response.getResponse(new File(respFilename));
    Date date = java.sql.Date.valueOf(localDate);
    double[] testRespAmp = respXML.getRespAmp(date,0,100, 100);
    double[] expectRespAmp = respExpected.getRespAmp(date,0,100, 100);

    int exponent = (int) Math.log10(expectRespAmp[1]);
    assertArrayEquals(expectRespAmp, testRespAmp, 1E-5 * Math.pow(10, exponent));
  }

  @Test
  public void testLoadStationXMLAzimuthEpochs() {
    String folderName = "src/test/resources/";
    String xmlFilename = folderName + "IU.ANMO.00.LH1.xml";
    LocalDate localDate = LocalDate.of(2014, 12, 18);

    Response respXML = Response.getResponseFromXML("IU", "ANMO", "00","LH1", xmlFilename);

    Double azim = respXML.getEnclosingEpochAzimuth(
        java.sql.Date.valueOf(localDate));

    assertEquals(17., azim, 1E-5);
  }

  @Test
  public void testLoadStationXMLAzimuthNull() {
    String folderName = "src/test/resources/";
    String xmlFilename = folderName + "IU.ANMO.00.LH1.xml";

    Response respXML = Response.getResponseFromXML("IU", "ANMO", "00","LH1", xmlFilename);

    Double azim = respXML.getEpochStartAzimuth(
        java.sql.Date.valueOf(LocalDate.of(2014, 12, 18)));

    assertNull(azim);
  }
}