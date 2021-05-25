/**
 *
 */
package com.isti.traceview.data;

import com.isti.traceview.common.TimeInterval;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 * Represent continuous set of raw trace data without gaps belongs to same data source. A Seismic trace
 * therefore is defined as a sorted (by time) list of segments.
 *
 * @author Max Kokoulin
 */
public class Segment implements Externalizable, Cloneable {
	public static final long serialVersionUID = 1;
	private static final Logger logger = Logger.getLogger(Segment.class);
	/**
	 * Gap Tolerance - 1.0 is a gap of 2*sample rate
	 */
	private static double gapTolerance = 1.0;

	private int[] data = null;

	private int currentPos = 0;

	// used to skip over data points which are overlapping
	private int trimStart;

	private long startTime;

	private double sampleRate; // sample rate is in cycles

	/**
	 * Quantity of data values in the segment
	 */
	private int sampleCount;

	/**
	 * Starting position of this segment in the data source
	 */
	private long startOffset;

	private ISource dataSource;

	/**
	 * Offset of this segment in trace data serialized temporary file, if it exist
	 */
	private long startOffsetSerial;

	/**
	 * Maximal data value in segment
	 */
	private int maxValue;

	/**
	 * Minimal data value in segment
	 */
	private int minValue;

	/**
	 * ordinal number segment's data source in raw data provider
	 */
	private int sourceSerialNumber;

	/**
	 * Segment ordinal number in channel, differ from sourceSerialNumber as in this point of view
	 * segment can lay in several data sources if it hasn't gaps between
	 */
	private int channelSerialNumber;

	/**
	 * Sequential number of continue data area in trace, to which this segment belongs. 
	 * Similar to channelSerialNumber, but takes into account only gaps, not overlaps 
	 */
	private int continueAreaNumber;

	private RawDataProvider rdp = null;

	// map of time-offset pairs for blocks to quick find block by time
	private SortedMap<Long, Long> blockMap = null;

	private transient RandomAccessFile dataStream = null;

	// MTH: Use to combine segments read with -t and -d within a single PlotDataProvider
	private boolean isLoaded = false;

	/**
	 * @param dataSource
	 *            data source containing this segment
	 * @param startOffset
	 *            segment starting offset in data source
	 * @param startTime
	 *            segment data start time
	 * @param sampleRate
	 *            segment data sample rate
	 * @param sampleCount
	 *            count of samples in the segment
	 * @param RDPserialNumber
	 *            ordinal number of segment in the data source
	 */
	public Segment(ISource dataSource, long startOffset, Date startTime, double sampleRate, int sampleCount, int RDPserialNumber) {
		this.dataSource = dataSource;
		this.startOffset = startOffset;
		this.startTime = startTime.getTime();
		this.sampleCount = sampleCount;
		this.trimStart = 0;
		this.sampleRate = sampleRate;
		this.sourceSerialNumber = RDPserialNumber;
		this.maxValue = Integer.MIN_VALUE;
		this.minValue = Integer.MAX_VALUE;
		data = null;
		currentPos = 0;
		// logger.debug("Created: " + this);
	}

	/**
	 * Constructor to build a trimmed segment from an existing one, to fill in gaps with previously
	 * loaded data. If the times for trimming are outside of the boundary of the existing segment,
	 * the segment's original time interval will be used, unless the start point comes after the
	 * end point, in which case an empty segment (sample count of 0) will be created.
	 *
	 * This scanning operation
	 * @param segment Segment to trim data from
	 * @param newStartPoint epoch millisecond to start data at
	 * @param newEndPoint epoch millisecond to end data at
	 */
	public Segment(Segment segment, long newStartPoint, long newEndPoint) {
		// can't start before existing segment's start point
		newStartPoint = Math.max(segment.getStartTime().getTime(), newStartPoint);
		// can't end after existing segment's end point
		newEndPoint = Math.min(segment.getEndTime().getTime(), newEndPoint);

		this.currentPos = 0;
		this.data = null;

		this.dataSource = segment.getDataSource();
		this.startOffset = segment.getStartOffset();
		this.sampleRate = segment.getSampleRate();
		this.sampleCount = segment.getSampleCount();
		this.sourceSerialNumber = segment.getSourceSerialNumber();

		long untrimmedStartMilli = segment.getStartTime().getTime();
		// first sample to take of given data
		this.trimStart = (int) ((newStartPoint - untrimmedStartMilli) / sampleRate);
		// quantized start time, i.e., when the first untrimmed sample actually occurs
		this.startTime = untrimmedStartMilli + (int) (trimStart * sampleRate);
		this.sampleCount = (int) ((newEndPoint - newStartPoint) / sampleRate);

		if (sampleCount <= 0) {
			// if newStartPoint is set to be after the newEndPoint, we must enforce 0 length
			// this happens if we try to see if the segment has data after another segment that might
			// have ended already -- in which case there wouldn't be any data to add
			// (i.e., we change the start time but leave the original segment's end time)
			sampleCount = 0;
			this.startTime = newStartPoint;
			data = new int[]{};
		}

	}

