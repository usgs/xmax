package com.isti.traceview.processing;

import java.util.Arrays;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.isti.jevalresp.RespUtils;
import com.isti.traceview.data.Channel;
import com.isti.traceview.data.Response;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.sc.seis.fissuresUtil.freq.Cmplx;

/**
 * ISTI utils math methods.
 */
public class IstiUtilsMath {
	private static Logger lg = Logger.getLogger(IstiUtilsMath.class);
	/**
	 * \ingroup isti_utils_retVal \brief SUCCESS
	 */
	public static final int ISTI_UTIL_SUCCESS = 1;

	/**
	 * \ingroup isti_utils_retVal \brief FAILURE
	 */
	public static final int ISTI_UTIL_FAILED = -1;

	/**
	 * \ingroup isti_utils_param \brief Conversion from nmeters to meters
	 */
	public static double ISTI_UTIL_NM2MTR = 1e9;

	/**
	 * \ingroup isti_utils_public_functions \brief Function to normalize
	 * response function using calib and calper. \note Modifies the first
	 * argument.
	 * 
	 * @param resp
	 *            the response.
	 * @param calper
	 *            calper calibration value from the RESP file.
	 * @param calib
	 *            calib calibration value from the RESP file.
	 * @param freqStart
	 *            the start frequency.
	 * @param freqEnd
	 *            the end frequency.
	 * @param freqNum
	 *            the number of frequencies.
	 * @return ISTI_UTIL_FAILED or ISTI_UTIL_SUCCESS.
	 */
	public static int calibAmpResp(double[] resp, final double calper, final double calib, final double freqStart, final double freqEnd, final int freqNum) {
		if (resp.length <= 0 || 1. / calper > freqEnd || 1. / calper < freqStart)
			return ISTI_UTIL_FAILED;

		double sqrt_resp;
		final double FreqStep = (freqEnd - freqStart) / ((double) (freqNum - 1));
		int cal_i = (int) ((1. / calper - freqStart) / FreqStep);
		if (cal_i <= 0)
			cal_i = 1;
		final double sqrt_cal = StrictMath.sqrt(resp[cal_i]);
		for (int i = 0; i < freqNum; i++) {
			sqrt_resp = StrictMath.sqrt(resp[i]) / sqrt_cal;
			sqrt_resp /= calib;
			resp[i] = sqrt_resp * sqrt_resp;
		}

		return ISTI_UTIL_SUCCESS;
	}

	/**
	 * \ingroup isti_utils_public_functions \brief Displacement to acceleration
	 * conversion for PSD
	 * 
	 * @param spectrum
	 *            the spectrum data.
	 * @param deltaF
	 *            the delta.
	 * @param len
	 *            the length.
	 */
	public static void dispToAccel(double[] spectrum, final double deltaF, final int len) {
		double omega;
		for (int i = 0; i < len; i++) {
			omega = 2.0 * StrictMath.PI * deltaF * i;
			spectrum[i] *= StrictMath.pow(omega, 4.0);
		}
		return;
	}

	/**
	 * \ingroup isti_utils_public_functions \brief Velocity to acceleration
	 * conversion for PSD
	 * 
	 * @param spectrum
	 *            the spectrum data.
	 * @param deltaF
	 *            the delta.
	 * @param len
	 *            the length.
	 */
	public static void velToAccel(double[] spectrum, final double deltaF, final int len) {
		double omega;
		for (int i = 0; i < len; i++) {
			omega = 2.0 * StrictMath.PI * deltaF * i;
			spectrum[i] *= StrictMath.pow(omega, 2.0);
		}
		return;
	}

