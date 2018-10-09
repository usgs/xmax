package com.isti.traceview.processing;

import com.isti.traceview.data.PlotDataProvider;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import org.apache.log4j.Logger;
import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import com.isti.traceview.data.PlotDataPoint;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.gui.IColorModeState;

/**
 * This class holds all information to describe traces rotation and provides methods to compute
 * rotated traces
 *
 * @author Nick Falco
 */
public class Rotation {
  public enum RotationType {
    /**
     * Rotation specific to individual digitizer models
     */
    STANDARD,
    /**
     * Horizontal rotation of 2 components (used for azimuth correction)
     */
    HORIZONTAL
  };


  /**
   * Some hardware uses multiple sensors at fixed locations to get 3D data that isn't in a
   * typical North-East-Vertical (NEZ) orientation, using a unique representation with axes
   * usually referred to as a UVW triplet, at angles to the surface of the earth.
   * These correspond to rotation matrices given in the sensor manufacturers' datasheets.
   */
  public enum StandardRotation {
    /**
     * Rotate STS-2's UVW into NEZ
     */
    STS2_UVW_TO_XMAX,
    /**
     * Rotate from NEZ into STS-2 UVW coordinates
     */
    STS2_XMAX_TO_UVW,
    /**
     * Rotate from a Trillium sensor's UVW into NEZ
     */
    TRIL_UVW_TO_XMAX,
    /**
     * Rotate from NEZ into Trillium UVW coordinates
     */
    TRIL_XMAX_TO_UVW
  };

  private static final Logger logger = Logger.getLogger(Rotation.class);

  private static double[][] UVWtoXMAXsts2 = { { -Math.sqrt(2.0 / 3.0), Math.sqrt(1.0 / 6.0), Math.sqrt(1.0 / 6.0) },
      { 0.0,                   Math.sqrt(0.5),      -Math.sqrt(0.5)      },
      { Math.sqrt(1.0 / 3.0),  Math.sqrt(1.0 / 3.0), Math.sqrt(1.0 / 3.0) }
  };
  private static double[][] XMAXtoUVWsts2 = { { -Math.sqrt(2.0 / 3.0), 0.0,                  Math.sqrt(1.0 / 3.0) },
      { Math.sqrt(1.0 / 6.0),  Math.sqrt(0.5),       Math.sqrt(1.0 / 3.0) },
      { Math.sqrt(1.0 / 6.0), -Math.sqrt(0.5),       Math.sqrt(1.0 / 3.0) }
  };

  private static double[][] UVWtoXMAXtrill = { { Math.sqrt(2. / 3.), -Math.sqrt(1. / 6.), -Math.sqrt(1. / 6.) },
      { 0.0,                 Math.sqrt(1. / 2.), -Math.sqrt(1. / 2.) },
      { Math.sqrt(1. / 3.),  Math.sqrt(1. / 3.),  Math.sqrt(1. / 3.) }
  };

  private static double[][] XMAXtoUVWtrill = { {  Math.sqrt(2. / 3.),  0.0,                Math.sqrt(1. / 3.) },
      { -Math.sqrt(1. / 6.),  Math.sqrt(1. / 2.), Math.sqrt(1. / 3.) },
      { -Math.sqrt(1. / 6.), -Math.sqrt(1. / 2.), Math.sqrt(1. / 3.) }
  };

  private Matrix matrix = null;

  private RotationType type = null;


  private double angle = 0;

  /**
   * Constructor for STANDARD type rotation
   */
   public Rotation(StandardRotation standardRotation) {
     initMatrix(standardRotation);
   }

   /**
    * Constructor for HORIZONTAL type rotation
    */
   public Rotation(double horizontalRotationAngle) {
     initMatrix(horizontalRotationAngle);
   }

