package com.isti.traceview.transformations.psd;

import com.isti.jevalresp.RespUtils;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Response;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Rotation;
import com.isti.traceview.processing.Spectra;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.configuration.Configuration;
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
			} catch (RuntimeException e) {
				logger.error(e);
				e.printStackTrace();
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
		input.parallelStream().forEachOrdered(channel -> {

			int[] intData;
			try {
				intData = channel.getContinuousGaplessDataOverRange(ti);
			} catch (XMAXException e) {
				throw new RuntimeException(e.getMessage());
			}
			int ds;
			if (intData.length > maxDataLength) {
				ds = maxDataLength; // maxDataLength is already a power of two
				int[] tempIntData = new int[ds];
				System.arraycopy(intData, 0, tempIntData, 0, maxDataLength);
				intData = tempIntData;
				((XMAXframe) parentFrame).getStatusBar().setMessage(
						"Points count (" + intData.length + ") exceeds max value for trace " + channel
								.getName());
			} else {
				ds = intData.length;
			}
			if (ds > effectiveLength) {
				effectiveLength = ds;
			}

			logger.debug("data size = " + ds);

			/*
			 * Here we compute the power spectral density of the selected data
			 * using the Welch method with 13 windows 75% overlap. The actual
			 * PSD is calculated in the getPSD function within Spectra.java.
			 */
			int dsDataSegment = new Double(Math.round(intData.length / 4.0)).intValue();

			int smallDataSegmentLimit = new Double(
					Math.ceil(Math.pow(2, (Math.ceil(IstiUtilsMath.log2(dsDataSegment)) - 1)))).intValue();
			// set smallDataSegment limit to be one power of 2 less than the dsDataSegment length

			int[] data = new int[smallDataSegmentLimit]; // data values in the time domain
			Cmplx[] noise_spectra = new Cmplx[smallDataSegmentLimit]; // array w/ current segment FFT

			double[] finalNoiseSpectraData = new double[(smallDataSegmentLimit / 2) + 1];
			// array containing the cumulative sum of each segment's FFT
			// this is an array of doubles because doing the arithmetic w/ complex data causes noise

			// loop indexes
			int dsDataSegmentLimit = dsDataSegment;
			// keeps track of where a segment ends in the data array
			int cnt = 0; // keeps track where in the intData array the index is
			int segIndex = 0; // keeps track of where the index is within an individual segment

			// Perform windowing and compute the FFT of each segment. The
			// finalNoiseSpectraData array contains the sum of the FFTs for all
			// segments.
			int numsegs = 1;
			while (cnt < intData.length) {

				if (cnt < dsDataSegmentLimit) {
					if (segIndex < smallDataSegmentLimit)
						data[segIndex] = intData[cnt];
					cnt++;
					segIndex++;
				} else {
					if (filter != null) {
						data = new FilterFacade(filter, channel).filter(data);
					}

					// Make a copy of data to make it an array of doubles
					double[] dataCopy = new double[data.length];
					for (int i = 0; i < data.length; i++)
						dataCopy[i] = data[i];

					// Norm the data: remove mean
					dataCopy = IstiUtilsMath.normData(dataCopy);

					// Apply Hanning window
					dataCopy = IstiUtilsMath.windowHanning(dataCopy);

					// Calculate FFT of the current segment
					noise_spectra = IstiUtilsMath.processFft(dataCopy);

					// Compute a running total of the FFTs for all segments
					for (int i = 0; i < noise_spectra.length; i++) {
						finalNoiseSpectraData[i] += noise_spectra[i].mag();
					}

					// move cursors
					segIndex = 0;
					if (cnt + smallDataSegmentLimit > intData.length) // correction for last segment
					{
						cnt = intData.length - smallDataSegmentLimit;
						dsDataSegmentLimit = intData.length;
					} else {
						cnt = cnt - ((smallDataSegmentLimit * 3) / 4); // move window backwards 75%
						dsDataSegmentLimit = dsDataSegmentLimit + (smallDataSegmentLimit / 4);
						// increase new dsDataSegmentLimit by 25%
						numsegs++;
					}

				}

			}

			// average each bin by dividing by the number of segments
			for (int i = 0; i < finalNoiseSpectraData.length; i++) {
				finalNoiseSpectraData[i] /= numsegs;
				// square the PSD
				finalNoiseSpectraData[i] = Math.pow(finalNoiseSpectraData[i], 2);
			}

			// Note that channel.getSampleRate() really returns the sampling
			// interval. (e.g. For a sample frequency of 40Hz you have
			// 1000.0/channel.getSampleRate() = 1000.0/25 = 40Hz)
			final Response.FreqParameters fp = Response.getFreqParameters(smallDataSegmentLimit,
					1000.0 / channel.getSampleRate());
			final double[] frequenciesArray = RespUtils
					.generateFreqArray(fp.startFreq, fp.endFreq, fp.numFreq, false);

			Cmplx[] response;
			try {
				response = channel.getResponse().getResp(ti.getStartTime(), fp.startFreq, fp.endFreq,
						Math.max(finalNoiseSpectraData.length, fp.numFreq));
			} catch (TraceViewException e) {
				logger.error("Caught exception while trying to get response data: ", e);
				throw new RuntimeException(e.getMessage());
			}

			if (response != null) {
				XYSeries xys = new XYSeries(channel.getName());
				for (int i = 0; i < frequenciesArray.length; ++i) {
					// convert resp to units of acceleration and then deconvolve from spectral data
					Cmplx resp = response[i];
					Cmplx scaleFactor = new Cmplx(0., -1 / (2. * Math.PI * frequenciesArray[i]));
					resp = Cmplx.mul(resp, scaleFactor);
					resp = Cmplx.mul(resp, resp.conjg());

					if (resp.mag() == 0) {
						resp = new Cmplx(Double.MIN_VALUE, 0.);
					}

					xys.add(1./ frequenciesArray[i],
							20 * Math.log10(finalNoiseSpectraData[i] / resp.mag()));
				}
				dataset.add(xys);
			} else {
				respNotFound.append(", ");
				respNotFound.append(channel.getName());
			}
		});
		long endl = System.nanoTime() - startl;
		double duration = endl * Math.pow(10, -9);
		logger.info("\nPSD calculation duration = " + duration + " sec");

		if (input.size() == 0) {
			throw new XMAXException("Cannot find responses for any channels");
		} else {
			if (respNotFound.length() > 0) {
				JOptionPane.showMessageDialog(parentFrame, "Can not find responses for channels: " +
								respNotFound.toString(),
						"Warning", JOptionPane.WARNING_MESSAGE);
			}
		}

		return dataset;
	}


	@Override
	public String getName() {
		return TransPSD.NAME;
	}

}
