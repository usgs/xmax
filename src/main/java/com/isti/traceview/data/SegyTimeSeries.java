package com.isti.traceview.data;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.apache.log4j.Logger;

/**
 * This is the header for the PASSCAL SEGY trace data. The PASSCAL SEGY trace format is a modified
 * form of the SEG-Y trace format. The modification comes is because we use some of the unspecified
 * header words to store information pertinent to the PASSCAL data. The data values for each trace
 * are preceded by a 240 byte header. This format is given below. All integer values are stored with
 * the most significant byte first. Data values are either 16 0r 32 bit integers depending on byte
 * 206 of the header, the field named "data_form". SEGYHEAD is now typedef'ed Reading bytes directly
 * into this header will allow access to all of the fields. The number in the comment is the byte
 * offset into the segy file. An "X" in the comment indicates that field is NEVER set. Fields that
 * are set to default values contain that value and follow a ":" in the comment ("grep : segy.h"
 * will spit all default fields out). Two pairs of fields exist to cover an inherited limitation;
 * sampleLength/num_samps and deltaSample/samp_rate. When the value is too large to fit in a short,
 * sampleLength or deltaSample become flags and require their int counterparts, num_samps and
 * samp_rate, to contain that value.
 */
public class SegyTimeSeries { /* Offset Description */
	private static final Logger logger = Logger.getLogger(SegyTimeSeries.class);
	static final boolean doRepacketize = false;

	private boolean bigEndian;

	String oNetwork = "";
	String oChannel = "";
	String oStation = "";
	boolean setOInfo = false;