   /**
    * Constructor with visual query dialog to describe rotation
    * @param frame JFrame for rotation menu prompt
    * @param numberOfChannels The total number of channels that are ultimately rotated (2 for horizontal, 3 for standard)
    */
   public Rotation(JFrame frame, int numberOfChannels) {
     RotationDialog dialog = new RotationDialog(frame, numberOfChannels);
     type = dialog.type;
     if(type==null){
       dialog.dispose();
     } else if (type.equals(RotationType.STANDARD)) {
       initMatrix(dialog.standardRotation);
       dialog.dispose();
     } else if (type.equals(RotationType.HORIZONTAL)) {
       initMatrix(dialog.horizontalAng);
       this.angle = dialog.horizontalAng;
       dialog.dispose();
     }
   }

   public RotationType getRotationType() {
     return type;
   }


   /**
    * Gets current rotation matrix
    * @return Matrix - current rotation matrix
    */
   public Matrix getMatrix(){
     return matrix;
   }

   /**
    * Computes rotation matrix
    *
    * @param angle
    *            in degrees
    */

   private void initMatrix(double angle) {
     angle = Math.toRadians(angle); //convert to radians
     double[][] matrixData = {
         { Math.cos(angle) , -Math.sin(angle),  0},
         { Math.sin(angle), Math.cos(angle),  0},
         { 0               ,               0,  1}
     };
     matrix = new Matrix(matrixData);
   }

   /**
    * Computes rotation matrix
    */
   private void initMatrix(StandardRotation standardRotation) {
     switch (standardRotation) {
       case STS2_UVW_TO_XMAX:
         matrix = new Matrix(UVWtoXMAXsts2);
         break;
       case STS2_XMAX_TO_UVW:
         matrix = new Matrix(XMAXtoUVWsts2);
         break;
       case TRIL_UVW_TO_XMAX:
         matrix = new Matrix(UVWtoXMAXtrill);
         break;
       case TRIL_XMAX_TO_UVW:
         matrix = new Matrix(XMAXtoUVWtrill);
         break;
     }
     //matrix.show();
   }

   public String getRotationAngleText() {
     if(this.getRotationType() == RotationType.HORIZONTAL)
       return Double.toString(this.angle) + "\u00b0";
     else if (this.getRotationType() == RotationType.STANDARD)
       return "STD";
     else
       return "";
   }

