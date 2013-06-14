package com.isti.traceview.processing;

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
import javax.swing.border.EtchedBorder;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import com.isti.traceview.data.PlotDataPoint;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.gui.IColorModeState;

/**
 * This class holds all information to describe traces rotation and provides methods to compute
 * rotated traces
 * 
 * @author Max Kokoulin
 */
public class Rotation {
	public enum RotationType {
		/**
		 * Three-angles cartesian rotation
		 */
		ARBITRARY,
		/**
		 * Rotation specific to individual digitizer models
		 */
		STANDARD
	};

	public enum StandardRotation {
		THRILLIUM_UVW_TO_XMAX, THRILLIUM_XMAX_TO_UVW, STS2_UVW_TO_XMAX, STS2_XMAX_TO_UVW
	};

	private static Logger lg = Logger.getLogger(Rotation.class);

	private static double[][] UVWtoXMAXthrillium = { { Math.sqrt(2.0 / 3.0), -Math.sqrt(1.0 / 6.0), -Math.sqrt(1.0 / 6.0) },
			{ 0.0, Math.sqrt(0.5), -Math.sqrt(0.5) }, { Math.sqrt(1.0 / 3.0), Math.sqrt(1.0 / 3.0), Math.sqrt(1.0 / 3.0) } };
	private static double[][] XMAXtoUVWthrillium = { { Math.sqrt(2.0 / 3.0), 0.0, Math.sqrt(1.0 / 3.0) },
			{ -Math.sqrt(1.0 / 6.0), Math.sqrt(0.5), Math.sqrt(1.0 / 3.0) }, { -Math.sqrt(1.0 / 6.0), -Math.sqrt(0.5), Math.sqrt(1.0 / 3.0) } };
	private static double[][] UVWtoXMAXsts2 = { { -Math.sqrt(2.0 / 3.0), Math.sqrt(1.0 / 6.0), Math.sqrt(1.0 / 6.0) },
			{ 0.0, Math.sqrt(0.5), -Math.sqrt(0.5) }, { Math.sqrt(1.0 / 3.0), Math.sqrt(1.0 / 3.0), Math.sqrt(1.0 / 3.0) } };
	private static double[][] XMAXtoUVWsts2 = { { -Math.sqrt(2.0 / 3.0), 0.0, Math.sqrt(1.0 / 3.0) },
			{ Math.sqrt(1.0 / 6.0), Math.sqrt(0.5), Math.sqrt(1.0 / 3.0) }, { Math.sqrt(1.0 / 6.0), -Math.sqrt(0.5), Math.sqrt(1.0 / 3.0) } };
	private Matrix matrix = null;

	/**
	 * Constructor for ARBITRARY type, with three angles (in degrees)
	 */
	public Rotation(double X, double Y, double Z) {
		initMatrix(X, Y, Z);
	}

	/**
	 * Constructor for STANDARD type rotation
	 */
	public Rotation(StandardRotation standardRotation) {
		initMatrix(standardRotation);
	}

	/**
	 * Constructor with visual query dialog to describe rotation
	 */
	public Rotation(JFrame frame) {
		RotationDialog dialog = new RotationDialog(frame);
		RotationType type = dialog.type;
		StandardRotation standardRotation = dialog.standardRotation;
		if(type==null){
			dialog.dispose();
		} else if (type.equals(RotationType.ARBITRARY)) {
			initMatrix(dialog.X, dialog.Y, dialog.Z);
			dialog.dispose();
		} else {
			initMatrix(dialog.standardRotation);
			dialog.dispose();
		}
	}
	
	public Matrix getMatrix(){
		return matrix;
	}

	/**
	 * Computes rotation matrix
	 * 
	 * @param X
	 *            in degrees
	 * @param Y
	 *            in degrees
	 * @param Z
	 *            in degrees
	 */

