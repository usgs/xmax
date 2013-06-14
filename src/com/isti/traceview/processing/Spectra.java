package com.isti.traceview.processing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;

import com.isti.jevalresp.OutputGenerator;
import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.data.Channel;
import com.isti.traceview.data.Response;

import edu.sc.seis.fissuresUtil.freq.Cmplx;

/**
 * This class holds all data to plot channel's spectra and psd
 * 
 * @author Max Kokoulin
 */
public class Spectra {
	private static Logger lg = Logger.getLogger(Spectra.class);
	/**
	 * Noise Spectra.
	 */
	Date date = null;
	private final Cmplx[] spectra;
	/**
	 * The frequency array.
	 */
	private final double[] frequenciesArray;
	private final Cmplx[] resp;
	private final double sampFreq;
	private final Channel channel;
	private final String err;

	/**
	 * @param date
	 *            time of beginning of trace interval used to build spectra, we will find
	 *            deconvolving responses for this date.
	 * @param spectra
	 *            complex spectra
	 * @param frequenciesArray
	 *            array of frequencies used to build spectra
	 * @param resp
	 *            complex response
	 * @param sampleRate
	 *            sample rate of trace
	 * @param sampFreq
	 *            frequency interval (see {@link Response#FreqParameters})
	 * @param name
	 *            trace name
	 * @param err
	 *            this string contains errors during building spectra and response.
	 */
	public Spectra(Date date, Cmplx[] spectra, double[] frequenciesArray, Cmplx[] resp, double sampFreq, Channel channel, String err) {
		this.date = date;
		this.spectra = spectra;
		this.frequenciesArray = frequenciesArray;
		this.resp = resp;
		this.sampFreq = sampFreq;
		this.channel = channel;
		this.err = err;
	}

	private double getSampleRate(){
		return 1000.0/channel.getSampleRate();
	}
	
	/**
	 * Get complex spectra
	 */
	public Cmplx[] getSpectra() {
		return spectra;
	}

	/**
	 * Get frequency array used in spectra building
	 */
	public double[] getFrequencies() {
		return frequenciesArray;
	}

	/**
	 * Get response
	 */
	public Cmplx[] getResp() {
		return resp;
	}

	/**
	 * Get trace name
	 */
	public String getName() {
		return channel.getName();
	}
	
	public String getChannelName() {
		return channel.getChannelName();
	}
	
	public String getLocationName() {
		return channel.getLocationName();
	}
	
	public String getNetworkName() {
		return channel.getNetworkName();
	}
	
	public String getStationName() {
		return channel.getStation().getName();
	}
	
	public Channel getChannel(){
		return channel;
	}

	/**
	 * Get error messages during spectra and responses computation
	 */
	public String getError() {
		return err;
	}

	/**
	 * Get frequency for first spectra value
	 */
	public double getStartFreq() {
		return frequenciesArray[0];
	}

	/**
	 * Get frequency for last spectra value
	 */
	public double getEndFreq() {
		return frequenciesArray[frequenciesArray.length - 1];
	}

	/**
	 * Get amplitude of spectra
	 * 
	 * @param isDeconvolve
	 *            flag if we use deconvolution
	 * @param respToConvolve
	 *            response for deconvolution
	 * @return amplitude of complex spectra
	 */
	public double[] getSpectraAmp(boolean isDeconvolve, String respToConvolve) {
		Cmplx[] processed = copyOf(spectra, spectra.length, spectra.getClass());
		if (isDeconvolve && resp != null) {
			processed = IstiUtilsMath.complexDeconvolution(spectra, resp);
		}
		if (respToConvolve != null && !respToConvolve.equals("None")) {
			File respFile = new File(TraceView.getConfiguration().getResponsePath() + File.separator + respToConvolve);
			Response respExternal = Response.getResponse(respFile);
			if (respExternal != null) {
				try {
					Cmplx[] respExt = respExternal.getResp(date, getStartFreq(), getEndFreq(), Math.min(processed.length, frequenciesArray.length));
					// Cmplx[] respExt = respExternal.getResp(getStartFreq(), getEndFreq(), frequenciesArray.length);
					// respExt = copyOf(respExt, Math.min(processed.length,
					// frequenciesArray.length));
					processed = IstiUtilsMath.complexConvolution(processed, respExt);
				} catch (TraceViewException e) {
					lg.error("Cant convolve with response " + respToConvolve + ": " + e);
				}
			}
		}
		return IstiUtilsMath.getSpectraAmplitude(processed);
	}

	/**
	 * Compute PSD for this spectra
	 */
	public double[] getPSD(int inputUnits) {
		//log("Spectra", spectra);
		//log("RESP", resp);
		Cmplx[] deconvolved = IstiUtilsMath.complexDeconvolution(spectra, resp);
		//log("Deconvolved", deconvolved);
		double[] psd = new double[deconvolved.length];
		for (int i = 0; i < deconvolved.length; i++) {
			psd[i] = (deconvolved[i].r * deconvolved[i].r + deconvolved[i].i * deconvolved[i].i) / (getSampleRate() / 2.0 * getSampleRate() / 2.0) * 2.0
					* sampFreq;
		}
		switch (inputUnits) {
		case OutputGenerator.DISPLACE_UNIT_CONV:
			IstiUtilsMath.dispToAccel(psd, sampFreq, spectra.length);
			break;
		case OutputGenerator.VELOCITY_UNIT_CONV:
			IstiUtilsMath.velToAccel(psd, sampFreq, spectra.length);
			break;
		default:
			; // Do nothing
			break;
		}
		return psd;
	}

	/**
	 * Get amplitude of spectra as jFreeChart's series
	 */
	public XYSeries getSpectraSeries(boolean isDeconvolve, String respToConvolve) {
		XYSeries series = new XYSeries(getName());
		double[] out = getSpectraAmp(isDeconvolve, respToConvolve);
		for (int i = 1; i < spectra.length; i++) {
			double x = 1.0 / frequenciesArray[i];
			double y = out[i];
			series.add(x, y);
		}
		return series;
	}

	/**
	 * Get PSD as jFreeChart's series
	 */
	public XYSeries getPSDSeries(int inputUnits) {
		XYSeries series = new XYSeries(getName());
		double[] out = getPSD(inputUnits);
		for (int i = 1; i < spectra.length; i++) {
			double x = 1.0 / frequenciesArray[i];
			double y = 10.0 * Math.log10(out[i]);
			series.add(x, y);
		}
		return series;
	}

	public void printout() {
		try {
			PrintStream pStr = null;
			pStr = new PrintStream(new BufferedOutputStream(new FileOutputStream("OutFile.txt")));
			for (int i = 0; i < spectra.length; i++) {
				pStr.println("freq=" + frequenciesArray[i] + ", r=" + spectra[i].r + ", i=" + spectra[i].i + ", mag=" + spectra[i].mag());
			}
			pStr.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
	}
	
	public static void log(String name, Cmplx[] spectra){
		System.out.println("-----------------------------------------------------------------------");
		System.out.println(name);
		System.out.println("-----------------------------------------------------------------------");
		for (int i = 0; i < spectra.length; i++) {
			System.out.println(/*"r=" + spectra[i].r + ", i=" + spectra[i].i + ", mag=" + */spectra[i].mag());
		}
	}

	/**
	 * This code was copied from Java 6 Arrays class sources. In Java 5 this class has not such
	 * method.
	 */
	public static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
		T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength] : (T[]) Array.newInstance(newType.getComponentType(),
				newLength);
		System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
		return copy;
	}
}
