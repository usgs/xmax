package com.isti.traceview.processing;

import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.filters.IFilter;
import java.util.function.Function;
import org.apache.log4j.Logger;

/**
 * Facade to simplify filter operation
 * 
 * @author Max Kokoulin
 */
public class FilterFacade {
	private static final Logger logger = Logger.getLogger(FilterFacade.class);
	private final Function<double[], double[]> filter;

	/**
	 * @param filter
	 *            Used filter
	 * @param channel
	 *            Filtered PlotDataProvider
	 */
	public FilterFacade(IFilter filter, RawDataProvider channel) {
		filter.init(channel);
		this.filter = filter.getFilterFunction();
	}

	/**
	 * Method to filter array of data
	 */
	public int[] filter(int[] data) {
		double[] toFilt = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			toFilt[i] = data[i];
		}
		toFilt = filter.apply(toFilt);
		for (int i = 0; i < data.length; i++) {
			data[i] = (int) toFilt[i];
		}
		return data;
	}
}
