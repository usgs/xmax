package com.isti.traceview.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import edu.iris.Fissures.IfNetwork.Stage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ResponseTest {

  @Test
  public void testResponseEpochsMatch() {

    String webservicesURL = "http://service.iris.edu:80/fdsnws/station/1/query";

    Response resp = Response.getResponseFromWeb("IU", "GUMO", "00", "LHZ",
        webservicesURL);

    // XMAX/evalresp had difficulty getting the epoch that at time this test was written
    // (27 Mar 2020) was open (starting at 2017). The point of this test is to assure that we can
    // get the correct sensitivity value for that time.
    assertNotNull(resp);
    edu.iris.Fissures.IfNetwork.Response fissuresResp = resp
        .getEnclosingEpochResponse(Date.from(Instant.ofEpochMilli(1585334122156L)));
    double expectedSensitivity = 1.820070016E9;
    double gottenSensitivity = fissuresResp.the_sensitivity.sensitivity_factor;
    assertEquals(expectedSensitivity, gottenSensitivity, 0.);
  }

}
