package com.isti.traceview.processing;

import com.isti.jevalresp.RespUtils;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.Channel;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.xmax.XMAXException;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.Pair;
import org.jfree.data.xy.XYSeries;
import uk.me.berndporr.iirj.Butterworth;

/**
 * Holds the data returned from a power spectral density calculation
 * (The PSD data (without response correction) and frequencies of the FFT)
 * Most methods that either calculate or involve FFT calculations exist here,
 * such as raw PSD calculation, inverse and forward trimmed FFTs,
 * and band-pass filtering.
 *
 * @author akearns - KBRWyle
 */
public class FFTResult {

  final private Complex[] transform; // the FFT data
  final private double[] freqs; // array of frequencies matching the fft data

  final public static double TAU = 2 * StrictMath.PI;
  
  /**
   * Instantiate the structure holding an FFT and its frequency range
   * (Used to return data from the spectral density calculations)
   * Holds results of an FFT calculation already performed, usable in return
   * statements
   *
   * @param inPSD Precalculated FFT result for some timeseries
   * @param inFreq Frequencies matched up to each FFT value
   */
  private FFTResult(Complex[] inPSD, double[] inFreq) {
    transform = inPSD;
    freqs = inFreq;
  }

  /**
   * Filter out data outside of the range between the low and high frequencies;
   * can be used for a low-pass filter if low frequency is set to 0
   * and high-pass if higher frequency is set to sample rate
   *
   * @param toFilter series of data to do a band-pass filter on
   * @param sps sample rate of the current data (samples / sec)
   * @param lowCorner low corner frequency of band-pass filter
   * @param highCorner high corner frequency of band-pass filter
   * @return timeseries with band-pass filter applied
   */
  public static double[]
  bandFilter(double[] toFilter, double sps, double lowCorner, double highCorner) {

    // make sure the low value is actually the lower of the two
    double temp = Math.min(lowCorner, highCorner);
    highCorner = Math.max(lowCorner, highCorner);
    lowCorner = temp;

    Butterworth casc = new Butterworth();
    // center = low-corner location plus half the distance between the corners
    // width is exactly the distance between them
    double width = highCorner - lowCorner;
    double center = lowCorner + (width) / 2.;
    // filter library defines bandpass with center frequency and notch width
    casc.bandPass(2, sps, center, width);

    double[] filtered = new double[toFilter.length];
    for (int i = 0; i < toFilter.length; ++i) {
      filtered[i] = casc.filter(toFilter[i]);
    }

    return filtered;

  }

  /**
   * Calculates and performs an in-place cosine taper on an incoming data set.
   * Used for windowing for performing FFT.
   *
   * @param dataSet The dataset to have the taper applied to.
   * @param taperW Width of taper to be used
   * @return Value corresponding to power loss from application of taper.
   */
  public static double cosineTaper(double[] dataSet, double taperW) {
    /*
    double widthSingleSide = taperW/2;
    int ramp = (int) (widthSingleSide * dataSet.length);
    widthSingleSide = (double) ramp / dataSet.length;
    */
    double wss = 0.0; // represents power loss

    double[] taperCurve = getCosTaperCurveSingleSide(dataSet.length, taperW);
    int ramp = taperCurve.length;

    for (int i = 0; i < taperCurve.length; i++) {
      double taper = taperCurve[i];
      dataSet[i] *= taper;
      int idx = dataSet.length - i - 1;
      dataSet[idx] *= taper;
      wss += 2.0 * taper * taper;
    }

    wss += (dataSet.length - (2 * ramp));

    return wss;
  }

  public static FFTResult getPSD(PlotDataProvider channel, TimeInterval ti)
      throws XMAXException, TraceViewException {

    int[] data = channel.getContinuousGaplessDataOverRange(ti);
    double[] doubleData = new double[data.length];

    IntStream.range(0, doubleData.length).parallel().forEach(i ->
      doubleData[i] = (double) data[i]
    );

    FFTResult psd = spectralCalc(doubleData, doubleData, (long) channel.getSampleRate());

    assert(psd.getFreq(0) == 0.);

    double[] freqs = psd.getFreqs();
    double endFreq = freqs[freqs.length - 1]; // get last frequency

    Cmplx[] response = channel.getResponse().getResp(ti.getStartTime(), 0,
        endFreq, freqs.length);

    Complex[] responseAdapted = new Complex[response.length];
    IntStream.range(0, responseAdapted.length).parallel().forEach(i ->
      responseAdapted[i] = new Complex(response[i].real(), response[i].imag())
    );

    return crossPower(psd.getFFT(), freqs, responseAdapted, responseAdapted);
  }



