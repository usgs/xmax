package com.isti.traceview.transformations.psd;

/**
 * Noise model
 */
class NoiseModel {
	private final static int PERIOD = 0;
	private final static int A = 1;
	private final static int B = 2;
	private final static double NHNM_DATA[][] = new double[][] { { 0.1, -108.73, -17.23 }, { 0.22, -150.34, -80.50 },
			{ 0.32, -122.31, -23.87 }, { 0.80, -116.85, 32.51 }, { 3.80, -108.48, 18.08 }, { 4.60, -74.66, -32.95 },
			{ 6.30, 0.66, -127.18 }, { 7.90, -93.37, -22.42 }, { 15.40, 73.54, -162.98 }, { 20.00, -151.52, 10.01 },
			{ 354.80, -206.66, 31.63 }, { 10000, -206.66, 31.63 } };
	private final static double NLNM_DATA[][] = new double[][] { { 0.1, -162.36, 5.64 }, { 0.17, -166.7, 0 },
			{ 0.4, -170, -8.3 }, { 0.8, -166.4, 28.9 }, { 1.24, -168.6, 52.48 }, { 2.4, -159.98, 29.81 },
			{ 4.3, -141.1, 0 }, { 5, -71.36, -99.77 }, { 6, -97.26, -66.49 }, { 10, -132.18, -31.57 },
			{ 12, -205.27, 36.16 }, { 15.6, -37.65, -104.33 }, { 21.9, -114.37, -47.1 }, { 31.6, -160.58, -16.28 },
			{ 45, -187.5, 0 }, { 70, -216.47, 15.7 }, { 101, -185, 0 }, { 154, -168.34, -7.61 }, { 328, -217.43, 11.9 },
			{ 600, -258.28, 26.6 }, { 10000, -346.88, 48.75 }, { 100000, -346.88, 48.75 } };

	/**
	 * Evaluation of noise model for a given model (low or high) and period
	 * 
	 * @param data
	 *            noise model data
	 * @param p
	 *            period
	 * @return noise value
	 */
	private static double fnnm(double[][] data, double p) {
		final double nnm;
		final int lastIndex = data.length - 1;

		if (p < data[0][PERIOD]) // if value is less than minimum
		{
			if (data == NLNM_DATA) // if low noise model
			{
				// New model undefined, use old model
				nnm = -168.0;
			} else // high noise model
			{
				// New model undefined
				nnm = 0.0;
			}
		} else if (p > data[lastIndex][PERIOD]) // if value is greater than
												// maximum
		{
			// New model undefined
			nnm = 0.0;
		} else {
			int k;
			for (k = 0; k < lastIndex; k++)
				if (p < data[k + 1][PERIOD])
					break;
			nnm = data[k][A] + data[k][B] * Math.log10(p);
		}

		return nnm;
	}

	/**
	 * Evaluation of low noise model for a given period output in Acceleration
	 * 
	 * @param p
	 *            period
	 * @return new noise model value
	 */
	static double fnlnm(double p) {
		return fnnm(NLNM_DATA, p);
	}

	/**
	 * Evaluation of high noise model for a given period output in Acceleration
	 * 
	 * @param p
	 *            period
	 * @return new high noise model value
	 */
	static double fnhnm(double p) {
		return fnnm(NHNM_DATA, p);
	}
}