	/**
	 * \ingroup isti_utils_public_functions \brief Function for normalizing data
	 * vector with Hanning window. \note We apply Hanning window S(n) * 1/2 [1-
	 * cos (2PI*n/N)] \note to our data to reduce leakage in PSD computation;
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] windowHanning(double[] data) {
		double[] ret = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i] * (0.5 * (1.0 - StrictMath.cos((2.0 * StrictMath.PI * i) / (data.length - 1))));
		}
		return ret;
	}

	/**
	 * Function for normalizing data vector with Hamming window.
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] windowHamming(double[] data) {
		double[] ret = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i] * (0.53836 - 0.46164 * StrictMath.cos((2.0 * StrictMath.PI * i) / (data.length - 1)));
		}
		return ret;
	}

	/**
	 * Function for normalizing data vector with Cosine window.
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] windowCosine(double[] data) {
		double[] ret = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i] * StrictMath.sin((StrictMath.PI * i) / (data.length - 1));
		}
		return ret;
	}

	/**
	 * Function for normalizing data vector with triangular window.
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] windowTriangular(double[] data) {
		double[] ret = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i] * 2 / data.length * (data.length / 2 - StrictMath.abs(i - (data.length - 1) / 2));
		}
		return ret;
	}

	/**
	 * Function for normalizing data vector with Bartlett window (zero-valued
	 * triangular).
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] windowBartlett(double[] data) {
		double[] ret = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i] * 2 / (data.length - 1) * ((data.length - 1) / 2 - StrictMath.abs(i - (data.length - 1) / 2));
		}
		return ret;
	}

	/**
	 * Function for normalizing data vector with Gauss window.
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] windowGauss(double[] data) {
		double theta = 0.4;
		double[] ret = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i] * StrictMath.pow(StrictMath.E, -StrictMath.pow((i - (data.length - 1) / 2) / (theta * (i - 1) / 2), 2) / 2);
		}
		return ret;
	}

	/**
	 * Function for normalizing data vector with Blackman window.
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] windowBlackman(double[] data) {
		double alpha = 0.16;
		double[] ret = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i]
					* ((1 - alpha) / 2 - StrictMath.cos((2 * StrictMath.PI * i) / (data.length - 1)) / 2 + alpha / 2
							* StrictMath.cos((4 * StrictMath.PI * i) / (data.length - 1)));
		}
		return ret;
	}

	/**
	 * \ingroup isti_utils_public_functions \brief Demeans data in-place.
	 * 
	 * @param data
	 *            the data.
	 */
	public static double[] normData(double[] data) {
		double[] ret = new double[data.length];
		double sumData = 0.0;
		for (int i = 0; i < data.length; i++)
			sumData += data[i];
		final double meanData = sumData / data.length;
		for (int i = 0; i < data.length; i++)
			ret[i] = data[i] - meanData;
		return ret;
	}

	public static double[] normData(int[] data) {
		double[] ret = new double[data.length];
		double sumData = 0.0;
		for (int i = 0; i < data.length; i++)
			sumData += data[i];
		final double meanData = new Double(sumData) / data.length;
		for (int i = 0; i < data.length; i++)
			ret[i] = data[i] - meanData;
		return ret;
	}

	/**
	 * \ingroup isti_utils_public_functions \brief Function for real numbers
	 * deconvolution of double array. \note We assume that the length of both
	 * arays are the same and save the \note output into the data. \note The
	 * output is saved in the first and second input parameters. Beware!
	 * 
	 * @param denom
	 *            the denominator array.
	 * @param numer
	 *            the numerator array.
	 * @param len
	 *            the length.
	 */
	public static void realDeconvolution(double[] denom, double[] numer, int len) {
		final double small = 10e-30;
		for (int i = 0; i < len; i++) {
			if (numer[i] == 0)
				denom[i] /= small;
			else
				denom[i] /= numer[i];
		}
	}

	/**
	 * Compute complex deconvolution
	 */
	public static final Cmplx[] complexDeconvolution(Cmplx[] f, Cmplx[] g) {
		if (f.length != g.length)
			throw new IllegalArgumentException("complexDeconvolution: both arrays must have same length. " + f.length + " " + g.length);
		Cmplx[] ret = new Cmplx[f.length];
		for (int i = 0; i < f.length; i++)
			ret[i] = Cmplx.div(f[i], g[i]);
		return ret;
	}

	/**
	 * Compute complex convolution
	 */
	public static final Cmplx[] complexConvolution(Cmplx[] f, Cmplx[] g) {
		if (f.length != g.length)
			throw new IllegalArgumentException("complexConvolution: both arrays must have same length. " + f.length + " " + g.length);
		Cmplx[] ret = new Cmplx[f.length];
		for (int i = 0; i < f.length; i++)
			ret[i] = Cmplx.mul(f[i], g[i]);
		return ret;
	}

	/**
	 * Compute amplitude of complex spectra
	 */
	public static final double[] getSpectraAmplitude(Cmplx[] spectra) {
		final double[] ret = new double[spectra.length];
		for (int i = 0; i < spectra.length; i++) {
			ret[i] = spectra[i].mag();
		}
		return ret;
	}