	int lineSeq; /* 0 Sequence numbers within line */
	int reelSeq; /* 4 Sequence numbers within reel */
	int event_number; /* 8 Original field record number or trigger number */
	int channel_number; /*
						 * 12 Trace channel number within the original field record
						 */
	int energySourcePt; /* 16 X */
	int cdpEns; /* 20 X */
	int traceInEnsemble; /* 24 X */
	short traceID; /* 28 Trace identification code: seismic data = 1 */
	short vertSum; /* 30 X */
	short horSum; /* 32 X */
	short dataUse; /* 34 X */
	int sourceToRecDist; /* 36 X */
	int recElevation; /* 40 X */
	int sourceSurfaceElevation; /* 44 X */
	int sourceDepth; /* 48 X */
	int datumElevRec; /* 52 X */
	int datumElevSource; /* 56 X */
	int sourceWaterDepth; /* 60 X */
	int recWaterDepth; /* 64 X */
	short elevationScale; /* 68 Elevation Scaler: scale = 1 */
	short coordScale; /* 70 Coordinate Scaler: scale = 1 */
	int sourceLongOrX; /* 72 X */
	int sourceLatOrY; /* 76 X */
	int recLongOrX; /* 80 X */
	int recLatOrY; /* 84 X */
	short coordUnits; /* 88 Coordinate Units: = 2 (Lat/Long) */
	short weatheringVelocity; /* 90 X */
	short subWeatheringVelocity; /* 92 X */
	short sourceUpholeTime; /* 94 X */
	short recUpholeTime; /* 96 X */
	short sourceStaticCor; /* 98 X */
	short recStaticCor; /* 100 X */
	short totalStatic; /*
						 * 102 Total Static in MILLISECS added to Trace Start Time (lower 2 bytes)
						 */
	short lagTimeA; /* 104 X */
	short lagTimeB; /* 106 X */
	short delay; /* 108 X */
	short muteStart; /* 110 X */
	short muteEnd; /* 112 X */
	short sampleLength; /* 114 Number of samples in this trace (unless == 32767) */
	short deltaSample; /* 116 Sampling interval in MICROSECONDS (unless == 1) */
	short gainType; /* 118 Gain Type: 1 = Fixed Gain */
	short gainConst; /* 120 Gain of amplifier */
	short initialGain; /* 122 X */
	short correlated; /* 124 X */
	short sweepStart; /* 126 X */
	short sweepEnd; /* 128 X */
	short sweepLength; /* 130 X */
	short sweepType; /* 132 X */
	short sweepTaperAtStart; /* 134 X */
	short sweepTaperAtEnd; /* 136 X */
	short taperType; /* 138 X */
	short aliasFreq; /* 140 X */
	short aliasSlope; /* 142 X */
	short notchFreq; /* 144 X */
	short notchSlope; /* 146 X */
	short lowCutFreq; /* 148 X */
	short hiCutFreq; /* 150 X */
	short lowCutSlope; /* 152 X */
	short hiCutSlope; /* 154 X */
	short year; /* 156 year of Start of trace */
	short day; /* 158 day of year at Start of trace */
	short hour; /* 160 hour of day at Start of trace */
	short minute; /* 162 minute of hour at Start of trace */
	short second; /* 164 second of minute at Start of trace */
	short timeBasisCode; /* 166 Time basis code: 2 = GMT */
	short traceWeightingFactor; /* 168 X */
	short phoneRollPos1; /* 170 X */
	short phoneFirstTrace; /* 172 X */
	short phoneLastTrace; /* 174 X */
	short gapSize; /* 176 X */
	short taperOvertravel; /* 178 X */
	String station_name; /* 180 Station Name code (5 chars + \0) */
	String sensor_serial; /* 186 Sensor Serial code (7 chars + \0) */
	String channel_name; /* 194 Channel Name code (3 chars + \0) */
	short totalStaticHi; /*
							 * 198 Total Static in MILLISECS added to Trace Start Time (high 2
							 * bytes)
							 */
	int samp_rate; /* 200 Sample interval in MICROSECS as a 32 bit integer */
	short data_form; /* 204 Data Format flag: 0=16 bit, 1=32 bit integer */
	short m_secs; /* 206 MILLISECONDS of seconds of Start of trace */
	short trigyear; /* 208 year of Trigger time */
	short trigday; /* 210 day of year at Trigger time */
	short trighour; /* 212 hour of day at Trigger time */
	short trigminute; /* 214 minute of hour at Trigger time */
	short trigsecond; /* 216 second of minute at Trigger time */
	short trigmills; /* 218 MILLISECONDS of seconds of Trigger time */
	float scale_fac; /* 220 Scale Factor (IEEE 32 bit float) */
	short inst_no; /* 224 Instrument Serial Number */
	short not_to_be_used; /* 226 X */
	int num_samps;
	/*
	 * 228 Number of Samples as a 32 bit integer (when sampleLength == 32767)
	 */
	int max; /* 232 Maximum value in Counts */
	int min; /* 236 Minimum value in Counts */
	/* end of segy trace header */

	/**TODO: This member needs documentation and a better name*/
	public int[] y;

	/**
	 * Constructor declaration
	 */
	public SegyTimeSeries() {
		logger.debug("ENTER");
		bigEndian = true;
		//lg.debug("SegyTimeSeries::SegyTimeSeries() left");
	}

	/**
	 * Method declaration
	 * 
	 * @return the y member
	 * 
	 * @deprecated This is not called by anything
	 */
	public int[] getDataArray() {
		return y;
	}