	/**
	 * Constructor for testing purposes
	 */
	Segment(int[] newData, long startTime, double sampleRate) {
		this.data = newData;
		this.startTime = startTime;
		this.sampleRate = sampleRate;
		this.sampleCount = newData.length;
	}

	/**
	 * Constructor to work during deserialization
	 */
	public Segment() {
		logger.debug("Created empty segment");
	}

	/**
	 * Getter of the property <tt>startTime</tt>
	 *
	 * @return segment data start time
	 */
	public Date getStartTime() {
		return new Date(startTime);
	}

	public long getStartTimeMillis() {
		return startTime;
	}

	/**
	 * Getter of the property <tt>endTime</tt>
	 *
	 * @return segment data end time
	 */
	public Date getEndTime() {
		// sampleRate is really interval in ms (i.e., millseconds per sample)
		// then (ms / sample) * samples = millisecond length of data
		long time = (long) (sampleCount * sampleRate);
		return new Date(getStartTime().getTime() + time);
	}

	public long getEndTimeMillis() {
		return getStartTimeMillis() + (long) (sampleCount * sampleRate);
	}

	/**
	 * Gets ordinal number of segment in the data source
	 */
	public int getSourceSerialNumber() {
		return sourceSerialNumber;
	}

	/**
	 * Sets ordinal number of segment in the data source
	 */
	public void setSourceSerialNumber(int serialNumber) {
		this.sourceSerialNumber = serialNumber;
	}

	/**
	 * Sets ordinal number of segment in the trace, we count only gaps, not sources boundaries in
	 * this case
	 */
	public int getChannelSerialNumber() {
		return channelSerialNumber;
	}

	/**
	 * Gets ordinal number of segment in the trace, we count only gaps and overlaps, not sources boundaries in
	 * this case
	 */
	public void setChannelSerialNumber(int serialNumber) {
		this.channelSerialNumber = serialNumber;
	}

	/**
	 * Gets sequential number of continue data area in the trace, to which this segment belongs.
	 * We takes into account only gaps, not overlaps in this case.
	 */
	public int getContinueAreaNumber() {
		return continueAreaNumber;
	}

	/**
	 * Sets sequential number of continue data area in the trace, to which this segment belongs.
	 * We takes into account only gaps, not overlaps in this case.
	 */
	public void setContinueAreaNumber(int serialNumber) {
		this.continueAreaNumber = serialNumber;
	}

	/**
	 * @return raw data provider to which this segment belongs
	 */
	public RawDataProvider getRawDataProvider() {
		return rdp;
	}

	/**
	 * @param rawDataProvider
	 *            raw data provider to which this segment belongs
	 */
	public void setRawDataProvider(RawDataProvider rawDataProvider) {
		this.rdp = rawDataProvider;
	}

	/**
	 * MTH: Currently not using this since serialized data is being
	 *      {@literal read in dumpData() --> InitCache --> Segment.getData()}
	 *
	 * Load the int[] data from a .DATA file into this Segment data[]
	 *      Needed so that -T will work with existing serialized data
	 */
	public int[] loadDataInt() {
		int[] ret = null;
		if (dataStream == null) {
			logger.error("dataStream == null!! --> Exiting");
			System.exit(0);
		} else {
			ret = new int[sampleCount];
			try {
				dataStream.seek(startOffsetSerial);
				for (int i = 0; i < sampleCount; i++) {
					ret[i] = dataStream.readInt();
				}
			} catch (IOException e) {
				logger.error("IOException:", e);
			}
			// Copy into this Segment's int[] data:
			data = new int[sampleCount];
			System.arraycopy(ret, trimStart, data, 0, sampleCount);
		}
		return ret;
	}


	/**
	 * Reads all data from loaded segment
	 *
	 * NOTE: Will add {@code ArrayList<Integer>} dataList constructor for SegmentData (for future use)
	 */
	public SegmentData getData() {
		if (dataStream == null) {
			return new SegmentData(startTime, sampleRate, sourceSerialNumber, channelSerialNumber, continueAreaNumber, data);
		} else {
			int[] ret = new int[sampleCount];
			try {
				dataStream.seek(startOffsetSerial);
				for (int i = 0; i < sampleCount; i++) {
					ret[i] = dataStream.readInt();
				}
			} catch (IOException e) {
				logger.error("IOException:", e);
			}
			return new SegmentData(startTime, sampleRate, sourceSerialNumber, channelSerialNumber, continueAreaNumber, ret);
		}
	}

