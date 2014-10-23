package com.isti.traceview.data;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;

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
	private static Logger lg = Logger.getLogger(SegyTimeSeries.class);
	static final boolean doRepacketize = false;

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

	public int[] y;

	/**
	 * Constructor declaration
	 * 
	 * @see
	 */
	public SegyTimeSeries() {
		lg.debug("SegyTimeSeries::SegyTimeSeries() entered");
		lg.debug("SegyTimeSeries::SegyTimeSeries() left");
	}

	/**
	 * Method declaration
	 * 
	 * @return
	 * @see
	 */
	public int[] getDataArray() {
		lg.debug("SegyTimeSeries::getDataArray() entered");
		lg.debug("SegyTimeSeries::getDataArray() left");
		return y;
	}

	/**
	 * returns a vector with the time of the first data point. min and max values, and the data
	 * array as an int array
	 */
	public int[] getDataArray(TimeInterval ti) throws TraceViewException {
		lg.debug("SegyTimeSeries::getDataArray(TimeInterval ti) entered");
		int retAR[];
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
			lg.debug("i: " + i + " count: " + count + " retAR: " + retAR.length + " y: " + y.length);
			retAR[count] = y[i];
			count++;
			if (min > y[i]) {
				min = y[i];
			}
			if (max < y[i]) {
				max = y[i];
			}
		}
		lg.debug("SegyTimeSeries::getDataArray(TimeInterval ti) left");
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
		lg.debug("SegyTimeSeries::read(File file) entered");
		read(file.getCanonicalPath());
		lg.debug("SegyTimeSeries::read(File file) left");
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
		lg.debug("SegyTimeSeries::read() entered");
		BufferedRandomAccessFile dis = new BufferedRandomAccessFile(filename, "r");
		double bgn = System.currentTimeMillis();
		/*
		 * checkNeedToSwap(dis); skipHeader(dis);
		 */
		readHeader(dis);
		double afterHead = System.currentTimeMillis();
		int num_bits = 4;
		if (data_form == 1) {
			num_bits = 4;
		}
		if (data_form == 0) {
			num_bits = 2;
		}
		lg.debug("SEGY File Length Check temporarily disabled.");
		/*
		 * if ((sampleLength != 32767 && segyFile.length() != sampleLength * num_bits + 240) ||
		 * (sampleLength == 32767 && segyFile.length() != num_samps * num_bits + 240)) { throw new
		 * IOException(segyFileName + " does not appear to be a segy file!"); }
		 */
		double beforeData = System.currentTimeMillis();
		readData(dis);
		dis.close();
		double afterData = System.currentTimeMillis();
		lg.debug("SegyTimeSeries::read() left");
	}

	/**
	 * Check the byte order by checking the date/time fields
	 */
	protected void checkNeedToSwap(BufferedRandomAccessFile dis, long pointer) throws IOException {
		dis.seek(156);
		int tyear = dis.readShort(); /* 156 year of Start of trace */
		int tday = dis.readShort(); /* 158 day of year at Start of trace */
		int thour = dis.readShort(); /* 160 hour of day at Start of trace */
		int tmin = dis.readShort(); /* 162 minute of hour at Start of trace */
		int tsec = dis.readShort(); /* 164 second of minute at Start of trace */
		if (tyear < 1900 | tyear > 3000)
			dis.order(BufferedRandomAccessFile.LITTLE_ENDIAN);
		if (tday < 0 | tday > 366)
			dis.order(BufferedRandomAccessFile.LITTLE_ENDIAN);
		if (thour < 0 | thour > 23)
			dis.order(BufferedRandomAccessFile.LITTLE_ENDIAN);
		if (tmin < 0 | tmin > 59)
			dis.order(BufferedRandomAccessFile.LITTLE_ENDIAN);
		if (tsec < 0 | tsec > 59)
			dis.order(BufferedRandomAccessFile.LITTLE_ENDIAN);
		dis.seek(pointer);
		return;
	}

	/**
	 * reads the header from the given stream.
	 */
	protected void skipHeader(BufferedRandomAccessFile dis) throws IOException {
		// skip forware 240 bytes.
		if (dis.skipBytes(240) != 240) {
			throw new IOException("could not read 240 bytes are start of segy file.");
		}
	}

	/**
	 * reads just the segy header specified by the filename. No checks are made to be sure the file
	 * really is a segy file.
	 */
	public void readHeader(String filename) throws FileNotFoundException, TraceViewException {
		lg.debug("SegyTimeSeries::readHeader(String filename) entered");
		BufferedRandomAccessFile dis = null;
		try {
			dis = new BufferedRandomAccessFile(filename, "r");
			readHeader(dis);
		} catch (IOException e) {
			throw new TraceViewException(e.toString());
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
				// do nothing
			}
		}
		lg.debug("SegyTimeSeries::readHeader(String filename) left");
	}

	/**
	 * reads the header from the given stream.
	 */
	protected void readHeader(BufferedRandomAccessFile dis) throws FileNotFoundException, IOException, TraceViewException {
		checkNeedToSwap(dis, dis.getFilePointer());
		lg.debug("SegyTimeSeries::readHeader(BufferedRandomAccessFile dis) entered");
		lineSeq = dis.readInt(); /* 0 Sequence numbers within line */
		reelSeq = dis.readInt(); /* 4 Sequence numbers within reel */
		event_number = dis.readInt(); /* 8 Original field record number or trigger number */
		channel_number = dis.readInt(); /*
										 * 12 Trace channel number within the original field record
										 */
		energySourcePt = dis.readInt(); /* 16 X */
		cdpEns = dis.readInt(); /* 20 X */
		traceInEnsemble = dis.readInt(); /* 24 X */
		traceID = dis.readShort(); /* 28 Trace identification code: seismic data = 1 */
		if (traceID != 1 && traceID != 2 && traceID != 3 && traceID != 4 && traceID != 5 && traceID != 6 && traceID != 7 && traceID != 8
				&& traceID != 9)
			throw new TraceViewException("Segy Format Exception");
		vertSum = dis.readShort(); /* 30 X */
		horSum = dis.readShort(); /* 32 X */
		dataUse = dis.readShort(); /* 34 X */
		sourceToRecDist = dis.readInt(); /* 36 X */
		recElevation = dis.readInt(); /* 40 X */
		sourceSurfaceElevation = dis.readInt(); /* 44 X */
		sourceDepth = dis.readInt(); /* 48 X */
		datumElevRec = dis.readInt(); /* 52 X */
		datumElevSource = dis.readInt(); /* 56 X */
		sourceWaterDepth = dis.readInt(); /* 60 X */
		recWaterDepth = dis.readInt(); /* 64 X */
		elevationScale = dis.readShort(); /* 68 Elevation Scaler: scale = 1 */
		coordScale = dis.readShort(); /* 70 Coordinate Scaler: scale = 1 */
		sourceLongOrX = dis.readInt(); /* 72 X */
		sourceLatOrY = dis.readInt(); /* 76 X */
		recLongOrX = dis.readInt(); /* 80 X */
		recLatOrY = dis.readInt(); /* 84 X */
		coordUnits = dis.readShort(); /* 88 Coordinate Units: = 2 (Lat/Long) */
		if (coordUnits != 1 && coordUnits != 2)
			throw new TraceViewException("Segy Format Exception");
		weatheringVelocity = dis.readShort(); /* 90 X */
		subWeatheringVelocity = dis.readShort(); /* 92 X */
		sourceUpholeTime = dis.readShort(); /* 94 X */
		recUpholeTime = dis.readShort(); /* 96 X */
		sourceStaticCor = dis.readShort(); /* 98 X */
		recStaticCor = dis.readShort(); /* 100 X */
		totalStatic = dis.readShort(); /*
										 * 102 Total Static in MILLISECS added to Trace Start Time
										 * (lower 2 bytes)
										 */
		lagTimeA = dis.readShort(); /* 104 X */
		lagTimeB = dis.readShort(); /* 106 X */
		delay = dis.readShort(); /* 108 X */
		muteStart = dis.readShort(); /* 110 X */
		muteEnd = dis.readShort(); /* 112 X */
		sampleLength = dis.readShort(); /* 114 Number of samples in this trace (unless == 32767) */
		deltaSample = dis.readShort(); /* 116 Sampling interval in MICROSECONDS (unless == 1) */
		gainType = dis.readShort(); /* 118 Gain Type: 1 = Fixed Gain */
		if (gainType != 1 && gainType != 2 && gainType != 3 && gainType != 4)
			throw new TraceViewException("Segy Format Exception");
		gainConst = dis.readShort(); /* 120 Gain of amplifier */
		initialGain = dis.readShort(); /* 122 X */
		correlated = dis.readShort(); /* 124 X */
		sweepStart = dis.readShort(); /* 126 X */
		sweepEnd = dis.readShort(); /* 128 X */
		sweepLength = dis.readShort(); /* 130 X */
		sweepType = dis.readShort(); /* 132 X */
		sweepTaperAtStart = dis.readShort(); /* 134 X */
		sweepTaperAtEnd = dis.readShort(); /* 136 X */
		taperType = dis.readShort(); /* 138 X */
		aliasFreq = dis.readShort(); /* 140 X */
		aliasSlope = dis.readShort(); /* 142 X */
		notchFreq = dis.readShort(); /* 144 X */
		notchSlope = dis.readShort(); /* 146 X */
		lowCutFreq = dis.readShort(); /* 148 X */
		hiCutFreq = dis.readShort(); /* 150 X */
		lowCutSlope = dis.readShort(); /* 152 X */
		hiCutSlope = dis.readShort(); /* 154 X */
		year = dis.readShort(); /* 156 year of Start of trace */
		if (year < 0 || year > 3000)
			throw new TraceViewException("Segy Format Exception");
		day = dis.readShort(); /* 158 day of year at Start of trace */
		if (day < 0 || day > 366)
			throw new TraceViewException("Segy Format Exception");
		hour = dis.readShort(); /* 160 hour of day at Start of trace */
		if (hour < 0 || hour > 23)
			throw new TraceViewException("Segy Format Exception");
		minute = dis.readShort(); /* 162 minute of hour at Start of trace */
		if (minute < 0 || minute > 59)
			throw new TraceViewException("Segy Format Exception");
		second = dis.readShort(); /* 164 second of minute at Start of trace */
		if (second < 0 || second > 59)
			throw new TraceViewException("Segy Format Exception");
		timeBasisCode = dis.readShort(); /* 166 Time basis code: 2 = GMT */
		if (timeBasisCode != 1 && timeBasisCode != 2 && timeBasisCode != 3)
			throw new TraceViewException("Segy Format Exception");
		traceWeightingFactor = dis.readShort(); /* 168 X */
		phoneRollPos1 = dis.readShort(); /* 170 X */
		phoneFirstTrace = dis.readShort(); /* 172 X */
		phoneLastTrace = dis.readShort(); /* 174 X */
		gapSize = dis.readShort(); /* 176 X */
		taperOvertravel = dis.readShort(); /* 178 X */

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
		totalStaticHi = dis.readShort(); /*
											 * 198 Total Static in MILLISECS added to Trace Start
											 * Time (high 2 bytes)
											 */
		samp_rate = dis.readInt(); /* 200 Sample interval in MICROSECS as a 32 bit integer */
		data_form = dis.readShort(); /* 204 Data Format flag: 0=16 bit, 1=32 bit integer */
		m_secs = dis.readShort(); /* 206 MILLISECONDS of seconds of Start of trace */
		trigyear = dis.readShort(); /* 208 year of Trigger time */
		trigday = dis.readShort(); /* 210 day of year at Trigger time */
		trighour = dis.readShort(); /* 212 hour of day at Trigger time */
		trigminute = dis.readShort(); /* 214 minute of hour at Trigger time */
		trigsecond = dis.readShort(); /* 216 second of minute at Trigger time */
		trigmills = dis.readShort(); /* 218 MILLISECONDS of seconds of Trigger time */
		scale_fac = dis.readFloat(); /* 220 Scale Factor (IEEE 32 bit float) */
		inst_no = dis.readShort(); /* 224 Instrument Serial Number */
		not_to_be_used = dis.readShort(); /* 226 X */
		num_samps = dis.readInt();
		/*
		 * 228 Number of Samples as a 32 bit integer (when sampleLength == 32767)
		 */
		max = dis.readInt(); /* 232 Maximum value in Counts */
		min = dis.readInt(); /* 236 Minimum value in Counts */
		lg.debug("SegyTimeSeries::readHeader(BufferedRandomAccessFile dis) left");
	}

	/**
	 * read the data portion of the given File
	 */
	protected void readData(BufferedRandomAccessFile fis) throws IOException {
		lg.debug("SegyTimeSeries::readData(BufferedRandomAccessFile fis) entered");
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
		boolean bigEndian = fis.isBE();
		while ((numRead = fis.read(buf)) > 0) {
			if ((data_form == 1 && numRead % 4 != 0) || (data_form == 0 && numRead % 2 != 0)) {
				throw new EOFException();
			}
			i = 0;
			if (data_form == 1) {
				// 32 bit ints
				while (i < numRead) {
					if (bigEndian) {
						y[numAdded++] = ((buf[i++] & 0xff) << 24) + ((buf[i++] & 0xff) << 16) + ((buf[i++] & 0xff) << 8) + ((buf[i++] & 0xff) << 0);
					} else {
						y[numAdded++] = ((buf[i + 3] & 0xff) << 24) + ((buf[i + 2] & 0xff) << 16) + ((buf[i + 1] & 0xff) << 8)
								+ ((buf[i] & 0xff) << 0);
						i += 4;
					}
				}
			} else {
				// 16 bit shorts
				while (i < numRead) {
					if (bigEndian) {
						y[numAdded++] = ((buf[i++] & 0xff) << 8) + ((buf[i++] & 0xff) << 0);
					} else {
						y[numAdded++] = ((buf[i + 1] & 0xff) << 8) + ((buf[i] & 0xff) << 0);
						i += 2;
					}
				}
			}
		}
		lg.debug("SegyTimeSeries::readData(BufferedRandomAccessFile fis) left");
	}

	/**
	 * Method declaration
	 * 
	 * @return
	 * @see
	 */
	public TimeInterval getTimeRange() {
		lg.debug("SegyTimeSeries::getTimeRange() entered");
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
		lg.debug("SegyTimeSeries::getTimeRange() left");
		return new TimeInterval(start, new Double(start + tot_time).longValue());
	}

	/**
	 * Method declaration
	 * 
	 * @return
	 * @see
	 */
	public int getNumSamples() {
		lg.debug("SegyTimeSeries::getNumSamples() entered");
		int samples;
		// get the number of samples
		if (sampleLength != 32767) {
			samples = sampleLength;
		} else {
			samples = num_samps;
		}
		lg.debug("SegyTimeSeries::getNumSamples() left");
		return samples;
	}

	/**
	 * get the sample rate as samples/second
	 */
	public double getRateSampPerSec() {
		lg.debug("SegyTimeSeries::getRateSampPerSec() entered");
		double rate;
		if (deltaSample != 1) {
			rate = 1000000.0 / ((double) deltaSample);
		} else {
			rate = 1000000.0 / ((double) samp_rate);
		}
		lg.debug("SegyTimeSeries::getRateSampPerSec() left");
		return rate;
	}

	/**
	 * get the sample rate as microSec/sample
	 */
	public double getRateMicroSampPerSec() {
		lg.debug("SegyTimeSeries::getRateMicroSampPerSec() entered");
		double rate;
		if (deltaSample != 1) {
			rate = (double) deltaSample;
		} else {
			rate = (double) samp_rate;
		}
		lg.debug("SegyTimeSeries::getRateMicroSampPerSec() left");
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
	 * Method declaration
	 * 
	 * @return
	 * @see
	 */
	public String getNetwork() {
		if (!setOInfo) {
			initOInfo();
		}
		lg.debug("SegyTimeSeries::getNetwork() entered");
		lg.debug("SegyTimeSeries::getNetwork() left");
		return oNetwork;
		// if (!((station_name.trim()).equals("")))
		// return (new String(station_name));
		// else
		// return (new String(sensor_serial));
	}

	/**
	 * Method declaration
	 * 
	 * @return
	 * @see
	 */
	public String getStation() {
		if (!setOInfo) {
			initOInfo();
		}
		lg.debug("SegyTimeSeries::getStation() entered");
		lg.debug("SegyTimeSeries::getStation() left");
		return oStation;
		// return (Integer.toString((int) inst_no));
	}

	/**
	 * Method declaration
	 * 
	 * @return
	 * @see
	 */
	public String getChannel() {
		if (!setOInfo) {
			initOInfo();
		}
		lg.debug("SegyTimeSeries::getChannel() entered");
		lg.debug("SegyTimeSeries::getChannel() left");
		return oChannel;
		// return (Integer.toString(channel_number));
	}

	/**
	 * writes this object out as a segy file.
	 */
	public void write(String filename) throws FileNotFoundException, IOException {
		lg.debug("SegyTimeSeries::write(String filename) entered");
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
		dos.close();
		lg.debug("SegyTimeSeries::write(String filename) left");
		throw new IOException("SEGY write not yet implmented");
	}

	/**
	 * just for testing. Reads the filename given as the argument and writes it back out as
	 * "tempsegyfile".
	 */
	public static void main(String[] args) {
		lg.debug("SegyTimeSeries::main(String[] args) entered");
		SegyTimeSeries data = new SegyTimeSeries();
		if (args.length != 1) {
			System.out.println("Usage: java SegyTimeSeries sourcefile ");
			System.exit(1);
		}
		try {
			data.read(args[0]);
			System.out.println("Done reading");
		} catch (FileNotFoundException e) {
			System.out.println("File " + args[0] + " doesn't exist.");
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		} catch (TraceViewException e) {
			System.out.println("TraceViewException: " + e.getMessage());
		}
		lg.debug("SegyTimeSeries::main(String[] args) left");
	}
}