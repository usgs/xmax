package com.isti.traceview.transformations.ppm;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;
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

	private static final Logger logger = Logger.getLogger(TransPPM.class);

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
				double[][] bothData = createDataset(inputRepositioned, filter, ti);

				XYDataset dataset = populateDataset(bothData[0], bothData[1],
						inputRepositioned.get(0).getName()
								+ " " + inputRepositioned.get(1).getName());

				// OK, also make sure to calculate the expected orientation of the data
				double backAzimuth = estimateBackAzimuth(bothData[0], bothData[1]);

				@SuppressWarnings("unused")
				ViewPPM vr = new ViewPPM(parentFrame, dataset, ti,
						"N:" + inputRepositioned.get(0).getName() + "  E:" + inputRepositioned.get(1).getName(),
						filter, backAzimuth);
			} catch (XMAXException | RuntimeException e) {
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
	double[][] createDataset(List<PlotDataProvider> input, IFilter filter, TimeInterval ti)
			throws XMAXException {

		PlotDataProvider channel1 = input.get(0); // N/S
		PlotDataProvider channel2 = input.get(1); // E/W
		if (channel1.getSampleRate() != channel2.getSampleRate())
			throw new XMAXException("Channels have different sample rate");

		// not a synchronized list because we use a forEachOrdered stream loo=XX
		List<double[]> output = new ArrayList<>();
		Stream.of(channel1, channel2).parallel().forEachOrdered(channel -> {
			try {
				int[] intData = channel.getContinuousGaplessDataOverRange(ti);
				if (filter != null) {
					intData = new FilterFacade(filter, channel).filter(intData);
				}
				// haven't tried doing a stream in a stream, might be faster
				double avg = 0;
				for (int intDatum : intData) {
					avg += intDatum;
				}
				avg /= intData.length;
				double[] doubleData = new double[intData.length];
				for (int i = 0; i < intData.length; ++i) {
					doubleData[i] = intData[i] - avg;
				}
				output.add(doubleData);
			} catch (XMAXException e) {
				logger.error("Caught exception while iterating through transformation: ", e);;
				throw new RuntimeException(e.getMessage());
			}
		});

		return output.toArray(new double[][]{});
	}

	XYDataset populateDataset(double[] data1, double[] data2, String seriesName) throws XMAXException {
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries series = new XYSeries(seriesName, false);

		int dataSize = Math.min(data1.length, data2.length);
		if (dataSize > maxDataLength) {
			throw new XMAXException("Too many datapoints are selected.");
		}

		for (int i = 0; i < data1.length; ++i) {
			double x = data1[i];
			double y = data2[i];
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
	 * or out by 180 degrees); .
	 * @param north Data from sensor in north-facing direction
	 * @param east Data from sensor in east-facing direction
	 * @return Estimated azimuth of the sensor based on the inputs's slope and phasing
	 */
	static double estimateBackAzimuth(double[] north, double[] east) {

		// we don't care about the intercept, only the slope
		SimpleRegression slopeCalculation = new SimpleRegression(false);
		for (int i = 0; i < north.length; ++i) {
			slopeCalculation.addData(east[i], north[i]);
		}
		double slope = slopeCalculation.getSlope();
		System.out.println(slope);


		double backAzimuth;
		// if this is the case, signal is pure north, so azimuth is zero
		if (Double.isNaN(slope)) {
			backAzimuth = 0;
		} else {
			backAzimuth = Math.atan(1. / slope);
			backAzimuth = (Math.toDegrees(backAzimuth) + 360) % 360;
		}

		return backAzimuth;
	}

	@Override
	public String getName() {
		return TransPPM.NAME;
	}
}