  private static FFTResult crossPower(Complex[] results, double[] freqs,
      Complex[] freqRespd1, Complex[] freqRespd2) {

    Complex[] out = new Complex[freqs.length];

    for (int j = 0; j < freqs.length; ++j) {
      // response curves in velocity, put them into acceleration
      Complex scaleFactor =
          new Complex(0.0, -1.0 / (TAU * freqs[j]));
      Complex resp1 = freqRespd1[j].multiply(scaleFactor);
      Complex resp2 = freqRespd2[j].multiply(scaleFactor);

      Complex respMagnitude =
          resp1.multiply(resp2.conjugate());

      if (respMagnitude.abs() == 0) {
        respMagnitude = new Complex(Double.MIN_VALUE, 0);
      }

      out[j] = results[j].divide(respMagnitude);
    }

    return new FFTResult(out, freqs);

  }

  /**
   * Return the cosine taper curve to multiply against data of a specified length, with taper of
   * given width
   *
   * @param length Length of data being tapered (to per-element multply against)
   * @param width Width of (half-) taper curve (i.e., decimal fraction of the data being tapered)
   * Because this parameter is used to create the actual length of the data, this should be half
   * the value of the full taper.
   * @return Start of taper curve, symmetric to end, with all other entries being implicitly 1.0
   */
  public static double[] getCosTaperCurveSingleSide(int length, double width) {
    // width = width/2;
    int ramp = (int) ((((length * width) + 1) / 2.) - 1);

    // int limit = (int) Math.ceil(ramp);

    double[] result = new double[ramp];
    for (int i = 0; i < ramp; i++) {
      double taper = 0.5 * (1.0 - Math.cos(i * Math.PI / ramp));
      result[i] = taper;
    }

    return result;
  }

  static Pair<Complex[], Double> getSpectralWindow(double[] toFFT, int padding) {
    // demean and detrend work in-place on the list
    TimeSeriesUtils.demeanInPlace(toFFT);
    Double wss = cosineTaper(toFFT, 0.05);
    // presumably we only need the last value of wss

    toFFT = Arrays.copyOfRange(toFFT, 0, padding);
    FastFourierTransformer fft =
        new FastFourierTransformer(DftNormalization.STANDARD);

    Complex[] frqDomn1 = fft.transform(toFFT, TransformType.FORWARD);
    int singleSide = padding / 2 + 1;
    // use arraycopy now (as it's fast) to get the first half of the fft
    return new Pair<>(Arrays.copyOfRange(frqDomn1, 0, singleSide), wss);
  }

  /**
   * Apply a low pass filter to some timeseries data
   *
   * @param toFilt Data to be filtered
   * @param sps Sample rate of the data in Hz
   * @param corner Corner frequency of LPF
   * @return lowpass-filtered timeseries data
   */
  public static double[] lowPassFilter(double[] toFilt, double sps, double corner) {
    Butterworth casc = new Butterworth();
    // order 1 filter
    casc.lowPass(2, sps, corner);

    double[] filtered = new double[toFilt.length];
    for (int i = 0; i < toFilt.length; ++i) {
      filtered[i] = casc.filter(toFilt[i]);
    }

    return filtered;
  }

  /**
   * Function for padding and returning the result of a forward FFT.
   * This does not trim the negative frequencies of the result; it returns
   * the full FFT result as an array of Complex numbers
   *
   * @param dataIn Array of doubles representing timeseries data
   * @return Complex array representing forward FFT values, including
   * symmetric component (second half of the function)
   */
  private static Complex[] simpleFFT(double[] dataIn) {

    int padding = 2;
    while (padding < dataIn.length) {
      padding *= 2;
    }

    double[] toFFT = Arrays.copyOf(dataIn, padding);

    FastFourierTransformer fft =
        new FastFourierTransformer(DftNormalization.STANDARD);

    return fft.transform(toFFT, TransformType.FORWARD);
  }