	/**
	 * Reads range of data from loaded segment
	 */
	public SegmentData getData(TimeInterval ti) {
		return getData(ti.getStart(), ti.getEnd());
	}

	public Double getPointAtTime(long start) {
		// The conditional is here because in between segments there may be very slight timing
		// discrepancies that cause samples to have a slight error compared to their normal sample rate.
		// Normally we expect the end time of a segment and the start of the next one to be equal, but
		// sometimes they may be off by a few milliseconds.
		// We should ignore this discrepancy and just get the next segment's first point when possible
		// which is why we have the specific gap check and move the start index up to 0
		boolean isGap = getStartTimeMillis() - start > (long) sampleRate;
		if (isGap || start >= getEndTimeMillis()) {
			return Double.NaN;
		}

		int startIndex = (int) Math.round((start - startTime) / sampleRate);
		startIndex = Math.max(startIndex, 0);
		return (double) data[startIndex];
	}

	/**
	 * returns array of data in requested time range, from loaded segment.
	 *
	 * @param start
	 *            start time in milliseconds
	 * @param end
	 *            end time in milliseconds
	 */
	@SuppressWarnings("null")
	public SegmentData getData(double start, double end) {
		double temp = Math.min(start, end);
		end = Math.max(start, end);
		start = temp;
		if (data == null) {
			// try to load the data if it still hasn't been read in (might happen in test cases)
			// and then produce the error if that doesn't work correctly
			load();
			if (data == null) {
				logger.error("== Underlying array has not been initialized");
			}
		}

		int[] ret = null;
		int previous = Integer.MAX_VALUE;
		int next = Integer.MAX_VALUE;
		double startt = Math.max(startTime, start);
		double endt = Math.min(getEndTime().getTime(), end);
		int startIndex = (int) ((startt - startTime) / sampleRate);
		int endIndex = (int) ((endt - startTime) / sampleRate);
		if (startIndex != endIndex) {
			ret = new int[endIndex - startIndex];
			// logger.debug("Getting segment data: startindex " + startIndex + ", endindex " + endIndex);
			if (dataStream == null) {
				ret = Arrays.copyOfRange(data, startIndex, endIndex);

				if (startIndex > 0) {
					previous = data[startIndex-1];
				}
				if (endIndex < sampleCount) {
					next = data[endIndex];
				}
			} else {
				// we use serialized data file
				logger.debug("== dataStream is NOT null --> Load points from dataStream.readInt() "
						+ "to data[] | startTime=" + startTime + " endTime=" + getEndTime().getTime());
				try {
					if(startIndex>0){
						dataStream.seek(startOffsetSerial + startIndex * 4L - 4);
						previous = dataStream.readInt();
					} else {
						dataStream.seek(startOffsetSerial);
					}
					for (int i = startIndex; i < endIndex; i++) {
						ret[i - startIndex] = dataStream.readInt();
					}
					if(endIndex<sampleCount){
						next = dataStream.readInt();
					}
					// MTH: Use this if we are in the -T mode and we need to load existing serialized data (from .DATA)
					if (com.isti.traceview.TraceView.getConfiguration().getDumpData()) {
						logger.debug("We are in -T dataDump mode --> read this Segment from dataStream");
						if (data == null) {
							if (ret.length != sampleCount) {
								//System.out.format("== Segment.getData(): Warning: sampleCount=[%d pnts] BUT data.length=[%d pnts]\n", sampleCount, ret.length);
								logger.warn(String.format("sampleCount=[%d pnts] BUT data.length=[%d pnts]\n", sampleCount, ret.length));
							}
							//data = new int[sampleCount];
							data = new int[ret.length];
							System.arraycopy(ret, 0, data, 0, ret.length);
						}
						else {
							//System.out.println("== Segment.getData(): We are in -T dataDump mode but data IS NOT null!!!");
							logger.debug("We are in -T dataDump mode but data IS NOT null!!!");
						}
					}
				} catch (IOException e) {
					logger.error("IOException:", e);
				}
			}
		}
		return new SegmentData( (long) (startTime + startIndex*sampleRate), sampleRate,
				sourceSerialNumber, channelSerialNumber, continueAreaNumber, previous, next, ret);
	}

