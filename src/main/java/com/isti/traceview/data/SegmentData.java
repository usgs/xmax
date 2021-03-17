package com.isti.traceview.data;

public class SegmentData {
	long startTime = 0;
	double sampleRate = Double.POSITIVE_INFINITY;
	int previous = Integer.MAX_VALUE;
	int next = Integer.MAX_VALUE;
	public int[] data = null;
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

	public SegmentData(long startTime, double sampleRate, int sourceSerialNumber, int channelSerialNumber, int continueAreaNumber, int[] data) {
		this(startTime, sampleRate, sourceSerialNumber, channelSerialNumber, continueAreaNumber, Integer.MAX_VALUE, Integer.MAX_VALUE, data);
	}

	public long endTime() {
		if (data == null || data.length == 0) {
			return startTime;
		}
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
		double startvalue = (startt - startTime) / sampleRate;
		int startIndex = 0;
		if (startvalue > 0.000000001) {
			startIndex = new Double(startvalue).intValue() + 1;
		}
		int endIndex = 0;
		if (data != null && data.length > 0) {
			endIndex = Math.min(
					new Double((endt - startTime) / sampleRate).intValue(), data.length - 1);
		}
		if (startIndex <= endIndex) {
			ret = new int[endIndex - startIndex + 1];
			// lg.debug("PlotDataProvider.getData()-getting segment data: startindex " + startIndex
			// + ", endindex " + endIndex);
			if (endIndex + 1 - startIndex >= 0)
				System.arraycopy(data,
						startIndex, ret, startIndex - startIndex, endIndex + 1 - startIndex);
		}
		if (startIndex > 0 && data != null)
			_previous = data[startIndex - 1];
		if (data != null && endIndex < data.length-1)
			_next = data[endIndex];
		return new SegmentData(startTime, sampleRate, sourceSerialNumber, channelSerialNumber,
				continueAreaNumber, _previous, _next, ret);
	}

	/**
	 * Computes linear interpolated value for any time on data array.
	 * 
	 * @param time
	 *            time argument to get interpolated value
	 * @return the linear interpolated value
	 */
	public double interpolateValue(double time) {
		// lg.debug("interpolateValue: dataLength " + data.length + ", startTime " + dataTI.getStartTime() + ", time " + time);
		int nextIndex = new Double((time - startTime) / sampleRate + 1).intValue();
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

	/**
	 * Return the last value (used to get bias for steim encoding for seed file writing)
	 * @return value of last point in data array (length - 1)
	 */
  public int getLastValue() {
		return data[data.length - 1];
  }

}
