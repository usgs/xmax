package com.isti.traceview.data;

import java.io.Serializable;
import java.util.Set;

/**
 * Interface to represent data source
 * 
 * @author Max Kokoulin
 */
public interface ISource extends Serializable {
	/**
	 * Enumeration for data source types
	 */
	enum SourceType {
		/**
		 * File source
		 */
		FILE,
		/**
		 * Network socket source
		 */
		SOCKET

	}

  /**
	 * Enumeration for supported source formats
	 */
	enum FormatType {
		MSEED, SEED, SAC, SEGY, SEGD, IMS, ASCII
	}

  /**
	 * @return Type of this source
	 */
	SourceType getSourceType();

	/**
	 * @return Format of this source
	 */
	FormatType getFormatType();

	/**
	 * Parse this data source, i.e scans it, determine which traces placed inside, filling metadata
	 * how we can find desired trace information using direct access method, see
	 * {@link ISource#load(Segment)}
	 *
	 * @return list of found traces
	 */
	Set<PlotDataProvider> parse();

	/**
	 * @return name of this data source
	 */
	String getName();

	/**
	 * Load trace data from this data source into a segment object.
	 * Segment objects represent contiguous ranges of time; multiple segments are used to handle
	 * gaps between
	 * 
	 * These params are inaccurate, but potentially useful
	 * param offset
	 *            offset where we starts
	 * param sampleCount
	 *            how many points we want to load
	 * return array of integers contains the data
	 * 
	 * @param segment the segment to load offset and samplecounts are most likely used.
	 */
	void load(Segment segment);
	
	/**
	 * Get text representation of block header for given format
	 * 
	 * @param blockStartOffset
	 *            file pointer position to read block
	 */
	String getBlockHeaderText(long blockStartOffset);

}