	/**
	 * Compute correlation
	 */
	public static final double[] correlate(double fdata[], double gdata[]) {
		if (fdata.length != gdata.length)
			throw new IllegalArgumentException("fdata and gdata must have same length. " + fdata.length + " " + gdata.length);
		int dataLength = fdata.length;
		int paddedDataLength = new Double(Math.pow(2, new Double(IstiUtilsMath.log2(dataLength)).intValue() + 1)).intValue();
		double sumF = 0;
		double sumG = 0;
		double[] fdataPadded = new double[paddedDataLength * 2];
		double[] gdataPadded = new double[paddedDataLength * 2];
		for (int i = 0; i < paddedDataLength * 2; i++) {
			if (i < dataLength) {
				fdataPadded[i] = fdata[i];
				gdataPadded[i] = gdata[i];
				sumF += fdata[i] * fdata[i];
				sumG += gdata[i] * gdata[i];
			} else {
				fdataPadded[i] = 0;
				gdataPadded[i] = 0;
			}
		}
		double scale = StrictMath.sqrt(sumF * sumG);
		Cmplx fTrans[] = processFft(fdataPadded);
		Cmplx gTrans[] = processFft(gdataPadded);
		for (int i = 0; i < fTrans.length; i++)
			fTrans[i] = Cmplx.mul(fTrans[i], gTrans[i].conjg());
		double[] corr = inverseFft(fTrans);
		double[] crosscorr = new double[2 * dataLength - 1];
		for (int i = 0; i < dataLength; i++) {
			crosscorr[dataLength - 1 + i] = corr[i] / scale;
			if (i < dataLength - 1) {
				crosscorr[i] = corr[2 * paddedDataLength - dataLength + i] / scale;
			}
		}
		return crosscorr;
	}

