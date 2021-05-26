package com.isti.traceview.common;

import com.isti.traceview.TraceView;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.log4j.Logger;

/**
 * Class to represent interval of time
 * 
 * @author Max Kokoulin
 */

public class TimeInterval {
	/**
	 * Enumeration for string time representation formats. We use formats with different accuracy
	 * for convinient date plotting in different situations
	 */
	public enum DateFormatType {
		/**
		 * format yyyy,DDD,HH:mm:ss.SSS
		 */
		DATE_FORMAT_NORMAL,

		/**
		 * format yyyy,DDD,HH:mm:ss
		 */
		DATE_FORMAT_MIDDLE,

		/**
		 * format yyyy,DDD,HH:mm
		 */
		DATE_FORMAT_LONG
	}

	private static final Logger logger = Logger.getLogger(TimeInterval.class);

	public static final ThreadLocal<SimpleDateFormat> df = ThreadLocal.withInitial(() -> {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy,DDD,HH:mm:ss.SSS");
		sdf.setTimeZone(TraceView.timeZone);
		return sdf;
	});
	public static ThreadLocal<SimpleDateFormat> df_middle = ThreadLocal.withInitial(() -> {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy,DDD,HH:mm:ss");
		sdf.setTimeZone(TraceView.timeZone);
		return sdf;
	});
	public static ThreadLocal<SimpleDateFormat> df_long = ThreadLocal.withInitial(() -> {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy,DDD,HH:mm");
		sdf.setTimeZone(TraceView.timeZone);
		return sdf;
	});


	private long startTime;
	private long endTime;

	/**
	 * Default constructor
	 */
	public TimeInterval() {
		startTime = Long.MAX_VALUE;
		endTime = Long.MIN_VALUE;
	}

