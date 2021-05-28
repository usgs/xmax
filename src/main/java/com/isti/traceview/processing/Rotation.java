package com.isti.traceview.processing;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.gui.IColorModeState;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.log4j.Logger;

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
  }


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
  }

  private static final Logger logger = Logger.getLogger(Rotation.class);

  private static double[][] UVWtoXMAXsts2 = {
      { -Math.sqrt(2.0 / 3.0), Math.sqrt(1.0 / 6.0), Math.sqrt(1.0 / 6.0) },
      { 0.0,                   Math.sqrt(0.5),      -Math.sqrt(0.5)      },
      { Math.sqrt(1.0 / 3.0),  Math.sqrt(1.0 / 3.0), Math.sqrt(1.0 / 3.0) }
  };
  private static double[][] XMAXtoUVWsts2 = {
      { -Math.sqrt(2.0 / 3.0), 0.0,                  Math.sqrt(1.0 / 3.0) },
      { Math.sqrt(1.0 / 6.0),  Math.sqrt(0.5),       Math.sqrt(1.0 / 3.0) },
      { Math.sqrt(1.0 / 6.0), -Math.sqrt(0.5),       Math.sqrt(1.0 / 3.0) }
  };

  private static double[][] UVWtoXMAXtrill = {
      { Math.sqrt(2. / 3.), -Math.sqrt(1. / 6.), -Math.sqrt(1. / 6.) },
      { 0.0,                 Math.sqrt(1. / 2.), -Math.sqrt(1. / 2.) },
      { Math.sqrt(1. / 3.),  Math.sqrt(1. / 3.),  Math.sqrt(1. / 3.) }
  };

  private static double[][] XMAXtoUVWtrill = {
      {  Math.sqrt(2. / 3.),  0.0,                Math.sqrt(1. / 3.) },
      { -Math.sqrt(1. / 6.),  Math.sqrt(1. / 2.), Math.sqrt(1. / 3.) },
      { -Math.sqrt(1. / 6.), -Math.sqrt(1. / 2.), Math.sqrt(1. / 3.) }
  };

  private RealMatrix matrix = null;

  private RotationType type = null;

  private Map<String, List<Segment>> cachedData;
  private TimeInterval cachedTimeInterval = null; // set to non-null when data is cached

  private double angle = 0;

  /**
   * Constructor for STANDARD type rotation
   */
  public Rotation(StandardRotation standardRotation) {
    initMatrix(standardRotation);
    cachedData = new HashMap<>();
  }

  /**
   * Constructor for HORIZONTAL type rotation
   */
  public Rotation(double horizontalRotationAngle) {
    initMatrix(horizontalRotationAngle);
    type = RotationType.HORIZONTAL;
    cachedData = new HashMap<>();
  }

  /**
   * Constructor with visual query dialog to describe rotation
   * @param frame JFrame for rotation menu prompt
   * @param numberOfChannels The total number of channels that are ultimately rotated (2 for horizontal, 3 for standard)
   */
  public Rotation(JFrame frame, int numberOfChannels) {
    this(frame, numberOfChannels, 0.);
    cachedData = new HashMap<>();
  }

  /**
   * Constructor with visual query dialog to describe rotation
   * @param frame JFrame for rotation menu prompt
   * @param numberOfChannels The total number of channels that are ultimately rotated (2 for horizontal, 3 for standard)
   * @param rotationInitialization Value to set initial rotation angle to for horizontal data
   */
  public Rotation(JFrame frame, int numberOfChannels, double rotationInitialization) {
    RotationDialog dialog = new RotationDialog(frame, numberOfChannels, rotationInitialization);
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
    cachedData = new HashMap<>();
  }

  public RotationType getRotationType() {
    return type;
  }


  /**
   * Gets current rotation matrix
   * @return Matrix - current rotation matrix
   */
  public RealMatrix getMatrix(){
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
    // order of data is E, N, Z
    // rotation of N looks like (- e sin theta + n cos theta)
    // rotation of E looks like (e cos theta + n sin theta)
    double[][] matrixData = {
        { Math.cos(angle) , -Math.sin(angle),  0},
        { Math.sin(angle), Math.cos(angle),  0},
        { 0               ,               0,  1}
    };
    matrix = MatrixUtils.createRealMatrix(matrixData);
  }

  /**
   * Computes rotation matrix
   */
  private void initMatrix(StandardRotation standardRotation) {
    type = RotationType.STANDARD;
    switch (standardRotation) {
      case STS2_UVW_TO_XMAX:
        matrix = MatrixUtils.createRealMatrix(UVWtoXMAXsts2);
        break;
      case STS2_XMAX_TO_UVW:
        matrix = MatrixUtils.createRealMatrix(XMAXtoUVWsts2);
        break;
      case TRIL_UVW_TO_XMAX:
        matrix = MatrixUtils.createRealMatrix(UVWtoXMAXtrill);
        break;
      case TRIL_XMAX_TO_UVW:
        matrix = MatrixUtils.createRealMatrix(XMAXtoUVWtrill);
        break;
    }
    //matrix.show();
  }

  public String getRotationAngleText() {
    if(this.getRotationType() == RotationType.HORIZONTAL)
      return this.angle + "\u00b0";
    else if (this.getRotationType() == RotationType.STANDARD)
      return "STD";
    else
      return "";
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
      throws TraceViewException, RotationGapException {

    // is the cached data already set? if so, we run this operation to get the already-rotated data
    // this will happen if we're doing calculations with a channel's complementary data
    // i.e., if we're doing a PSD on both LH1 and LH2 -- over the same time range
    if (cachedTimeInterval != null &&
        cachedTimeInterval.getStart() == ti.getStart() &&
        cachedTimeInterval.getEnd() == ti.getEnd() &&
        cachedData.containsKey(channel.getName())) {
      return cachedData.get(channel.getName());
    }

    // first step is to make sure that there are no gaps in the data; otherwise don't rotate
    // this will allow us to just get all the data over the time ranges and examine per-point
    RawDataProvider[] triplet = getChannelsTriplet(channel);
    for (int i = 0; i < triplet.length; i++) {
      RawDataProvider checkForGaps = triplet[i];
      if (i == 2 && getRotationType() == RotationType.HORIZONTAL) {
        continue;
      }
      assert(checkForGaps != null);
      if (checkForGaps.hasGaps(ti)) {
        throw new RotationGapException(
            "Cannot rotate due to presence of gaps in " + checkForGaps.getName());
      }
    }

    cachedTimeInterval = ti;
    cachedData = new HashMap<>();

    List<Segment> first = new ArrayList<>();
    List<Segment> second = new ArrayList<>();
    List<Segment> third = new ArrayList<>();
    double[] pointPosition = new double[3];

    int segIndexFirst = 0;
    List<Segment> rawSegmentsFirst = triplet[0].getRawData(ti);
    long endTime = Math.min(ti.getEnd(),
        rawSegmentsFirst.get(rawSegmentsFirst.size() - 1).getEndTimeMillis());
    int pointIndexFirst = (int)
        Math.round((ti.getStart() - rawSegmentsFirst.get(0).getStartTimeMillis())
            / channel.getSampleRate());
    int segIndexSecond = 0;
    List<Segment> rawSegmentsSecond = triplet[1].getRawData(ti);
    endTime = Math.min(endTime,
        rawSegmentsSecond.get(rawSegmentsSecond.size() - 1).getEndTimeMillis());
    int pointIndexSecond = (int)
        Math.round((ti.getStart() - rawSegmentsSecond.get(0).getStartTimeMillis())
            / channel.getSampleRate());

    // this code is only needed if the rotation is standard; otherwise we just populate the
    // vertical trace with zeros, because it isn't part of the horizontal calculations
    int segIndexThird = 0;
    int pointIndexThird = 0;
    List<Segment> rawSegmentsThird = null;
    if (triplet[2] == null && getRotationType() == RotationType.STANDARD) {
      throw new TraceViewException("Can't find vertical complementary channel to rotate "
          + channel.getName());
    }
    if (triplet[2] != null || getRotationType() == RotationType.STANDARD) {
      rawSegmentsThird = triplet[2].getRawData(ti);
      endTime = Math.min(endTime,
          rawSegmentsThird.get(rawSegmentsThird.size() - 1).getEndTimeMillis());
      pointIndexThird = (int)
          Math.round((ti.getStart() - rawSegmentsThird.get(0).getStartTimeMillis())
              / channel.getSampleRate());
    }

    ti = new TimeInterval(ti.getStart(), endTime);

    long currentTime = ti.getStart();

    Segment segment = rawSegmentsFirst.get(0);
    int sampleCount = segment.getSampleCount() - pointIndexFirst;
    Segment firstRotated = new Segment(null, segment.getStartOffset(),
        Date.from(Instant.ofEpochMilli(currentTime)), segment.getSampleRate(), sampleCount,
        segment.getSourceSerialNumber());
    Segment secondRotated = new Segment(null, segment.getStartOffset(),
        Date.from(Instant.ofEpochMilli(currentTime)), segment.getSampleRate(), sampleCount,
        segment.getSourceSerialNumber());
    Segment thirdRotated = new Segment(null, segment.getStartOffset(),
        Date.from(Instant.ofEpochMilli(currentTime)), segment.getSampleRate(), sampleCount,
        segment.getSourceSerialNumber());

    while (currentTime < ti.getEnd()) {
      pointPosition[0] = rawSegmentsFirst.get(segIndexFirst).getData().data[pointIndexFirst];
      pointPosition[1] = rawSegmentsSecond.get(segIndexSecond).getData().data[pointIndexSecond];
      if (getRotationType() == RotationType.HORIZONTAL) {
        pointPosition[2] = 0;
      } else {
        assert(rawSegmentsThird != null);
        pointPosition[2] = rawSegmentsThird.get(segIndexThird).getData().data[pointIndexThird];
      }

      RealVector rotatedPointPosition =
          matrix.operate(MatrixUtils.createRealVector(pointPosition));
      firstRotated.addDataPoint((int) rotatedPointPosition.getEntry(0));
      secondRotated.addDataPoint((int) rotatedPointPosition.getEntry(1));
      thirdRotated.addDataPoint((int) rotatedPointPosition.getEntry(2));

      // the remaining is basically all iteration steps. that's awful but happens because the data
      // is in 3 different 2-D data structures and the indices are different for each one

      // the outgoing segments will all match the boundaries of the first trace in the triplet
      // when we hit the end of a segment, we add the completed rotated segments to the big list
      // and then create new segments to put points into
      if (pointIndexFirst + 1 == rawSegmentsFirst.get(segIndexFirst).getSampleCount()) {
        if (firstRotated.getData().data.length > 0) {
          first.add(firstRotated);
          second.add(secondRotated);
          third.add(thirdRotated);
        }

        // start at first point of next segment
        pointIndexFirst = 0;
        ++segIndexFirst;
        if (segIndexFirst >= rawSegmentsFirst.size()) {
          break;
        }
        // set up all the data structures for a new segment
        segment = rawSegmentsFirst.get(segIndexFirst);
        sampleCount = segment.getSampleCount();
        if (currentTime + (sampleCount * channel.getSampleRate()) >= ti.getEnd()) {
          sampleCount = (int) Math.ceil((ti.getEnd() - currentTime) / channel.getSampleRate());
        }
        firstRotated = new Segment(null, segment.getStartOffset(),
            segment.getStartTime(), segment.getSampleRate(), sampleCount,
            segment.getSourceSerialNumber());
        secondRotated = new Segment(null, segment.getStartOffset(),
            segment.getStartTime(), segment.getSampleRate(), sampleCount,
            segment.getSourceSerialNumber());
        thirdRotated = new Segment(null, segment.getStartOffset(),
            segment.getStartTime(), segment.getSampleRate(), sampleCount,
            segment.getSourceSerialNumber());
      } else {
        ++pointIndexFirst;
      }

      if (pointIndexSecond == rawSegmentsSecond.get(segIndexSecond).getSampleCount() - 1) {
        pointIndexSecond = 0;
        ++segIndexSecond;
      } else {
        ++pointIndexSecond;
      }

      if (getRotationType() != RotationType.HORIZONTAL) {
        if (pointIndexThird == rawSegmentsThird.get(segIndexThird).getSampleCount() - 1) {
          pointIndexThird = 0;
          ++segIndexThird;
        } else {
          ++pointIndexThird;
        }
      }


      // iteration step
      currentTime += channel.getSampleRate();
    }
    if (firstRotated.getData().data.length > 0) {
      first.add(firstRotated);
      second.add(secondRotated);
      third.add(thirdRotated);
    }

    cachedData.put(triplet[0].getName(), first);
    cachedData.put(triplet[1].getName(), second);
    if (getRotationType() != RotationType.HORIZONTAL) {
      cachedData.put(triplet[2].getName(), third);
    }

    return cachedData.get(channel.getName());
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
    List<Character> channelNames = new ArrayList<>();
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
  public static boolean isComplementaryChannel(RawDataProvider channel1, RawDataProvider channel2,
      RawDataProvider channel3) {
    List<Character> channelNames = new ArrayList<>();
    channelNames.add(channel1.getChannelName().charAt(channel1.getChannelName().length() - 1));
    channelNames.add(channel2.getChannelName().charAt(channel2.getChannelName().length() - 1));
    channelNames.add(channel3.getChannelName().charAt(channel3.getChannelName().length() - 1));
    return channelNames.contains('1') && channelNames.contains('2') && channelNames.contains('Z') ||
        channelNames.contains('N') && channelNames.contains('E') && channelNames.contains('Z');
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
      SwingUtilities.invokeLater(
          () -> JOptionPane.showMessageDialog(TraceView.getFrame(), e, "Rotation warning", JOptionPane.WARNING_MESSAGE));
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
    if (chs[2] != null && !channel.getTimeRange().isIntersect(chs[2].getTimeRange()))
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
      // we will allow for vertical channels to return null, allowing 2D rotations when no vertical is loaded
      if (channelType == 'Z') {
        return null;
      }
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
  public static class RotationDialog extends JDialog implements PropertyChangeListener, ItemListener {
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

    public RotationDialog(JFrame frame, int numberOfChannels, double initialAngle) {
      super(frame, "Rotation options", true);
      this.frame = frame;
      this.numberOfChannels = numberOfChannels;
      this.horizontalAng = initialAngle;
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
        ComboBoxModel<String> rotationTypeCBModel = new DefaultComboBoxModel<>(rotationOptions);
        rotationTypeCB = new JComboBox<>();
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
        ComboBoxModel<String> rotationTypeCBModel = new DefaultComboBoxModel<>(
            new String[]{"STS2 UVW to XMAX", "STS2 XMAX to UVW",
                "Trill. UVW to XMAX", "Trill XMAX to UVW"});
        standardRotationCB = new JComboBox<>();
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
        horizontalAngField.setText("" + horizontalAng);
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

  public static class RotationGapException extends Exception {

    public RotationGapException(String s) {
      super(s);
    }
  }
}