  /**
   * Calculates the FFT of some timeseries data (double array)
   * and returns the positive frequencies resulting from the FFT calculation
   *
   * @param data Timeseries data
   * @param sps Sample rate of the timeseries data
   * @param mustFlip True if signal is inverted (for step cal)
   * @return Complex array of FFT values, and double array of matching frequencies
   */
  static FFTResult singleSidedFFT(double[] data, double sps, boolean mustFlip) {
    for (int i = 0; i < data.length; ++i) {
      if (mustFlip) {
        data[i] *= -1;
      }
    }

    data = TimeSeriesUtils.demean(data);
    FFTResult.cosineTaper(data, 0.05);
    // data = TimeSeriesUtils.normalize(data);

    Complex[] frqDomn = simpleFFT(data);

    int padding = frqDomn.length;
    int singleSide = padding / 2 + 1;

    double nyquist = sps / 2;
    double deltaFrq = nyquist / (singleSide - 1);

    Complex[] fftOut = new Complex[singleSide];
    double[] frequencies = new double[singleSide];

    for (int i = 0; i < singleSide; ++i) {
      fftOut[i] = frqDomn[i];
      frequencies[i] = i * deltaFrq;
    }

    return new FFTResult(fftOut, frequencies);

  }

  /**
   * Do the inverse FFT on the result of a single-sided FFT operation.
   * The negative frequencies are reconstructed as the complex conjugates of
   * the positive corresponding frequencies
   *
   * @param freqDomn Complex array (i.e., the result of a previous FFT calc)
   * @param trim How long the original input data was
   * @return A list of doubles representing the original timeseries of the FFT
   */
  public static double[] singleSidedInverseFFT(Complex[] freqDomn, int trim) {
    FastFourierTransformer fft =
        new FastFourierTransformer(DftNormalization.STANDARD);

    int padding = (freqDomn.length - 1) * 2;

    Complex[] padded = new Complex[padding];
    System.arraycopy(freqDomn, 0, padded, 0, freqDomn.length);
    for (int i = 1; i < padding / 2; ++i) {
      padded[padded.length - i] = padded[i].conjugate();
    }

    Complex[] timeSeriesCpx =
        fft.transform(padded, TransformType.INVERSE);

    double[] timeSeries = new double[trim];
    for (int i = 0; i < trim; ++i) {
      timeSeries[i] = timeSeriesCpx[i].getReal();
    }

    return timeSeries;
  }

  static int findFFTPaddingLength(int size) {
    int padding = 2;
    while (padding < size) {
      padding = padding << 1;
    }
    return padding;
  }