	/**
	 * Loads segment data from memory from data source
	 */
	public void load() {
		dataSource.load(this);
		setIsLoaded(true);
	}

	/**
	 * Adds sample to the end of segment data
	 */
	public synchronized void addDataPoint(int value) {
		if (data == null){
			currentPos = 0;
			data = new int[sampleCount];
		}
		data[currentPos++] = value;
		setMaxValue(value);
		setMinValue(value);
	}

	public synchronized void setData(int[] intData) {
		if (data == null){
			currentPos = 0;
			this.data = new int[sampleCount];
		}
		System.arraycopy(intData, 0, this.data, currentPos, data.length);
		currentPos += intData.length;
	}

	/**
	 * Getter of the property <tt>sampleRate</tt>
	 *
	 * @return Returns the sample rate.
	 */
	public double getSampleRate() {
		return sampleRate;
	}

	/**
	 * Getter of the property <tt>startOffset</tt>
	 *
	 * @return Starting position of this segment in the data source
	 */
	public long getStartOffset() {
		return startOffset;
	}

	/**
	 * @return data source to which this segment belongs
	 */
	public ISource getDataSource() {
		return dataSource;
	}

	/**
	 * Getter of the property <tt>sampleCount</tt>
	 *
	 * @return the count of samples in the segment data
	 */
	public int getSampleCount() {
		return sampleCount;
	}

	/**
	 * Clears segment data
	 */
	public void drop() {
		for (int i = 0; i < currentPos; i++) {
			data[i] = 0;
		}
		data = null;
		currentPos = 0;
	}

	/**
	 * Getter of the property <tt>maxValue</tt>
	 *
	 * @return maximum raw data value in the segment
	 */
	public int getMaxValue() {
		return maxValue;
	}

	/**
	 * Setter of the property <tt>maxValue</tt>
	 *
	 * @param maxValue
	 *            The maxValue to set.
	 */
	public void setMaxValue(int maxValue) {
		if (maxValue > this.maxValue) {
			this.maxValue = maxValue;
		}
	}

	/**
	 * Getter of the property <tt>minValue</tt>
	 *
	 * @return minimum raw data value in the segment
	 */
	public int getMinValue() {
		return minValue;
	}

	/**
	 * Setter of the property <tt>minValue</tt>
	 *
	 * @param minValue
	 *            The minValue to set.
	 */
	public void setMinValue(int minValue) {
		if (minValue < this.minValue) {
			this.minValue = minValue;
		}
	}

	public void addBlockDescription(long startTime, long offset){
		if(blockMap == null){
			blockMap = new TreeMap<>();
		}
		blockMap.put(startTime, offset);
	}

	public String getBlockHeaderText(long time){
		if(blockMap != null){
			long blockStartTime = blockMap.headMap(time).lastKey();
			return dataSource.getBlockHeaderText(blockMap.get(blockStartTime));
		} else {
			return "<html>Block marks are unavailable for this file type</html>";
		}
	}

	public String toString() {
		long endTime = startTime + (long) (sampleRate * sampleCount);
		return "Segment: startTime "
				+ Instant.ofEpochMilli(startTime)
				+ ", endTime "
				+ Instant.ofEpochMilli(endTime)
				+ ", sampleRate " + sampleRate + ", sampleCount " + sampleCount
				+ ", startOffset " + startOffset + ", maxValue " + maxValue + ", minValue " + minValue + ", rdpNumber " + sourceSerialNumber
				+ ", serialNumber " + channelSerialNumber + ", isLoaded=" + isLoaded + ";";
		//+ ", serialNumber " + channelSerialNumber + ";";
	}

	/**
	 * Sets data stream to serialize this segment
	 *
	 * @param dataStream the new dataStream
	 */
	public void setDataStream(RandomAccessFile dataStream) {
		this.dataStream = dataStream;
	}
	// MTH:
	public RandomAccessFile getDataStream() {
		return dataStream;
	}

