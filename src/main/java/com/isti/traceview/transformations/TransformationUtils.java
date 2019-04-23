package com.isti.traceview.transformations;

import uk.me.berndporr.iirj.Butterworth;

/**
 * This class includes helper functions used by multiple transformation examples.
 * The primary use for this class is to enable access to methods by which to downsample data
 * so that some signal processing can be done with data using mismatched sample intervals.
 */
public class TransformationUtils {

  /**
   * Interval for data that has been sampled at 1 Hz in milliseconds
   */
  public final static long ONE_HZ_INTERVAL = 1000L;

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
  public static int[] decimate(int[] data, long source, long target) {

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
    double[] converted = new double[data.length];
    for (int i = 0; i < data.length; ++i) {
      converted[i] = (double) data[i];
    }

    double[] upped = upsample(converted, upf);
    double[] lpfed = lowPassFilter(upped, higherFreq, lowerFreq);
    double[] downsample = downsample(lpfed, dnf);
    int[] returnValue = new int[downsample.length];
    for (int i = 0; i < downsample.length; ++i) {
      returnValue[i] = (int) downsample[i];
    }
    return returnValue;

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
   * Apply a low pass filter to some timeseries data
   *
   * @param toFilt Data to be filtered
   * @param sps Sample rate of the data in Hz
   * @param corner Corner frequency of LPF
   * @return lowpass-filtered timeseries data
   */
  /**
   * Apply a low pass filter to some timeseries data
   *
   * @param toFilt Data to be filtered
   * @param sps Sample rate of the data in Hz
   * @param corner Corner frequency of LPF
   * @return lowpass-filtered timeseries data
   */
  public static double[] lowPassFilter(double[] toFilt, double sps, double corner) {
    uk.me.berndporr.iirj.Butterworth casc = new Butterworth();
    // order 1 filter
    casc.lowPass(2, sps, corner);

    double[] filtered = new double[toFilt.length];
    for (int i = 0; i < toFilt.length; ++i) {
      filtered[i] = casc.filter(toFilt[i]);
    }

    return filtered;
  }


}
