import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.RawDataProvider;
import com.isti.traceview.data.Response;
import com.isti.traceview.processing.IFilter;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.xmax.XMAX;
import com.isti.xmax.gui.XMAXframe;

import edu.sc.seis.fissuresUtil.freq.Cmplx;

public class Reconvolution extends JDialog implements IFilter, PropertyChangeListener {
	
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(Reconvolution.class);
	
	private final static int comboBoxHeight = 22;
	private final static int maxDataLength = 16385;
	private static boolean warningWasShown = false;
	private RawDataProvider channel = null;
	private JOptionPane optionPane;
	private boolean needProcessing = false;

	private JLabel convolveL;
	private JComboBox<Object> convolveCB;
	private static String respFile = null;

	/**
	 * Default constructor
	 */
	public Reconvolution() {
		super(XMAXframe.getInstance(), "Deconvolution/reconvolution", true);
		Object[] options = { "OK", "Close" };
		// Create the JOptionPane.
		optionPane = new JOptionPane(createDesignPanel(), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION, null, options, options[0]);
		// Make this dialog display it.
		setContentPane(optionPane);
		optionPane.addPropertyChangeListener(this);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change the JOptionPane's
				 * value property.
				 */
				optionPane.setValue("Close");
			}
		});
		pack();
		setLocationRelativeTo(super.getOwner());
		setVisible(true);
	}
	
	public int getMaxDataLength(){
		return maxDataLength;
	}

	/**
	 * design routine
	 * 
	 * @param channel
	 *            trace to retrieve information
	 */
	synchronized public void init(RawDataProvider channel) {
		this.channel = channel;
		warningWasShown = false;
	}

	/**
	 * Performs filtering of a time series.
	 * 
	 * @param data
	 *            = data array
	 * @param length
	 *            = number of samples in data array
	 * @return filtered data array
	 */
	synchronized public double[] filter(double[] data, int length) throws TraceViewException {
		
		if((data.length>maxDataLength) && !warningWasShown &&
			JOptionPane.showConfirmDialog(XMAX.getFrame(), "Too long data, processing could take time. Do you want to continue?", "Warning", JOptionPane.OK_CANCEL_OPTION)==JOptionPane.CANCEL_OPTION){
			warningWasShown = true;
			throw new TraceViewException("Filtering was calcelled by user");
		}
	
		//Make a copy of data since we gonna modify it.
		//Count limits
		double[] traceCopy;
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		double mean = 0;
		int traceCopyLength = 0;
		if(data.length%2==1){
			traceCopyLength = data.length-1; 
		} else {
			traceCopyLength = data.length; 
			}
		traceCopy = new double[traceCopyLength];
		for (int i = 0; i < traceCopyLength; i++){
			traceCopy[i] = data[i];
			mean = mean+data[i];
			if(data[i]>max){
				max = data[i];
			}
			if(data[i]<min){
				min = data[i];
			}
		}	
		mean = mean/traceCopyLength;
		
		final Response.FreqParameters fp = Response.getFreqParameters(traceCopy.length, 1000.0 / channel.getSampleRate());
		//final double[] frequenciesArray = RespUtils.generateFreqArray(fp.startFreq, fp.endFreq, fp.numFreq, false);

		// Norm the data: remove trend
		traceCopy = IstiUtilsMath.normData(traceCopy);

		// Apply Hanning window
		traceCopy = IstiUtilsMath.windowHanning(traceCopy);
		

		// Do FFT and get imag and real parts of the data spectrum
		Cmplx[] spectra = IstiUtilsMath.processFft_Even(traceCopy);

		// Get response
		Response response = XMAX.getDataModule().getResponse(channel.getNetworkName(), channel.getStation().getName(), channel.getLocationName(),channel.getChannelName());
		Cmplx[] resp = null;
		try {
			resp = response.getResp(channel.getTimeRange().getStartTime(), fp.startFreq, fp.endFreq, spectra.length);
			resp = normData(resp);
		
			//Remove signal in spectra where response is near 0
			spectra = removeExcessFrequencies(spectra, resp);
		} catch (TraceViewException e1) {
			throw new TraceViewException("File " + response.getFileName() + ": " + e1);
		} catch (ReconvolutionException e2) {
			logger.error("ReconvolutionException:", e2);
		}
		
		// Deconvolve
		try {	
			Cmplx[] deconvolved = null;
			deconvolved = IstiUtilsMath.complexDeconvolution(spectra, resp);
		
			// Convolve if needed
			Cmplx[] reconvolved = null;
			String selectedFileName = (String) convolveCB.getSelectedItem();
			if (!selectedFileName.equals("None")) {
				Response respExternal = Response.getResponse(new File(selectedFileName));
				if (respExternal != null) {
					Cmplx[] respExt;
					try {
						respExt = respExternal.getResp(channel.getTimeRange().getStartTime(), fp.startFreq, fp.endFreq, spectra.length);
						respExt = normData(respExt);
						reconvolved = IstiUtilsMath.complexConvolution(removeExcessFrequencies(deconvolved,respExt), respExt);
					} catch (TraceViewException e) {
						throw new TraceViewException("File " + respExternal.getFileName() + ": " + e);
					} catch (ReconvolutionException e) {
						logger.error("ReconvolutionException:", e);
					}
				}
			}
		
			if (reconvolved==null && deconvolved==null){
				return data;
			}

			// Reverse fourier transformation
			double[] inversedTrace = null;
			if(reconvolved !=null){
				inversedTrace = IstiUtilsMath.inverseFft_Even(reconvolved);
			} else if (deconvolved != null){
				inversedTrace = IstiUtilsMath.inverseFft_Even(deconvolved);
			}
		
			double inversedMax = Double.NEGATIVE_INFINITY;
			double inversedMin = Double.POSITIVE_INFINITY;
		
			//Count inversed trace limits
			for (int i = 0; i < inversedTrace.length; i++){
				if(data[i]>inversedMax){
					inversedMax = inversedTrace[i];
				}
				if(data[i]<inversedMin){
					inversedMin = inversedTrace[i];
				}
			}		
			double normCoeff = (max-min)/(inversedMax-inversedMin);
			double[] processedTrace = new double[data.length];
			for(int i = 0; i < inversedTrace.length; i++){
				processedTrace[i] = normCoeff*inversedTrace[i]+mean;
			}
			if(data.length%2==1){
				processedTrace[data.length-1] = processedTrace[data.length-2];
			} 			
			return processedTrace;
		} catch (IllegalArgumentException e) {
			logger.error("IllegalArgumentException:", e);
			System.exit(0);
			return null;
		}
	}
	
	public String getName() {
		return "Reconvolve";
	}

	public boolean needProcessing() {
		return needProcessing;
	}

	private JLabel getConvolveL() {
		if (convolveL == null) {
			convolveL = new JLabel();
			convolveL.setText("Convolve:");
		}
		return convolveL;
	}
	
	private JComboBox<Object> getConvolveCB() {
		if (convolveCB == null) {
			convolveCB = new JComboBox<Object>();
			FontMetrics fontMetrics = convolveCB.getFontMetrics(convolveCB.getFont());
			List<String> options = new ArrayList<String>();
			options.add("None");
			try {
				XMAX.getDataModule();	
				for (String respFile: DataModule.getAllResponseFiles()) {
					options.add(respFile);
					int width = fontMetrics.stringWidth(respFile);
					if ((width + comboBoxHeight) > convolveCB.getPreferredSize().getWidth()) {
						convolveCB.setPreferredSize(new java.awt.Dimension(width + comboBoxHeight, comboBoxHeight));
					}
				}
			} catch (TraceViewException e) {
				logger.error("Can't load response from file: " + e);
			}
			ComboBoxModel<Object> convolveCBModel = new DefaultComboBoxModel<Object>(options.toArray());
			convolveCB.setModel(convolveCBModel);
			if (respFile != null) {
				convolveCB.setSelectedItem(respFile);
			}
		}
		return convolveCB;
	}

	private JPanel createDesignPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		panel.setLayout(new GridBagLayout());

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.EAST;
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new Insets(2, 3, 2, 3);
		panel.add(getConvolveL(), gridBagConstraints);

		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.gridy = 1;
		gridBagConstraints1.gridx = 2;
		panel.add(getConvolveCB(), gridBagConstraints1);
		return panel;
	}

	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if (isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
			Object value = optionPane.getValue();
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			// If you were going to check something
			// before closing the window, you'd do
			// it here.
			if (value.equals("Close")) {
				setVisible(false);
				needProcessing = false;
			} else if (value.equals("OK")) {
				respFile = (String) convolveCB.getSelectedItem();
				setVisible(false);
				needProcessing = true;
			}
		}
	}
	
	public static Cmplx[] normData(Cmplx[] data) {
		Cmplx[] ret = new Cmplx[data.length];
		Cmplx sum = new Cmplx(0,0);
		for (int i = 0; i < data.length; i++)
			sum = Cmplx.add(sum, data[i]);
		final Cmplx mean = Cmplx.div(sum, data.length);
		double maxAmp = 0;
		for (int i = 0; i < data.length; i++){
			ret[i] = Cmplx.sub(data[i], mean);
			if(ret[i].mag()>maxAmp){
				maxAmp = ret[i].mag(); 
			}
		}
		for (int i = 0; i < data.length; i++){
			ret[i] = Cmplx.div(ret[i], maxAmp);
		}
		return ret;
	}

/*
	public static Cmplx[] normData(Cmplx[] data) {
		Cmplx[] ret = new Cmplx[data.length];
		double maxAmp = 0;
		for (int i = 0; i < data.length; i++){
			if(data[i].mag()>maxAmp){
				maxAmp = data[i].mag(); 
			}
		}
		for (int i = 0; i < data.length; i++){
			ret[i] = Cmplx.div(data[i], maxAmp);
		}
		return ret;
	}
*/

	public static Cmplx[] removeExcessFrequencies(Cmplx[] spectra, Cmplx[] resp)
	throws ReconvolutionException
	{
		double cutOffRatio = 100.0;
		if(spectra.length!=resp.length){
			throw new ReconvolutionException("Arrays length should be equal");
		}
		double maxAmp = 0;
		for (int i = 0; i < resp.length; i++){
			if(resp[i].mag()>maxAmp){
				maxAmp = resp[i].mag(); 
			}
		}
		Cmplx[] ret = new Cmplx[spectra.length];
		for (int i = 0; i < spectra.length; i++){
			if(maxAmp/resp[i].mag() > cutOffRatio){
				ret[i]=new Cmplx(0,0);
			} else {
				ret[i]=spectra[i];
			}
		}
		return ret;
	}
}
