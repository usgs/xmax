package com.isti.traceview.processing;

import static org.junit.Assert.assertArrayEquals;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.data.Response;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import org.junit.Test;

public class RunEvalRespTest {

  @Test
  public void testLoadStationXMLToRespValues() throws IOException, TraceViewException {
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

}