	public static double[] floatToDoubleArray(float[] arr) {
		double[] ret = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ret[i] = new Float(arr[i]).doubleValue();
		}
		return ret;
	}

	public static float[] doubleToFloatArray(double[] arr) {
		float[] ret = new float[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ret[i] = new Double(arr[i]).floatValue();
		}
		return ret;
	}

	public static float[] intToFloatArray(int[] arr) {
		float[] ret = new float[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ret[i] = new Integer(arr[i]).floatValue();
		}
		return ret;
	}

	public static double[] intToDoubleArray(int[] arr) {
		double[] ret = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ret[i] = new Integer(arr[i]).doubleValue();
		}
		return ret;
	}

	/**
	 * Builds amplitude spectra of trace. proper response function out of RESP
	 * file.
	 * 
	 * @param trace
	 *            the trace array.
	 * @param numSamples
	 *            the number of samples.
	 * @param sampRate
	 *            the sample rate.
	 * @param verboseDebug
	 *            true for verbose debug messages
	 * @return the noise spectra.
	 */
	public static Spectra getNoiseSpectra(int[] trace, Response response, Date date, Channel channel, boolean verboseDebug) {
		// Init error string
		lg.debug("getNoiseSpectra begin");
		String errString = "";

		final Response.FreqParameters fp = Response.getFreqParameters(trace.length, 1000.0 / channel.getSampleRate());
		final double[] frequenciesArray = RespUtils.generateFreqArray(fp.startFreq, fp.endFreq, fp.numFreq, false);

		// Make a copy of data since we gonna modify it
		double[] traceCopy = new double[trace.length];
		for (int i = 0; i < trace.length; i++)
			traceCopy[i] = trace[i];

		// Norm the data: remove trend
		traceCopy = normData(traceCopy);

		// Apply Hanning window
		traceCopy = windowHanning(traceCopy);

		// Do FFT and get imag and real parts of the data spectrum
		final Cmplx[] noise_spectra = processFft(traceCopy);
		Cmplx[] resp = null;
		try {
			resp = response.getResp(date, fp.startFreq, fp.endFreq, Math.min(noise_spectra.length, fp.numFreq));
		} catch (Exception e) {
			errString = "Can't get response for channel " + channel.getName() + ": " + e.toString();
		}
		lg.debug("getNoiseSpectra end");
		return new Spectra(date, noise_spectra, frequenciesArray, resp, fp.sampFreq, channel, errString);
	}

	/**
	 * \ingroup sscdns_process_private_functions \brief Function for processing
	 * fft. \note See http://www.w.org/doc/fftw_2.html#SEC5 for comments on how
	 * \note fft works
	 * 
	 * @param indata
	 *            the input data, count of points must be power of 2
	 * @param n
	 *            the size of the data.
	 * @return the FFT output.
	 */

	public static Cmplx[] processFft(double[] indata) {
		lg.debug("processFft begin");
		int n = indata.length;
		DoubleFFT_1D fft = new DoubleFFT_1D(n);
		fft.realForward(indata);
		Cmplx[] ret = null;
		int l = 0;
		if(n%2==0){
			l = n/2;
			ret = new Cmplx[l+1];
			for (int k = 0; k <= l; k++) {
				ret[k] = new Cmplx(k==l?indata[1]:indata[2*k], (k==0 || k==l) ? 0 : indata[2*k+1]);
			}
		} else {
			l = (n-1)/2;
			ret = new Cmplx[l+1];
			for (int k = 0; k <= l; k++) {
				double im;
				if(k==0){
					im=0;
				} else if(k==l) {
					im=indata[1];
				} else {
					im=indata[2*k+1];
				}
				ret[k] = new Cmplx(indata[2*k], im);
			}
		}
		lg.debug("processFft end");
		return ret;
	}

	/**
	 * Inverse FFT processing
	 * 
	 * @param indata
	 *            spectra to process, count of points must be power of 2
	 */
	
	public static double[] inverseFft(Cmplx[] indata) {
		DoubleFFT_1D fft = new DoubleFFT_1D(indata.length * 2-2);
		double[] dataToProcess = new double[indata.length * 2-2];
		for (int k = 0; k < indata.length-1; k++) {
			dataToProcess[2*k] = indata[k].r;
			dataToProcess[2*k+1] = (k==0?indata[indata.length-1].r:indata[k].i);
		}
		fft.realInverse(dataToProcess, true);
		return dataToProcess;
	}

	/**
	 * Logariphm with base 2
	 */
	public static double log2(double x) {
		return Math.log10(x) / Math.log10(2.0);
	}

	/*
	 * Subroutine varismooth smooths psd by variable-point averaging. Translated
	 * from fortran in old XYZ. The value MAXAVE (largest # of pts. of average
	 * at high frequency end of plot) is calculated based on NX, the # of PSD
	 * points in file. Starting with shortest periods (highest freq.) first: If
	 * nx > 1024: For first 90% of pts., use nave=.01*nx. From there to end of
	 * plot, use nave = 9 pts. If nx <= 1024: then nave=3.
	 */
	public static XYSeriesCollection varismooth(XYSeriesCollection toSmooth) {
		XYSeriesCollection ret = new XYSeriesCollection();
		for (int i = 0; i < toSmooth.getSeriesCount(); i++) {
			XYSeries toSmoothSeries = toSmooth.getSeries(i);
			XYSeries smoothed = new XYSeries(toSmooth.getSeriesKey(i));
			int ntail = new Double(0.9 * toSmoothSeries.getItemCount()).intValue();
			int radius = new Double(0.01 * toSmoothSeries.getItemCount()).intValue();
			for (int j = 0; j < toSmoothSeries.getItemCount(); j++) {
				if (toSmoothSeries.getItemCount() < 1024) {
					smoothed.add(toSmoothSeries.getX(j), getMovingAverage(toSmoothSeries, j, 3));
				} else {
					if (j < ntail) {
						smoothed.add(toSmoothSeries.getX(j), getMovingAverage(toSmoothSeries, j, radius));
					} else {
						smoothed.add(toSmoothSeries.getX(j), getMovingAverage(toSmoothSeries, j, 9));
					}
				}
			}
			ret.addSeries(smoothed);
		}
		return ret;
	}

	private static double getMovingAverage(XYSeries series, int center, int radius) {
		double ret = 0.0;
		int internalRadius = radius;
		if (center < radius) {
			internalRadius = center;
		}
		if ((series.getItemCount() - center - 1) < internalRadius) {
			internalRadius = series.getItemCount() - center - 1;
		}
		for (int pos = center - internalRadius; pos <= center + internalRadius; pos++) {
			ret = ret + (series.getY(pos)).doubleValue();
		}
		return ret / (2 * internalRadius + 1);
	}
	
	public static double median(int[] m) {
		int[] sorted = Arrays.copyOf(m, m.length);
		Arrays.sort(sorted);
	    int middle = sorted.length/2;  // subscript of middle element
	    if (sorted.length%2 == 1) {
	        // Odd number of elements -- return the middle one.
	        return sorted[middle];
	    } else {
	       // Even number -- return average of middle two
	       // Must cast the numbers to double before dividing.
	       return (sorted[middle-1] + sorted[middle]) / 2.0;
	    }
	}

	static public int[] padArray(int[] original, int[] toAdd) {
		// int[] ret = Arrays.copyOf(original, original.length + toAdd.length);

		// so as Mac OSX java doesn't contain Arrays.copyOf method
		int[] ret = new int[original.length + toAdd.length];
		for (int i = 0; i < original.length; i++) {
			ret[i] = original[i];
		}
		// --------
		for (int i = 0; i < toAdd.length; i++) {
			ret[original.length + i] = toAdd[i];
		}
		return ret;
	}
}