	/**
	 * returns a vector with the time of the first data point. min and max values, and the data
	 * array as an int array
	 * 
	 * @deprecated This method is not called by anything
	 */
	public int[] getDataArray(TimeInterval ti) throws TraceViewException {
		int[] retAR;
		if (!ti.isIntersect(getTimeRange())) {
			throw (new TraceViewException("SegyTimeSeries: start after end time"));
		}
		long realStart = getTimeRange().getStart();
		int startPos = 0;
		int endPos = y.length;
		// System.out.println("endpos is " + endPos);
		// if start is after getstarttime, then we need to trim the beginning
		if (ti.getStart() > realStart) {
			// find the number of seconds between the 2 target times
			long desiredstart = ti.getStart();
			long thisstart = getTimeRange().getStart();
			long diff = desiredstart - thisstart;
			double samples = diff * getRateSampPerSec();
			startPos = (int) (Math.ceil(samples));
			// the reported start time must be adjusted
			realStart = realStart + new Double(startPos / getRateSampPerSec()).longValue();
		}
		// System.out.println("end is " + end + ". endtime is " + getEndTime());
		// if endis before getendtime, then we need to trim the end
		if (ti.getEnd() < (getTimeRange().getEnd())) {
			// find the number of seconds between the 2 target times
			long desiredend = ti.getEnd();
			// System.out.println("desiredend is " + desiredend);
			long thisend = getTimeRange().getEnd();
			// System.out.println("thisend is " + thisend);
			long diff = thisend - desiredend;
			// System.out.println("diff is " + diff);
			double samples = diff * getRateSampPerSec();
			// System.out.println("sam/sec is " + getRateSampPerSec());
			// System.out.println("samples are " + samples);
			endPos -= (int) (Math.ceil(samples));
		}
		// if start is before getstart time and end is after
		// getendtime return the entire array (the default settings
		// for start and end pos)
		// System.out.println("endPos - " + endPos + ". start - " + startPos);
		int count = 0;
		retAR = new int[endPos - startPos + 1];
		// retAR = new int[endPos + 1];
		for (int i = startPos; i <= endPos; i++) {
			logger.debug("i: " + i + " count: " + count + " retAR: " + retAR.length + " y: " + y.length);
			retAR[count] = y[i];
			count++;
			if (min > y[i]) {
				min = y[i];
			}
			if (max < y[i]) {
				max = y[i];
			}
		}
		logger.debug("SegyTimeSeries::getDataArray(TimeInterval ti) left");
		return retAR;
	}

	/**
	 * reads the segy file specified by the filename. No checks are made to be sure the file really
	 * is a segy file.
	 * 
	 * @throws FileNotFoundException
	 *             if the file cannot be found
	 * @throws IOException
	 *             if it happens
	 */
	public void read(File file) throws FileNotFoundException, IOException, TraceViewException {
		logger.debug("Read file");	
		read(file.getCanonicalPath());
	}

	/**
	 * reads the segy file specified by the filename. No checks are made to be sure the file really
	 * is a segy file.
	 * 
	 * @throws FileNotFoundException
	 *             if the file cannot be found
	 * @throws IOException
	 *             if it happens
	 */
	public void read(String filename) throws FileNotFoundException, IOException, TraceViewException {
		RandomAccessFile dis = new RandomAccessFile(filename, "r");
		//double bgn = System.currentTimeMillis();
		/*
		 * checkNeedToSwap(dis); skipHeader(dis);
		 */
		readHeader(dis);
		//double afterHead = System.currentTimeMillis();
		@SuppressWarnings("unused")	
		int num_bits = 4;
		if (data_form == 1) {
			num_bits = 4;
		}
		if (data_form == 0) {
			num_bits = 2;
		}
		logger.debug("SEGY File Length Check temporarily disabled.");
		/*
		 * if ((sampleLength != 32767 && segyFile.length() != sampleLength * num_bits + 240) ||
		 * (sampleLength == 32767 && segyFile.length() != num_samps * num_bits + 240)) { throw new
		 * IOException(segyFileName + " does not appear to be a segy file!"); }
		 */
		//double beforeData = System.currentTimeMillis();
		readData(dis);
		dis.close();
		//double afterData = System.currentTimeMillis();
	}

	/**
	 * Check the byte order by checking the date/time fields
	 */
	protected void checkNeedToSwap(RandomAccessFile dis, long pointer) throws IOException {
		bigEndian = true;
		dis.seek(156);
		int tyear = readShort(dis); /* 156 year of Start of trace */
		int tday = readShort(dis); /* 158 day of year at Start of trace */
		int thour = readShort(dis); /* 160 hour of day at Start of trace */
		int tmin = readShort(dis); /* 162 minute of hour at Start of trace */
		int tsec = readShort(dis); /* 164 second of minute at Start of trace */
		bigEndian = (tyear < 1900 || tyear > 3000 || tday < 0 || tday > 366 || thour < 0 || thour > 23 ||
				tmin < 0 || tmin > 59 || tsec < 0 || tsec > 59);
		dis.seek(pointer);
		return;
	}

