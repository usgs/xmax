package com.isti.traceview.filters;

import org.apache.log4j.Logger;
import uk.me.berndporr.iirj.Butterworth;

/**
 * Low-pass Butterworth filter Algorithm is from Stearns, 1975
 */

public class FilterLP extends AbstractFilter {
	public static final String DESCRIPTION = "Apply Low Pass filter for selected channels";
	public static final String NAME = "LP";

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(FilterLP.class); // @jve:decl-index=0:

	private final double cutFrequency;
	private final int order;

	/**
	 * @param order
	 *            int number of sections (two poles per section)
	 * @param cutFrequency
	 *            double cutoff frequency in Hz
	 */
	public FilterLP(int order, double cutFrequency) {
		this.order = order;
		this.cutFrequency = cutFrequency;
		reinitializeFilter();
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

	@Override
	public void reinitializeFilter() {
		casc = new Butterworth();
		casc.lowPass(order, sampleRate, cutFrequency);
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
