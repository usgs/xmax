package com.isti.traceview.filters;

import java.util.function.Function;
import uk.me.berndporr.iirj.Butterworth;

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

public class FilterBP extends AbstractFilter {

	public static final String NAME = "BP";

	private final int order;
	private final double cutLowFrequency;
	private final double cutHighFrequency;

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * @param order
	 *            int number of sections (each section = 4 poles: 2 low freq
	 *            poles and 2 hi freq poles)
	 * @param cutLowFrequency
	 *            double cutoff (3-db) frequency in Hz
	 *            double cutoff (3-db) frequency in Hz
	 */
	public FilterBP(int order, double cutLowFrequency, double cutHighFrequency) {
		this.order = order;
		this.cutLowFrequency = cutLowFrequency;
		this.cutHighFrequency = cutHighFrequency;
	}

	/**
	 * Default constructor
	 * Called via reflection in XMAX.java
	 */
	public FilterBP() {
		this(4, 0.1, 0.5);
	}

	@Override
	public Function<double[], double[]> getFilterFunction() {
		return Filter.BANDPASS.getFilter(sampleRate, false, order, cutLowFrequency, cutHighFrequency);
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
			return (order == arg.getOrder()) && (cutLowFrequency == arg.getCutLowFrequency())
					&& (cutHighFrequency == arg.getCutHighFrequency());
		}
		return false;
	}
}