	private void initMatrix(double X, double Y, double Z) {
		double XRAD = X * Math.PI / 180.0;
		double YRAD = Y * Math.PI / 180.0;
		double ZRAD = Z * Math.PI / 180.0;
		double[][] matrixData = {
	{ Math.cos(YRAD) * Math.cos(ZRAD),	Math.sin(XRAD) * Math.sin(YRAD) * Math.cos(ZRAD) + Math.cos(XRAD) * Math.sin(ZRAD),		-Math.cos(XRAD) * Math.sin(YRAD) * Math.cos(ZRAD) + Math.sin(XRAD) * Math.sin(ZRAD) },
	{ -Math.cos(YRAD) * Math.sin(ZRAD),	-Math.sin(XRAD) * Math.sin(YRAD) * Math.sin(ZRAD) + Math.cos(XRAD) * Math.cos(ZRAD),	Math.cos(XRAD) * Math.sin(YRAD) * Math.sin(ZRAD) + Math.sin(XRAD) * Math.cos(ZRAD) },
	{ Math.sin(YRAD), 					-Math.sin(XRAD) * Math.cos(YRAD), 														Math.cos(XRAD) * Math.cos(YRAD) } };
		matrix = new Matrix(matrixData);
		//matrix.show();
	}

	/**
	 * Computes rotation matrix
	 */
	private void initMatrix(StandardRotation standardRotation) {
		switch (standardRotation) {
		case THRILLIUM_UVW_TO_XMAX:
			matrix = new Matrix(UVWtoXMAXthrillium);
			break;
		case THRILLIUM_XMAX_TO_UVW:
			matrix = new Matrix(XMAXtoUVWthrillium);
			break;
		case STS2_UVW_TO_XMAX:
			matrix = new Matrix(UVWtoXMAXsts2);
			break;
		case STS2_XMAX_TO_UVW:
			matrix = new Matrix(XMAXtoUVWsts2);
			break;
		}
		//matrix.show();
	}

