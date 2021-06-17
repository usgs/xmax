package com.isti.traceview.transformations.correlation;

import asl.utils.NumericUtils;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
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
					"You should select two channels to view correlation\n"
							+ "or one channel to view autocorrelation",
					"Error", JOptionPane.ERROR_MESSAGE);
		} else {
			try {
				List<String> channelNames = new ArrayList<>();
				for (PlotDataProvider channel : input) {
					channelNames.add(channel.getName());
				}

				// sample rate is interval in ms -- larger sample rate is the lower-frequency data
				// and if they don't match up we should downsample to the lower frequency rate

				double sampleRate = input.get(0).getSampleInterval();
				if (input.size() == 2) {
					sampleRate = Math.max(input.get(0).getSampleInterval(), input.get(1).getSampleInterval());
				}

				@SuppressWarnings("unused")
				ViewCorrelation vc =
						new ViewCorrelation(parentFrame, createData(input, filter, ti, sampleRate),
								channelNames, sampleRate, ti);
			} catch (RuntimeException e) {
			  logger.error(e);
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
	 */
	private List<double[]> createData(List<PlotDataProvider> input, IFilter filter, TimeInterval ti,
			double sampleRate) {
		List<double[]> ret = new ArrayList<>();

		input.parallelStream().forEachOrdered(trace -> {
			try {
				int[] intData = trace.getContinuousGaplessDataOverRange(ti);
				logger.debug("size = " + intData.length);
				if (filter != null) {
					intData = new FilterFacade(filter, trace).filter(intData);
				}
				double[] dblData = IstiUtilsMath.normData(intData);
				dblData =
            NumericUtils.decimate(dblData, (long) trace.getSampleInterval(), (long) sampleRate);
				if (dblData.length > maxDataLength) {
					throw new XMAXException("Too many datapoints are selected.");
				}
				ret.add(dblData);
			} catch (XMAXException e) {
				logger.error("Caught exception while iterating through transformation: ", e);
				throw new RuntimeException(e.getMessage());
			}
		});

		return ret;
	}

	@Override
	public String getName() {
		return TransCorrelation.NAME;
	}
}
