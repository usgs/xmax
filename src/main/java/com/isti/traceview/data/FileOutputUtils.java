package com.isti.traceview.data;

import com.isti.xmax.XMAXconfiguration;
import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;

public class FileOutputUtils {

  public static File getOutputFromConfigOrUser(String filename, Component parent) {

    String fileExtension = filename.substring(filename.lastIndexOf('.')+1);

    String expectedFileName =
        XMAXconfiguration.getInstance().getOutputPath() + File.separator + filename;

    File outputtedFile = new File(expectedFileName);

    // if this would produce a file write error then we should prompt the user for a file
    // this is likely a result of permissions based on the configuration's default output path
    // so we will now default to the user's home directory, which should be writeable
    if (!outputtedFile.canWrite()) {
      JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
      int returnVal = fileChooser.showSaveDialog(parent);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        outputtedFile = fileChooser.getSelectedFile();
        if (!outputtedFile.getName().toLowerCase().endsWith(fileExtension.toLowerCase())) {
          outputtedFile = new File(outputtedFile.toString() + fileExtension);
        }
      } else {
        return null;
      }
    }
    return outputtedFile;
  }

}
