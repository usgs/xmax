package com.isti.traceview.transformations.spectra;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Spectra;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;

/**
 * Spectra transformation. Prepares data for presentation in {@link ViewSpectra}
 * 
 * @author Max Kokoulin
 */
public class TransSpectra implements ITransformation {
	private static final Logger logger = Logger.getLogger(TransSpectra.class);
	private static final boolean verboseDebug = false;
	public static final String NAME = "Spectra";

	@Override
	public void transform(List<PlotDataProvider> input, TimeInterval timeInterval, IFilter filter, Object configuration,
			JFrame parentFrame) {
		if (input.size() == 0) {
			JOptionPane.showMessageDialog(parentFrame, "Please select channels", "Spectra computation warning",
					JOptionPane.WARNING_MESSAGE);
		} else {
			try {
				@SuppressWarnings("unused")
				ViewSpectra vs = new ViewSpectra(parentFrame, createData(input, filter, timeInterval, parentFrame),
						timeInterval);
			} catch (XMAXException e) {
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
	 *            Filter applied to traces before processing spectra
	 * @param timeInterval
	 *            Time interval to define processed range
	 * @param parentFrame
	 *            parent frame
	 * @return list of spectra for selected traces and time ranges
	 * @throws XMAXException
	 *             if sample rates differ, gaps in the data, or no data for a
	 *             channel
	 */
	private List<Spectra> createData(List<PlotDataProvider> input, IFilter filter, TimeInterval timeInterval,
			JFrame parentFrame) throws XMAXException {
		List<Spectra> dataset = new ArrayList<>();
		for (PlotDataProvider channel : input) {
			double sampleRate = 0;
			List<Segment> segments = channel.getRawData(timeInterval);
			int[] intData = new int[0];
			if (segments.size() > 0) {
				long segment_end_time = 0;
				sampleRate = segments.get(0).getSampleRate();
				for (Segment segment : segments) {
					if (segment.getSampleRate() != sampleRate) {
						throw new XMAXException(
								"You have data with different sample rate for channel " + channel.getName());
					}
					if (segment_end_time != 0
							&& Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), sampleRate)) {
						throw new XMAXException("You have gap in the data for channel " + channel.getName());
					}
					segment_end_time = segment.getEndTime().getTime();
					intData = IstiUtilsMath.padArray(intData, segment.getData(timeInterval).data);
				}

			} else {
				throw new XMAXException("You have no data for channel " + channel.getName());
			}
			int dataSize;
			if (intData.length > maxDataLength) {
				dataSize = maxDataLength; // maxDataLength is set to be 2^30, a power of two
				((XMAXframe) parentFrame).getStatusBar().setMessage(
						"Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
			} else {
				dataSize = (int) Math.pow(2, (int) IstiUtilsMath.log2(intData.length));
			}

			logger.debug("data size = " + dataSize);
			int[] data = new int[dataSize];
			for (int i = 0; i < dataSize; i++) {
				data[i] = intData[i];
			}
			if (filter != null) {
				data = new FilterFacade(filter, channel).filter(data);
			}
			try {
				Spectra spectra = IstiUtilsMath.getNoiseSpectra(data, channel.getResponse(),
						timeInterval.getStartTime(), channel, verboseDebug);
				dataset.add(spectra);
			} catch (TraceViewException e) {
				logger.error("TraceViewException:", e);
			}
		}
		return dataset;
	}

	@Override
	public String getName() {
		return TransSpectra.NAME;
	}
}