   /**
    * Rotate pixelized data If we have overlap on the trace we take only first
    * segment.
    *
    * @param channel
    *            plot data provider to rotate
    * @param ti
    *            processed time range
    * @param pointCount
    *            requested point count in the resulting plotdata
    * @param filter
    *            filter to apply before rotation
    * @return pixelized rotated data
    * @throws TraceViewException
    *             if a variety of issue occurs in called methods, this method
    *             throws if the channel type can not be determined.
    */
   public PlotData rotate(PlotDataProvider channel, TimeInterval ti,
       int pointCount, IFilter filter, IColorModeState colorMode)
           throws TraceViewException, RemoveGainException {
     PlotData[] tripletPlotData = new PlotData[3];
     char channelType = channel.getType();
     PlotData toProcess = channel.getOriginalPlotData(ti, pointCount, filter, null, colorMode);
     PlotData ret = new PlotData(channel.getName(), channel.getColor());
     if (channelType == 'E' || channelType == '2') {
       tripletPlotData[0] = toProcess;
       try{
         tripletPlotData[1] = getComplementaryPlotData(channel, '1', ti, pointCount, filter, colorMode);
       } catch (TraceViewException te) {
         logger.error("TraceViewException:", te);
         tripletPlotData[1] = getComplementaryPlotData(channel, 'N', ti, pointCount, filter, colorMode);
       }
       tripletPlotData[2] = getComplementaryPlotData(channel, 'Z', ti, pointCount, filter, colorMode);

     } else if (channelType == 'N' || channelType == '1') {
       try{
         tripletPlotData[0] = getComplementaryPlotData(channel, '2', ti, pointCount, filter, colorMode);
       } catch (TraceViewException te) {
         logger.error("TraceViewException:", te);
         tripletPlotData[0] = getComplementaryPlotData(channel, 'E', ti, pointCount, filter, colorMode);
       }
       tripletPlotData[1] = toProcess;
       tripletPlotData[2] = getComplementaryPlotData(channel, 'Z', ti, pointCount, filter, colorMode);

     } else if (channelType == 'Z') {
       try{
         tripletPlotData[0] = getComplementaryPlotData(channel, '2', ti, pointCount, filter, colorMode);
       } catch (TraceViewException te) {
         logger.error("TraceViewException:", te);
         tripletPlotData[0] = getComplementaryPlotData(channel, 'E', ti, pointCount, filter, colorMode);
       }
       try{
         tripletPlotData[1] = getComplementaryPlotData(channel, '1', ti, pointCount, filter, colorMode);
       } catch (TraceViewException te) {
         logger.error("TraceViewException:", te);
         tripletPlotData[1] = getComplementaryPlotData(channel, 'N', ti, pointCount, filter, colorMode);
       }
       tripletPlotData[2] = toProcess;

     } else if (channelType == 'U') {
       tripletPlotData[0] = toProcess;
       tripletPlotData[1] = getComplementaryPlotData(channel, 'V', ti, pointCount, filter, colorMode);
       tripletPlotData[2] = getComplementaryPlotData(channel, 'W', ti, pointCount, filter, colorMode);

     } else if (channelType == 'V') {
       tripletPlotData[0] = getComplementaryPlotData(channel, 'U', ti, pointCount, filter, colorMode);
       tripletPlotData[1] = toProcess;
       tripletPlotData[2] = getComplementaryPlotData(channel, 'W', ti, pointCount, filter, colorMode);
     } else if (channelType == 'W') {
       tripletPlotData[0] = getComplementaryPlotData(channel, 'U', ti, pointCount, filter, colorMode);
       tripletPlotData[1] = getComplementaryPlotData(channel, 'V', ti, pointCount, filter, colorMode);
       tripletPlotData[2] = toProcess;
     } else {
       throw new TraceViewException("Can't determine channel type for rotation: " + channel.getName());
     }
     for (int i = 0; i < pointCount; i++) {
       double[][] mean = new double[3][1];
       double[][][] cubicle = new double[8][3][1];
       boolean allDataFound = true;

       PlotDataPoint E = tripletPlotData[0].getPixels().get(i)[0];
       PlotDataPoint N = tripletPlotData[1].getPixels().get(i)[0];
       PlotDataPoint Z = tripletPlotData[2].getPixels().get(i)[0];
       if ((E.getRawDataProviderNumber() >= 0) && (N.getRawDataProviderNumber() >= 0) && (Z.getRawDataProviderNumber() >= 0)) {
         cubicle[0][0][0] = E.getBottom();
         cubicle[0][1][0] = N.getBottom();
         cubicle[0][2][0] = Z.getBottom();

         cubicle[1][0][0] = E.getTop();
         cubicle[1][1][0] = N.getBottom();
         cubicle[1][2][0] = Z.getBottom();

         cubicle[2][0][0] = E.getTop();
         cubicle[2][1][0] = N.getTop();
         cubicle[2][2][0] = Z.getBottom();

         cubicle[3][0][0] = E.getBottom();
         cubicle[3][1][0] = N.getTop();
         cubicle[3][2][0] = Z.getBottom();

         cubicle[4][0][0] = E.getTop();
         cubicle[4][1][0] = N.getTop();
         cubicle[4][2][0] = Z.getTop();

         cubicle[5][0][0] = E.getBottom();
         cubicle[5][1][0] = N.getTop();
         cubicle[5][2][0] = Z.getTop();

         cubicle[6][0][0] = E.getBottom();
         cubicle[6][1][0] = N.getBottom();
         cubicle[6][2][0] = Z.getTop();

         cubicle[7][0][0] = E.getTop();
         cubicle[7][1][0] = N.getBottom();
         cubicle[7][2][0] = Z.getTop();

         mean[0][0] = E.getMean();
         mean[1][0] = N.getMean();
         mean[2][0] = Z.getMean();
       } else {
         allDataFound = false;
       }
       PlotDataPoint pdp = null;
       if (allDataFound) {
         double[][][] rotatedCubicle = new double[8][3][1];
         double[][] rotatedMean = new double[3][1];
         try {
           for (int j = 0; j < 8; j++) {
             rotatedCubicle[j] = matrix.times(new Matrix(cubicle[j])).getData();
           }
           rotatedMean = matrix.times(new Matrix(mean)).getData();
         } catch (MatrixException e) {
           logger.error("MatrixException:", e);
           System.exit(0);
         }
         int index = 0;
         if (channelType == 'E' || channelType == 'U' || channelType== '2') {
           index = 0;
         } else if (channelType == 'N' || channelType == 'V' || channelType == '1') {
           index = 1;
         } else if (channelType == 'Z' || channelType == 'W') {
           index = 2;
         }
         double top = Double.NEGATIVE_INFINITY;
         double bottom = Double.POSITIVE_INFINITY;
         for (int j = 0; j < 8; j++) {
           if (rotatedCubicle[j][index][0] > top) {
             top = rotatedCubicle[j][index][0];
           }
           if (rotatedCubicle[j][index][0] < bottom) {
             bottom = rotatedCubicle[j][index][0];
           }
         }
         pdp = new PlotDataPoint(top, bottom, rotatedMean[index][0], toProcess.getPixels().get(i)[0].getSegmentNumber(),
             toProcess.getPixels().get(i)[0].getRawDataProviderNumber(),
             toProcess.getPixels().get(i)[0].getContinueAreaNumber(),
             toProcess.getPixels().get(i)[0].getEvents());

       } else {
         pdp = new PlotDataPoint(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, -1, -1, -1, null);
       }
       // lg.debug("Result: " + pdp);
       PlotDataPoint[] pdpArray = new PlotDataPoint[1];
       pdpArray[0] = pdp;
       ret.addPixel(pdpArray);
     }

     /*
      * lg.debug("E: " + tripletPlotData[0]); lg.debug("N: " + tripletPlotData[1]); lg.debug("Z: " +
      * tripletPlotData[2]); lg.debug("R: " + ret);
      */
     return ret;
   }

