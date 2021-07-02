package com.isti.traceview.transformations.psd;


import static asl.utils.NumericUtils.detrend;
import static com.isti.traceview.processing.IstiUtilsMath.getSmoothedPSD;

import asl.utils.FFTResult;
import asl.utils.Filter;
import asl.utils.timeseries.TimeSeriesUtils;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Response;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.math3.complex.Complex;
import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;

/**
 * Power spectra density transformation. Prepares data for presentation in
 * {@link ViewPSD}
 *
 * @author Max Kokoulin
 */
public class TransPSD implements ITransformation {
	private static final Logger logger = Logger.getLogger(TransPSD.class);

	public static final String NAME = "Power spectra density";
	public static final double SMOOTHING_FACTOR = 8;

	private static final int POINTS_TO_CONSIDER_LIMITING_DATA =
			20 * 60 * 60 * 24; // length of one day of 20 Hz data (25 * 86400 seconds in a day)

	/**
	 * Default value to use for window length divisor (i.e., 25% of data is in each window)
	 */
	public static final int DEFAULT_WINDOW_LENGTH_DIVISOR = 4;
	/**
	 * Default value to use for shift length divisor, the fraction of window size that is shifted
	 * for each iteration of Welch's method, i.e., 75% of data is common between sequential windows
	 */
	public static final int DEFAULT_SHIFT_LENGTH_DIVISOR = 4;

