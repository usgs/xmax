package com.isti.traceview.filters;

import asl.utils.FilterUtils;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.LPFilterException;
import org.apache.log4j.Logger;

/**
 * Low-pass Butterworth filter Algorithm is from Stearns, 1975
 */

public class FilterLP implements IFilter {
	public static final String DESCRIPTION = "Apply Low Pass filter for selected channels";
	public static final String NAME = "LP";

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(FilterLP.class); // @jve:decl-index=0:

	/**
	 * number of filter sections
	 */
	int order = 0;
	double cutFrequency = Double.NaN;
	double sampleRate = 0.;

	public int getMaxDataLength() {
		return Integer.MAX_VALUE;
	}

	/**
	 * @param order
	 *            int number of sections (two poles per section)
	 * @param cutFrequency
	 *            double cutoff frequency in Hz
	 */
	public FilterLP(int order, double cutFrequency) {
		this.order = order;
		this.cutFrequency = cutFrequency;
	}

	/**
	 * Default constructor
	 */
	public FilterLP() {
		this(4, 0.05);
	}

	@Override
	public String getName() {
		return FilterLP.NAME;
	}

	/**
	 * design routine
	 * 
	 * @param channel
	 *            trace to retrieve information
	 */
	synchronized public void init(RawDataProvider channel) {
		sampleRate = 1000.0 / channel.getSampleRate();
	}

	/**
	 * Performs low-pass Butterworth filtering of a time series.
	 * 
	 * @param data
	 *            = data array
	 * @param length
	 *            = number of samples in data array
	 * @return filtered data array
	 */
	synchronized public double[] filter(double[] data, int length) throws LPFilterException {
		if (data.length > length)
			throw new LPFilterException("Requested filtering length exceeds provided array length");

		return FilterUtils.lowPassFilter(data, sampleRate, cutFrequency, order);
	}

	public boolean needProcessing() {
		return true;
	}

	public int getOrder() {
		return order;
	}

	public double getCutFrequency() {
		return cutFrequency;
	}

	public boolean equals(Object o) {
		if (o instanceof FilterLP) {
			FilterLP arg = (FilterLP) o;
			if ((order == arg.getOrder()) && (cutFrequency == arg.getCutFrequency())) {
				return true;
			}
		}
		return false;
	}
}
