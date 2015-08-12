import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IFilter;
import com.isti.traceview.processing.ITransformation;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Spectra;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;

import edu.sc.seis.fissuresUtil.freq.Cmplx;
import java.io.*;
import com.isti.jevalresp.RespUtils;

import com.isti.traceview.data.Response;

/**
 * Power spectra density transformation. Prepares data for presentation in {@link ViewPSD}
 * 
 * @author Max Kokoulin
 */
public class TransPSD implements ITransformation {
	private static final Logger logger = Logger.getLogger(TransPSD.class);
	private static final boolean verboseDebug = false;
	public int maxDataLength = 262144;
	private int effectiveLength = 0;

	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration, JFrame parentFrame) {
		if (input.size() == 0) {
			JOptionPane.showMessageDialog(parentFrame, "Please select channels", "PSD computation warning", JOptionPane.WARNING_MESSAGE);
		} else {
			try {
				List<Spectra> spList = createData(input, filter, ti, parentFrame);
				TimeInterval effectiveInterval = new TimeInterval(ti.getStart(), ti.getStart() + new Double(input.get(0).getSampleRate()*effectiveLength).longValue());
				@SuppressWarnings("unused")	
				ViewPSD vp = new ViewPSD(parentFrame, spList, effectiveInterval, (Configuration)configuration, input);
			} catch (XMAXException e) {
				if (!e.getMessage().equals("Operation cancelled")) {
					JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
				}
			} catch (TraceViewException e) {
				JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}
		((XMAXframe)parentFrame).getGraphPanel().forceRepaint();
	}

	public void setMaxDataLength(int dataLength) {
		this.maxDataLength = dataLength;
	}

	/**
	 * @param input
	 *            List of traces to process
	 * @param filter
	 *            Filter applied to traces before correlation
	 * @param ti
	 *            Time interval to define processed range
	 * @param parentFrame
	 *            parent frame
	 * @return list of spectra for selected traces and time ranges
	 * @throws XMAXException if sample rates differ, gaps in the data, or no data for a channel
	 */

	private List<Spectra> createData(List<PlotDataProvider> input, IFilter filter, TimeInterval ti, JFrame parentFrame) throws TraceViewException,
			XMAXException {
		List<Spectra> dataset = new ArrayList<Spectra>();
		ListIterator<PlotDataProvider> li = input.listIterator();
		String respNotFound = "";
		//int userAnswer = -1;
		while (li.hasNext()) {
			PlotDataProvider channel = li.next();
			List<Segment> segments = channel.getRawData(ti);
			double samplerate;
			long segment_end_time = 0;
			int[] intData = new int[0];
			if (segments.size() > 0) {
				samplerate = segments.get(0).getSampleRate();
				for (Segment segment: segments) {
					if (segment.getSampleRate() != samplerate) {
						throw new XMAXException("You have data with different sample rate for channel " + channel.getName());
					}
					if (segment_end_time != 0 && Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), samplerate)) {
						throw new XMAXException("You have gap in the data for channel " + channel.getName());
					}
					segment_end_time = segment.getEndTime().getTime();
					intData = IstiUtilsMath.padArray(intData, segment.getData(ti).data);
				}

			} else {
				throw new XMAXException("You have no data for channel " + channel.getName());
			}
			int ds;
			if (intData.length > maxDataLength) {
				ds = getPower2Length(maxDataLength);		
				int[] tempIntData = new int[ds];
				for(int i = 0; i < maxDataLength; i++)
					tempIntData[i] = intData[i];
				intData = tempIntData; 
				((XMAXframe) parentFrame).getStatusBar().setMessage(
						"Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
			} else {
				ds = getPower2Length(intData.length);
				System.out.println("ds = " + ds); 
			}
			if(ds>effectiveLength){
				effectiveLength = ds;
			}
			/*
			 * // this code shows pop-up if point count is exceeded if (ds > maxDataLength &&
			 * userAnswer == -1) { Object[] options = { "Proceed with ALL points", "Proceed with
			 * first " + maxDataLength + " points", "Cancel" }; userAnswer =
			 * JOptionPane.showOptionDialog(parentFrame, "Too many points. Computation could be
			 * slow.", "Too many points", JOptionPane.YES_NO_CANCEL_OPTION,
			 * JOptionPane.QUESTION_MESSAGE, null, options, options[1]); } if (userAnswer != -1) {
			 * if (userAnswer == JOptionPane.NO_OPTION) { if (ds > maxDataLength) { ds = new
			 * Double(Math.pow(2, new
			 * Double(IstiUtilsMath.log2(maxDataLength)).intValue())).intValue(); } } else if
			 * (userAnswer == JOptionPane.CANCEL_OPTION) { throw new XMAXException("Operation
			 * cancelled"); } }
			 */
			logger.debug("data size = " + ds);
			int dsDataSegment = new Double(Math.round(intData.length/4.0)).intValue(); // length of each segment for 13 segments 75% overlap
			int smallDataSegmentLimit = new Double(Math.ceil(Math.pow(2, (new Double(Math.ceil(IstiUtilsMath.log2(dsDataSegment)) - 1))))).intValue(); 
			
			int[] data = new int[smallDataSegmentLimit]; //array containing data values in the time domain
			//find power of 2 greater than data segment
			int power2value = new Double(Math.ceil(Math.pow(2, (new Double(Math.ceil(IstiUtilsMath.log2(dsDataSegment))))))).intValue(); 
			
			Cmplx[] noise_spectra = new Cmplx[smallDataSegmentLimit]; //array containing a running total of the ffts of each segment
			Cmplx[] finalNoiseSpectraData = new Cmplx[(smallDataSegmentLimit/2) + 1]; 
			
			for(int i = 0; i < finalNoiseSpectraData.length; i++)
			{
				finalNoiseSpectraData[i] = new Cmplx(0,0);
			}
			
			//loop indexes
			int dsDataSegmentLimit = dsDataSegment; // keeps track of where a segment ends in the data array
			
			System.out.println("dsDataSegmentLimit = " + dsDataSegmentLimit);
			System.out.println("smallDataSegmentLimit = " + smallDataSegmentLimit); 
			
			int cnt = 0; //keeps track where in the intData array the index is
			int segIndex = 0; //keeps track of where the index is within an individual segment
			/*
			while(cnt < intData.length) {
				
				if(cnt < dsDataSegmentLimit)
				{
					data[segIndex] = intData[cnt];
					cnt++;
				    segIndex++;	
				}
				else
				{

					//zero pad data to the nearest power of 2
					int[] zerosArray = new int[power2value - data.length];
					for(int i = 0; i < zerosArray.length; i++)
						zerosArray[i] = 0;
					data = IstiUtilsMath.padArray(data, zerosArray); 
					
					if (filter != null) 
					{
						data = new FilterFacade(filter, channel).filter(data);
					}	
					
					// Make a copy of data to make it an array of doubles
					double[] dataCopy = new double[data.length];
					for (int i = 0; i < data.length; i++)
						dataCopy[i] = data[i];
					
					// Norm the data: remove mean
					dataCopy = IstiUtilsMath.normData(dataCopy);

					// Apply Hanning window
					dataCopy = IstiUtilsMath.windowHanning(dataCopy);
					
					
					//Calculate fft of current segment 
					noise_spectra = IstiUtilsMath.processFft(dataCopy);
					
					for(int i = 0; i < noise_spectra.length; i++) 
					{
						finalNoiseSpectraData[i] = Cmplx.add(finalNoiseSpectraData[i], noise_spectra[i]); 
					}
					
					//move cursors
				    segIndex = 0; 
					if(cnt + dsDataSegment > intData.length) // correction for last segment
					{
						cnt = intData.length - dsDataSegment; 
						dsDataSegmentLimit = intData.length; 
					}
					else
					{
						cnt = cnt - ( (dsDataSegment * 3) / 4 ); // move window backwards 75%
						dsDataSegmentLimit = dsDataSegmentLimit + (dsDataSegment / 4); // increase new dsDataSegmentLimit by 25%
					}			
				}
				
			}*/
			
			while(cnt < intData.length) {
				
				if(cnt < dsDataSegmentLimit)
				{
					if(segIndex < smallDataSegmentLimit)
						data[segIndex] = intData[cnt];
					//else
					//	data[segIndex] = 0; 
					cnt++;
				    segIndex++;	
				}
				else
				{

					//zero pad data to the nearest power of 2
					//int[] zerosArray = new int[power2value - data.length];
					//for(int i = 0; i < zerosArray.length; i++)
					//	zerosArray[i] = 0;
					//data = IstiUtilsMath.padArray(data, zerosArray); 
					
					if (filter != null) 
					{
						data = new FilterFacade(filter, channel).filter(data);
					}	
					
					// Make a copy of data to make it an array of doubles
					double[] dataCopy = new double[data.length];
					for (int i = 0; i < data.length; i++)
						dataCopy[i] = data[i];
					
					// Norm the data: remove mean
					dataCopy = IstiUtilsMath.normData(dataCopy);

					// Apply Hanning window
					dataCopy = IstiUtilsMath.windowHanning(dataCopy);
					
					
					//Calculate fft of current segment 
					noise_spectra = IstiUtilsMath.processFft(dataCopy);
					
					for(int i = 0; i < noise_spectra.length; i++) 
					{
						finalNoiseSpectraData[i] = Cmplx.add(finalNoiseSpectraData[i], noise_spectra[i]); 
					}
					
					//move cursors
				    segIndex = 0; 
					if(cnt + dsDataSegment > intData.length) // correction for last segment
					{
						cnt = intData.length - dsDataSegment; 
						dsDataSegmentLimit = intData.length; 
					}
					else
					{
						cnt = cnt - ( (dsDataSegment * 3) / 4 ); // move window backwards 75%
						dsDataSegmentLimit = dsDataSegmentLimit + (dsDataSegment / 4); // increase new dsDataSegmentLimit by 25%
					}			
				}
				
			}
			
			//average each bin
			for(int i = 0; i < finalNoiseSpectraData.length; i++)
			{
				finalNoiseSpectraData[i] = Cmplx.div(finalNoiseSpectraData[i], 13.0); 
			}
			
			
			final Response.FreqParameters fp = Response.getFreqParameters(smallDataSegmentLimit, 1000.0/channel.getSampleRate());
			final double[] frequenciesArray = RespUtils.generateFreqArray(fp.startFreq, fp.endFreq, fp.numFreq, false);
			
			
			System.out.println("sample rate =  " + channel.getSampleRate());
			System.out.println("1000/sample rate = " + 1000/channel.getSampleRate()); 
			System.out.println("fp.startFreq = " + fp.startFreq);
			System.out.println("fp.endFreq = " + fp.endFreq);
			System.out.println("fp.numFreq = " + fp.numFreq);
			
			Cmplx[] resp = null;
			try {
				resp = channel.getResponse().getResp(ti.getStartTime(), fp.startFreq, fp.endFreq, Math.max(finalNoiseSpectraData.length, fp.numFreq));
			
			} catch (Exception e) {
				
				//respNotFound = channel.getName(); 
				//errString = "Can't get response for channel " + channel.getName() + ": ";
				///logger.error(errString, e);	
			}

			Spectra spectra = new Spectra(ti.getStartTime(), finalNoiseSpectraData, frequenciesArray, resp, fp.sampFreq, channel, "");

			if (spectra.getResp() != null) 
			{
				dataset.add(spectra);
			} 
			else 
			{
				if (respNotFound.length() > 0) 
				{
					respNotFound = respNotFound + ", ";
				}
				respNotFound = respNotFound + channel.getName();
				li.remove();
			}
			
		}
		
		if (input.size() == 0) {
			throw new XMAXException("Can not find responses");
		} else {
			if (respNotFound.length() > 0) {
				JOptionPane.showMessageDialog(parentFrame, "Can not find responses for channels: " + respNotFound, "Warning",
						JOptionPane.WARNING_MESSAGE);
			}
		}
		
		return dataset;
	}
	
	protected static int getPower2Length(int length){
		return new Double(Math.pow(2, new Double(Math.ceil(IstiUtilsMath.log2(length))))).intValue();		
	}

}
