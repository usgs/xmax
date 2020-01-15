package com.isti.traceview.source;

import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import edu.sc.seis.seisFile.segd.ChannelSet;
import edu.sc.seis.seisFile.segd.ScanType;
import edu.sc.seis.seisFile.segd.SegdRecord;
import edu.sc.seis.seisFile.segd.Trace;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;


public class SourceFileSEGD extends SourceFile implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SourceFileSEGD.class);
	private final int NORM_AMPLITUDE =100000;
	
	public SourceFileSEGD(File file) {
		super(file);
		logger.info("Created: " + this);
	}

	@Override
	public FormatType getFormatType() {
		return FormatType.SEGD;
	}
	
	@Override
	public Set<PlotDataProvider> parse() {
		Set<PlotDataProvider> ret = new HashSet<>();
		try {
			SegdRecord segd = new SegdRecord(getFile());
			segd.readHeaders();
			for(ScanType st: segd.getScanTypes()){
				for(ChannelSet cs:st.getChannelSets()){
					for(Trace trace: cs.getTraces()){
						PlotDataProvider channel = new PlotDataProvider("Z",									//Channel
								DataModule.getOrAddStation(Double.toString(trace.getReceiverLineNumber())),	//Station ID
								Integer.toString(segd.getManufacturerCode()),									//Network ID
								Double.toString(trace.getReceiverPointNumber()));								//Location
						ret.add(channel);
						Segment segment = new Segment(this, trace.getDataOffset(), trace.getTimeRange().getStartTime(), segd.getBaseScanInterval(), trace.getSamplesNumber(), 0);
						channel.addSegment(segment);						
					}
				}
			}
		} catch (IOException e) {
			logger.error("IO error: ", e);
		}
		return ret;
	}

	@Override
	public void load(Segment segment) {
		logger.info("Loading: " + this);
		int[] data = null;
		try {
			float[] traceData = Trace.getData(getFile(), segment.getStartOffset(), segment.getSampleCount());
			float maxDataValue = Float.MIN_VALUE;
			float minDataValue = Float.MAX_VALUE;
			for(float val:traceData){
				if(val>maxDataValue){
					maxDataValue = val;
				}
				if(val<minDataValue){
					minDataValue = val;
				}
			}
			double normCoeff = NORM_AMPLITUDE/(maxDataValue - minDataValue);
			data = new int[traceData.length];
			int i = 0;
			for (float val: traceData) {
				data[i++]=new Double(normCoeff*val).intValue();
			}
		} catch (IOException e) {
			logger.error("IOException:", e);	
			//throw new RuntimeException(e);
			System.exit(0);	
		}
		for (int value: data) {
			segment.addDataPoint(value);
		}

	}


	
	public String toString() {
		return "SourceFileSEGD: file " + (getFile() == null ? "absent" : getFile().getName()) + ";";
	}
}
