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
					JOptionPane.WARNING_MESSAGE);
		} else if (input.get(0).getDataLength(ti) < 32) {
			JOptionPane.showMessageDialog(parentFrame, "One or more of the traces you selected does not contain enough datapoints (<32). "
					+ "Please select a longer dataset.", "PSD computation warning",
					JOptionPane.WARNING_MESSAGE);
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
					JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
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
		List<XYSeries> dataset = new ArrayList<>();
		ListIterator<PlotDataProvider> li = input.listIterator();
		StringBuilder respNotFound = new StringBuilder();
		long startl = System.nanoTime();

		for (PlotDataProvider channel : input) {
			try {
				FFTResult data = getPSD(channel, ti);
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
				dataset.add(xys);
			} catch (TraceViewException e) {
				respNotFound.append(", ");
				respNotFound.append(channel.getName());
			}
		}

		long endl = System.nanoTime() - startl;
		double duration = endl * Math.pow(10, -9);
		logger.info("\nPSD calculation duration = " + duration + " sec");

		if (input.size() == 0) {
			throw new XMAXException("Cannot find responses for any channels");
		} else {
			if (respNotFound.length() > 0) {
			  String message = "Cannot find responses for these channels: " + respNotFound.toString();
				JOptionPane.showMessageDialog(parentFrame, message,
            "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}

		return dataset;
	}

	public static FFTResult getPSD(PlotDataProvider channel, TimeInterval ti)
			throws XMAXException, TraceViewException {

		int[] data = channel.getContinuousGaplessDataOverRange(ti);
		double[] doubleData = new double[data.length];

		IntStream.range(0, doubleData.length).parallel().forEach(i ->
				doubleData[i] = (double) data[i]
		);

		int dataLength = doubleData.length / 4;
		int padLength = 2;
		while (padLength < dataLength) {
			padLength = padLength << 1;
		}

		long interval = (long) channel.getSampleRate();
		double period = interval / (double) TimeSeriesUtils.ONE_HZ_INTERVAL;
		double deltaFreq = 1. / (padLength * period);
		double endFreq = deltaFreq * (padLength - 1);

		Cmplx[] response = channel.getResponse().getResp(ti.getStartTime(), 0,
				endFreq, padLength);

		Complex[] responseAdapted = new Complex[response.length];
		IntStream.range(0, responseAdapted.length).parallel().forEach(i ->
				responseAdapted[i] = new Complex(response[i].real(), response[i].imag())
		);

		return FFTResult.powerSpectra(doubleData, interval, responseAdapted);
	}

	@Override
	public String getName() {
		return TransPSD.NAME;
	}

}