	/**
	 * Rotate pixelized data
	 * If we have overlap on the trace we take only first segment.
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
	 */
	public PlotData rotate(PlotDataProvider channel, TimeInterval ti, int pointCount, IFilter filter, IColorModeState colorMode) throws TraceViewException {
		PlotData[] tripletPlotData = new PlotData[3];
		char channelType = channel.getType();
		PlotData toProcess = channel.getPlotData(ti, pointCount, null, filter, colorMode);
		PlotData ret = new PlotData(channel.getName(), channel.getColor());
		if (channelType == 'E' || channelType == '2') {
			tripletPlotData[0] = toProcess;
			try{
				tripletPlotData[1] = getComplementaryPlotData(channel, 'N', ti, pointCount, filter, colorMode);
			} catch (TraceViewException te) {
				tripletPlotData[1] = getComplementaryPlotData(channel, '1', ti, pointCount, filter, colorMode);
			}
			tripletPlotData[2] = getComplementaryPlotData(channel, 'Z', ti, pointCount, filter, colorMode);

		} else if (channelType == 'N' || channelType == '1') {
			try{
				tripletPlotData[0] = getComplementaryPlotData(channel, 'E', ti, pointCount, filter, colorMode);
			} catch (TraceViewException te) {
				tripletPlotData[0] = getComplementaryPlotData(channel, '2', ti, pointCount, filter, colorMode);
			}
			tripletPlotData[1] = toProcess;
			tripletPlotData[2] = getComplementaryPlotData(channel, 'Z', ti, pointCount, filter, colorMode);

		} else if (channelType == 'Z') {
			try{
				tripletPlotData[0] = getComplementaryPlotData(channel, 'E', ti, pointCount, filter, colorMode);
			} catch (TraceViewException te) {
				tripletPlotData[0] = getComplementaryPlotData(channel, '2', ti, pointCount, filter, colorMode);
			}
			try{
				tripletPlotData[1] = getComplementaryPlotData(channel, 'N', ti, pointCount, filter, colorMode);
			} catch (TraceViewException te) {
				tripletPlotData[1] = getComplementaryPlotData(channel, '1', ti, pointCount, filter, colorMode);
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
				for (int j = 0; j < 8; j++) {
					rotatedCubicle[j] = matrix.times(new Matrix(cubicle[j])).getData();
				}
				double[][] rotatedMean = matrix.times(new Matrix(mean)).getData();
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
				pdp = new PlotDataPoint(top, bottom, mean[index][0], toProcess.getPixels().get(i)[0].getSegmentNumber(), 
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
	 */
	public List<Segment> rotate(PlotDataProvider channel, TimeInterval ti) throws TraceViewException {
		PlotDataProvider[] triplet = getChannelsTriplet(channel, ti);
		char channelType = channel.getType();
		List<Segment> ret = new ArrayList<Segment>();
		double[][] pointPosition = new double[3][1];
		for (Segment segment: channel.getRawData(ti)) {
			Segment rotated = new Segment(null, segment.getStartOffset(), segment.getStartTime(), segment.getSampleRate(), segment.getSampleCount(),
					segment.getSourceSerialNumber());
			double currentTime = segment.getStartTime().getTime();
			for (int value: segment.getData().data) {
				pointPosition[0][0] = triplet[0].getRawData(currentTime);
				pointPosition[1][0] = triplet[1].getRawData(currentTime);
				pointPosition[2][0] = triplet[2].getRawData(currentTime);
				if (pointPosition[0][0] == Integer.MIN_VALUE || pointPosition[0][0] == Integer.MIN_VALUE || pointPosition[0][0] == Integer.MIN_VALUE) {

				} else {
					double[][] rotatedPointPosition = matrix.times(new Matrix(pointPosition)).getData();
					if (channelType == 'E' || channelType == 'U' || channelType== '2') {
						rotated.addDataPoint(new Double(rotatedPointPosition[0][0]).intValue());
					} else if (channelType == 'N' || channelType == 'V' || channelType == '1') {
						rotated.addDataPoint(new Double(rotatedPointPosition[1][0]).intValue());
					} else if (channelType == 'Z' || channelType == 'W') {
						rotated.addDataPoint(new Double(rotatedPointPosition[2][0]).intValue());
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
	 * Checks if we have loaded traces for all three coordinates to process rotation
	 * 
	 * @param channel
	 *            trace to check
	 * @param ti
	 *            time interval to process
	 * @return flag
	 */
	public static boolean isComplementaryChannelExist(PlotDataProvider channel, TimeInterval ti) {
		try {
			PlotDataProvider[] triplet = getChannelsTriplet(channel, ti);
		} catch (TraceViewException e) {
			JOptionPane.showMessageDialog(TraceView.getFrame(), e, "Rotation warning", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

	/**
	 * Finds triplet of traces for cartesian coordinates
	 * 
	 * @param channel
	 *            trace to check
	 * @param ti
	 *            time interval to process
	 * @return array of 3 traces for 3 coordinates
	 * @throws TraceViewException
	 */
	public static PlotDataProvider[] getChannelsTriplet(PlotDataProvider channel, TimeInterval ti) throws TraceViewException {
		PlotDataProvider[] chs = new PlotDataProvider[3];
		char channelType = channel.getType();
		if (channelType == 'E' || channelType == '2') {
			chs[0] = channel;
			try{
				chs[1] = getComplementaryChannel(channel, 'N');
			} catch (TraceViewException te) {
				chs[1] = getComplementaryChannel(channel, '1');
			}
			chs[2] = getComplementaryChannel(channel, 'Z');

		} else if (channelType == 'N' || channelType == '1') {
			try{
				chs[0] = getComplementaryChannel(channel, 'E');
			} catch (TraceViewException te) {
				chs[0] = getComplementaryChannel(channel, '2');
			}
			chs[1] = channel;
			chs[2] = getComplementaryChannel(channel, 'Z');

		} else if (channelType == 'Z') {
			try{
				chs[0] = getComplementaryChannel(channel, 'E');
			} catch (TraceViewException te) {
				chs[0] = getComplementaryChannel(channel, '2');
			}
			try{
				chs[1] = getComplementaryChannel(channel, 'N');
			} catch (TraceViewException te) {
				chs[1] = getComplementaryChannel(channel, '1');
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

	private static PlotDataProvider getComplementaryChannel(PlotDataProvider channel, char channelType) throws TraceViewException {
		String channelName = channel.getChannelName().substring(0, channel.getChannelName().length() - 1) + channelType;
		PlotDataProvider channelComplementary = TraceView.getDataModule().getChannel(channelName, channel.getStation(), channel.getNetworkName(),
				channel.getLocationName());
		if (channelComplementary != null && channelComplementary.getSampleRate() == channel.getSampleRate()) {
			return channelComplementary;
		} else {
			throw new TraceViewException("Can't find channels triplet to rotate " + channel.getName() + ": " + channel.getNetworkName() + "/"
					+ channel.getStation().getName() + "/" + channel.getLocationName() + "/" + channelName);
		}
	}

	private static PlotData getComplementaryPlotData(PlotDataProvider channel, char channelType, TimeInterval ti, int pointCount, IFilter filter, IColorModeState colorMode)
			throws TraceViewException {
		return getComplementaryChannel(channel, channelType).getPlotData(ti, pointCount, null, filter, colorMode);
	}

	/**
	 * Visual dialog to enter rotation description
	 */
	public class RotationDialog extends JDialog implements PropertyChangeListener, ItemListener {
		private JFrame frame;
		private RotationType type = RotationType.ARBITRARY;
		private StandardRotation standardRotation = StandardRotation.THRILLIUM_UVW_TO_XMAX;
		private double X;
		private double Y;
		private double Z;
		private JOptionPane optionPane = null;
		private JPanel mainPanel;
		private JLabel XL;
		private JLabel YL;
		private JLabel ZL;
		private JTextField XTF;
		private JTextField YTF;
		private JTextField ZTF;
		private JComboBox rotationTypeCB;
		private JPanel arbitraryPanel;
		private JPanel standardPanel;
		private JComboBox standardRotationCB;
		private JLabel rotationTypeL;
		private JPanel swithPanel;
		private JLabel standardRotationL;

		public RotationDialog(JFrame frame) {
			super(frame, "Rotation options", true);
			this.frame = frame;
			X = 0.0;
			Y = 0.0;
			Z = 0.0;
			Object[] options = { "OK", "Close" };
			// Create the JOptionPane.
			optionPane = new JOptionPane(createDesignPanel(type, standardRotation), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION, null,
					options, options[0]);
			// Make this dialog display it.
			setContentPane(optionPane);
			optionPane.setPreferredSize(new java.awt.Dimension(295, 180));
			optionPane.addPropertyChangeListener(this);
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter(){
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
			getRotationTypeCB().setSelectedIndex(type.ordinal());
			getXTF().setText(new Double(X).toString());
			getYTF().setText(new Double(Y).toString());
			getZTF().setText(new Double(Z).toString());
			if (type.equals(RotationType.STANDARD)) {
				mainPanel.add(getStandardPanel());
			} else if (type.equals(RotationType.ARBITRARY)) {
				mainPanel.add(getArbitraryPanel());
			}
			return mainPanel;
		}

		/** Listens to the check boxes. */
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource().equals(getRotationTypeCB())) {
				type = RotationType.values()[getRotationTypeCB().getSelectedIndex()];
				if (type.equals(RotationType.STANDARD)) {
					mainPanel.remove(getArbitraryPanel());
					mainPanel.add(getStandardPanel());
					getStandardPanel().setVisible(false);
					getStandardPanel().setVisible(true);
				} else if (type.equals(RotationType.ARBITRARY)) {
					mainPanel.remove(getStandardPanel());
					mainPanel.add(getArbitraryPanel());
					getArbitraryPanel().setVisible(false);
					getArbitraryPanel().setVisible(true);
				}
			} else if (e.getSource().equals(getStandardRotationCB())) {
				standardRotation = StandardRotation.values()[getStandardRotationCB().getSelectedIndex()];
			}
		}

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
					} else if (type.equals(RotationType.ARBITRARY)) {
						try {
							X = Double.parseDouble(getXTF().getText());
							Y = Double.parseDouble(getYTF().getText());
							Z = Double.parseDouble(getZTF().getText());
							setVisible(false);
						} catch (NumberFormatException e1) {
							JOptionPane.showMessageDialog(frame, "Check correct double number format", "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
		}

		private JComboBox getRotationTypeCB() {
			if (rotationTypeCB == null) {
				ComboBoxModel rotationTypeCBModel = new DefaultComboBoxModel(new String[]{ "Arbitrary", "Standard" });
				rotationTypeCB = new JComboBox();
				rotationTypeCB.setModel(rotationTypeCBModel);
				rotationTypeCB.setPreferredSize(new java.awt.Dimension(141, 21));
				rotationTypeCB.addItemListener(this);
			}
			return rotationTypeCB;
		}

		private JPanel getArbitraryPanel() {
			if (arbitraryPanel == null) {
				arbitraryPanel = new JPanel();
				GridBagLayout arbitraryPanelLayout = new GridBagLayout();
				arbitraryPanel.setPreferredSize(new java.awt.Dimension(304, 196));
				arbitraryPanelLayout.rowWeights = new double[]{ 0.1, 0.1, 0.1 };
				arbitraryPanelLayout.rowHeights = new int[]{ 7, 7, 7 };
				arbitraryPanelLayout.columnWeights = new double[]{ 0.1, 0.1 };
				arbitraryPanelLayout.columnWidths = new int[]{ 7, 7 };
				arbitraryPanel.setLayout(arbitraryPanelLayout);
				arbitraryPanel.add(getXTF(), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				arbitraryPanel.add(getYTF(), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				arbitraryPanel.add(getZTF(), new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 0));

				arbitraryPanel.add(getXL(), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 0));
				arbitraryPanel.add(getYL(), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				arbitraryPanel.add(getZL(), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 0));
			}
			return arbitraryPanel;
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

		private JComboBox getStandardRotationCB() {
			if (standardRotationCB == null) {
				ComboBoxModel rotationTypeCBModel = new DefaultComboBoxModel(new String[]{ "Thrillium UVW to XMAX", "Thrillium XMAX to UVW",
						"STS2 UVW to XMAX", "STS2 XMAX to UVW" });
				standardRotationCB = new JComboBox();
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

		private JTextField getXTF() {
			if (XTF == null) {
				XTF = new JTextField();
				XTF.setSize(80, 22);
				XTF.setPreferredSize(new java.awt.Dimension(80, 22));
			}
			return XTF;
		}

		private JTextField getZTF() {
			if (ZTF == null) {
				ZTF = new JTextField();
				ZTF.setSize(80, 22);
				ZTF.setPreferredSize(new java.awt.Dimension(80, 22));
			}
			return ZTF;
		}

		private JTextField getYTF() {
			if (YTF == null) {
				YTF = new JTextField();
				YTF.setSize(80, 22);
				YTF.setPreferredSize(new java.awt.Dimension(80, 22));
			}
			return YTF;
		}

		private JLabel getXL() {
			if (XL == null) {
				XL = new JLabel();
				XL.setText("X:");
			}
			return XL;
		}

		private JLabel getYL() {
			if (YL == null) {
				YL = new JLabel();
				YL.setText("Y:");
			}
			return YL;
		}

		private JLabel getZL() {
			if (ZL == null) {
				ZL = new JLabel();
				ZL.setText("Z:");
			}
			return ZL;
		}
	}
}
