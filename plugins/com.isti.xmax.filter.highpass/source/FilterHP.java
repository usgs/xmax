import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.processing.IFilter;
import com.isti.traceview.processing.HPFilterException;

/**
 * High-pass Butterworth filter Algorithm is from Stearns, 1975
 */

public class FilterHP implements IFilter {
	/**
	 * number of filter sections (1 section = 2 poles)
	 */
	int order = 0;
	double cutFrequency = Double.NaN;

	// filter coefficients, array size is order parameter
	double[] a;
	double[] b;
	double[] c;
	// 10 pairs of frequency and power gain
	// (graf(1,k) and graf(2,k) for k=1 thru 10)
	double[][] graf = new double[2][10];
	
	public int getMaxDataLength(){
		return Integer.MAX_VALUE;
	}

	/**
	 * @param order
	 *            int number of sections (two poles per section)
	 * @param cutFrequency
	 *            double cutoff frequency in Hz
	 */
	public FilterHP(int order, double cutFrequency) {
		this.order = order;
		this.cutFrequency = cutFrequency;
		a = new double[order];
		b = new double[order];
		c = new double[order];
	}

	/**
	 * Default constructor
	 */
	public FilterHP() {
		this(2, 50);
	}

	/**
	 * design routine
	 * 
	 * @param channel
	 *            trace to retrieve information
	 */
	synchronized public void init(RawDataProvider channel) {
		double sampleRate = channel.getSampleRate() / 1000.0;
		double wcp = Math.sin(cutFrequency * Math.PI * sampleRate) / Math.cos(cutFrequency * Math.PI * sampleRate);
		for (int k = 0; k < order; k++) {
			double cs = Math.cos(Math.PI * (2.0 * (k + 1 + order) - 1) / (4.0 * order));
			a[k] = 1.0 / (1.0 + wcp * wcp - 2.0 * wcp * cs);
			b[k] = 2.0 * (wcp * wcp - 1.0) * a[k];
			c[k] = (1.0 + wcp * wcp + 2.0 * wcp * cs) * a[k];
		}
		for (int k = 0; k < 10; k++) {
			graf[1][k] = 0.01 + 0.98 * k / 9.0;
			double x = Math.atan(wcp * Math.pow(1.0 / graf[1][k] - 1.0, -1.0 / (4 * order)));
			graf[0][k] = x / (Math.PI * sampleRate);
		}
	}

	/**
	 * Performs high-pass Butterworth filtering of a time series.
	 * 
	 * @param data =
	 *            data array
	 * @param length =
	 *            number of samples in data array
	 * @return filtered data array
	 */
	synchronized public double[] filter(double[] data, int length) 
	throws HPFilterException
	{
		if (data.length > length)
			throw new HPFilterException("Requested filtering length exceeds provided array length");
		//int mean = new Double(demean(data, length)).intValue();
		double[][] f = new double[order + 1][3];
		for (int i = 0; i <= order; i++) {
			for (int j = 0; j < 3; j++) {
				f[i][j] = 0.0;
			}
		}
		// loop over each sample in input series
		for (int i = 0; i < length; i++) {
			f[0][2] = data[i];
			// Go through order filter sections.
			for (int j = 0; j < order; j++) {
				double temp = a[j] * (f[j][2] - f[j][1] - f[j][1] + f[j][0]);
				f[j + 1][2] = temp - b[j] * f[j + 1][1] - c[j] * f[j + 1][0];
			}
			// Update all past values of signals.
			for (int j = 0; j <= order; j++) {
				f[j][0] = f[j][1];
				f[j][1] = f[j][2];
			}
			//data[i] = f[order][2] + mean;
			data[i] = f[order][2];
		}
		return data;
	}

	public String getName() {
		return "HP";
	}

	public boolean needProcessing() {
		return true;
	}

	/**
	 * remove mean from a buffer
	 * 
	 * **NOTE: This will be done with the mean button fix
	 */
	@SuppressWarnings("unused")
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

	public double getCutFrequency() {
		return cutFrequency;
	}

	public boolean equals(Object o) {
		if (o instanceof FilterLP) {
			FilterLP arg = (FilterLP) o;
			if ((order == arg.getOrder()) && (cutFrequency == arg.getCutFrequency())) {
				return true;
			}
		}
		return false;
	}
}
