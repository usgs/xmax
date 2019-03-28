package com.isti.traceview.processing;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import org.apache.log4j.Logger;

/**
 * Facade to simplify filter operation
 * 
 * @author Max Kokoulin
 */
public class FilterFacade {
	private static final Logger logger = Logger.getLogger(FilterFacade.class);

	private IFilter filter;

	/**
	 * @param filter
	 *            Used filter
	 * @param channel
	 *            Filtered PlotDataProvider
	 */
	public FilterFacade(IFilter filter, RawDataProvider channel) {
		this.filter = filter;
		filter.init(channel);
	}

	/**
	 * Method to filter segment
	 * 
	 * @param segment
	 *            Segment to filter
	 * @param ti
	 *            Time interval to process
	 * @return filtered segment
	 */
	public Segment filter(Segment segment, TimeInterval ti) {
		Segment clone = null;
		try {
			clone = (Segment) segment.clone();
			int[] data = null;
			if (ti == null) {
				data = clone.getData().data;
			} else {
				data = clone.getData(ti).data;
			}
			data = filter(data);
		} catch (CloneNotSupportedException e) {
			logger.error("Can't filter segment: " + e);
			return segment;
		}
		return clone;
	}

	/**
	 * Method to filter array of data
	 */
	public int[] filter(int[] data) {
		double[] toFilt = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			toFilt[i] = (double) data[i];
		}
		try {
			toFilt = filter.filter(toFilt, toFilt.length);
			for (int i = 0; i < data.length; i++) {
				data[i] = new Double(toFilt[i]).intValue();
			}
		} catch (TraceViewException e) {
			logger.error("Can't filter data: ", e);
		} catch (BPFilterException e) {
			logger.error("BPFilterException:", e);
		} catch (HPFilterException e) {
			logger.error("HPFilterException:", e);
		} catch (LPFilterException e) {
			logger.error("LPFilterException:", e);
		}
		return data;
	}

	/**
	 * Method to filter whole segment
	 */
	public Segment filter(Segment segment) {
		return filter(segment, null);
	}
}
