package com.isti.traceview.source;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.data.SegyTimeSeries;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * File SEGY data source
 * @author Max Kokoulin
 *
 */
public class SourceFileSEGY extends SourceFile implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SourceFileSEGY.class);

	public SourceFileSEGY(File file) {
		super(file);
		logger.info("Created: " + this);
	}
	
	public FormatType getFormatType(){
		return FormatType.SEGY;
	}

	public Set<PlotDataProvider> parse() {
		Set<PlotDataProvider> ret = new HashSet<>();
		try {
			SegyTimeSeries segy = new SegyTimeSeries();
			segy.readHeader(getFile().getCanonicalPath());
			PlotDataProvider channel = new PlotDataProvider(segy.getChannel(), DataModule.getOrAddStation(segy.getStation()), segy.getNetwork(), "");
			ret.add(channel);
			Segment segment = new Segment(this, 0, segy.getTimeRange().getStartTime(), segy.getRateMicroSampPerSec()/1000.0, segy.getNumSamples(), 0);
			channel.addSegment(segment);
		} catch (IOException | TraceViewException e) {
			logger.error("IO error: ", e);
		}
		return ret;
	}
	
	
	public void load(Segment segment){
		int[] data = null;
		try {
			SegyTimeSeries segy = new SegyTimeSeries();
			segy.read(getFile());
			data = new int[segment.getSampleCount()];
			int i = 0;
			for (float val: segy.y) {
				data[i++]=new Float(val).intValue();
			}
		} catch (IOException e) {
			logger.error("IOException:", e);
			//throw new RuntimeException(e);
			System.exit(0);	
		} catch (TraceViewException e) {
			logger.error("TraceViewException:", e);	
			//throw new RuntimeException(e);
			System.exit(0);	
		}
		for (int value: data) {
			segment.addDataPoint(value);
		}
	}
	
	public String toString() {
		return "SourceFileSEGY: file " + (getFile() == null ? "absent" : getFile().getName()) + ";";
	}
}
