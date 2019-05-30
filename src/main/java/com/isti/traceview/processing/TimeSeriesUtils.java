package com.isti.traceview.processing;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

/**
 * Contains static methods for grabbing data from miniSEED files
 * and some very basic timeseries processing tools (i.e., decimation)
 *
 * @author akearns
 */
public class TimeSeriesUtils {


  /**
   * Interval for data that has been sampled at 1 Hz in milliseconds
   */
  public final static long ONE_HZ_INTERVAL = 1000L;

  /**
   * Sample rate of a 1 Hz sample, in Hz, as a double (that is, 1.0)
   */
  public final static double ONE_HZ = 1.0;

  // divide by this to go from nanoseconds to milliseconds
  public static final int TO_MILLI_FACTOR = 1000000;

  public static final ThreadLocal<SimpleDateFormat> DATE_TIME_FORMAT =
      ThreadLocal.withInitial(() -> {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.DDD.HH:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
      });

  public static String formatEpochMillis(long millis) {
    return DATE_TIME_FORMAT.get().format(Date.from(Instant.ofEpochMilli(millis)));
  }

  /**
   * Initial driver for the decimation utility
   * which takes a timeseries of unknown rate and
   * runs downsampling to convert it to a target
   * frequency of a 1Hz interval.
   *
   * @param data The timeseries to be decimated
   * @param source The source frequency as interval between samples (milliseconds)
   * @return A timeseries decimated to the correct frequency
   */
  public static double[] decimate(double[] data, long source, long target) {

    // a sample lower than 1Hz frq has longer time between samples
    // since it's an inverse relationship and all
    if (source >= target) {
      // if data is too low-frequency to decimate, do nothing
      return data;
    }

    // find what the change in size is going to be
    long gcd = euclidGCD(source, target);
    // conversion up- and down-factors
    // (upsample by target, downsample by source)
    // cast is valid because any discrete interval
    // from 1Hz and up is already expressable
    // as an int
    int upf = (int) (source / gcd);
    int dnf = (int) (target / gcd);

    double higherFreq = (1. / source) * upf * ONE_HZ_INTERVAL;
    double lowerFreq = (1. / target) * ONE_HZ_INTERVAL / 2;
    // nyquist rate of downsampled data

    // one valid sample rate for data is 2.5Hz
    // with 1Hz that comes out as a ratio of 5/2, which won't
    // downsample neatly in some cases so we would first upsample,
    // filter out any noise terms, then downsample

    double[] upped = upsample(data, upf);
    double[] lpfed = FFTResult.lowPassFilter(upped, higherFreq, lowerFreq);

    return downsample(lpfed, dnf);

  }

  /**
   * Remove mean (constant value) from a dataset and include
   *
   * @return timeseries as numeric list with previous mean subtracted
   */
  public static double[] demean(double[] dataSet) {
    double[] dataOut = dataSet.clone();
    TimeSeriesUtils.demeanInPlace(dataOut);
    return dataOut;
  }

  /**
   * In-place subtraction of mean from each point in an incoming data set.
   * This is a necessary step in calculating the power-spectral density.
   *
   * @param dataSet The data to have the mean removed from.
   */
  public static void demeanInPlace(double[] dataSet) {

    // I'm always getting the demeaning tasks, huh?

    if (dataSet.length == 0) {
      return; // shouldn't happen but just in case
    }

    double mean = getMean(dataSet);
    // mean /= dataSet.length;

    for (int i = 0; i < dataSet.length; ++i) {
      // iterate over index rather than for-each cuz we must replace data
      dataSet[i] -= mean;
    }

    // test shows this works as in-place method
  }

  /**
   * Linear detrend applied to an array of doubles rather than a list.
   * This operation is not done in-place.
   *
   * @param dataSet The double array to be detrended
   * @return Array of doubles with linear detrend removed
   */
  public static double[] detrend(double[] dataSet) {
    double sumX = 0.0;
    double sumY = 0.0;
    double sumXSqd = 0.0;
    double sumXY = 0.0;

    for (int i = 0; i < dataSet.length; ++i) {
      sumX += i;
      sumXSqd += (double) i * (double) i;
      double value = dataSet[i];
      sumXY += value * i;
      sumY += value;
    }

    // brackets here so you don't get confused thinking this should be
    // algebraic division (in which case we'd just factor out the size term)
    //

    double del = sumXSqd - (sumX * sumX / dataSet.length);

    double slope = sumXY - (sumX * sumY / dataSet.length);
    slope /= del;

    double yOffset = (sumXSqd * sumY) - (sumX * sumXY);
    yOffset /= del * dataSet.length;

    double[] detrended = new double[dataSet.length];

    for (int i = 0; i < dataSet.length; ++i) {
      detrended[i] = dataSet[i] - ((slope * i) + yOffset);
    }

    return detrended;
  }

  public static double[] detrendEnds(double[] data) {
    int lastIdx = data.length - 1;
    double start = data[0];
    double end = data[lastIdx];
    double diff = end - start;
    double delta = diff / data.length;
    double[] dataCopy = data.clone();
    for (int i = 0; i < data.length; ++i) {
      dataCopy[i] -= start + ((delta * i) + 1) * diff / (lastIdx);
    }

    return dataCopy;
  }