   /**
    * Rotates raw data
    *
    * @param channel
    *            raw data provider to rotate
    * @param ti
    *            processed time range
    * @return rotated raw data
    * @throws TraceViewException
    *             if thrown in
    *             {@link com.isti.traceview.processing.Rotation#getChannelsTriplet(RawDataProvider)}
    */
   public List<Segment> rotate(RawDataProvider channel, TimeInterval ti)
       throws TraceViewException {
     RawDataProvider[] triplet = getChannelsTriplet(channel);
     char channelType = channel.getType();
     List<Segment> ret = new ArrayList<Segment>();
     double[][] pointPosition = new double[3][1];
     for (Segment segment: channel.getRawData(ti)) {
       Segment rotated = new Segment(null, segment.getStartOffset(), segment.getStartTime(), segment.getSampleRate(), segment.getSampleCount(),
           segment.getSourceSerialNumber());
       double currentTime = segment.getStartTime().getTime();
       for (@SuppressWarnings("unused") int value: segment.getData().data) {
         currentTime = currentTime + segment.getStartOffset() + segment.getSampleRate();
         pointPosition[0][0] = triplet[0].getRawData(currentTime); //x
         pointPosition[1][0] = triplet[1].getRawData(currentTime); //y
         pointPosition[2][0] = triplet[2].getRawData(currentTime); //z
         if (pointPosition[0][0] == Integer.MIN_VALUE || pointPosition[1][0] == Integer.MIN_VALUE || pointPosition[2][0] == Integer.MIN_VALUE) {
         } else {
           try {
             double[][] rotatedPointPosition = this.getMatrix().times(new Matrix(pointPosition)).getData();
             if (channelType == 'E' || channelType == 'U' || channelType== '2') {
               rotated.addDataPoint(new Double(rotatedPointPosition[0][0]).intValue());
             } else if (channelType == 'N' || channelType == 'V' || channelType == '1') {
               rotated.addDataPoint(new Double(rotatedPointPosition[1][0]).intValue());
             } else if (channelType == 'Z' || channelType == 'W') {
               rotated.addDataPoint(new Double(rotatedPointPosition[2][0]).intValue());
             }
           } catch (MatrixException e) {
             logger.error("MatrixException:", e);
             System.exit(0);
           }
         }
       }
       if (rotated.getData().data.length > 0) {
         ret.add(rotated);
       }
     }
     return ret;
   }

