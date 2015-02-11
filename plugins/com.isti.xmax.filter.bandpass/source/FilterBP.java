import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.IFilter;
import com.isti.traceview.processing.BPFilterException;

/**
 * <p>
 * Band-pass Butterworth filter Algorithm is from Stearns, 1975
 * </p>
 * <p>
 * The digital filter has ns filter sections in the cascade. The k'th section has the transfer
 * function with the transfer function:
 * </p>
 * <p>
 * h(z) = (a(k)*(z**4-2*z**2+1))/(z**4+b(k)*z**3+c(k)*z**2+d(k)*z+e(k))
 * </p>
 * <p>
 * Thus, if f(m) and g(m) are the input and output at time m*t, then
 * </p>
 * <p>
 * g(m) = a(k)*(f(m)-2*f(m-2)+f(m-4))-b(k)*g(m-1)-c(k)*g(m-2)-d(k)*g(m-3)-e(k)*g(m-4)
 * </p>
 */

public class FilterBP implements IFilter {

	int order = 0;
	double cutLowFrequency = Double.NaN;
	double cutHighFrequency = Double.NaN;

	// filter coefficients for each section, array size is order parameter
	double[] a;
	double[] b;
	double[] c;
	double[] d;
	double[] e;

	/**
	 * 20 pairs of frequency and power gain. graf(1,k) and graf(2,k) for k = 1 thru 20
	 */
	double[][] graf = new double[2][20];

	public int getMaxDataLength(){
		return Integer.MAX_VALUE;
	}
	
	/**
	 * @param order
	 *            int number of sections (each section = 4 poles: 2 low freq poles and 2 hi freq
	 *            poles)
	 * @param cutLowFrequency
	 *            double cutoff (3-db) frequency in Hz
	 * @param cutHighFrequency
	 *            double cutoff (3-db) frequency in Hz
	 */
	public FilterBP(int order, double cutLowFrequency, double cutHighFrequency) {
		this.order = order;
		this.cutLowFrequency = cutLowFrequency;
		this.cutHighFrequency = cutHighFrequency;
		a = new double[order];
		b = new double[order];
		c = new double[order];
		d = new double[order];
		e = new double[order];
	}

	/**
	 * Default constructor
	 */
	public FilterBP() {
		this(2, 0.1, 0.5);
	}

	/**
	 * Bandpass butterworth digital filter design subroutine
	 * 
	 * @param channel
	 *            trace to retrieve information
	 */
	synchronized public void init(RawDataProvider channel) {
		double sampleRate = channel.getSampleRate() / 1000.0;
		double w1 = Math.sin(cutLowFrequency * Math.PI * sampleRate) / Math.cos(cutLowFrequency * Math.PI * sampleRate);
		double w2 = Math.sin(cutHighFrequency * Math.PI * sampleRate) / Math.cos(cutHighFrequency * Math.PI * sampleRate);
		double wc = w2 - w1;
		double q = wc * wc + 2.0 * w1 * w2;
		double s = w1 * w1 * w2 * w2;

		for (int k = 0; k < order; k++) {
			double cs = Math.cos(Math.PI * (2.0 * (k + 1 + order) - 1) / (4.0 * order));
			double p = -2.0 * wc * cs;
			double r = p * w1 * w2;
			double x = 1.0 + p + q + r + s;
			a[k] = wc * wc / x;
			b[k] = (-4.0 - 2.0 * p + 2.0 * r + 4.0 * s) / x;
			c[k] = (6.0 - 2.0 * q + 6.0 * s) / x;
			d[k] = (-4.0 + 2.0 * p - 2.0 * r + 4.0 * s) / x;
			e[k] = (1.0 - p + q - r + s) / x;
		}
		graf[1][0] = 0.0001;
		graf[1][1] = 0.01;
		graf[1][2] = 0.0316228;
		graf[1][3] = 0.1;
		graf[1][4] = 0.251189;
		graf[1][5] = 0.5;
		graf[1][6] = 0.794328;
		graf[1][7] = 0.891251;
		graf[1][8] = 0.954993;
		graf[1][9] = 0.977237;
		for (int j = 0; j < 10; j++) {
			graf[1][19 - j] = graf[1][j];
		}

		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 10; i++) {
				int k = i * (1 - j) + (19 - i) * j;
				double x = Math.pow(1.0 / graf[1][k] - 1.0, 1.0 / (4.0 * order));
				double sq = Math.sqrt(wc * wc * x * x + 4.0 * w1 * w2);
				graf[0][k] = Math.abs(Math.atan(0.5 * (wc * x + (2.0 * (j + 1) - 3) * sq))) / (Math.PI * sampleRate);
			}
		}
	}

	/**
	 * Performs band-pass Butterworth filtering of a time series.
	 * 
	 * @param data =
	 *            data array
	 * @param length =
	 *            number of samples to filter
	 * @return filtered data array
	 */
	synchronized public double[] filter(double[] data, int length) 
	throws BPFilterException	
	{
		if (data.length > length)
			throw new BPFilterException("Requested filtering length exceeds provided array length");
		int mean = new Double(demean(data, length)).intValue();
		double[][] f = new double[order + 1][5];
		for (int i = 0; i <= order; i++) {
			for (int j = 0; j < 5; j++) {
				f[i][j] = 0.0;
			}
		}
		// loop over each sample in input series
		for (int i = 0; i < length; i++) {
			f[0][4] = data[i];
			// Go through order filter sections.
			for (int j = 0; j < order; j++) {
				double temp = a[j] * (f[j][4] - f[j][2] - f[j][2] + f[j][0]);
				f[j + 1][4] = temp - b[j] * f[j + 1][3] - c[j] * f[j + 1][2] - d[j] * f[j + 1][1] - e[j] * f[j + 1][0];
			}
			// Update all past values of signals.
			for (int j = 0; j <= order; j++) {
				f[j][0] = f[j][1];
				f[j][1] = f[j][2];
				f[j][2] = f[j][3];
				f[j][3] = f[j][4];
			}
			//data[i] = f[order][4] + mean;
			data[i] = f[order][4];
		}
		return data;
	}

	public String getName() {
		return "BP";
	}

	public boolean needProcessing() {
		return true;
	}

	/**
	 * remove mean from a buffer
	 */
	private double demean(double buf[], int n) {
		double sum = 0.0;
		for (int i = 0; i < n; i++) {
			sum = sum + buf[i];
		}
		sum = sum / n;
		
		//This removes mean from original data
		for (int i = 0; i < n; i++) {
			buf[i] = buf[i] - sum;
		}
		return sum;
	}

	public int getOrder() {
		return order;
	}

	public double getCutLowFrequency() {
		return cutLowFrequency;
	}

	public double getCutHighFrequency() {
		return cutHighFrequency;
	}

	public boolean equals(Object o) {
		if (o instanceof FilterBP) {
			FilterBP arg = (FilterBP) o;
			if ((order == arg.getOrder()) && (cutLowFrequency == arg.getCutLowFrequency()) && (cutHighFrequency == arg.getCutHighFrequency())) {
				return true;
			}
		}
		return false;
	}
}
