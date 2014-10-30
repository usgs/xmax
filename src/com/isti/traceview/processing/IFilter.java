package com.isti.traceview.processing;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.data.RawDataProvider;

/**
 * Interface to represent abstract filter. Filter gets data from RawDataProvider and outputs modified
 * seismic trace segments
 * 
 * @author Max Kokoulin
 */
public interface IFilter {
	/**
	 * @return Maximum length of data to filter without warning message
	 */
	public int getMaxDataLength();

	/**
	 * filter design routine, filter should be initialized before using
	 */
	public void init(RawDataProvider channel);

	/**
	 * Performs filtering
	 * 
	 * @param data
	 *            array to filter
	 * @param length
	 *            number of samples to filter
	 * @return filtered data array
	 */
	public double[] filter(double[] data, int length) 
		throws TraceViewException,
			   BPFilterException,
			   HPFilterException,
			   LPFilterException;
	/**
	 * @return Filter's name
	 */
	public String getName();

	public boolean needProcessing();
}