   /**
    * Checks if two or traces are complementary channels
    *
    * @param channel1
    *            first trace to check
    * @param channel2
    *            second trace to check
    * @return true if channels are complementary (one is N and other is E, or 1 and 2)
    */
   public static boolean isComplementaryChannel(RawDataProvider channel1, RawDataProvider channel2) {
     List<Character> channelNames = new ArrayList<Character>();
     channelNames.add(channel1.getChannelName().charAt(channel1.getChannelName().length() - 1));
     channelNames.add(channel2.getChannelName().charAt(channel2.getChannelName().length() - 1));
     if(channelNames.contains('1') && channelNames.contains('2') ||
         channelNames.contains('N') && channelNames.contains('E'))
       return true;
     else
       return false;
   }

   /**
    * Checks if three or traces are complementary channels
    *
    * @param channel1
    *            first trace to check
    * @param channel2
    *            second trace to check
    * @param channel3
    *            third trace to check
    * @return true if one channel is E, one N, one Z; or if one is 1, one 2, one Z
    */
   public static boolean isComplementaryChannel(RawDataProvider channel1, RawDataProvider channel2, RawDataProvider channel3) {
     List<Character> channelNames = new ArrayList<Character>();
     channelNames.add(channel1.getChannelName().charAt(channel1.getChannelName().length() - 1));
     channelNames.add(channel2.getChannelName().charAt(channel2.getChannelName().length() - 1));
     channelNames.add(channel3.getChannelName().charAt(channel3.getChannelName().length() - 1));
     if(channelNames.contains('1') && channelNames.contains('2') && channelNames.contains('Z') ||
         channelNames.contains('N') && channelNames.contains('E')  && channelNames.contains('Z'))
       return true;
     else
       return false;
   }