	/**
	 * Constructor from Java standard time values
	 * 
	 * @param startTime
	 *            start time of interval in milliseconds (Java standard time)
	 * @param endTime
	 *            end time of interval in milliseconds (Java standard time)
	 */
	public TimeInterval(long startTime, long endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * Constructor from Java {@link Date} values
	 * 
	 * @param startTime
	 *            startTime start time of interval
	 * @param endTime
	 *            end time of interval
	 */
	public TimeInterval(Date startTime, Date endTime) {
		this(startTime.getTime(), endTime.getTime());
	}

	/**
	 * Extends time interval
	 * 
	 * @param date
	 *            new end value
	 */
	public void setMaxValue(Date date) {
		long newVal = date.getTime();
		if (newVal > endTime) {
			endTime = newVal;
		}
	}

	/**
	 * Extends time interval
	 * 
	 * @param date
	 *            new start value
	 */
	public void setMinValue(Date date) {
		long newVal = date.getTime();
		if (newVal < startTime) {
			startTime = newVal;
		}
	}

	/**
	 * Getter of startTime
	 * 
	 * @return start time of interval
	 */
	public Date getStartTime() {
		Date dateStart = new Date(startTime);
		return dateStart;
	}

	/**
	 * Getter of startTime
	 * 
	 * @return start time of interval in Java standard time form
	 */
	public long getStart() {
		return startTime;
	}

	/**
	 * Get the start time as Instant
	 * @return start time of interval as converted to Instant
	 */
	public Instant getStartInstant() {
		return Instant.ofEpochMilli(startTime);
	}



	/**
	 * Getter of endTime
	 * 
	 * @return end time of interval
	 */
	public Date getEndTime() {
		Date dateEnd = new Date(endTime);
		return dateEnd;
	}

	/**
	 * Getter of endTime
	 * 
	 * @return end time of interval in Java standard time form
	 */
	public long getEnd() {
		return endTime;
	}

	/**
	 * Get the end time as Instant
	 * @return end time of interval as converted to Instant
	 */
	public Instant getEndInstant() {
		return Instant.ofEpochMilli(endTime);
	}


	/**
	 * @return duration of interval in milliseconds
	 */
	public long getDuration() {
		return endTime - startTime;
	}

	/**
	 * @param date
	 *            time to test
	 * @return flag if this interval contains given time
	 */
	public boolean isContain(Date date) {
		return isContain(date.getTime());
	}

	/**
	 * @param date
	 *            time to test in Java standard time form
	 * @return flag if this interval contains given time
	 */
	public boolean isContain(long date) {
		return (startTime <= date && date <= endTime);
	}

	/**
	 * @param ti
	 *            time interval to test
	 * @return flag if this interval contains given one
	 */
	public boolean isContain(TimeInterval ti) {
		return (startTime <= ti.getStart() && endTime >= ti.getEnd());
	}

	/**
	 * @param range
	 *            time interval to test
	 * @return flag if this interval intersects with given one
	 */
	public boolean isIntersect(TimeInterval range) {
		if (range.getStartTime() == null || range.getEndTime() == null || getStartTime() == null || getEndTime() == null) {
			return true;
		} else {
			return !((startTime >= range.getEndTime().getTime() && endTime >= range.getEndTime().getTime())
					|| (startTime <= range.getStartTime().getTime() && endTime <= range.getStartTime().getTime()));
		}
	}

	/**
	 * String representation of time interval in the debugging purposes
	 */
	public String toString() {
		return "start time: " + formatDate(getStartTime(), DateFormatType.DATE_FORMAT_NORMAL) + ", end time: "
				+ formatDate(getEndTime(), DateFormatType.DATE_FORMAT_NORMAL);
	}

	/**
	 * String representation of this time interval's duration
	 * 
	 * @see TimeInterval#getStringDiff
	 */
	public String convert() {
		return TimeInterval.getStringDiff(getDuration());
	}

	/**
	 * String representation of duration in seconds (if duration less then hour), hours (if duration less then day) or decimal days
	 */
	public static String getStringDiff(long duration) {
		logger.debug("duration = " + duration);	
		String ret = "";
		if (duration < 0) {
			duration = -duration;
			ret = "-";
		} else {
			ret = "+";
		}
		if(duration < 86400000) {
			if (duration < 3600000) {
				double sec = (double) duration / 1000;
				ret = ret + sec + " s";
			} else {
				Double h = (double) duration / 3600000;

				ret = ret + new DecimalFormat("#######.###").format(h) + " h";
			}
		} else {
			Double days = (double) duration / 86400000;
			ret = ret + new DecimalFormat("#######.###").format(days) + " d";
		}
		return ret;
	}

	/**
	 * Intersect two time intervals
	 * 
	 * @return time interval which is intersection of two given time intervals, or null
	 */
	public static TimeInterval getIntersect(TimeInterval ti1, TimeInterval ti2) {
		if (ti1 == null || ti2 == null)
			return null;
		long start = Math.max(ti1.getStart(), ti2.getStart());
		long end = Math.min(ti1.getEnd(), ti2.getEnd());
		if (end > start) {
			return new TimeInterval(new Date(start), new Date(end));
		} else {
			return null;
		}
	}

	/**
	 * Aggregate two time intervals
	 * 
	 * @return time interval which aggregate two given time intervals, or null
	 */
	public static TimeInterval getAggregate(TimeInterval ti1, TimeInterval ti2) {
		if (ti1 == null || ti2 == null)
			return null;
		long start = Math.min(ti1.getStart(), ti2.getStart());
		long end = Math.max(ti1.getEnd(), ti2.getEnd());
		if (end > start) {
			TimeInterval TI = new TimeInterval(new Date(start), new Date(end));
			return TI;
		} else {
			return null;
		}
	}

	/**
	 * Constructs GregorianCalendar from integer values
	 * 
	 * @param year the year
	 * @param jday the julian day
	 * @param hour_of_day the hour
	 * @param minute the minute
	 * @param second the second
	 * @param millisecond the millisecond
	 * @return time in GregorianCalendar form
	 */
	public static long getTime(int year, int jday, int hour_of_day, int minute, int second, int millisecond) {
		Calendar cal = new GregorianCalendar(TraceView.timeZone);
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.DAY_OF_YEAR, jday);
		cal.set(Calendar.HOUR_OF_DAY, hour_of_day);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		cal.set(Calendar.MILLISECOND, millisecond);
		return cal.getTimeInMillis();
	}

	/**
	 * Parse string to get date according given date format
	 * 
	 * @param date
	 *            string representation of date
	 * @param type
	 *            date format
	 * @return parsed date
	 */
	public static Date parseDate(String date, DateFormatType type) {
		Date ret = null;
		try {
			switch (type) {
			case DATE_FORMAT_NORMAL:
				ret = df.get().parse(date);
				break;
			case DATE_FORMAT_MIDDLE:
				ret = df_middle.get().parse(date);
				break;
			case DATE_FORMAT_LONG:
				ret = df_long.get().parse(date);
				break;
			default:
				logger.error("Wrong date format type: " + type);
			}
		} catch (ParseException e) {
			logger.error("Cant parse date from string " + date, e);
		}
		return ret;
	}

	/**
	 * Gets string representation of date according given date format
	 * 
	 * @param date
	 *            date to process
	 * @param type
	 *            date format
	 * @return string representation of date
	 */
	public static String formatDate(Date date, DateFormatType type) {
		switch (type) {
		case DATE_FORMAT_NORMAL:
			return df.get().format(date);
		case DATE_FORMAT_MIDDLE:
			return df_middle.get().format(date);
		case DATE_FORMAT_LONG:
			return df_long.get().format(date);
		default:
			logger.error("Wrong date format type: " + type);
			return null;
		}
	}
}
