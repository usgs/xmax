package com.isti.traceview.data;

import com.isti.xmax.XMAXconfiguration;
import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class FileOutputUtils {

  public static File getOutputFromConfigOrUser(String filename, Component parent) {

    String fileExtension = filename.substring(filename.lastIndexOf('.')+1);
    String outputPath = XMAXconfiguration.getInstance().getOutputPath();
    String expectedFileName = outputPath + File.separator + filename;

    File outputtedFile = new File(expectedFileName);
    boolean configOutputPathInvalid = !outputtedFile.canWrite();
    String startingPath = outputPath;

    // if this would produce a file write error then we should prompt the user for a file
    // this is likely a result of permissions based on the configuration's default output path
    // so we will now default to the user's home directory, which should be writeable
    if(outputtedFile.exists()) {
      JOptionPane.showMessageDialog(parent,"Expected filename already exists.\n"
          + "Please choose a new file (or allow the old file to be overwritten).",
          "FILE OVERWRITE WARNING.", JOptionPane.WARNING_MESSAGE);
    }
    if (configOutputPathInvalid) {
      JOptionPane.showMessageDialog(parent,"Cannot write to expected directory.\n"
              + "Please choose where to save the file.",
          "FILE WRITE ERROR.", JOptionPane.ERROR_MESSAGE);
      startingPath = System.getProperty("user.home");
    }

    JFileChooser fileChooser = new JFileChooser(startingPath);
    int returnVal = fileChooser.showSaveDialog(parent);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      outputtedFile = fileChooser.getSelectedFile();
      if (!outputtedFile.getName().toLowerCase().endsWith(fileExtension.toLowerCase())) {
        outputtedFile = new File(outputtedFile.toString() + fileExtension);
      }
    } else {
      return null;
    }

    if (configOutputPathInvalid) {
      XMAXconfiguration.getInstance().setOutputPath(outputtedFile.getPath());
    }

    return outputtedFile;
  }

}
