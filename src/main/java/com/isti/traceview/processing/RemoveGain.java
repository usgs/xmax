package com.isti.traceview.processing;

import java.io.StringReader;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotData;
import com.isti.traceview.data.PlotDataPoint;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.gui.IColorModeState;

/**
 * This class holds all information to remove the instrument gain from a trace
 * 
 * @author Nick Falco
 */
public class RemoveGain {
	public Boolean removestate; 
	
	public RemoveGain(Boolean removestate){
		this.removestate = removestate;
	}
	
	/**
	 * Removes the gain from pixelized data If we have overlap on the trace we take only first
	 * segment.
	 * 
	 * @param channel
	 *            plot data provider to rotate
	 * @param ti
	 *            processed time range
	 * @param pointCount
	 *            requested point count in the resulting plotdata
	 * @param filter
	 *            filter to apply before removing the gain
	 * @return pixelized rotated data
	 * @throws TraceViewException
	 *             if a variety of issue occurs in called methods, this method
	 *             throws if the channel type can not be determined.
	 */
	public PlotData removegain(PlotDataProvider channel, TimeInterval ti,
			int pointCount, IFilter filter, IColorModeState colorMode)
			throws TraceViewException, RemoveGainException{
		double minFreqValue = 0.0001;
		double maxFreqValue = 500.0 / channel.getRawData().get(0).getSampleRate();
		int numberFreqs = 500;
		double sensitivity = 1;
		if(removestate){
			try{
				RunEvalResp evalResp = new RunEvalResp(false, false);
				evalResp.generateResponse(minFreqValue, maxFreqValue, numberFreqs, ti.getStartTime(), new StringReader(channel.getResponse().getContent()));
				sensitivity = evalResp.sensitivity; 
			}
			catch(NullPointerException e){
				throw new RemoveGainException("Unable to remove gain. No response found.");
			}
		}
		PlotData toProcess = channel.getPlotData(ti, pointCount, null, filter,  null, colorMode);
		PlotData ret = new PlotData(channel.getName(), channel.getColor());
		
		PlotDataPoint pdp = null;
		for(int r = 0; r < channel.getSegmentCount(); r++){
			for (int i = 0; i < pointCount; i++) {

				int value = channel.getRawData(ti).get(r).getData().data[i];
				pdp = toProcess.getPixels().get(i)[0];
				
				pdp = new PlotDataPoint(removestate ? pdp.getTop()/sensitivity : pdp.getTop(), 
										removestate ? pdp.getBottom()/sensitivity : pdp.getBottom(), 
										removestate ? value / sensitivity : value, 
							            toProcess.getPixels().get(i)[0].getSegmentNumber(), 
										toProcess.getPixels().get(i)[0].getRawDataProviderNumber(), 
										toProcess.getPixels().get(i)[0].getContinueAreaNumber(), 
										toProcess.getPixels().get(i)[0].getEvents());
				PlotDataPoint[] pdpArray = new PlotDataPoint[1];
				pdpArray[0] = pdp;
				ret.addPixel(pdpArray);
			}
		}
		
		return ret;
	}
}
