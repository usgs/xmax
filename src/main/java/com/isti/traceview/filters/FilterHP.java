package com.isti.traceview.filters;

import java.util.function.Function;
import uk.me.berndporr.iirj.Butterworth;

/**
 * High-pass Butterworth filter Algorithm is from Stearns, 1975
 */

public class FilterHP extends AbstractFilter {

	public static final String DESCRIPTION = "Apply High Pass filter for selected channels";
	public static final String NAME = "HP";

	/**
	 * number of filter sections
	 */
	private final int order;
	private final double cutFrequency;


	/**
	 * @param order
	 *            int number of sections (two poles per section)
	 * @param cutFrequency
	 *            double cutoff frequency in Hz
	 */
	public FilterHP(int order, double cutFrequency) {
		this.order = order;
		this.cutFrequency = cutFrequency;
	}

	/**
	 * Default constructor
	 */
	public FilterHP() {
		this(4, 1);
	}

	@Override
	public String getName() {
		return FilterHP.NAME;
	}

	@Override
	public Function<double[], double[]> getFilterFunction() {
		return Filter.HIGHPASS.getFilter(sampleRate, false, order, cutFrequency, null);
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
		if (o instanceof FilterHP) {
			FilterHP arg = (FilterHP) o;
			return (order == arg.getOrder()) && (cutFrequency == arg.getCutFrequency());
		}
		return false;
	}
}
