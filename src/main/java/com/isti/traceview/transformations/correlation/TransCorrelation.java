package com.isti.traceview.transformations.correlation;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.transformations.ITransformation;
import com.isti.traceview.transformations.TransformationUtils;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;

/**
 * Correlation transformation. It only prepares data, correlation itself and
 * hanning window applying performs in {@link ViewCorrelation}
 * 
 * @author Max Kokoulin
 */
public class TransCorrelation implements ITransformation {

	private static final Logger logger = Logger.getLogger(TransCorrelation.class);

	public static final String NAME = "Correlation";


	@Override
	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration,
			JFrame parentFrame) {
		if ((input == null) || (input.size() == 0) || (input.size() > 2)) {
			JOptionPane.showMessageDialog(parentFrame,
					"You should select two channels to view correlation\nor one channel to view autocorrelation",
					"Error", JOptionPane.ERROR_MESSAGE);
		} else {
			try {
				List<String> channelNames = new ArrayList<>();
				for (PlotDataProvider channel : input) {
					channelNames.add(channel.getName());
				}

				// sample rate is interval in ms -- larger sample rate is the lower-frequency data
				// and if they don't match up we should downsample to the lower frequency rate

				double sampleRate = input.get(0).getSampleRate();
				if (input.size() == 2) {
					sampleRate = Math.max(input.get(0).getSampleRate(), input.get(1).getSampleRate());
				}

				@SuppressWarnings("unused")
				ViewCorrelation vc = new ViewCorrelation(parentFrame, createData(input, filter, ti, sampleRate), channelNames,
						sampleRate, ti);
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
	 * @param sampleRate
	 * 						Sampling interval of data (in milliseconds)
	 * @return list of arrays - double raw data for selected traces and time
	 *         ranges
	 * @throws XMAXException
	 *             if sample rates differ, gaps in the data, no data, or the
	 *             data is too long.
	 */
	private List<double[]> createData(List<PlotDataProvider> input, IFilter filter, TimeInterval ti,
			double sampleRate)
			throws XMAXException {
		List<double[]> ret = new ArrayList<>();
		PlotDataProvider channel1 = input.get(0);
		List<Segment> segments1; 
		if(channel1.getRotation() != null)
			segments1 = channel1.getRawData(ti);
		else {
			segments1 = channel1.getRawData(channel1.getRotation(), ti);
		}
		int[] intData1 = new int[0];
		if (segments1.size() > 0) {
			long segment_end_time = 0;
			double firstSampleRate = segments1.get(0).getSampleRate();
			for (Segment segment : segments1) {
				if (segment.getSampleRate() != firstSampleRate) {
					throw new XMAXException(
							"You have data with different sample rate for channel " + channel1.getName());
				}
				if (segment_end_time != 0
						&& Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), sampleRate)) {
					throw new XMAXException("You have gap in the data for channel " + channel1.getName());
				}
				segment_end_time = segment.getEndTime().getTime();
				intData1 = IstiUtilsMath.padArray(intData1, segment.getData(ti).data);
			}
			if (firstSampleRate < sampleRate) {
				intData1 =
						TransformationUtils.decimate(intData1, (long) firstSampleRate, (long) sampleRate);
			}

		} else {
			throw new XMAXException("You have no data for channel " + channel1.getName());
		}

		logger.debug("size = " + intData1.length);
		if (filter != null) {
			intData1 = new FilterFacade(filter, channel1).filter(intData1);
		}
		double[] dblData1 = IstiUtilsMath.normData(intData1);
		if (dblData1.length > maxDataLength) {
			throw new XMAXException("Too many datapoints are selected.");
		}
		/*
		 * if(dblData1.length%2 == 1){ dblData1 = Arrays.copyOf(dblData1,
		 * dblData1.length-1); }
		 */
		ret.add(dblData1);
		if (input.size() == 2) {
			PlotDataProvider channel2 = input.get(1);
			List<Segment> segments2; 
			if(channel1.getRotation() != null)
				segments2 = channel2.getRawData(ti);
			else {
				segments2 = channel2.getRawData(channel2.getRotation(), ti);
			}
			int[] intData2 = new int[0];
			if (segments2.size() > 0) {
				long segment_end_time = 0;
				for (Segment segment : segments2) {
					if (segment.getSampleRate() != sampleRate) {
						throw new XMAXException("Channels " + channel1.getName() + " and " + channel2.getName()
								+ " have different sample rates: " + sampleRate + " and " + segment.getSampleRate());
					}
					if (segment_end_time != 0
							&& Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), sampleRate)) {
						throw new XMAXException("You have gap in the data for channel " + channel2.getName());
					}
					segment_end_time = segment.getEndTime().getTime();
					intData2 = IstiUtilsMath.padArray(intData2, segment.getData(ti).data);
				}
				if (segments2.get(0).getSampleRate() < sampleRate) {
					intData2 =
							TransformationUtils.decimate(intData2,
									(long) segments2.get(0).getSampleRate(), (long) sampleRate);
				}

			} else {
				throw new XMAXException("You have no data for channel " + channel1.getName());
			}

			if (filter != null) {
				intData2 = new FilterFacade(filter, channel2).filter(intData2);
			}
			double[] dblData2 = IstiUtilsMath.normData(intData2);
			if (dblData2.length > maxDataLength) {
				throw new XMAXException(" Too many datapoints are selected");
			}
			/*
			 * if(dblData2.length%2 == 1){ dblData2 = Arrays.copyOf(dblData2,
			 * dblData2.length-1); }
			 */
			ret.add(dblData2);
		}
		return ret;
	}

	@Override
	public String getName() {
		return TransCorrelation.NAME;
	}
}
