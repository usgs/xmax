package com.isti.traceview.transformations.ppm;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Particle motion transformation. Prepares data for presentation in
 * {@link ViewPPM}
 * 
 */
public class TransPPM implements ITransformation {

	public static final String NAME = "Particle motion";


	@Override
	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration,
			JFrame parentFrame) {
		if ((input == null) || (input.size() != 2)) {
			JOptionPane.showMessageDialog(parentFrame, "You should select two channels to view PPM", "Error",
					JOptionPane.ERROR_MESSAGE);
		} else {
			try {
				/*
				 * Whenever we use channels with N and E as a third symbol
				 * (like, BHN/NHE) N channel ALWAYS denotes the north and E
				 * channel always denotes the East (regardless of the selection
				 * order). Same for channels BH1 and BH2 : 1 is North; 2 is
				 * East. For all other channel pairs, they go in the selection
				 * order: first NS, second EW.
				 */
				List<PlotDataProvider> inputRepositioned = new ArrayList<>();
				char type1 = input.get(0).getType();
				char type2 = input.get(1).getType();
				if (((type2 == 'N' || type2 == '1') && type1 != 'N' && type1 != '1')
						|| ((type1 == 'E' || type1 == '2') && type2 != 'E' && type2 != '2')) {
					inputRepositioned.add(input.get(1));
					inputRepositioned.add(input.get(0));
				} else {
					inputRepositioned.add(input.get(0));
					inputRepositioned.add(input.get(1));
				}

				// first index of outer array is north data, second is east
				// (inner data is, of course, the actual data associated with the trace & filters)
				int[][] bothData = createDataset(inputRepositioned, filter, ti);

				XYDataset dataset = populateDataset(bothData[0], bothData[1],
						inputRepositioned.get(0).getName()
								+ " " + inputRepositioned.get(1).getName());

				// OK, also make sure to calculate the expected orientation of the data
				double backAzimuth = estimateBackAzimuth(bothData[0], bothData[1]);

				@SuppressWarnings("unused")
				ViewPPM vr = new ViewPPM(parentFrame, dataset, ti,
						"N:" + inputRepositioned.get(0).getName() + "  E:" + inputRepositioned.get(1).getName(),
						filter, backAzimuth);
			} catch (XMAXException e) {
				JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}
		((XMAXframe) parentFrame).getGraphPanel().forceRepaint();
	}

	/**
	 * @param input
	 *            List of traces to process
	 * @param filter
	 *            Filter applied to traces before correlation
	 * @param ti
	 *            Time interval to define processed range
	 * @return int[][] 2-D array of format {xData, yData} (xData and yData are the int arrays holding
	 * the raw data from the traces being analyzed)
	 * @throws XMAXException
	 *             if sample rates differ, gaps in the data, or no data for a
	 *             channel
	 */
	int[][] createDataset(List<PlotDataProvider> input, IFilter filter, TimeInterval ti)
			throws XMAXException {

		PlotDataProvider channel1 = input.get(0); // N/S
		PlotDataProvider channel2 = input.get(1); // E/W
		if (channel1.getSampleRate() != channel2.getSampleRate())
			throw new XMAXException("Channels have different sample rate");
		double sampleRate;
		List<Segment> segments1;
		if (channel1.getRotation() != null && channel1.isRotated()) {
			segments1 = channel1.getRawData(channel1.getRotation(), ti);
		} else
			segments1 = channel1.getRawData(ti);

		int[] intData1 = new int[0];
		if (segments1.size() > 0) {
			long segment_end_time = 0;
			sampleRate = segments1.get(0).getSampleRate();
			for (Segment segment : segments1) {
				if (segment.getSampleRate() != sampleRate) {
					throw new XMAXException(
							"You have data with different sample rate for channel " + channel1.getName());
				}
				if (segment_end_time != 0
						&& Segment
						.isDataBreak(segment_end_time, segment.getStartTime().getTime(), sampleRate)) {
					throw new XMAXException("You have gap in the data for channel " + channel1.getName());
				}
				segment_end_time = segment.getEndTime().getTime();
				intData1 = IstiUtilsMath.padArray(intData1, segment.getData(ti).data);
			}

		} else {
			throw new XMAXException("You have no data for channel " + channel1.getName());
		}
		List<Segment> segments2;
		if (channel2.getRotation() != null && channel2.isRotated())
			segments2 = channel2.getRawData(channel2.getRotation(), ti);
		else
			segments2 = channel2.getRawData(ti);
		int[] intData2 = new int[0];
		if (segments2.size() > 0) {
			long segment_end_time = 0;
			for (Segment segment : segments2) {
				if (segment.getSampleRate() != sampleRate) {
					throw new XMAXException("Channels " + channel1.getName() + " and " + channel2.getName()
							+ " have different sample rates: " + sampleRate + " and " + segment.getSampleRate());
				}
				if (segment_end_time != 0
						&& Segment
						.isDataBreak(segment_end_time, segment.getStartTime().getTime(), sampleRate)) {
					throw new XMAXException("You have gap in the data for channel " + channel2.getName());
				}
				segment_end_time = segment.getEndTime().getTime();
				intData2 = IstiUtilsMath.padArray(intData2, segment.getData(ti).data);
			}

		} else {
			throw new XMAXException("You have no data for channel " + channel1.getName());
		}
		if (filter != null) {
			intData1 = new FilterFacade(filter, channel1).filter(intData1);
			intData2 = new FilterFacade(filter, channel2).filter(intData2);
		}

		return new int[][]{intData1, intData2};
	}

	XYDataset populateDataset(int[] intData1, int[] intData2, String seriesName) throws XMAXException {
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries series = new XYSeries(seriesName, false);

		int dataSize = Math.min(intData1.length, intData2.length);
		if (dataSize > maxDataLength) {
			throw new XMAXException("Too many datapoints are selected.");
		}
		ArrayValues values1 = new ArrayValues(intData1, dataSize);
		ArrayValues values2 = new ArrayValues(intData2, dataSize);
		for (int i = 0; i < dataSize; i++) {
			double x = intData1[i] - values1.getAverage();
			double y = intData2[i] - values2.getAverage();
			double radius = Math.sqrt(x * x + y * y);
			double theta = 180 * Math.atan2(y, x) / Math.PI;
			series.add(theta, radius);
		}
		dataset.addSeries(series);
		return dataset;
	}

	/**
	 * Calculates regression on event particle motion to get slope and then uses arctan to
	 * calculate the actual slope angle as back azimuth value (i.e., either the azimuth value
	 * or out by 180 degrees) which is then set to the correct quadrant based on input signs.
	 * @param north Data from sensor in north-facing direction
	 * @param east Data from sensor in east-facing direction
	 * @return Estimated azimuth of the sensor based on the inputs's slope and phasing
	 */
	static double estimateBackAzimuth (int[] north, int[] east) {
		// we don't care about the intercept, only the slope
		SimpleRegression slopeCalculation = new SimpleRegression(false);
		for (int i = 0; i < north.length; ++i) {
			slopeCalculation.addData(east[i], north[i]);
		}
		double backAzimuth = Math.atan(1. / slopeCalculation.getSlope());
		backAzimuth = Math.toDegrees(backAzimuth);

		// get a data point out from start to see if the inputs are in phase or not
		// we assume that a single point near the end of the window will be all we need
		int signumIndex = north.length - 19;

		int signumN = (int) Math.signum(north[signumIndex]);
		int signumE = (int) Math.signum(east[signumIndex]);

		return correctBackAzimuthQuadrant(backAzimuth, signumN, signumE);
	}

	/**
	 * Correct the back azimuth quadrant based on whether the phase of north and east data matches
	 * Note that data can still be out by 180 degrees and "correct" due to how slope works, which
	 * would depend on vertical data not used in these calculations.
	 * @param azimuth Estimated back azimuth value calculated from particle motion slope best-fit
	 * @param signumN North data sign value
	 * @param signumE East data sign value
	 * @return Angle corrected to the proper quadrants based on whether signs match or not
	 */
	static double correctBackAzimuthQuadrant(double azimuth, int signumN, int signumE) {
		double minValue = 0;
		double maxValue = 90;
		// due to how cursor works, fit angle is in either both quadrants 1 and 3 or 2 and 4
		// so we'll focus range of resulting angle to be between 0 and 180 (q. 1 vs. q. 2)
		// The quadrant pair chosen depends on whether or not the data has the same sign or not
		if (signumN != signumE) {
				minValue = 90;
				maxValue = 180;
		}
		while (azimuth < minValue) {
			azimuth += 90;
		}
		while (azimuth > maxValue) {
			azimuth -= 90;
		}
		return azimuth;
	}

	private class ArrayValues {
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		int average = 0;

		public ArrayValues(int[] array, int size) {
			for (int i = 0; i < size; i++) {
				if (array[i] > max)
					max = array[i];
				if (array[i] < min)
					min = array[i];
				average = average + array[i];
			}
			average = average / size;
		}

		@SuppressWarnings("unused")
		public int getMax() {
			return max;
		}

		@SuppressWarnings("unused")
		public int getMin() {
			return min;
		}

		public double getAverage() {
			return average;
		}
	}

	@Override
	public String getName() {
		return TransPPM.NAME;
	}
}
