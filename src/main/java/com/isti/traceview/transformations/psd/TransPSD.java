package com.isti.traceview.transformations.psd;

import asl.utils.FFTResult;
import asl.utils.TimeSeriesUtils;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
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

	private int effectiveLength = 0;

	@Override
	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration,
			JFrame parentFrame) {

		if (input.size() == 0) {
			JOptionPane.showMessageDialog(parentFrame, "Please select channels", "PSD computation warning",
					JOptionPane.ERROR_MESSAGE);
		} else if (input.get(0).getDataLength(ti) < 32) {
			JOptionPane.showMessageDialog(parentFrame, "One or more of the traces you selected does not contain enough datapoints (<32). "
					+ "Please select a longer dataset.", "PSD computation warning",
					JOptionPane.ERROR_MESSAGE);
		} else {
			try {
				List<XYSeries> plotData = createData(input, filter, ti, parentFrame);
				TimeInterval effectiveInterval = new TimeInterval(ti.getStart(),
						ti.getStart() + new Double(input.get(0).getSampleRate() * effectiveLength).longValue());
				@SuppressWarnings("unused")
				ViewPSD vp = new ViewPSD(parentFrame, plotData, effectiveInterval, (Configuration) configuration, input);
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

	public List<XYSeries> createData(List<PlotDataProvider> input, IFilter filter,
			TimeInterval ti, JFrame parentFrame) throws XMAXException {

		// evalresp doesn't play nicely with threads so let's get that out of the way first
		StringBuffer respNotFound = new StringBuffer();
		StringBuffer traceHadError = new StringBuffer();
		List<Complex[]> responses = generateResponses(input, ti, respNotFound);

		List<XYSeries> dataset = Collections.synchronizedList(new ArrayList<>());
		long startl = System.nanoTime();
		final int[] emptyResponses = {0};
		// this is a single-element array to allow value increments during parallel loop
		// since final primitives would not be able to be incremented

		IntStream.range(0, input.size()).parallel().forEach( i-> {
			PlotDataProvider channel = input.get(i);
			Complex[] respCurve = responses.get(i);
			if (respCurve == null) {
				++emptyResponses[0];
				return; // skip to next PSD -- this lambda is basically its own method
			}
			try {
				XYSeries xys = convertToPlottableSeries(channel, ti, respCurve);
				dataset.add(xys);
			} catch (XMAXException e) {
				logger.error(e);
				traceHadError.append(e.getMessage()).append("\n");
			}
		});

		dataset.sort(new XYSeriesComparator());

		long endl = System.nanoTime() - startl;
		double duration = endl * Math.pow(10, -9);
		logger.info("\nPSD calculation duration = " + duration + " sec");

		if (emptyResponses[0] == responses.size()) {
			throw new XMAXException("Cannot find responses for any channels selected.");
		} else if (dataset.size() == 0) {
			String message = "The following errors were caught while trying to produce a PSD:\n" +
					traceHadError.toString();
			throw new XMAXException(message);
		} else {
			StringBuilder message = new StringBuilder();
			if (respNotFound.length() > 0) {
				message.append("Error attempting to load responses for these channels:\n")
						.append(respNotFound.toString());
			}
			if (traceHadError.length() > 0) {
				message.append("The following errors occurred while trying to get trace data:\n")
						.append(traceHadError.toString());
			}
			if (message.length() > 0) {
				JOptionPane.showMessageDialog(parentFrame, message.toString(),
						"Warning", JOptionPane.WARNING_MESSAGE);
			}

		}

		return dataset;
	}

	private class XYSeriesComparator implements Comparator<XYSeries> {

		@Override
		public int compare(XYSeries o1, XYSeries o2) {
			// These should always be strings in our usage
			// and since this is a private method anyway this is overly-defensive programming
			// we COULD do o1.getKey().compareTo(o2.getKey)) but that would cause a warning to show up
			// and I don't like compiler warnings and neither should you, so we'll do this
			// and since they're already strings, the toString() method just ensures type-safety without
			// having to do any weird stuff with casts
			String key1 = o1.getKey().toString();
			String key2 = o2.getKey().toString();
			// anyway since strings are nicely comparable, we'll just call compareTo with those now
			return key1.compareTo(key2);
		}
	}

	private static XYSeries convertToPlottableSeries(PlotDataProvider channel, TimeInterval ti, Complex[] response)
			throws XMAXException {
		FFTResult data = getPSD(channel, ti, response);
		double[] frequenciesArray = data.getFreqs();
		Complex[] psd = data.getFFT();
		XYSeries xys = new XYSeries(channel.getName());
		for (int i = 0; i < frequenciesArray.length; ++i) {
			double period = 1. / frequenciesArray[i];
			// past 1E6 results are imprecise/unstable, and 0Hz is unplottable
			if (period < 1E6) {
				xys.add(1. / frequenciesArray[i], 10 * Math.log10(psd[i].abs()));
			}
		}
		return xys;
	}

	private static FFTResult getPSD(PlotDataProvider channel, TimeInterval ti, Complex[] response)
			throws XMAXException {

		int[] data = channel.getContinuousGaplessDataOverRange(ti);
		double[] doubleData = new double[data.length];
		IntStream.range(0, doubleData.length).parallel().forEach(i ->
				doubleData[i] = (double) data[i]
		);
		long interval = (long) channel.getSampleRate();

		return FFTResult.powerSpectra(doubleData, interval, response);
	}

	@Override
	public String getName() {
		return TransPSD.NAME;
	}

	private List<Complex[]> generateResponses(List<PlotDataProvider> channels, TimeInterval ti,
			StringBuffer respNotFound) {
		// generate the response curve for each channel and compile them into a list
		List<Complex[]> responses = new ArrayList<>();

		for (PlotDataProvider channel : channels) {
			// first, get the range of data for this channel
			long interval = (long) channel.getSampleRate();
			long traceLength = (ti.getEnd() - ti.getStart()) / interval;
			// divide that length by 4 because the PSD uses windows sized at 1/4 of the data
			int dataLength = (int) traceLength / 4;
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

			Cmplx[] response; // response is initially in a specific complex class from fissures
			try {
				response = channel.getResponse().getResp(ti.getStartTime(), 0,
						endFreq, padLength);
				// convert this into apache complex class as it's what the PSD calculator uses
				Complex[] responseAdapted = new Complex[response.length];
				IntStream.range(0, responseAdapted.length).parallel().forEach(i ->
						responseAdapted[i] = new Complex(response[i].real(), response[i].imag())
				);
				responses.add(responseAdapted);
			} catch (TraceViewException | NullPointerException e) {
				if (respNotFound.length() > 0) {
					respNotFound.append(", ");
				}
				respNotFound.append(channel.getName());
				// if the response doesn't exist, then
				responses.add(null);
			}
		}

		return responses;
	}

}
