package com.isti.traceview.processing;

import com.isti.traceview.data.Channel;
import com.isti.traceview.data.Response;
import com.isti.traceview.jnt.FFT.RealDoubleFFT_Even;
import com.isti.traceview.transformations.psd.TransPSD;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.math3.complex.Complex;
import org.apache.log4j.Logger;

/**
 * ISTI utils math methods.
 */
public class IstiUtilsMath {
	private static final Logger logger = Logger.getLogger(IstiUtilsMath.class);
	/**
	 * \ingroup isti_utils_retVal \brief SUCCESS
	 */
	public static final int ISTI_UTIL_SUCCESS = 1;

	/**
	 * \ingroup isti_utils_retVal \brief FAILURE
	 */
	private static final int ISTI_UTIL_FAILED = -1;

	private static final double SMOOTHING_FACTOR = TransPSD.SMOOTHING_FACTOR;

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
	static void dispToAccel(double[] spectrum, final double deltaF, final int len) {
		double omega;
		for (int i = 0; i < len; i++) {
			omega = 2.0 * StrictMath.PI * deltaF * i;
			spectrum[i] *= StrictMath.pow(omega, 4.0);
		}
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
	static void velToAccel(double[] spectrum, final double deltaF, final int len) {
		double omega;
		for (int i = 0; i < len; i++) {
			omega = 2.0 * StrictMath.PI * deltaF * i;
			spectrum[i] *= StrictMath.pow(omega, 2.0);
		}
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
	 * Calculates and performs an in-place cosine taper on an incoming data set.
	 * Used for windowing for performing FFT.
	 *
	 * @param dataSet The dataset to have the taper applied to.
	 * @param taperW Width of taper to be used
	 * @return Value corresponding to power loss from application of taper.
	 */
	public static double cosineTaperInPlace(double[] dataSet, double taperW) {
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
		for (double datum : data) {
			sumData += datum;
		}
		final double meanData = sumData / data.length;
		for (int i = 0; i < data.length; i++)
			ret[i] = data[i] - meanData;
		return ret;
	}

	public static double[] normData(int[] data) {
		double[] ret = new double[data.length];
		double sumData = 0.0;
		for (int datum : data) {
			sumData += datum;
		}
		final double meanData = sumData / data.length;
		for (int i = 0; i < data.length; i++)
			ret[i] = data[i] - meanData;
		return ret;
	}

	/**
	 * Compute complex deconvolution
	 */
	public static Complex[] complexDeconvolution(Complex[] f, Complex[] g) {
		if (f.length != g.length)
			throw new IllegalArgumentException("both arrays must have same length. " + f.length + " " + g.length);
		
		Complex[] ret = new Complex[f.length];
		for (int i = 0; i < f.length; i++)
			ret[i] = f[i].divide(g[i]);
		return ret;
	}

	/**
	 * Compute complex convolution
	 */
	public static Complex[] complexConvolution(Complex[] f, Complex[] g) {
		if (f.length != g.length)
			throw new IllegalArgumentException("both arrays must have same length. " + f.length + " " + g.length);
		Complex[] ret = new Complex[f.length];
		for (int i = 0; i < f.length; i++)
			ret[i] = f[i].multiply(g[i]);
		return ret;
	}

	/**
	 * Compute amplitude of complex spectra
	 */
	public static double[] getSpectraAmplitude(Complex[] spectra) {
		final double[] ret = new double[spectra.length];
		for (int i = 0; i < spectra.length; i++) {
			ret[i] = spectra[i].abs();
		}
		return ret;
	}

	/**
	 * Compute correlation
	 */
	public static double[] correlate(double[] fdata, double[] gdata) {
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
		Complex[] fTrans = processFft(fdataPadded);
		Complex[] gTrans = processFft(gdataPadded);
		for (int i = 0; i < fTrans.length; i++)
			fTrans[i] = fTrans[i].multiply(gTrans[i].conjugate());
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

	public static double[] generateFreqArray(double startFreq, double endFreq, int numFreqs) {
		if (numFreqs < 0) {
			return new double[0];
		}
		double temp = Math.min(startFreq, endFreq);
		endFreq = Math.max(startFreq, endFreq);
		startFreq = temp;
		double[] frequencies = new double[numFreqs];
		double freqSpacing = (endFreq - startFreq) / numFreqs;
		for (int i = 0; i < frequencies.length; ++i) {
			frequencies[i] = startFreq + (freqSpacing * i);
		}
		return frequencies;
	}

	/**
	 * Builds amplitude spectra of trace. proper response function out of RESP
	 * file.
	 * 
	 * @param trace
	 *            the trace array.
	 * @param verboseDebug
	 *            true for verbose debug messages
	 * @return the noise spectra.
	 */
	public static Spectra getNoiseSpectra(int[] trace, Response response, Date date, Channel channel, boolean verboseDebug) {
		// Init error string
		logger.debug("Getting noise spectra");
		String errString = "";

		final Response.FreqParameters fp = Response.getFreqParameters(trace.length, 1000.0 / channel.getSampleRate());
		final double[] frequenciesArray = generateFreqArray(fp.startFreq, fp.endFreq, fp.numFreq);

		// Make a copy of data since we gonna modify it
		double[] traceCopy = new double[trace.length];
		for (int i = 0; i < trace.length; i++)
			traceCopy[i] = trace[i];

		// Norm the data: remove trend
		traceCopy = normData(traceCopy);

		// Apply Hanning window
		traceCopy = windowHanning(traceCopy);

		// Do FFT and get imag and real parts of the data spectrum
		final Complex[] noise_spectra = processFft(traceCopy);
		Complex[] resp = null;
		try {
			resp = response.getResp(date, fp.startFreq, fp.endFreq, Math.min(noise_spectra.length, fp.numFreq));
		} catch (Exception e) {
			errString = "Can't get response for channel " + channel.getName() + ": ";
			logger.error(errString, e);	
		}
		return new Spectra(date, noise_spectra, frequenciesArray, resp, fp.sampFreq, channel, errString);
	}

	/**
	 * \ingroup sscdns_process_private_functions \brief Function for processing
	 * fft. \note See http://www.w.org/doc/fftw_2.html#SEC5 for comments on how
	 * \note fft works
	 * 
	 * @param indata
	 *            the input data, count of points must be power of 2
	 * @return the FFT output.
	 */

	public static Complex[] processFft(double[] indata) {
		int n = indata.length;
		DoubleFFT_1D fft = new DoubleFFT_1D(n);
		fft.realForward(indata);
		Complex[] ret;
		int l;
		if(n%2==0){
			l = n/2;
			ret = new Complex[l+1];
			for (int k = 0; k <= l; k++) {
				ret[k] = new Complex(k==l?indata[1]:indata[2*k], (k==0 || k==l) ? 0 : indata[2*k+1]);
			}
		} else {
			l = (n-1)/2;
			ret = new Complex[l+1];
			for (int k = 0; k <= l; k++) {
				double im;
				if(k==0){
					im=0;
				} else if(k==l) {
					im=indata[1];
				} else {
					im=indata[2*k+1];
				}
				ret[k] = new Complex(indata[2*k], im);
			}
		}
		return ret;
	}
	
	/**
	 * Real FFT processing
	 * 
	 * @param indata
	 *            data to process, count of points must be even
	 * @return half of symmetric complex spectra
	 */
	public static Complex[] processFft_Even(double[] indata) {
		RealDoubleFFT_Even fft = new RealDoubleFFT_Even(indata.length);
		fft.transform(indata);

		final int outLen = indata.length / 2;
		final Complex[] ret = new  Complex[outLen];
		for (int k = 0; k < outLen; k++) {
			ret[k] = new Complex(indata[k], k == 0 ? 0 : indata[indata.length - k]);
		}
		return ret;
	}

	/**
	 * Inverse FFT processing
	 * 
	 * @param indata
	 *            spectra to process, count of points must be power of 2
	 */
	
	private static double[] inverseFft(Complex[] indata) {
		DoubleFFT_1D fft = new DoubleFFT_1D(indata.length * 2-2);
		double[] dataToProcess = new double[indata.length * 2-2];
		for (int k = 0; k < indata.length-1; k++) {
			dataToProcess[2*k] = indata[k].getReal();
			dataToProcess[2*k+1] = (k==0?indata[indata.length-1].getReal():indata[k].getImaginary());
		}
		fft.realInverse(dataToProcess, true);
		return dataToProcess;
	}
	
	/**
	 * Inverse complex symetric FFT processing
	 * 
	 * @param indata
	 * 			  half of spectra to process
	 */
	public static double[] inverseFft_Even(Complex[] indata) {
		RealDoubleFFT_Even fft = new RealDoubleFFT_Even(indata.length * 2);
		double[] dataToProcess = new double[indata.length * 2];
		for (int k = 0; k < indata.length; k++) {
			dataToProcess[k] = indata[k].getReal();
			dataToProcess[dataToProcess.length - k - 1] = indata[k].getImaginary();
		}
		fft.inverse(dataToProcess);
		return dataToProcess;
	}

	/**
	 * Logariphm with base 2
	 */
	public static double log2(double x) {
		return Math.log10(x) / Math.log10(2.0);
	}

	static public int[] padArray(int[] original, int[] toAdd) {
		// so as Mac OSX java doesn't contain Arrays.copyOf method
		int[] ret = new int[original.length + toAdd.length];
		System.arraycopy(original, 0, ret, 0, original.length);
		// --------
		System.arraycopy(toAdd, 0, ret, original.length, toAdd.length);
		return ret;
	}

	/**
	 * Perform fractional-octave (variable length smoothing) over a series of data, used for plotting
	 * PSDs, etc. A moving-average value is cached along with the points in range in order to speed
	 * up the calculation of this data. The smoothing uses the lower fractional-octave for the
	 * smoothing and centers the data being averaged at that point, so it is not strictly speaking
	 * a fraction of the octave on either side of the data. This allows the smoothing to be reasonably
	 * precise while still efficient, and to gracefully taper away as we reach the end of the data.
	 * The fraction of the octave used to smooth is 1/{@value #SMOOTHING_FACTOR}.
	 * @param frequenciesArray array of frequencies associated with each point in the PSD
	 * @param psdData array of the PSD-like values being plotted
	 * @param startIdx first index of PSD-like to analyze from (some particularly low frequency values
	 * may have predominantly noise and be cut off)
	 * @return Smoothed version of the data using 1/xth octave where x is {@value #SMOOTHING_FACTOR}
	 */
	public static double[] getSmoothedPSD(double[] frequenciesArray, double[] psdData, int startIdx) {
		assert(frequenciesArray.length == psdData.length);
		double[] smoothedData = new double[psdData.length];
		BigDecimal windowedRunningTotal = BigDecimal.valueOf(0.);
		int currentPointIndexInQueue = 0; // current point is center value -- where in queue it is
		int nextPointToLoad = startIdx; // index of first point in data not yet added to queue

		// indices here keep track of simulated queue structure backed by psdData array
		int queueStartIndex = nextPointToLoad; // index of frequency at the head of the queue
		int queueEndIndex = nextPointToLoad;
		int size = 0; // increments with end index, decrements when start index increments

		for (int i = startIdx; i < frequenciesArray.length; ++i) {

			// add new points to the running total until we have enough to fit our current window
			while (currentPointIndexInQueue >= size) {
				double anotherPoint = psdData[nextPointToLoad];
				++queueEndIndex;
				++size; // manually update based on loop conditional, increases with end index too
				windowedRunningTotal =
						windowedRunningTotal.add(BigDecimal.valueOf(anotherPoint));
				++nextPointToLoad;
			}

			// use a sampling range of 1/4 of the octave at the frequency in question
			// which means half of that -- smoothing *radius* -- is 1/8
			double freqLowerBound = frequenciesArray[i] / Math.pow(2., 1./SMOOTHING_FACTOR);

			// pop off any points in the list lower than the bounding frequency we calculated
			while (queueStartIndex < queueEndIndex) { // conditional is true if queue size > 0
				double peekFreq = frequenciesArray[queueStartIndex];
				// control if points are bound by cutoff based on ordering determined above
				if (peekFreq > freqLowerBound) {
					break;
				}
				// remove item at front of queue, subtract its value from the running total
				double removed = psdData[queueStartIndex];
				++queueStartIndex; // now first point of cachedPoints is next frequency up
				--size; // start index +1 means size decreases by 1
				windowedRunningTotal =
						windowedRunningTotal.subtract(BigDecimal.valueOf(removed));
				--currentPointIndexInQueue; // removing that point shifts all points to the left
			}

			// now remove more points if the current point is past the center of the data
			if (nextPointToLoad >= psdData.length) {
				// note that because we sample an equal num. of points on either side of data
				// that the center of the data should be the location of the current point
				// if length of data is 5, midpoint is 2 (indices start at 0) -- 2 = (5-1)/2
				int midpoint = (size - 1) / 2;
				int pointsToTrim = currentPointIndexInQueue - midpoint;
				for (int k = 0; k < pointsToTrim; ++k) {
					// remove item at front of queue, subtract value from the running total
					double removed = psdData[queueStartIndex];
					++queueStartIndex; // now first point of cachedPoints is next frequency up
					--size;
					windowedRunningTotal =
							windowedRunningTotal.subtract(BigDecimal.valueOf(removed));
					--currentPointIndexInQueue; // everything is shifted over by 1
				} // end of for loop (points to trim)
			} else {
				// sampling radius is twice len. of cached data left of current point, plus that point
				// if current index is 2, expected length is 5 -- 5 = 2*2+1
				// again, recall that indices start at 0, so this is THIRD entry in list
				int expectedLength = (currentPointIndexInQueue * 2) - 1;
				while (size < expectedLength && nextPointToLoad < psdData.length) {
					double itemToAdd = psdData[nextPointToLoad];
					windowedRunningTotal = windowedRunningTotal.add(BigDecimal.valueOf(itemToAdd));
					++queueEndIndex;
					++size;
					++nextPointToLoad;
				} // end of while loop
			}

			// now it's possible that we have reached the end of the list in the above iteration
			// where we still had points to add. If so, one more double check that the point of
			// interest is actually at the center of the data
			if (nextPointToLoad >= psdData.length) {
				int midpoint = ((size - 1) / 2);
				int pointsToTrim = currentPointIndexInQueue - midpoint;
				for (int k = 0; k < pointsToTrim; ++k) {
					// remove item at front of queue, subtract value from the running total
					double removed = psdData[queueStartIndex];
					++queueStartIndex;
					--size;
					windowedRunningTotal = windowedRunningTotal.subtract(BigDecimal.valueOf(removed));
					--currentPointIndexInQueue; // everything is shifted over by 1
				}
			}

			// and now at last it is time to crunch the number we have in the windowed total variable
			smoothedData[i] = windowedRunningTotal.doubleValue() / size;

			++currentPointIndexInQueue; // next point in list is one past the current point
		}
		return smoothedData;
	}

}
