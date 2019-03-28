package com.isti.traceview.processing;

import com.isti.jevalresp.ComplexBlk;
import com.isti.jevalresp.OutputGenerator;
import com.isti.jevalresp.RespFileParser;
import com.isti.jevalresp.RunExt;
import com.isti.traceview.TraceViewException;
import edu.iris.Fissures.IfNetwork.Response;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import java.io.Reader;
import java.util.Date;

/**
 * JEvalResp related logic.
 */
public class RunEvalResp extends RunExt {
	protected final boolean verboseDebug;

	public double frequencyOfSensitivity = 0.0;
	public double sensitivity = 0.0;
	public int inputUnits = OutputGenerator.UNIT_CONV_DEFIDX;

	/**
	 * Creates a run evalresp object.
	 * 
	 * @param logSpacingFlag
	 *            log spacing flag
	 * @param verboseDebug
	 *            true for verbose debug messages
	 */
	public RunEvalResp(boolean logSpacingFlag, boolean verboseDebug) {
		this.logSpacingFlag = logSpacingFlag;
		this.verboseDebug = verboseDebug;
	}

	/**
	 * Gets the frequencies array.
	 * 
	 * @return the frequencies array.
	 */
	public double[] getFrequenciesArray() {
		return frequenciesArray;
	}

	/**
	 * Gets the frequency of sensitivity.
	 * 
	 * @return the frequency of sensitivity.
	 */
	public double getFrequencyOfSensitivity() {
		return frequencyOfSensitivity;
	}

	/**
	 * Computes complex response
	 * 
	 * @param minFreqValue
	 *            the minimum frequency to generate output for.
	 * @param maxFreqValue
	 *            the maximum frequency to generate output for.
	 * @param numberFreqs
	 *            the number of frequencies to generate output for.
	 * @param date
	 *            Date for which we want compute response
	 * @param respReader
	 *            response reader.
	 * @return an array of amplitude values.
	 */
	public Cmplx[] generateResponse(double minFreqValue, double maxFreqValue, int numberFreqs, Date date, Reader respReader)
			throws TraceViewException {
		Cmplx[] spectra = null;
		String[] staArr = null;
		String[] chaArr = null;
		String[] netArr = null;
		String[] siteArr = null;
		this.minFreqValue = minFreqValue;
		this.maxFreqValue = maxFreqValue;
		this.numberFreqs = numberFreqs;
		if (checkGenerateFreqArray()) // check/generate frequencies array
		{
			final String inFName = "(reader)";
			final RespFileParser parserObj = new RespFileParser(respReader, inFName);
			if (parserObj.getErrorFlag()) {
				// error creating parser object;
				throw new TraceViewException("Error in 'stdin' data:  " + parserObj.getErrorMessage());
			}
			parserObj.findChannelId(staArr, chaArr, netArr, siteArr, date, null);

			// read and parse response data from input:
			final Response respObj = parserObj.readResponse();
			if (respObj == null) {
				throw new TraceViewException("Unable to parse response file: " + parserObj.getErrorMessage());
			}
			// create output generator:
			final OutputGenerator outGenObj = new OutputGenerator(respObj);
			// check validity of response:
			if (!outGenObj.checkResponse()) {
				// error in response; set error code & msg
				throw new TraceViewException("Error in response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
			}
			// response checked OK; do normalization:
			if (!outGenObj.normalizeResponse(startStageNum, stopStageNum)) {
				// normalization error; set error message
				throw new TraceViewException("Error normalizing response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
			}
			// response normalized OK; calculate output:
			if (!outGenObj.calculateResponse(frequenciesArray, logSpacingFlag, outUnitsConvIdx, startStageNum, stopStageNum)) {
				// calculation error; set error message
				throw new TraceViewException("Error calculating response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
			}
			// get the frequency of sensitivity
			frequencyOfSensitivity = outGenObj.getCalcSenseFrequency();
			sensitivity = outGenObj.getCalcSensitivity();
			inputUnits = OutputGenerator.toUnitConvIndex(outGenObj.getFirstUnitProc());
			double[] calcFreqArray = outGenObj.getCalcFreqArray();

			// final AmpPhaseBlk ampPhaseArray[] = outGenObj.getAmpPhaseArray();
			ComplexBlk[] spectraBlk = outGenObj.getCSpectraArray();
			spectra = new Cmplx[spectraBlk.length];
			for (int i = 0; i < spectraBlk.length; i++) {
				spectra[i] = new Cmplx(spectraBlk[i].real, spectraBlk[i].imag);
				if (verboseDebug)
					System.out.println("resp[" + i + "]: r= " + spectra[i].r + ", i= " + spectra[i].i + ", freq=" + calcFreqArray[i]);
			}
			parserObj.close();
			System.out.println(outGenObj.getRespInfoString());
			System.out.println(outGenObj.getStagesListStr());
		}
		return spectra;
	}
}