	@Override
	public void transform(List<PlotDataProvider> input, TimeInterval ti, Filter filter,
			Object configuration, JFrame parentFrame) {

		if (input.size() == 0) {
			JOptionPane.showMessageDialog(parentFrame, "Please select channels", "PSD computation warning",
					JOptionPane.ERROR_MESSAGE);
		} else if (input.get(0).getDataLength(ti) < 32) {
			JOptionPane.showMessageDialog(parentFrame, "One or more of the traces you selected does not contain enough datapoints (<32). "
							+ "Please select a longer dataset.", "PSD computation warning",
					JOptionPane.ERROR_MESSAGE);
		} else {

			boolean doSmoothing = true;
			for (PlotDataProvider channel : input) {
				if (channel.getDataLength(ti) >= POINTS_TO_CONSIDER_LIMITING_DATA) {

					String message = "One or more traces of data (" + channel.getName() + ") has at least as "
							+ "many points as a full day of 20 Hz Data.\nRunning the smoothing on this much data "
							+ "may take several minutes.\n" +
							"Do you want to still perform smoothing on these traces?";
					int selection = JOptionPane.showConfirmDialog(parentFrame, message,
							"Smoothing duration warning", JOptionPane.YES_NO_CANCEL_OPTION);
					if (selection == JOptionPane.CANCEL_OPTION) {
						JOptionPane.showMessageDialog(parentFrame, "Operation cancelled",
								"Warning", JOptionPane.ERROR_MESSAGE);
						return;
					}
					// only do smoothing if yes is chosen
					doSmoothing = (selection == JOptionPane.YES_OPTION);
					break;
				}
			}

			try {
				Configuration config = (Configuration) configuration;
				boolean useRinglersMethod = config.getBoolean("LongWindows", false);
				if (!config.containsKey("LongWindows")) {
					useRinglersMethod = config.getBoolean("RinglersMethod", false);
				}
				int windowLength = useRinglersMethod ? 2 : DEFAULT_WINDOW_LENGTH_DIVISOR;
				int shiftDivisor = useRinglersMethod ? 8 : DEFAULT_SHIFT_LENGTH_DIVISOR;
				logger.debug("Using Ringler's method? " + useRinglersMethod);
				logger.debug("Using the following window length and shift divisor values: (" +
						windowLength + ", " + shiftDivisor + ")");
				List<XYSeries> plotData = createData(input, filter, ti, doSmoothing,
						windowLength, shiftDivisor, parentFrame);
				@SuppressWarnings("unused")
				ViewPSD vp = new ViewPSD(parentFrame, plotData, ti, config, input);
			} catch (XMAXException e) {
				logger.error(e);
				if (!e.getMessage().equals("Operation cancelled")) {
					JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.ERROR_MESSAGE);
				}
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
	 * @param parentFrame
	 *            parent frame
	 * @return list of spectra for selected traces and time ranges
	 * @throws XMAXException
	 *             if sample rates differ, gaps in the data, or no data for a
	 *             channel
	 */
	public List<XYSeries> createData(List<PlotDataProvider> input, Filter filter,
			TimeInterval ti, boolean doSmoothing, JFrame parentFrame) throws XMAXException {

		return createData(input, filter, ti, doSmoothing, DEFAULT_WINDOW_LENGTH_DIVISOR,
				DEFAULT_SHIFT_LENGTH_DIVISOR, parentFrame);
	}

	/**
	 * @param input
	 *            List of traces to process
	 * @param filter
	 *            Filter applied to traces before correlation
	 * @param ti
	 *            Time interval to define processed range
	 * @param parentFrame
	 *            parent frame
	 * @param doSmoothing true if the data should be smoothed before plotting
	 * @param windowDivisor denominator x for which 1/x the data length is used per window
	 * @param shiftDivisor denominator x for which 1/x the window length is shifted per iteration
	 * @return list of spectra for selected traces and time ranges
	 * @throws XMAXException
	 *             if sample rates differ, gaps in the data, or no data for a
	 *             channel
	 */
	public List<XYSeries> createData(List<PlotDataProvider> input, Filter filter,
			TimeInterval ti, boolean doSmoothing, int windowDivisor, int shiftDivisor,
			JFrame parentFrame) throws XMAXException {

		// evalresp doesn't play nicely with threads so let's get that out of the way first
		// we can use a stringbuilder for error messages because getting the responses is not threaded
		StringBuilder respNotFound = new StringBuilder();
		final Map<String, Response> responses = new HashMap<>();
		for (PlotDataProvider channel : input) {
			try {
				Response resp = channel.getResponse();
				if (resp != null) {
					responses.put(channel.getName(), resp);
				}
			} catch (NullPointerException e) {
				logger.error("error with responses: " + e);
				if (respNotFound.length() > 0) {
					respNotFound.append(", ");
				}
				respNotFound.append(channel.getName());
			}
		}

		StringBuffer traceHadError = new StringBuffer();
		// this list needs to be synchronized because it's written to at the end of each thread
		List<XYSeries> dataset = Collections.synchronizedList(new ArrayList<>());
		long startl = System.nanoTime();
		IntStream.range(0, input.size()).parallel().forEach( i-> {
			PlotDataProvider channel = input.get(i);
			try {
				String key = channel.getName();
				if (!responses.containsKey(key)) {
					return; // skip to next PSD -- this lambda is basically its own method
				}
				Complex[] respCurve = generateResponse(responses.get(key), ti,
						(long) channel.getSampleInterval(), windowDivisor);
				if (respCurve == null) {
					return;
				}
				try {
					XYSeries[] xys =
							convertToPlottableSeries(channel, ti, respCurve, windowDivisor, shiftDivisor,
									doSmoothing);
					Collections.addAll(dataset, xys);
				} catch (XMAXException e) {
					logger.error(e);
					traceHadError.append(e.getMessage()).append("\n");
				}
			} catch (TraceViewException e) {
				logger.error(e);
				traceHadError.append(e.getMessage()).append("\n");
			}
		});

		dataset.sort(new XYSeriesComparator());

		long endl = System.nanoTime() - startl;
		double duration = endl * Math.pow(10, -9);
		logger.info("\nPSD calculation duration = " + duration + " sec");

		// throw an error if no data could be loaded. otherwise try with what we have
		if (responses.size() == 0) {
			throw new XMAXException("Cannot find responses for any channels selected.");
		} else if (dataset.size() == 0) {
			String message = "The following errors were caught while trying to produce a PSD:\n" +
					traceHadError;
			throw new XMAXException(message);
		} else {
			StringBuilder message = new StringBuilder();
			if (respNotFound.length() > 0) {
				message.append("Error attempting to load responses for these channels:\n")
						.append(respNotFound);
			}
			if (traceHadError.length() > 0) {
				message.append("The following errors occurred while trying to get trace data:\n")
						.append(traceHadError);
			}
			if (message.length() > 0) {
				JOptionPane.showMessageDialog(parentFrame, message.toString(),
						"Warning", JOptionPane.WARNING_MESSAGE);
			}

		}

		return dataset;
	}

	private static class XYSeriesComparator implements Comparator<XYSeries> {

		@Override
		public int compare(XYSeries o1, XYSeries o2) {
			// since they're already strings, the toString() method just ensures type-safety without
			// having to do any weird stuff with casts
			String key1 = o1.getKey().toString();
			String key2 = o2.getKey().toString();
			// handling the cases where only one set of data is smoothed; that one has higher priority
			if (key1.contains("smoothed")) {
				if (!key2.contains("smoothed")) {
					return -1;
				}
			} else if (key2.contains("smoothed")) {
				return 1;
			}
			// anyway since strings are nicely comparable, we'll just call compareTo with those now
			return key1.compareTo(key2);
		}
	}

	private static XYSeries[] convertToPlottableSeries(PlotDataProvider channel, TimeInterval ti,
			Complex[] response, int windowDivisor, int shiftDivisor, boolean performSmoothing)
			throws XMAXException {

		long interval = (long) channel.getSampleInterval();
		long traceLength = (ti.getEnd() - ti.getStart()) / interval;
		int windowLength = (int) (traceLength / windowDivisor);
		int shiftLength = windowLength / shiftDivisor;

		FFTResult data = getPSD(channel, ti, response, windowLength, shiftLength);
		double[] frequenciesArray = data.getFreqs();
		Complex[] psd = data.getFFT();
		double[] dbScaled = new double[psd.length];
		XYSeries xys = new XYSeries(channel.getName());

		int firstIndexWithPeriodAboveThreshhold = -1;
		for (int i = 0; i < frequenciesArray.length; ++i) {
			double period = 1. / frequenciesArray[i];
			// past 1E6 results are imprecise/unstable, and 0Hz is unplottable
			dbScaled[i] = 10 * Math.log10(psd[i].abs());
			if (period < 1E6) {
				if (firstIndexWithPeriodAboveThreshhold < 0) {
					firstIndexWithPeriodAboveThreshhold = i;
				}
				xys.add(period, dbScaled[i]);
			}
		}
		if (performSmoothing) {
			XYSeries smoothed = new XYSeries(channel.getName() + " smoothed");
			double[] smoothedData = getSmoothedPSD(
					frequenciesArray, dbScaled, firstIndexWithPeriodAboveThreshhold
			);
			for (int i = firstIndexWithPeriodAboveThreshhold; i < smoothedData.length; ++i) {
				double period = 1. / frequenciesArray[i];
				smoothed.add(period, smoothedData[i]);
			}
			return new XYSeries[]{xys, smoothed};
		}
		return new XYSeries[]{xys};
	}

	private static FFTResult getPSD(PlotDataProvider channel, TimeInterval ti,
			Complex[] response, int range, int slider)
			throws XMAXException {

		int[] data = channel.getContinuousGaplessDataOverRange(ti);
		double[] doubleData = new double[data.length];
		{
			// finalized reference for use in parallel stream
			double[] finalDoubleData = doubleData;
			IntStream.range(0, doubleData.length).parallel().forEach(i ->
					finalDoubleData[i] = data[i]
			);
		}
		// we need to detrend here because java utils expects that before the trace is passed in
		doubleData = detrend(doubleData);
		long interval = (long) channel.getSampleInterval();

		return FFTResult.powerSpectra(doubleData, interval, response, range, slider);
	}

	@Override
	public String getName() {
		return TransPSD.NAME;
	}

	private Complex[] generateResponse(Response response, TimeInterval ti,
			long interval, int windowDivisor) throws TraceViewException {
		Complex[] respCurve = null;

		// first, get the range of data for this channel
		long traceLength = (ti.getEnd() - ti.getStart()) / interval;
		// divide that length by 4 because the PSD uses windows sized at 1/4 of the data
		int dataLength = (int) traceLength / windowDivisor;
		// PSD output is padded to be the largest power of 2 above the length given
		int padLength = 2;
		while (padLength < dataLength) {
			padLength = padLength << 1;
		}
		// now get the frequency range for the PSD based on the sample interval in seconds
		double period = interval / (double) TimeSeriesUtils.ONE_HZ_INTERVAL;
		// these are in units of Hz
		double deltaFreq = 1. / (padLength * period);
		double endFreq = deltaFreq * (padLength - 1);

		assert(response != null);

		respCurve = response.getResp(ti.getStartTime(), 0,
				endFreq, padLength);
		return respCurve;
	}

}
