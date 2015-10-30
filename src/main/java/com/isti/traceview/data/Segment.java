/**
 * 
 */
package com.isti.traceview.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import com.isti.traceview.common.TimeInterval;

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

	private long startTime;

	private double sampleRate;

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

	private transient BufferedRandomAccessFile dataStream = null;

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
		this.sampleRate = sampleRate;
		this.sourceSerialNumber = RDPserialNumber;
		this.maxValue = Integer.MIN_VALUE;
		this.minValue = Integer.MAX_VALUE;
		data = null;
		currentPos = 0;
		logger.debug("Created: " + this);
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

	/**
	 * Getter of the property <tt>endTime</tt>
	 * 
	 * @return segment data end time
	 */
	public Date getEndTime() {
		long time = new Double((sampleRate * sampleCount)).longValue();
		return new Date(getStartTime().getTime() + time);
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
            System.arraycopy(ret, 0, data, 0, sampleCount);
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
		// lg.debug("startTime=" + startTime +", endTime=" + getEndTime().getTime());
		int[] ret = null;
		int previous = Integer.MAX_VALUE;
		int next = Integer.MAX_VALUE;
		double startt = Math.max(startTime, start);
		double endt = Math.min(getEndTime().getTime(), end);
		int startIndex = new Double((startt - startTime) / sampleRate).intValue();
		//int startIndex = new Long(Math.round(new Double((startt	- startTime) / sampleRate))).intValue();
		int endIndex = new Double((endt - startTime) / sampleRate).intValue();
		if (startIndex != endIndex) {
			ret = new int[endIndex - startIndex];
			logger.debug("Getting segment data: startindex " + startIndex + ", endindex " + endIndex);
			if (dataStream == null) {
                		logger.debug("== dataStream == null --> Get points from RAM data[] " +
                "startTime=" + startTime + " endTime=" + getEndTime().getTime());
				// we use internal data in the ram
				for (int i = startIndex; i < endIndex; i++) {
					ret[i - startIndex] = data[i];
				}
				
				if (startIndex > 0) {
					previous = data[startIndex-1];
				}
				if (endIndex < sampleCount) {
					next = data[endIndex];
				}
			} else {
				// we use serialized data file
                logger.debug("== dataStream is NOT null --> Load points from dataStream.readInt() to data[] " +
                		"startTime=" + startTime + " endTime=" + getEndTime().getTime());
				try {
					if(startIndex>0){
						dataStream.seek(startOffsetSerial + startIndex * 4 - 4);
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
		} else {
			if (dataStream == null) {
				ret = new int[1];
				ret[0] = data[startIndex];
				if(startIndex>0) {
					previous = data[startIndex-1];
				}
				if (endIndex<sampleCount) {
					next = data[endIndex];
				}
			} else {
				try {
					if(startIndex>0){
						dataStream.seek(startOffsetSerial + startIndex * 4 - 4);
						previous = dataStream.readInt();
					} else {
						dataStream.seek(startOffsetSerial);
					}	
					ret[0] = dataStream.readInt();
					if(endIndex<sampleCount){
						next = dataStream.readInt(); 
					}
				} catch (IOException e) {
					logger.error("IOException:", e);
				}
			}
		}
		return new SegmentData(new Double(startTime + startIndex*sampleRate).longValue(), sampleRate, sourceSerialNumber, channelSerialNumber, continueAreaNumber, previous, next, ret);
	}

	/**
	 * Loads segment data from memory from data source
	 */
	public void load() {
		dataSource.load(this);
	}

	/**
	 * Adds sample to the end of segment data
	 */
	public synchronized void addDataPoint(int value) {
		if (data == null){
			data = new int[sampleCount];
		}
		data[currentPos++] = value;
		setMaxValue(value);
		setMinValue(value);
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
			blockMap = new TreeMap<Long, Long>();
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
		return "Segment: startTime "
				+ TimeInterval.formatDate(new Date(startTime), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
				+ ", endTime "
				+ TimeInterval.formatDate(new Date(new Double(startTime + sampleRate * sampleCount).longValue()),
						TimeInterval.DateFormatType.DATE_FORMAT_NORMAL) + ", sampleRate " + sampleRate + ", sampleCount " + sampleCount
				+ ", startOffset " + startOffset + ", maxValue " + maxValue + ", minValue " + minValue + ", rdpNumber " + sourceSerialNumber
				+ ", serialNumber " + channelSerialNumber + ", isLoaded=" + isLoaded + ";";
				//+ ", serialNumber " + channelSerialNumber + ";";
	}

	/**
	 * Sets data stream to serialize this segment
	 * 
	 * @param dataStream the new dataStream
	 */
	public void setDataStream(BufferedRandomAccessFile dataStream) {
		this.dataStream = dataStream;
	}
    // MTH:
	public BufferedRandomAccessFile getDataStream() {
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
}