	/**
	 * Reads the header from the given stream.
	 * @param dis the BufferedRandomAccessFile to skip over the header of.
	 */
	protected void skipHeader(RandomAccessFile dis) throws IOException {
		// skip forward 240 bytes.
		if (dis.skipBytes(240) != 240) {
			throw new IOException("could not read 240 bytes are start of segy file.");
		}
	}

	/**
	 * reads just the segy header specified by the filename. No checks are made to be sure the file
	 * really is a segy file.
	 */
	public void readHeader(String filename) throws FileNotFoundException, TraceViewException {
		RandomAccessFile dis = null;
		try {
			dis = new RandomAccessFile(filename, "r");
			readHeader(dis);
		} catch (IOException e) {
			throw new TraceViewException(e.toString());
		} finally {
			try {
				if (dis != null) {
					dis.close();
				}
			} catch (IOException e) {
				// do nothing
				logger.error("IOException:", e);	
			}
		}
	}

	/**
	 * reads the header from the given stream.
	 */
	protected void readHeader(RandomAccessFile dis) throws FileNotFoundException, IOException, TraceViewException {
		// TODO: probably need to replace a lot of this with bytebuffer calls to handle byte order
		checkNeedToSwap(dis, dis.getFilePointer());
		lineSeq = readInt(dis); /* 0 Sequence numbers within line */
		reelSeq = readInt(dis); /* 4 Sequence numbers within reel */
		event_number = readInt(dis); /* 8 Original field record number or trigger number */
		channel_number = readInt(dis); /*
										 * 12 Trace channel number within the original field record
										 */
		energySourcePt = readInt(dis); /* 16 X */
		cdpEns = readInt(dis); /* 20 X */
		traceInEnsemble = readInt(dis); /* 24 X */
		traceID = readShort(dis); /* 28 Trace identification code: seismic data = 1 */
		if (traceID != 1 && traceID != 2 && traceID != 3 && traceID != 4 &&  traceID != 5
				&& traceID != 6 && traceID != 7 && traceID != 8 && traceID != 9)
			throw new TraceViewException("Segy Format Exception");
		vertSum = readShort(dis); /* 30 X */
		horSum = readShort(dis); /* 32 X */
		dataUse = readShort(dis); /* 34 X */
		sourceToRecDist = readInt(dis); /* 36 X */
		recElevation = readInt(dis); /* 40 X */
		sourceSurfaceElevation = readInt(dis); /* 44 X */
		sourceDepth = readInt(dis); /* 48 X */
		datumElevRec = readInt(dis); /* 52 X */
		datumElevSource = readInt(dis); /* 56 X */
		sourceWaterDepth = readInt(dis); /* 60 X */
		recWaterDepth = readInt(dis); /* 64 X */
		elevationScale = readShort(dis); /* 68 Elevation Scaler: scale = 1 */
		coordScale = readShort(dis); /* 70 Coordinate Scaler: scale = 1 */
		sourceLongOrX = readInt(dis); /* 72 X */
		sourceLatOrY = readInt(dis); /* 76 X */
		recLongOrX = readInt(dis); /* 80 X */
		recLatOrY = readInt(dis); /* 84 X */
		coordUnits = readShort(dis); /* 88 Coordinate Units: = 2 (Lat/Long) */
		if (coordUnits != 1 && coordUnits != 2)
			throw new TraceViewException("Segy Format Exception");
		weatheringVelocity = readShort(dis); /* 90 X */
		subWeatheringVelocity = readShort(dis); /* 92 X */
		sourceUpholeTime = readShort(dis); /* 94 X */
		recUpholeTime = readShort(dis); /* 96 X */
		sourceStaticCor = readShort(dis); /* 98 X */
		recStaticCor = readShort(dis); /* 100 X */
		totalStatic = readShort(dis); /*
										 * 102 Total Static in MILLISECS added to Trace Start Time
										 * (lower 2 bytes)
										 */
		lagTimeA = readShort(dis); /* 104 X */
		lagTimeB = readShort(dis); /* 106 X */
		delay = readShort(dis); /* 108 X */
		muteStart = readShort(dis); /* 110 X */
		muteEnd = readShort(dis); /* 112 X */
		sampleLength = readShort(dis); /* 114 Number of samples in this trace (unless == 32767) */
		deltaSample = readShort(dis); /* 116 Sampling interval in MICROSECONDS (unless == 1) */
		gainType = readShort(dis); /* 118 Gain Type: 1 = Fixed Gain */
		if (gainType != 1 && gainType != 2 && gainType != 3 && gainType != 4)
			throw new TraceViewException("Segy Format Exception");
		gainConst = readShort(dis); /* 120 Gain of amplifier */
		initialGain = readShort(dis); /* 122 X */
		correlated = readShort(dis); /* 124 X */
		sweepStart = readShort(dis); /* 126 X */
		sweepEnd = readShort(dis); /* 128 X */
		sweepLength = readShort(dis); /* 130 X */
		sweepType = readShort(dis); /* 132 X */
		sweepTaperAtStart = readShort(dis); /* 134 X */
		sweepTaperAtEnd = readShort(dis); /* 136 X */
		taperType = readShort(dis); /* 138 X */
		aliasFreq = readShort(dis); /* 140 X */
		aliasSlope = readShort(dis); /* 142 X */
		notchFreq = readShort(dis); /* 144 X */
		notchSlope = readShort(dis); /* 146 X */
		lowCutFreq = readShort(dis); /* 148 X */
		hiCutFreq = readShort(dis); /* 150 X */
		lowCutSlope = readShort(dis); /* 152 X */
		hiCutSlope = readShort(dis); /* 154 X */
		year = readShort(dis); /* 156 year of Start of trace */
		if (year < 0 || year > 3000)
			throw new TraceViewException("Segy Format Exception");
		day = readShort(dis); /* 158 day of year at Start of trace */
		if (day < 0 || day > 366)
			throw new TraceViewException("Segy Format Exception");
		hour = readShort(dis); /* 160 hour of day at Start of trace */
		if (hour < 0 || hour > 23)
			throw new TraceViewException("Segy Format Exception");
		minute = readShort(dis); /* 162 minute of hour at Start of trace */
		if (minute < 0 || minute > 59)
			throw new TraceViewException("Segy Format Exception");
		second = readShort(dis); /* 164 second of minute at Start of trace */
		if (second < 0 || second > 59)
			throw new TraceViewException("Segy Format Exception");
		timeBasisCode = readShort(dis); /* 166 Time basis code: 2 = GMT */
		if (timeBasisCode != 1 && timeBasisCode != 2 && timeBasisCode != 3)
			throw new TraceViewException("Segy Format Exception");
		traceWeightingFactor = readShort(dis); /* 168 X */
		phoneRollPos1 = readShort(dis); /* 170 X */
		phoneFirstTrace = readShort(dis); /* 172 X */
		phoneLastTrace = readShort(dis); /* 174 X */
		gapSize = readShort(dis); /* 176 X */
		taperOvertravel = readShort(dis); /* 178 X */

		byte[] sevenBytes = new byte[7];
		byte[] fiveBytes = new byte[5];
		byte[] threeBytes = new byte[3];
		byte[] oneBytes = new byte[1];

		/*
		 * 180 Station Name code (5 chars + \0) dis.readFully(fiveBytes,0,5); station_name = new
		 * String(fiveBytes); dis.readFully(oneBytes,0,1); 186 Sensor Serial code (7 chars + \0)
		 * dis.readFully(sevenBytes,0,7); sensor_serial = new String(sevenBytes);
		 * dis.readFully(oneBytes,0,1); 194 Channel Name code (3 chars + \0)
		 * dis.readFully(threeBytes,0,3); channel_name = new String(threeBytes,0,3);
		 * dis.readFully(oneBytes,0,1);
		 */

		/* 180 Station Name code (5 chars + \0) */
		if (dis.read(fiveBytes) == 5) {
			station_name = new String(fiveBytes);
		}
		dis.read(oneBytes);
		/* 186 Sensor Serial code (7 chars + \0) */
		if (dis.read(sevenBytes) == 7) {
			sensor_serial = new String(sevenBytes);
		}
		dis.read(oneBytes);
		/* 194 Channel Name code (3 chars + \0) */
		if (dis.read(threeBytes) == 3) {
			channel_name = new String(threeBytes);
		}
		dis.read(oneBytes);
		totalStaticHi = readShort(dis); /*
											 * 198 Total Static in MILLISECS added to Trace Start
											 * Time (high 2 bytes)
											 */
		samp_rate = readInt(dis); /* 200 Sample interval in MICROSECS as a 32 bit integer */
		data_form = readShort(dis); /* 204 Data Format flag: 0=16 bit, 1=32 bit integer */
		m_secs = readShort(dis); /* 206 MILLISECONDS of seconds of Start of trace */
		trigyear = readShort(dis); /* 208 year of Trigger time */
		trigday = readShort(dis); /* 210 day of year at Trigger time */
		trighour = readShort(dis); /* 212 hour of day at Trigger time */
		trigminute = readShort(dis); /* 214 minute of hour at Trigger time */
		trigsecond = readShort(dis); /* 216 second of minute at Trigger time */
		trigmills = readShort(dis); /* 218 MILLISECONDS of seconds of Trigger time */
		scale_fac = dis.readFloat(); /* 220 Scale Factor (IEEE 32 bit float) */
		inst_no = readShort(dis); /* 224 Instrument Serial Number */
		not_to_be_used = readShort(dis); /* 226 X */
		num_samps = readInt(dis);
		/*
		 * 228 Number of Samples as a 32 bit integer (when sampleLength == 32767)
		 */
		max = readInt(dis); /* 232 Maximum value in Counts */
		min = readInt(dis); /* 236 Minimum value in Counts */
	}

