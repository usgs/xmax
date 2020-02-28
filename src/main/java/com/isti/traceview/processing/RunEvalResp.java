package com.isti.traceview.processing;

import com.isti.jevalresp.ComplexBlk;
import com.isti.jevalresp.OutputGenerator;
import com.isti.jevalresp.RespFileParser;
import com.isti.jevalresp.RunExt;
import com.isti.traceview.TraceViewException;
import edu.iris.Fissures.IfNetwork.Response;
import edu.sc.seis.seisFile.segd.Trace;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.apache.commons.math3.complex.Complex;

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

	public Response getResponseFromFile(Date date, String respReader) throws TraceViewException {
		String[] staArr = null;
		String[] chaArr = null;
		String[] netArr = null;
		String[] siteArr = null;
		final RespFileParser parserObj;
		final String inFName = "(reader)";
		parserObj = new RespFileParser(new ByteArrayInputStream(respReader.getBytes(
				StandardCharsets.UTF_8)), inFName);
		if (parserObj.getErrorFlag()) {
			// error creating parser object;
			throw new TraceViewException("Error in 'stdin' data:  " + parserObj.getErrorMessage());
		}
		parserObj.findChannelId(staArr, chaArr, netArr, siteArr, date, null);
		// read and parse response data from input:
		final Response respObj = parserObj.readResponse();
		parserObj.close();
		if (respObj == null) {
			throw new TraceViewException("Unable to parse response file: " + parserObj.getErrorMessage());
		}
		return respObj;
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
	public Complex[] generateResponse(double minFreqValue, double maxFreqValue, int numberFreqs, Date date, String respReader)
			throws TraceViewException {
		Response respObj = getResponseFromFile(date, respReader);
		return generateResponse(minFreqValue, maxFreqValue, numberFreqs, respObj);
	}

	public Complex[] generateResponse(double minFreqValue, double maxFreqValue, int numberFreqs, Response response) throws TraceViewException {
		final OutputGenerator outGenObj = new OutputGenerator(response);
		this.minFreqValue = minFreqValue;
		this.maxFreqValue = maxFreqValue;
		this.numberFreqs = numberFreqs;
		Complex[] spectra = null;
		final String inFName = "(reader)";
		if (checkGenerateFreqArray()) // check/generate frequencies array
		{

			// check validity of response:
			if (!outGenObj.checkResponse()) {
				// error in response; set error code & msg
				throw new TraceViewException(
						"Error in response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
			}
			// response checked OK; do normalization:
			if (!outGenObj.normalizeResponse(startStageNum, stopStageNum)) {
				// normalization error; set error message
				throw new TraceViewException(
						"Error normalizing response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
			}
			// response normalized OK; calculate output:
			if (!outGenObj
					.calculateResponse(frequenciesArray, logSpacingFlag, outUnitsConvIdx, startStageNum,
							stopStageNum)) {
				// calculation error; set error message
				throw new TraceViewException(
						"Error calculating response from \"" + inFName + "\":  " + outGenObj.getErrorMessage());
			}
			// get the frequency of sensitivity
			frequencyOfSensitivity = outGenObj.getCalcSenseFrequency();
			sensitivity = outGenObj.getCalcSensitivity();
			inputUnits = OutputGenerator.toUnitConvIndex(outGenObj.getFirstUnitProc());
			double[] calcFreqArray = outGenObj.getCalcFreqArray();

			// final AmpPhaseBlk ampPhaseArray[] = outGenObj.getAmpPhaseArray();
			ComplexBlk[] spectraBlk = outGenObj.getCSpectraArray();
			spectra = new Complex[spectraBlk.length];
			for (int i = 0; i < spectraBlk.length; i++) {
				spectra[i] = new Complex(spectraBlk[i].real, spectraBlk[i].imag);
				if (verboseDebug)
					System.out.println("resp[" + i + "]: r= " + spectra[i].getReal() + ", i= " +
							spectra[i].getImaginary() + ", freq=" + calcFreqArray[i]);
			}
			System.out.println(outGenObj.getRespInfoString());
			System.out.println(outGenObj.getStagesListStr());

		}
		return spectra;
	}
}