	/**
	 * Special deserialization handler
	 *
	 * @param in
	 *            stream to deserialize object
	 * @see Serializable
	 * @throws IOException if thrown while reading ObjectInpu
	 * @throws ClassNotFoundException if thrown while reading the dataSource object
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		logger.debug("== ENTER");
		dataSource = (ISource) in.readObject();
		currentPos = in.readInt();
		startTime = in.readLong();
		sampleRate = in.readDouble();
		sampleCount = in.readInt();
		startOffset = in.readLong();
		maxValue = in.readInt();
		minValue = in.readInt();
		startOffsetSerial = in.readLong();
		sourceSerialNumber = in.readInt();
		channelSerialNumber = in.readInt();
		continueAreaNumber = in.readInt();

		// we don't load serialized channel data at start time - we do it when we need it
		//data = new int[sampleCount];
		//for (int i = 0; i < sampleCount; i++) {
		//data[i] = inData.readInt();
		//MTH: This should be data[i] = dataStream.readInt();
		logger.debug("== EXIT: Deserialized " + this);
	}

	/**
	 * Special serialization handler
	 *
	 * @param out
	 *            stream to serialize this object
	 * @see Serializable
	 * @throws IOException if there are problems writing the serialized file
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		logger.debug("==  Output the Segment to serial stream:");
		logger.debug("    Segment:" + this.toString() );
		logger.debug("    Segment: ObjectOutputStream:" + out.toString() );
		logger.debug("    Segment: dataSource:"  + dataSource );
		logger.debug("    Segment: dataStream:"  + dataStream );
		logger.debug("    Segment: sampleCount:" + sampleCount );

		out.writeObject(dataSource);
		out.writeInt(currentPos);
		out.writeLong(startTime);
		out.writeDouble(sampleRate);
		out.writeInt(sampleCount);
		out.writeLong(startOffset);
		out.writeInt(maxValue);
		out.writeInt(minValue);
		out.writeLong(dataStream.getFilePointer());
		out.writeInt(sourceSerialNumber);
		out.writeInt(channelSerialNumber);
		out.writeInt(continueAreaNumber);
		for (int i = 0; i < sampleCount; i++) {
			dataStream.writeInt(data[i]);
		}
		logger.debug("== DONE");
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * Sets gap tolerance to detect gaps between segments. 1.0 is a gap of 2*sample rate
	 *
	 * @param tolerance the new gapTolerance
	 */
	public static void setGapTolerance(double tolerance) {
		gapTolerance = tolerance;
	}

	/**
	 * detect is there is data break (gap or overlay) between two time points
	 *
	 * @param firstEndTime
	 *            first time point
	 * @param secondStartTime
	 *            second time point
	 * @param sampleRate
	 *            sample rate
	 */
	public static boolean isDataBreak(long firstEndTime, long secondStartTime, double sampleRate) {
		double gap = gapTolerance * 2.0 * sampleRate;
		long timediff = Math.abs(firstEndTime - secondStartTime);
		boolean compare = timediff > gap;
		return compare;
	}

	public static boolean isDataGap(long firstEndTime, long secondStartTime, double sampleRate) {
		double gap = gapTolerance * 2.0 * sampleRate;
		long timediff = secondStartTime - firstEndTime;
		boolean compare = timediff > gap;
		return compare;
	}

	public static boolean isDataOverlay(long firstEndTime, long secondStartTime, double sampleRate) {
		double gap = gapTolerance * 2.0 * sampleRate;
		long timediff = firstEndTime - secondStartTime;
		boolean compare = timediff > gap;
		return compare;
	}

	public boolean getIsLoaded() {
		return isLoaded;
	}
	public void setIsLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	public static Segment mergeSegments(Segment... segs) {
		long startTime = segs[0].startTime;
		double sampleRate = segs[0].getSampleRate();
		int[][] allSamples = new int[segs.length][];
		allSamples[0] = segs[0].getData().data;
		int totalLength = allSamples[0].length;
		long currentEndTime = startTime + (long) (totalLength * sampleRate);
		for (int i = 1; i < segs.length; ++i) {
			Segment seg = segs[i];
			assert (seg.getSampleRate() == sampleRate);
			assert (!isDataGap(segs[i-1].getEndTimeMillis(), seg.getStartTimeMillis(), sampleRate));
			TimeInterval ti = new TimeInterval(seg.getStartTimeMillis(), seg.getEndTimeMillis());
			if (seg.getStartTimeMillis() < currentEndTime) {
				ti = new TimeInterval(currentEndTime, seg.getEndTimeMillis());
			}
			allSamples[i] = seg.getData(ti).data;
			if (allSamples[i] == null) continue;
			totalLength += allSamples[i].length;
			currentEndTime = startTime + (long) (totalLength * sampleRate);
		}

		int[] data = new int[totalLength];
		int startingIndex = 0;
		for (int[] mergeIn : allSamples) {
			if (mergeIn == null) continue;
			System.arraycopy(mergeIn, 0, data, startingIndex, mergeIn.length);
			startingIndex += mergeIn.length;
		}
		Segment returnValue = new Segment(segs[0].dataSource, segs[0].startOffset,
				segs[0].getStartTime(), sampleRate, totalLength, segs[0].sourceSerialNumber);
		returnValue.data = data;
		return returnValue;
	}
}