	/**
	 * Take in 4 bytes from a random access file and parse as an integer.
	 * This is a custom implementation of RandomAccessFile's readInt() that handles little endianness
	 * (SegY files can be either big or little endian)
	 * @param raf SegY file being read in
	 * @return integer parsed in
	 * @throws IOException If the raf cannot be read from
	 */
	private int readInt(RandomAccessFile raf) throws IOException {
		int ch1 = raf.readUnsignedByte();
		int ch2 = raf.readUnsignedByte();
		int ch3 = raf.readUnsignedByte();
		int ch4 = raf.readUnsignedByte();


		if (bigEndian) {
			return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
		} else {
			return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
		}
	}

	/**
	 * Take in 2 bytes from a random access file and parse as a short.
	 * This is a custom implementation of RandomAccessFile's readShort() that handles little
	 * endianness
	 * (SegY files can be either big or little endian)
	 * @param raf SegY file being read in
	 * @return short parsed in
	 * @throws IOException If the raf cannot be read from
	 */
	private short readShort(RandomAccessFile raf) throws IOException {
		int ch1 = raf.readUnsignedByte();
		int ch2 = raf.readUnsignedByte();

		if (bigEndian) {
			return (short) ((ch1 << 8) + ch2);
		} else {
			return (short) ((ch2 << 8) + ch1);
		}
	}


