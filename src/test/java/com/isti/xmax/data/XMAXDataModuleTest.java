package com.isti.xmax.data;

import static org.junit.Assert.assertTrue;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.source.SourceFile;
import com.isti.xmax.XMAXconfiguration;
import java.io.File;
import java.util.List;
import org.junit.Test;

public class XMAXDataModuleTest {


  @Test
  public void loadData() throws TraceViewException {
    // NOTE: currently not matching anything as resources here are not included anywhere
    String mask = "src/test/resources/*.seed";
    XMAXconfiguration config = XMAXconfiguration.getInstance();
    TraceView.setConfiguration(config);
    TraceView.getConfiguration().setDataPath(mask);
    XMAXDataModule xdm = XMAXDataModule.getInstance();
    List<File> fileList = SourceFile.getDataFiles(mask);
    File[] files = xdm.getDataFiles();
    assertTrue(files.length > 0);
  }

}