package com.isti.traceview.filters;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.BPFilterException;

/**
 * Interface to represent abstract filter. Filter gets data from RawDataProvider
 * and outputs modified seismic trace segments
 * 
 * @author Max Kokoulin
 */
public interface IFilter {
	/**
	 * @return Maximum length of data to filter without warning message
	 */
  int getMaxDataLength();

	/**
	 * Filter design routine, filter should be initialized before using
	 * 
	 * @param channel
	 *            the raw data
	 */
  void init(RawDataProvider channel);

  boolean isInitialized();

	/**
	 * Performs filtering.
	 *
	 * @param data
	 *            array to filter
	 * @param length
	 *            number of samples to filter
	 * @return filtered data array
	 * @throws TraceViewException
	 *             the trace view exception
	 * @throws BPFilterException
	 *             the BP filter exception
	 */
  double[] filter(double[] data, int length)
			throws TraceViewException, BPFilterException;

	boolean needProcessing();

	/**
	 * Can be used instead of referencing NAME when used as Generic.
	 *
	 * @return the filter's name as stored in static NAME field.
	 */
  String getName();
}