	/**
	 * read the data portion of the given File
	 */
	protected void readData(RandomAccessFile fis) throws IOException {
		if (sampleLength != 32767) {
			y = new int[sampleLength];
		} else {
			y = new int[num_samps];
		}
		int numAdded = 0;
		int numRead;
		int i;
		byte[] buf = new byte[4096]; // buf length must be == 0 % 4 or 2 based on data_form
		// and for efficiency, should be
		// a multiple of the disk sector size
		while ((numRead = fis.read(buf)) > 0) {
			if ((data_form == 1 && numRead % 4 != 0) || (data_form == 0 && numRead % 2 != 0)) {
				throw new EOFException();
			}
			i = 0;
			if (data_form == 1) {
				// 32 bit ints
				while (i < numRead) {
					if (bigEndian) {
						y[numAdded++] = ((buf[i++] & 0xff) << 24) + ((buf[i++] & 0xff) << 16) +
								((buf[i++] & 0xff) << 8) + (( buf[i++] & 0xff));
					} else {
						y[numAdded++] = ((buf[i + 3] & 0xff) << 24) + ((buf[i + 2] & 0xff) << 16) +
								((buf[i + 1] & 0xff) << 8) + ((buf[i] & 0xff));
						i += 4;
					}
				}
			} else {
				// 16 bit shorts
				while (i < numRead) {
					if (bigEndian) {
						y[numAdded++] = ((buf[i++] & 0xff) << 8) + ((buf[i++] & 0xff));
					} else {
						y[numAdded++] = ((buf[i + 1] & 0xff) << 8) + ((buf[i] & 0xff));
						i += 2;
					}
				}
			}
		}
	}

