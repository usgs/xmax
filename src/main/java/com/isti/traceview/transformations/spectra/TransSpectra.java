package com.isti.traceview.transformations.spectra;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

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

/**
 * Spectra transformation. Prepares data for presentation in {@link ViewSpectra}
 * 
 * @author Max Kokoulin
 */
public class TransSpectra implements ITransformation {
	private static final Logger logger = Logger.getLogger(TransSpectra.class);
	private static final boolean verboseDebug = false;
	public static final String NAME = "Spectra";
	

	public int maxDataLength = 32768;

	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration,
			JFrame parentFrame) {
		if (input.size() == 0) {
			JOptionPane.showMessageDialog(parentFrame, "Please select channels", "Spectra computation warning",
					JOptionPane.WARNING_MESSAGE);
		} else {
			try {
				@SuppressWarnings("unused")
				ViewSpectra vs = new ViewSpectra(parentFrame, createData(input, filter, ti, parentFrame), ti);
			} catch (XMAXException e) {
				if (!e.getMessage().equals("Operation cancelled")) {
					JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
				}
			}
		}
		((XMAXframe) parentFrame).getGraphPanel().forceRepaint();
	}

	public void setMaxDataLength(int dataLength) {
		this.maxDataLength = dataLength;
	}

	/**
	 * @param input
	 *            List of traces to process
	 * @param filter
	 *            Filter applied to traces before processing spectra
	 * @param ti
	 *            Time interval to define processed range
	 * @param parentFrame
	 *            parent frame
	 * @return list of spectra for selected traces and time ranges
	 * @throws XMAXException
	 *             if sample rates differ, gaps in the data, or no data for a
	 *             channel
	 */
	private List<Spectra> createData(List<PlotDataProvider> input, IFilter filter, TimeInterval ti, JFrame parentFrame)
			throws XMAXException {
		List<Spectra> dataset = new ArrayList<Spectra>();
		for (PlotDataProvider channel : input) {
			double sampleRate = 0;
			List<Segment> segments = channel.getRawData(ti);
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
					intData = IstiUtilsMath.padArray(intData, segment.getData(ti).data);
				}

			} else {
				throw new XMAXException("You have no data for channel " + channel.getName());
			}
			int ds;
			if (intData.length > maxDataLength) {
				ds = new Double(Math.pow(2, new Double(IstiUtilsMath.log2(maxDataLength)).intValue())).intValue();
				((XMAXframe) parentFrame).getStatusBar().setMessage(
						"Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
			} else {
				ds = new Double(Math.pow(2, new Double(IstiUtilsMath.log2(intData.length)).intValue())).intValue();
			}
			/*
			 * // this code shows pop-up if point count is exceeded int ds = new
			 * Double(Math.pow(2, new
			 * Double(IstiUtilsMath.log2(intData.length)).intValue())).intValue(
			 * ); if (ds > maxDataLength && userAnswer == -1) { Object[] options
			 * = { "Proceed with ALL points", "Proceed with first " +
			 * maxDataLength + " points", "Cancel" }; userAnswer =
			 * JOptionPane.showOptionDialog(parentFrame,
			 * "Too many points. Computation could be slow.", "Too many points",
			 * JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
			 * null, options, options[1]); } if (userAnswer != -1) { if
			 * (userAnswer == JOptionPane.NO_OPTION) { if (ds > maxDataLength) {
			 * ds = new Double(Math.pow(2, new
			 * Double(IstiUtilsMath.log2(maxDataLength)).intValue())).intValue()
			 * ; } } else if (userAnswer == JOptionPane.CANCEL_OPTION) { throw
			 * new XMAXException("Operation cancelled"); } }
			 */
			logger.debug("data size = " + ds);
			int[] data = new int[ds];
			for (int i = 0; i < ds; i++) {
				data[i] = intData[i];
			}
			if (filter != null) {
				data = new FilterFacade(filter, channel).filter(data);
			}
			try {
				Spectra spectra = IstiUtilsMath.getNoiseSpectra(data, channel.getResponse(), ti.getStartTime(), channel,
						verboseDebug);
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