   /**
    * Checks if we have loaded traces for all three coordinates to process rotation
    *
    * @param channel
    *            trace to check
    * @return flag
    */
   public static boolean isComplementaryChannelTrupleExist(RawDataProvider channel) {
     try {
       @SuppressWarnings("unused")
       RawDataProvider[] triplet = getChannelsTriplet(channel);
     } catch (final TraceViewException e) {
       SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
           JOptionPane.showMessageDialog(TraceView.getFrame(), e, "Rotation warning", JOptionPane.WARNING_MESSAGE);
         }
       });
       return false;
     }
     return true;
   }


   /**
    * Finds triplet of traces for cartesian coordinates
    *
    * @param channel
    *            trace to check
    * @return array of 3 traces for 3 coordinates
    * @throws TraceViewException
    *             if channel is missing data or if thrown by
    *             {@link com.isti.traceview.processing.Rotation#getComplementaryChannel(RawDataProvider, char)}
    */
   public static RawDataProvider[] getChannelsTriplet(
       RawDataProvider channel)
           throws TraceViewException {
     RawDataProvider[] chs = new RawDataProvider[3];
     char channelType = channel.getType();
     if (channelType == 'E') {
       chs[0] = channel;
       chs[1] = getComplementaryChannel(channel, 'N');
       chs[2] = getComplementaryChannel(channel, 'Z');

     } else if (channelType == '2') {
       chs[0] = channel;
       chs[1] = getComplementaryChannel(channel, '1');
       chs[2] = getComplementaryChannel(channel, 'Z');
     }
     else if (channelType == 'N') {
       chs[0] = getComplementaryChannel(channel, 'E');
       chs[1] = channel;
       chs[2] = getComplementaryChannel(channel, 'Z');
     } else if (channelType == '1') {
       chs[0] = getComplementaryChannel(channel, '2');
       chs[1] = channel;
       chs[2] = getComplementaryChannel(channel, 'Z');
     } else if (channelType == 'Z') {
       try{
         chs[0] = getComplementaryChannel(channel, '2');
       } catch (TraceViewException te) {
         logger.error("TraceViewException:", te);
         chs[0] = getComplementaryChannel(channel, 'E');
       }
       try{
         chs[1] = getComplementaryChannel(channel, '1');
       } catch (TraceViewException te) {
         logger.error("TraceViewException:", te);
         chs[1] = getComplementaryChannel(channel, 'N');
       }
       chs[2] = channel;
     } else if (channelType == 'U') {
       chs[0] = channel;
       chs[1] = getComplementaryChannel(channel, 'V');
       chs[2] = getComplementaryChannel(channel, 'W');

     } else if (channelType == 'V') {
       chs[0] = getComplementaryChannel(channel, 'U');
       chs[1] = channel;
       chs[2] = getComplementaryChannel(channel, 'W');
     } else if (channelType == 'W') {
       chs[0] = getComplementaryChannel(channel, 'U');
       chs[1] = getComplementaryChannel(channel, 'V');
       chs[2] = channel;
     } else {
       throw new TraceViewException("Can't determine channel type for rotation: " + channel.getName());
     }
     if (!channel.getTimeRange().isIntersect(chs[0].getTimeRange()))
       throw new TraceViewException("Channel has no data in this time range: " + chs[0].getName());
     if (!channel.getTimeRange().isIntersect(chs[1].getTimeRange()))
       throw new TraceViewException("Channel has no data in this time range: " + chs[1].getName());
     if (!channel.getTimeRange().isIntersect(chs[2].getTimeRange()))
       throw new TraceViewException("Channel has no data in this time range: " + chs[2].getName());
     return chs;
   }

   private static RawDataProvider getComplementaryChannel(RawDataProvider channel, char channelType) throws TraceViewException {
     String channelName = channel.getChannelName().substring(0, channel.getChannelName().length() - 1) + channelType;
     RawDataProvider channelComplementary = TraceView.getDataModule().getChannel(channelName, channel.getStation(), channel.getNetworkName(),
         channel.getLocationName());
     if (channelComplementary != null && channelComplementary.getSampleRate() == channel.getSampleRate()) {
       return channelComplementary;
     } else {
       throw new TraceViewException("Can't find channels triplet to rotate " + channel.getName() + ": " + channel.getNetworkName() + "/"
           + channel.getStation().getName() + "/" + channel.getLocationName() + "/" + channelName);
     }
   }

   private static PlotData getComplementaryPlotData(PlotDataProvider channel, char channelType, TimeInterval ti, int pointCount, IFilter filter, IColorModeState colorMode)
       throws TraceViewException, RemoveGainException {
     PlotDataProvider complement = (PlotDataProvider) getComplementaryChannel(channel, channelType);
     return complement.getOriginalPlotData(ti, pointCount, filter, null, colorMode);
   }

   /**
    * Visual dialog to enter rotation description
    */
   public class RotationDialog extends JDialog implements PropertyChangeListener, ItemListener {
     /**
      *
      */
     private static final long serialVersionUID = 1L;
     private JFrame frame;
     private RotationType type = null;
     private RotationType lastRotationType = null;
     private StandardRotation standardRotation = StandardRotation.STS2_UVW_TO_XMAX;
     private double horizontalAng; // moves both X and Y components for horizontal rotation option
     private JOptionPane optionPane = null;
     private JPanel mainPanel;
     private JTextField horizontalAngField;
     private JLabel horizontalAngLabel;
     private JComboBox<String> rotationTypeCB;
     private JPanel horizontalPanel;
     private JPanel standardPanel;
     private JComboBox<String> standardRotationCB;
     private JLabel rotationTypeL;
     private JPanel swithPanel;
     private JLabel standardRotationL;
     private int numberOfChannels; //used to determine what rotation types you can perform based on the number of channels selected

     public RotationDialog(JFrame frame, int numberOfChannels) {
       super(frame, "Rotation options", true);
       this.frame = frame;
       this.numberOfChannels = numberOfChannels;
       this.horizontalAng = 0.0;
       Object[] options = { "OK", "Close" };
       if(numberOfChannels == 2)
         type = RotationType.HORIZONTAL;
       else
         type = RotationType.STANDARD;
       // Create the JOptionPane.
       optionPane = new JOptionPane(createDesignPanel(type, standardRotation), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION, null,
           options, options[0]);
       // Make this dialog display it.
       setContentPane(optionPane);
       optionPane.setPreferredSize(new java.awt.Dimension(450, 180));
       optionPane.addPropertyChangeListener(this);
       setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
       addWindowListener(new WindowAdapter(){
         @Override
         public void windowClosing(WindowEvent we) {
           /*
            * Instead of directly closing the window, we're going to change the
            * JOptionPane's value property.
            */
           optionPane.setValue("Close");
         }
       });
       pack();
       setLocationRelativeTo(super.getOwner());
       setVisible(true);
     }

     private JPanel createDesignPanel(RotationType type, StandardRotation standardRotation) {

       mainPanel = new JPanel();
       mainPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
       BoxLayout panelLayout = new BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS);
       mainPanel.setLayout(panelLayout);
       mainPanel.add(getSwithPanel());
       getRotationTypeCB().setSelectedIndex(0);
       if (type.equals(RotationType.STANDARD) && numberOfChannels == 3) {
         mainPanel.add(getStandardPanel());
       } else if (type.equals(RotationType.HORIZONTAL) && numberOfChannels == 2){
         mainPanel.add(getHorizontalPanel());
       }
       return mainPanel;
     }

     /** Listens to the check boxes. */
     @Override
     public void itemStateChanged(ItemEvent e) {
       if (e.getSource().equals(getRotationTypeCB())) {
         type = RotationType.values()[getRotationTypeCB().getSelectedIndex()];
         if (type.equals(RotationType.STANDARD)) {
           if (lastRotationType.equals(RotationType.HORIZONTAL))
             mainPanel.remove(getHorizontalPanel());

           mainPanel.add(getStandardPanel());
           lastRotationType = RotationType.STANDARD;
           getStandardPanel().setVisible(false);
           getStandardPanel().setVisible(true);
           mainPanel.repaint();

         } else if (type.equals(RotationType.HORIZONTAL)){

           if (lastRotationType.equals(RotationType.STANDARD))
             mainPanel.remove(getStandardPanel());

           mainPanel.add(getHorizontalPanel());
           lastRotationType = RotationType.HORIZONTAL;
           getHorizontalPanel().setVisible(false);
           getHorizontalPanel().setVisible(true);
           mainPanel.repaint();

         }
       } else if (e.getSource().equals(getStandardRotationCB())) {
         standardRotation = StandardRotation.values()[getStandardRotationCB().getSelectedIndex()];
       }
     }

     @Override
     public void propertyChange(PropertyChangeEvent e) {
       String prop = e.getPropertyName();
       if (isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
         Object value = optionPane.getValue();
         optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
         // If you were going to check something
         // before closing the window, you'd do
         // it here.
         if (value.equals("Close")) {
           type = null;
           standardRotation = null;
           setVisible(false);
         } else if (value.equals("OK")) {
           if (type.equals(RotationType.STANDARD)) {
             setVisible(false);
           } else if (type.equals(RotationType.HORIZONTAL)) {
             try{
               horizontalAng = Double.parseDouble(getHorizontalAng().getText());
               setVisible(false);
             } catch (NumberFormatException e1) {
               JOptionPane.showMessageDialog(frame, "Check correct double number format", "Error", JOptionPane.ERROR_MESSAGE);
             }
           }
         }
       }
     }

     private JComboBox<String> getRotationTypeCB() {
       if (rotationTypeCB == null) {
         String[] rotationOptions;
         if(this.numberOfChannels == 2)
           rotationOptions = new String[]{"Horizontal"};
         else
           rotationOptions = new String[]{"Standard"};
         ComboBoxModel<String> rotationTypeCBModel = new DefaultComboBoxModel<String>(rotationOptions);
         rotationTypeCB = new JComboBox<String>();
         rotationTypeCB.setModel(rotationTypeCBModel);
         rotationTypeCB.setPreferredSize(new java.awt.Dimension(141, 21));
         rotationTypeCB.addItemListener(this);
       }
       return rotationTypeCB;
     }

     private JPanel getHorizontalPanel() {
       if (horizontalPanel == null) {
         horizontalPanel = new JPanel();
         GridBagLayout horizontalPanelLayout = new GridBagLayout();
         horizontalPanel.setPreferredSize(new java.awt.Dimension(204, 196));
         horizontalPanelLayout.rowWeights = new double[]{ 0.1, 0.1, 0.1 };
         horizontalPanelLayout.rowHeights = new int[]{ 7, 7, 7 };
         horizontalPanelLayout.columnWeights = new double[]{ 0.1, 0.1 };
         horizontalPanelLayout.columnWidths = new int[]{ 7, 7 };
         horizontalPanel.setLayout(horizontalPanelLayout);
         horizontalPanel.add(getHorizontalAng(), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
             GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
         horizontalPanel.add(getHorizontalAngLabel(), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
             new Insets(0, 0, 0, 0), 0, 0));
       }
       return horizontalPanel;
     }

     private JPanel getStandardPanel() {
       if (standardPanel == null) {
         standardPanel = new JPanel();
         standardPanel.setPreferredSize(new java.awt.Dimension(304, 196));
         standardPanel.add(getStandardRotationL());
         standardPanel.add(getStandardRotationCB());
       }
       return standardPanel;
     }

     private JComboBox<String> getStandardRotationCB() {
       if (standardRotationCB == null) {
         ComboBoxModel<String> rotationTypeCBModel = new DefaultComboBoxModel<String>(
             new String[]{"STS2 UVW to XMAX", "STS2 XMAX to UVW",
                 "Trill. UVW to XMAX", "Trill XMAX to UVW"});
         standardRotationCB = new JComboBox<String>();
         standardRotationCB.setModel(rotationTypeCBModel);
         standardRotationCB.setPreferredSize(new java.awt.Dimension(180, 23));
         standardRotationCB.addItemListener(this);
       }
       return standardRotationCB;
     }

     private JLabel getStandardRotationL() {
       if (standardRotationL == null) {
         standardRotationL = new JLabel();
         standardRotationL.setText("Standard rotation:");
       }
       return standardRotationL;
     }

     private JPanel getSwithPanel() {
       if (swithPanel == null) {
         swithPanel = new JPanel();
         swithPanel.add(getRotationTypeL());
         swithPanel.add(getRotationTypeCB());
       }
       return swithPanel;
     }

     private JLabel getRotationTypeL() {
       if (rotationTypeL == null) {
         rotationTypeL = new JLabel();
         rotationTypeL.setText("Rotation Type:");
       }
       return rotationTypeL;
     }

     private JTextField getHorizontalAng() {
       if (horizontalAngField == null) {
         horizontalAngField = new JTextField();
         horizontalAngField.setSize(80, 22);
         horizontalAngField.setPreferredSize(new java.awt.Dimension(80, 22));
       }
       return horizontalAngField;
     }

     private JLabel getHorizontalAngLabel() {
       if (horizontalAngLabel == null) {
         horizontalAngLabel = new JLabel();
         horizontalAngLabel.setText("Angle:");
       }
       return horizontalAngLabel;
     }
   }
}