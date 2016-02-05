package com.asl.traceview.transformations.coherence;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.isti.jevalresp.RespUtils;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Response;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.transformations.ITransformation;
import com.asl.traceview.transformations.coherence.ViewCoherence;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;

import edu.sc.seis.fissuresUtil.freq.Cmplx;

public class TransCoherence implements ITransformation{

	public static final String NAME = "Coherence";
	
	private int maxDataLength = 1048576;
	private int effectiveLength = 0;
	
	@Override
	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration,
			JFrame parentFrame) {
		if (input.size() != 2) {
			JOptionPane.showMessageDialog(parentFrame, "Please select 2 channels", "Coherence computation warning",
					JOptionPane.WARNING_MESSAGE);
		}
		else if (input.get(0).getSampleRate() != input.get(1).getSampleRate()){
			JOptionPane.showMessageDialog(parentFrame, "Channel sample rates do not match. ("+input.get(0).getLocationName()+"/"
					+input.get(0).getChannelName()+"= "+input.get(0).getSampleRate()+", " +input.get(1).getLocationName()+"/"
					+input.get(1).getChannelName()+"= "+input.get(1).getSampleRate()+")",
					"Coherence computation warning",
					JOptionPane.WARNING_MESSAGE);
		}
		else {
			try {
				XYSeriesCollection plotSeries = createData(input, filter, ti, parentFrame);
				TimeInterval effectiveInterval = new TimeInterval(ti.getStart(),
						ti.getStart() + new Double(input.get(0).getSampleRate() * effectiveLength).longValue());
				@SuppressWarnings("unused")				
				ViewCoherence vc = new ViewCoherence(parentFrame, plotSeries, effectiveInterval);
			} catch (XMAXException e) {
				if (!e.getMessage().equals("Operation cancelled")) {
					JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
				}
			} catch (TraceViewException e) {
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

	private XYSeriesCollection createData(List<PlotDataProvider> input, IFilter filter, TimeInterval ti, JFrame parentFrame) 
			throws TraceViewException, XMAXException {
		XYSeriesCollection dataset = new XYSeriesCollection();
		ListIterator<PlotDataProvider> li = input.listIterator();
		List<Cmplx[]> traceArray = new ArrayList<Cmplx[]>();
		while (li.hasNext()) {
			PlotDataProvider channel = li.next();
			List<Segment> segments = channel.getRawData(ti);
			double samplerate;
			long segment_end_time = 0;
			int[] intData = new int[0];
			if (segments.size() > 0) {
				samplerate = segments.get(0).getSampleRate();
				for (Segment segment : segments) {
					if (segment.getSampleRate() != samplerate) {
						throw new XMAXException(
								"You have data with different sample rate for channel " + channel.getName());
					}
					if (segment_end_time != 0
							&& Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), samplerate)) {
						throw new XMAXException("You have gap in the data for channel " + channel.getName());
					}
					segment_end_time = segment.getEndTime().getTime();
					intData = IstiUtilsMath.padArray(intData, segment.getData(ti).data);
				}

			} else {
				throw new XMAXException("You have no data for channel " + channel.getName());
			}
			if (intData.length > maxDataLength) {
				int ds = getPower2Length(maxDataLength);
				int[] tempIntData = new int[ds];
				for (int i = 0; i < maxDataLength; i++)
					tempIntData[i] = intData[i];
				intData = tempIntData;
				((XMAXframe) parentFrame).getStatusBar().setMessage(
						"Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
			}
		
			/*
			 * Here we compute the power spectral density of the selected data
			 * using the Welch method with 13 windows 75% overlap. The actual
			 * PSD is calculated in the getPSD function within Spectra.java.
			 */
			int dsDataSegment = new Double(Math.round(intData.length / 4.0)).intValue(); // length
																							// of
																							// each
																							// segment
																							// for
																							// 13
																							// segments
																							// 75%
																							// overlap
			int smallDataSegmentLimit = new Double(
					Math.ceil(Math.pow(2, (new Double(Math.ceil(IstiUtilsMath.log2(dsDataSegment)) - 1))))).intValue(); // set
																														// smallDataSegment
																														// limit
																														// to
																														// be
																														// one
																														// power
																														// of
																														// 2
																														// less
																														// than
																														// the
																														// dsDataSegment
																														// length

			int[] data = new int[smallDataSegmentLimit]; // array containing
															// data values in
															// the time domain
			Cmplx[] noise_spectra = new Cmplx[smallDataSegmentLimit]; // array
																		// containing
																		// the
																		// fft
																		// of
																		// the
																		// current
																		// segment
			Cmplx[] finalNoiseSpectraData = new Cmplx[(smallDataSegmentLimit / 2) + 1]; // array
																						// containing
																						// the
																						// cumulative
																						// sum
																						// of
																						// the
																						// each
																						// segments
																						// fft.

			// initialize the finalNoiseSpectraData array to all zeros since we
			// will be taking a cumulative sum of the data.
			for (int i = 0; i < finalNoiseSpectraData.length; i++) {
				finalNoiseSpectraData[i] = new Cmplx(0, 0);
			}

			// loop indexes
			int dsDataSegmentLimit = dsDataSegment; // keeps track of where a
													// segment ends in the data
													// array
			int cnt = 0; // keeps track where in the intData array the index is
			int segIndex = 0; // keeps track of where the index is within an
								// individual segment

			int ds;
			if (intData.length > maxDataLength) {
				ds = getPower2Length(maxDataLength);
				int[] tempIntData = new int[ds];
				for (int i = 0; i < maxDataLength; i++)
					tempIntData[i] = intData[i];
				intData = tempIntData;
				((XMAXframe) parentFrame).getStatusBar().setMessage(
						"Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
			} else {
				ds = intData.length;
			}
			if (ds > effectiveLength) {
				effectiveLength = ds;
			}
			
			// Perform windowing and compute the FFT of each segment. The
			// finalNoiseSpectraData array contains the sum of the FFTs for all
			// segments.
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
						finalNoiseSpectraData[i] = Cmplx.add(finalNoiseSpectraData[i], noise_spectra[i]);
					}

					// move cursors
					segIndex = 0;
					if (cnt + smallDataSegmentLimit > intData.length) // correction
																		// for
																		// last
																		// segment
					{
						cnt = intData.length - smallDataSegmentLimit;
						dsDataSegmentLimit = intData.length;
					} else {
						cnt = cnt - ((dsDataSegment * 3) / 4); // move window
																// backwards 75%
						dsDataSegmentLimit = dsDataSegmentLimit + (dsDataSegment / 4); // increase
																						// new
																						// dsDataSegmentLimit
																						// by
																						// 25%
					}

				}

			}

			// average each bin by dividing by the number of segments
			for (int i = 0; i < finalNoiseSpectraData.length; i++) {
				finalNoiseSpectraData[i] = Cmplx.div(finalNoiseSpectraData[i], 13.0);
			}

			
			traceArray.add(finalNoiseSpectraData);
			
			if(traceArray.size() > 1){
				final double[] finalCoherence; 
				double[] coherenceTrace = new double[traceArray.get(0).length];
				for(int i = 0; i < traceArray.get(0).length; i++){
					//Calcluate |Pxy|^2
					Cmplx x = traceArray.get(0)[i];
					Cmplx y = traceArray.get(1)[i];
					double numerator = Math.pow(Cmplx.mul(x, y.conjg()).real(), 2);
					//Calculate |Pxx| * |Pyy|
					double denominator = Cmplx.mul(x, x.conjg()).real() * Cmplx.mul(y, y.conjg()).real();
					coherenceTrace[i] = numerator / denominator; //normalized coherence value
				}
				finalCoherence = coherenceTrace; 

				// Note that channel.getSampleRate() really returns the sampling
				// interval. (e.g. For a sample frequency of 40Hz you have
				// 1000.0/channel.getSampleRate() = 1000.0/25 = 40Hz)
				final Response.FreqParameters fp = Response.getFreqParameters(finalCoherence.length*2,
						1000.0 / channel.getSampleRate());
				final double[] frequenciesArray = RespUtils.generateFreqArray(fp.startFreq, fp.endFreq, fp.numFreq, false);

				XYSeries series = new XYSeries("raw series");
				
				for(int i = 0; i < finalCoherence.length; i++){
					series.add(1.0 / frequenciesArray[i], Math.sqrt(finalCoherence[i]));
				}
				
				dataset.addSeries(series);
			}
		}
		
		
		return dataset;
	}

	private static int getPower2Length(int length) {
		return new Double(Math.pow(2, new Double(Math.ceil(IstiUtilsMath.log2(length))))).intValue();
	}

	/**
	 * Sets maximum amount of processed data
	 */
	public void setMaxDataLength(int dataLength) {
		
	}

	/**
	 * Return name of transformation
	 */
	public String getName() {
		return TransCoherence.NAME;
	}
	
}