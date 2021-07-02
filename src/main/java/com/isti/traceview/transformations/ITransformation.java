package com.isti.traceview.transformations;

import asl.utils.Filter;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import java.util.List;
import javax.swing.JFrame;

/**
 * Interface to represent abstract transformation. Transformation accepts list
 * of traces and creates a new data product (for example PSD, Spectra) and
 * passes the output for the display or storage
 * 
 * @author Max Kokoulin
 */

public interface ITransformation {

  int maxDataLength = (int) Math.pow(2, 30);

	/**
	 * Performs transformations
	 * 
	 * @param input
	 *            List of traces to process
	 * @param ti
	 *            Time interval to define processed range
	 * @param filter
	 *            Filter applied before transformation
	 * @param parentFrame
	 *            Host frame
	 */
  void transform(List<PlotDataProvider> input, TimeInterval ti, Filter filter,
      Object configiration,
      JFrame parentFrame);

	/**
	 * Return name of transformation
	 */
  String getName();



}
