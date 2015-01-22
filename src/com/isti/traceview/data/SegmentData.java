package com.isti.traceview.data;

import java.util.List;
import java.util.ArrayList;

public class SegmentData {
	long startTime = 0;
	double sampleRate = Double.POSITIVE_INFINITY;
	int previous = Integer.MAX_VALUE;
	int next = Integer.MAX_VALUE;
	public int[] data = null;
	public List<Integer> dataList = null;
	int sourceSerialNumber = 0;
	int channelSerialNumber = 0;
	/**
	 * Sequential number of continue data area in trace, to which this point belongs. 
	 * Similar to segmentNumber, but takes into account only gaps, not overlaps 
	 */
	int continueAreaNumber = 0;

	public SegmentData(long startTime, double sampleRate, int sourceSerialNumber, int channelSerialNumber, int continueAreaNumber, int previous, int next, int[] data) {
		this.startTime = startTime;
		this.sampleRate = sampleRate;
		this.previous = previous;
		this.next = next;
		this.data = data;
		this.sourceSerialNumber = sourceSerialNumber;
		this.channelSerialNumber = channelSerialNumber;
		this.continueAreaNumber = continueAreaNumber;
	}
	
	// New constructor for ArrayList<Integer> data
	public SegmentData(long startTime, double sampleRate, int sourceSerialNumber, int channelSerialNumber, int continueAreaNumber, int previous, int next, List<Integer> dataList) {
		this.startTime = startTime;
		this.sampleRate = sampleRate;
		this.previous = previous;
		this.next = next;
		this.dataList = dataList;
		this.sourceSerialNumber = sourceSerialNumber;
		this.channelSerialNumber = channelSerialNumber;
		this.continueAreaNumber = continueAreaNumber;
	}

	public SegmentData(long startTime, double sampleRate, int sourceSerialNumber, int channelSerialNumber, int continueAreaNumber, int[] data) {
		this(startTime, sampleRate, sourceSerialNumber, channelSerialNumber, continueAreaNumber, Integer.MAX_VALUE, Integer.MAX_VALUE, data);
	}
	
	// This is a new constructor utilizing the properties of an ArrayList<Integer> dataList
	public SegmentData(long startTime, double sampleRate, int sourceSerialNumber, int channelSerialNumber, int continueAreaNumber, List<Integer> dataList) {
		this(startTime, sampleRate, sourceSerialNumber, channelSerialNumber, continueAreaNumber, Integer.MAX_VALUE, Integer.MAX_VALUE, dataList);
	}

	public long endTime() {
		return new Double(startTime + sampleRate * data.length).longValue();
	}

	/**
	 * returns subarray of data in requested time range, from array of loaded segment data.
	 * 
	 * @param start
	 *            start time of requested range in milliseconds
	 * @param end
	 *            end time of requested range in milliseconds
	 */
	public SegmentData getData(double start, double end) {
		// lg.debug("PlotDataProvider.getData(): start " + start + ", end " + end);
		int[] ret = new int[0];
		int _previous = Integer.MAX_VALUE;
		int _next = Integer.MAX_VALUE;
		double startt = Math.max(startTime, start);
		double endt = Math.min(endTime(), end);
		double startvalue = new Double((startt - startTime) / sampleRate);
		int startIndex = 0;
		if (startvalue > 0.000000001) {
			startIndex = new Double(startvalue).intValue() + 1;
		}
		int endIndex = Math.min(new Double((endt - startTime) / sampleRate).intValue(), data.length - 1);
		if (startIndex <= endIndex) {
			ret = new int[endIndex - startIndex + 1];
			// lg.debug("PlotDataProvider.getData()-getting segment data: startindex " + startIndex
			// + ", endindex " + endIndex);
			for (int i = startIndex; i <= endIndex; i++) {
				ret[i - startIndex] = data[i];
			}
		}
		if (startIndex > 0)
			_previous = data[startIndex - 1];
		if (endIndex < data.length-1)
			_next = data[endIndex];
		return new SegmentData(startTime, sampleRate, sourceSerialNumber, channelSerialNumber, continueAreaNumber, _previous, _next, ret);
	}

	/**
	 * Computes linear interpolated value for any time on data array.
	 * 
	 * @param data
	 *            array of data to interpolate
	 * @param dataTI
	 *            time range for this data
	 * @param sampleRate
	 *            data sample rate
	 * @param time
	 *            time argument to get interpolated value
	 * @return
	 */
	public double interpolateValue(double time) {
		// lg.debug("interpolateValue: dataLength " + data.length + ", startTime " + dataTI.getStartTime() + ", time " + time);
		int nextIndex = new Double((time - startTime ) / sampleRate + 1).intValue();
		if (nextIndex >= data.length) {
			nextIndex = data.length - 1;
		}
		int prevIndex = 0;
		if (nextIndex < 1) {
			nextIndex = 0;
		} else {
			prevIndex = nextIndex - 1;
		}
		// lg.debug("prevIndex = " + prevIndex + "; nextIndex = " + nextIndex);
		return data[prevIndex] + (data[nextIndex] - data[prevIndex]) * (time - startTime - sampleRate * prevIndex)
				/ (sampleRate * (nextIndex - prevIndex));

	}
}