  /**
   * Helper function to calculate power spectral density / crosspower.
   * Takes in two time series data and produces the windowed FFT over each.
   * The first is multiplied by the complex conjugate of the second.
   * If the two series are the same, this is the PSD of that series. If they
   * are different, this result is the crosspower.
   * The result is smoothed but does not have the frequency response applied,
   * and so does not give a full result -- this is merely a helper function
   * for the crossPower function.
   *
   * @param list1 First list of data to be given as input
   * @param list2 Second list of data to be given as input, which can be
   * the same as the first (and if so, is ignored)
   * @param interval Interval of the data (same for both lists)
   * @return FFTResult (FFT values and frequencies as a pair of arrays)
   * representing the power-spectral density / crosspower of the input data.
   */
  static FFTResult
  spectralCalc(double[] list1, double[] list2, long interval) {

    //Only the same data if the arrays are actually the same objects.
    //noinspection ArrayEquals
    boolean sameData = list1.equals(list2);

    // divide into windows of 1/4, moving up 1/16 of the data at a time

    int range = list1.length / 4;
    int slider = range / 4;

    // period is 1/sample rate in seconds
    // since the interval data is just that multiplied by a large number
    // let's divide it by that large number to get our period

    // shouldn't need to worry about a cast here
    double period = 1.0 / TimeSeriesUtils.ONE_HZ_INTERVAL;
    period *= interval;

    int padding = findFFTPaddingLength(range);

    int singleSide = padding / 2 + 1;
    double deltaFreq = 1. / (padding * period);

    Complex[] powSpectDens = new Complex[singleSide];
    double wss;

    int segsProcessed = 0;
    int rangeStart = 0;
    int rangeEnd = range;

    for (int i = 0; i < powSpectDens.length; ++i) {
      powSpectDens[i] = Complex.ZERO;
    }

    while (rangeEnd <= list1.length) {

      // give us a new list we can modify to get the data of
      double[] toFFT1 =
          Arrays.copyOfRange(list1, rangeStart, rangeEnd);
      double[] toFFT2 = null;

      if (!sameData) {
        toFFT2 = Arrays.copyOfRange(list2, rangeStart, rangeEnd);
      }

      Pair<Complex[], Double> windFFTData = getSpectralWindow(toFFT1, padding);
      Complex[] fftResult1 = windFFTData.getFirst(); // actual fft data
      wss = windFFTData.getSecond(); // represents some measure of power loss
      Complex[] fftResult2 = fftResult1;
      if (toFFT2 != null) {
        fftResult2 = getSpectralWindow(toFFT2, padding).getFirst();
      }

      for (int i = 0; i < singleSide; ++i) {

        Complex val1 = fftResult1[i];
        Complex val2 = val1;
        if (fftResult2 != null) {
          val2 = fftResult2[i];
        }

        // 2 * fft1 * fft2' / wss
        Complex temp = val1.multiply(val2.conjugate()).multiply(2).divide(wss);
        powSpectDens[i] = powSpectDens[i].add(temp);
      }

      ++segsProcessed;
      rangeStart += slider;
      rangeEnd += slider;

    }

    // get frequency per-point, also normalize PSD on number of segments processed (i.e., get mean)
    // and divide out the sample rate (multiply by period)
    double[] frequencies = new double[singleSide];
    for (int i = 0; i < singleSide; ++i) {
      powSpectDens[i] = powSpectDens[i].divide(segsProcessed).multiply(period);
      frequencies[i] = i * deltaFreq;
    }

    return new FFTResult(powSpectDens, frequencies);

  }

  /**
   * Get the index of the value closest to a given target frequency in a list assuming the entries
   * in the list are equally spaced
   *
   * @param frequencies List of frequencies to find the target location
   * @param targetFrequency Frequency of interest
   * @return Index of closest frequency value
   */
  public static int getIndexOfFrequency(double[] frequencies, double targetFrequency) {
    if (frequencies.length == 1) {
      return 0;
    }

    double deltaFreq = frequencies[1] - frequencies[0];
    int index = (int) Math.round((targetFrequency - frequencies[0]) / deltaFreq);
    // in almost all cases the index here should be in the list, but if not, bounds check
    index = Math.max(index, 0);
    return Math.min(index, frequencies.length - 1);
  }

  /**
   * Get the FFT for some sort of previously calculated data
   *
   * @return Array of FFT results, as complex numbers
   */
  public Complex[] getFFT() {
    return transform;
  }

  /**
   * Return the value of the FFT at the given index
   *
   * @param idx Index to get the FFT value at
   * @return FFT value at index
   */
  public Complex getFFT(int idx) {
    return transform[idx];
  }

  /**
   * Get the frequency value at the given index
   *
   * @param idx Index to get the frequency value at
   * @return Frequency value at index
   */
  public double getFreq(int idx) {
    return freqs[idx];
  }

  /**
   * Get the frequency range for the (previously calculated) FFT
   *
   * @return Array of frequencies (doubles), matching index to each FFT point
   */
  public double[] getFreqs() {
    return freqs;
  }

  /**
   * Get the size of the complex array of FFT values, also the size of the
   * double array of frequencies for the FFT at each index
   *
   * @return int representing size of thi's object's arrays
   */
  public int size() {
    return transform.length;
  }

}
