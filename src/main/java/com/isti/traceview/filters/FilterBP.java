package com.isti.traceview.filters;

import asl.utils.FilterUtils;
import asl.utils.TimeSeriesUtils;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.BPFilterException;

/**
 * <p>
 * Band-pass Butterworth filter Algorithm is from Stearns, 1975
 * </p>
 * <p>
 * The digital filter has ns filter sections in the cascade. The k'th section
 * has the transfer function with the transfer function:
 * </p>
 * <p>
 * h(z) = (a(k)*(z**4-2*z**2+1))/(z**4+b(k)*z**3+c(k)*z**2+d(k)*z+e(k))
 * </p>
 * <p>
 * Thus, if f(m) and g(m) are the input and output at time m*t, then
 * </p>
 * <p>
 * g(m) =
 * a(k)*(f(m)-2*f(m-2)+f(m-4))-b(k)*g(m-1)-c(k)*g(m-2)-d(k)*g(m-3)-e(k)*g(m-4)
 * </p>
 */

public class FilterBP implements IFilter {

	public static final String DESCRIPTION = "Apply Band Pass filter for selected channels";
	public static final String NAME = "BP";

	int order = 0;
	double cutLowFrequency = Double.NaN;
	double cutHighFrequency = Double.NaN;
	double sampleRate = 0.;

	@Override
	public String getName() {
		return NAME;
	}

	public int getMaxDataLength() {
		return Integer.MAX_VALUE;
	}

	/**
	 * @param order
	 *            int number of sections (each section = 4 poles: 2 low freq
	 *            poles and 2 hi freq poles)
	 * @param cutLowFrequency
	 *            double cutoff (3-db) frequency in Hz
	 * @param cutHighFrequency
	 *            double cutoff (3-db) frequency in Hz
	 */
	public FilterBP(int order, double cutLowFrequency, double cutHighFrequency) {
		this.order = order;
		this.cutLowFrequency = cutLowFrequency;
		this.cutHighFrequency = cutHighFrequency;
	}

	/**
	 * Default constructor
	 */
	public FilterBP() {
		this(4, 0.1, 0.5);
	}

	/**
	 * Bandpass butterworth digital filter design subroutine
	 * 
	 * @param channel
	 *            trace to retrieve information
	 */
	synchronized public void init(RawDataProvider channel) {
		sampleRate = 1000.0 / channel.getSampleRate();
	}

	/**
	 * Performs band-pass Butterworth filtering of a time series.
	 * 
	 * @param data
	 *            = data array
	 * @param length
	 *            = number of samples to filter
	 * @return filtered data array
	 */
	synchronized public double[] filter(double[] data, int length) throws BPFilterException {
		if (data.length > length)
			throw new BPFilterException("Requested filtering length exceeds provided array length");

		TimeSeriesUtils.demeanInPlace(data);
		return FilterUtils.bandFilter(data, sampleRate, cutLowFrequency, cutHighFrequency, order);
	}

	public boolean needProcessing() {
		return true;
	}

	public int getOrder() {
		return order;
	}

	public double getCutLowFrequency() {
		return cutLowFrequency;
	}

	public double getCutHighFrequency() {
		return cutHighFrequency;
	}

	public boolean equals(Object o) {
		if (o instanceof FilterBP) {
			FilterBP arg = (FilterBP) o;
			if ((order == arg.getOrder()) && (cutLowFrequency == arg.getCutLowFrequency())
					&& (cutHighFrequency == arg.getCutHighFrequency())) {
				return true;
			}
		}
		return false;
	}
}
