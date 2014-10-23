import com.isti.util.Math10;
import com.isti.util.database.DatabaseUtil;

/**
 * Noise model
 */
public class NoiseModel {
	public final static int PER = 0;
	public final static int A = 1;
	public final static int B = 2;
	protected final static double NHNM_DATA[][] = new double[][]{ { 0.1, -108.73, -17.23 }, { 0.22, -150.34, -80.50 }, { 0.32, -122.31, -23.87 },
			{ 0.80, -116.85, 32.51 }, { 3.80, -108.48, 18.08 }, { 4.60, -74.66, -32.95 }, { 6.30, 0.66, -127.18 }, { 7.90, -93.37, -22.42 },
			{ 15.40, 73.54, -162.98 }, { 20.00, -151.52, 10.01 }, { 354.80, -206.66, 31.63 }, { 10000, -206.66, 31.63 } };
	protected final static double NLNM_DATA[][] = new double[][]{ { 0.1, -162.36, 5.64 }, { 0.17, -166.7, 0 }, { 0.4, -170, -8.3 },
			{ 0.8, -166.4, 28.9 }, { 1.24, -168.6, 52.48 }, { 2.4, -159.98, 29.81 }, { 4.3, -141.1, 0 }, { 5, -71.36, -99.77 },
			{ 6, -97.26, -66.49 }, { 10, -132.18, -31.57 }, { 12, -205.27, 36.16 }, { 15.6, -37.65, -104.33 }, { 21.9, -114.37, -47.1 },
			{ 31.6, -160.58, -16.28 }, { 45, -187.5, 0 }, { 70, -216.47, 15.7 }, { 101, -185, 0 }, { 154, -168.34, -7.61 }, { 328, -217.43, 11.9 },
			{ 600, -258.28, 26.6 }, { 10000, -346.88, 48.75 }, { 100000, -346.88, 48.75 } };

	/**
	 * @return the NHNM data length
	 */
	public static int getNhnmDataLength() {
		return NHNM_DATA.length;
	}

	/**
	 * @return the NHNM data
	 * @param index
	 *            index of the data
	 * @param velocityFlag
	 *            true for velocity
	 * @param periodFlag
	 *            true if period
	 */
	public static double getNhnmData(int index, boolean velocityFlag, boolean periodFlag) {
		final double val = DatabaseUtil.convertToFrequency(NHNM_DATA[index][NoiseModel.PER], periodFlag);
		return val;
	}

	/**
	 * @return the NLNM data length
	 */
	public static int getNlnmDataLength() {
		return NLNM_DATA.length;
	}

	/**
	 * @return the NLNM data
	 * @param index
	 *            index of the data
	 * @param velocityFlag
	 *            true for velocity
	 * @param periodFlag
	 *            true if period
	 */
	public static double getNlnmData(int index, boolean velocityFlag, boolean periodFlag) {
		final double val = DatabaseUtil.convertToFrequency(NLNM_DATA[index][NoiseModel.PER], periodFlag);
		return val;
	}

	/**
	 * evaluation of noise model for a given model (low or high) and period
	 * 
	 * @param data
	 *            noise model data
	 * @param p
	 *            peroid
	 * @return noise value
	 */
	public static double fnnm(double[][] data, double p) {
		final double nnm;
		final int lastIndex = data.length - 1;

		if (p < data[0][PER]) // if value is less than minimum
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
		} else if (p > data[lastIndex][PER]) // if value is greater than maximum
		{
			// New model undefined
			nnm = 0.0;
		} else {
			int k;
			for (k = 0; k < lastIndex; k++)
				if (p < data[k + 1][PER])
					break;
			nnm = data[k][A] + data[k][B] * Math10.log10(p);
		}

		return nnm;
	}

	/**
	 * evaluation of low noise model for a given period output in Acceleration
	 * 
	 * @param p
	 *            peroid
	 * @return new noise model value
	 */
	public static double fnlnm(double p) {
		return fnnm(NLNM_DATA, p);
	}

	/**
	 * evaluation of low noise model for a given period with ouput in Acceleration or Velocity
	 * 
	 * @param p
	 *            peroid
	 * @param velocityFlag
	 *            true for velocity
	 * @return new low noise model value
	 */
	public static double fnlnm(double p, boolean velocityFlag) {
		return DatabaseUtil.convertToVel(fnlnm(p), p, velocityFlag);
	}

	/**
	 * evaluation of low noise model for a given frequency or period output in Acceleration or
	 * Velocity
	 * 
	 * @param val
	 *            value
	 * @param velocityFlag
	 *            true for velocity
	 * @param periodFlag
	 *            true if period
	 * @return new low noise model value
	 */
	public static double fnlnm(double val, boolean velocityFlag, boolean periodFlag) {
		final double p = DatabaseUtil.convertToFrequency(val, periodFlag);
		return fnlnm(p, velocityFlag);
	}

	/**
	 * evaluation of high noise model for a given period output in Acceleration
	 * 
	 * @param p
	 *            peroid
	 * @return new high noise model value
	 */
	public static double fnhnm(double p) {
		return fnnm(NHNM_DATA, p);
	}

	/**
	 * evaluation of high noise model for a given period with ouput in Acceleration or Velocity
	 * 
	 * @param p
	 *            peroid
	 * @param velocityFlag
	 *            true for velocity
	 * @return new high noise model value
	 */
	public static double fnhnm(double p, boolean velocityFlag) {
		return DatabaseUtil.convertToVel(fnhnm(p), p, velocityFlag);
	}

	/**
	 * evaluation of high noise model for a given peroid or frequency with ouput in Acceleration or
	 * Velocity
	 * 
	 * @param val
	 *            value
	 * @param velocityFlag
	 *            true for velocity
	 * @param periodFlag
	 *            true if period
	 * @return new high noise model value
	 */
	public static double fnhnm(double val, boolean velocityFlag, boolean periodFlag) {
		final double p = DatabaseUtil.convertToFrequency(val, periodFlag);
		return fnhnm(p, velocityFlag);
	}

	/**
	 * create sample Acceleration based on random value between NLNM and NHNM
	 * 
	 * @param lnm
	 *            low noise model value
	 * @param hnm
	 *            high noise model value
	 * @return sample value
	 */
	public static double createSample(double lnm, double hnm) {
		return lnm + (hnm - lnm) * Math.random();
	}

	/**
	 * create sample Acceleration based on random value between NLNM and NHNM
	 * 
	 * @param p
	 *            peroid
	 * @return sample value
	 */
	public static double createSample(double p) {
		final double lnm = fnlnm(p);
		final double hnm = fnhnm(p);
		return createSample(lnm, hnm);
	}

	/**
	 * create sample Acceleration or Velocity based on random value between NLNM and NHNM
	 * 
	 * @param p
	 *            peroid
	 * @param velocityFlag
	 *            true for velocity
	 * @return sample value
	 */
	public static double createSample(double p, boolean velocityFlag) {
		final double lnm = fnlnm(p, velocityFlag);
		final double hnm = fnhnm(p, velocityFlag);
		return createSample(lnm, hnm);
	}

	/**
	 * create sample Acceleration or Velocity based on random value between NLNM and NHNM
	 * 
	 * @param val
	 *            value
	 * @param velocityFlag
	 *            true for velocity
	 * @param periodFlag
	 *            true if period
	 * @return sample value
	 */
	public static double createSample(double val, boolean velocityFlag, boolean periodFlag) {
		final double lnm = fnlnm(val, velocityFlag, periodFlag);
		final double hnm = fnhnm(val, velocityFlag, periodFlag);
		return createSample(lnm, hnm);
	}
}
