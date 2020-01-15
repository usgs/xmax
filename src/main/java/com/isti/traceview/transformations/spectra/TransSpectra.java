package com.isti.traceview.transformations.spectra;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
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
			} catch (RuntimeException e) {
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
			JFrame parentFrame) {
		List<Spectra> dataset = new ArrayList<>();
		input.forEach(channel -> {
			int[] intData;
			try {
				intData = channel.getContinuousGaplessDataOverRange(timeInterval);
			} catch (XMAXException e) {
				logger.error("Caught exception while iterating through transformation: ", e);
				throw new RuntimeException(e);
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
			System.arraycopy(intData, 0, data, 0, dataSize);
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
		});
		return dataset;
	}

	@Override
	public String getName() {
		return TransSpectra.NAME;
	}
}