	/**
	 * @return a TimeInterval based on start and tot_time
	 */
	public TimeInterval getTimeRange() {
		if (year < 100) {
			if (year < 70) {
				year += 2000;
			} else {
				year += 1900;
			}
		}
		long start = TimeInterval.getTime(year, day, hour, minute, second, m_secs);
		int samples = getNumSamples();
		// System.out.println("num samples is " + samples);
		double rate = getRateSampPerSec();
		// System.out.println("samp/sec " + rate);
		double tot_time = (double) (samples - 1) / rate;
		// System.out.println("tot_time " + tot_time );
		return new TimeInterval(start, new Double(start + tot_time).longValue());
	}

	/**
	 * @return number of samples either sampleLength or num_samps depending on
	 *         sampleLength's value
	 */
	public int getNumSamples() {
		int samples;
		// get the number of samples
		if (sampleLength != 32767) {
			samples = sampleLength;
		} else {
			samples = num_samps;
		}
		return samples;
	}

	/**
	 * get the sample rate as samples/second
	 */
	public double getRateSampPerSec() {
		double rate;
		if (deltaSample != 1) {
			rate = 1000000.0 / ((double) deltaSample);
		} else {
			rate = 1000000.0 / ((double) samp_rate);
		}
		return rate;
	}

	/**
	 * get the sample rate as microSec/sample
	 */
	public double getRateMicroSampPerSec() {
		double rate;
		if (deltaSample != 1) {
			rate = (double) deltaSample;
		} else {
			rate = (double) samp_rate;
		}
		return rate;
	}

	private void initOInfo() {
		setOInfo = true;
		if (!((station_name.trim()).equals(""))) {
			oStation = station_name.trim();
			if (inst_no != 0)
				oNetwork = Integer.toString((int) inst_no);
			else {
				oNetwork = sensor_serial.trim();
			}
		} else {
			if (inst_no != 0) {
				oStation = Integer.toString((int) inst_no);
				oNetwork = sensor_serial.trim();
			} else {
				oStation = sensor_serial.trim();
				oNetwork = "";
			}
		}
		oChannel = Integer.toString(channel_number);
		/*
		 * if (!(channel_name.trim().equals(""))) oChannel = channel_name; else oChannel =
		 * Integer.toString(channel_number);
		 */
		return;
	}

	/**
	 * @return the network code?
	 */
	public String getNetwork() {
		if (!setOInfo) {
			initOInfo();
		}
		return oNetwork;
		// if (!((station_name.trim()).equals("")))
		// return (new String(station_name));
		// else
		// return (new String(sensor_serial));
	}

	/**
	 * @return the station code?
	 */
	public String getStation() {
		if (!setOInfo) {
			initOInfo();
		}
		return oStation;
		// return (Integer.toString((int) inst_no));
	}

	/**
	 * @return the channel code
	 */
	public String getChannel() {
		if (!setOInfo) {
			initOInfo();
		}
		return oChannel;
		// return (Integer.toString(channel_number));
	}

	/**
	 * writes this object out as a segy file.
	 */
	public void write(String filename) throws IOException {
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
		dos.close();
		throw new IOException("SEGY write not yet implmented");
	}
}