  /**
   * Downsamples data by a multiple of passed factor.
   * Result is data.length/factor cells in size, data.length is expected to be even multiple of factor
   * Requires previous use of a low-pass filter to avoid aliasing
   *
   * @param data The timeseries to be downsampled
   * @param factor The factor to decrease the size by
   * @return The downsampled series
   */
  public static double[] downsample(double[] data, int factor) {

    double[] downsamp = new double[data.length / factor];
    for (int i = 0; i < downsamp.length; i++) {
      downsamp[i] = data[i * factor];
    }

    return downsamp;
  }

  /**
   * Implements Euclid's algorithm for finding GCD
   * used to find common divisors to give us upsample
   * and downsample rates by dividing the timeseries intervals
   * by this value
   *
   * @param source Initially, one of two frequencies to calculate
   * @param target Initially, one of two frequencies to calculate
   * @return The GCD of the two frequencies
   */
  public static long euclidGCD(long source, long target) {

    // take remainders until we hit 0
    // which means the divisor is the gcd
    long rem = source % target;
    if (rem == 0) {
      return target;
    }

    return euclidGCD(target, rem);
  }

  /**
   * Return the calculation of the arithmetic mean (using a recursive definition for stability)
   *
   * @param dataSet Range of data to get the mean value from
   * @return the arithmetic mean
   */
  public static double getMean(double[] dataSet) {
    double mean = 0.0;
    double inc = 1;

    for (double data : dataSet) {
      mean = mean + ((data - mean) / inc);
      ++inc;
    }
    return mean;
  }

  /**
   * Normalize result according to value of absolute maximum of the data.
   * This is intended to replicate the normalization behavior of Obspy.
   *
   * @param data Time series data to be normalized
   * @return The data normalized by its maximum absolute value
   */
  public static double[] normalize(double[] data) {
    double absMax = Math.abs(data[0]); // initialize with first value in array
    // first get the absolute max
    for (double point : data) {
      absMax = Math.max(Math.abs(point), absMax);
    }

    if (absMax == 0) {
      return data.clone();
    }

    double[] normData = new double[data.length];
    // now scale the data accordingly
    for (int i = 0; i < data.length; ++i) {
      normData[i] = data[i] / absMax;
    }
    return normData;
  }

  /**
   * Rotates a north and east (known orthogonal) timeseries and produces a new
   * timeseries along the north axis in the rotated coordinate system from
   * the given angle, clockwise (y' = y cos theta - x sin theta). Though the
   * data given as input are not necessarily from a north-facing and
   * east-facing sensor, they are presumed to be orthogonal to each other.
   *
   * @param northData Timeseries data expected to point north
   * @param eastData Timeseries assumed to point east,
   * orthogonal to north sensor
   * @param angle Angle to rotate the data along
   * @return New timeseries data rotated data along the
   * given angle, facing north
   */
  public static double[] rotate(double[] northData, double[] eastData, double angle) {
    double[] rotatedData = new double[northData.length];

    // clockwise rotation matrix!! That's why things are so screwy
    double sinTheta = Math.sin(angle);
    double cosTheta = Math.cos(angle);

    for (int i = 0; i < northData.length; ++i) {
      rotatedData[i] =
          northData[i] * cosTheta -
              eastData[i] * sinTheta;
    }

    return rotatedData;
  }

  /**
   * Rotates a north and east (known orthogonal) timeseries and produces a new
   * timeseries along the east axis in the rotated coordinate system from
   * the given angle, clockwise (x' = x cos theta + y sin theta). Though the
   * data given as input are not necessarily from a north-facing and
   * east-facing sensor, they are presumed to be orthogonal to each other.
   *
   * @param northData Timeseries data expected to point north
   * @param eastData Timeseries assumed to point east,
   * orthogonal to north sensor
   * @param angle Angle to rotate the data along
   * @return New timeseries data rotated data along the
   * given angle, facing east.
   */
  private static double[] rotateX(double[] northData, double[] eastData, double angle) {
    double[] rotatedData = new double[northData.length];

    double sinTheta = Math.sin(angle);
    double cosTheta = Math.cos(angle);

    for (int i = 0; i < northData.length; ++i) {
      rotatedData[i] =
          eastData[i] * cosTheta +
              northData[i] * sinTheta;
    }

    return rotatedData;
  }

  /**
   * Upsamples data by a multiple of passed factor, placing zeros
   * between each data point. Result is data.length*factor cells in size.
   * Requires use of a low-pass filter to remove discontinuities.
   *
   * @param data The timeseries to be upsampled
   * @param factor The factor to increase the size by
   * @return The upsampled series
   */
  private static double[] upsample(double[] data, int factor) {

    int newLength = data.length * factor;

    double[] upsamp = new double[newLength];

    for (int i = 0; i < data.length; ++i) {
      upsamp[i * factor] = data[i]; // index, element
    }

    return upsamp;
  }

}

