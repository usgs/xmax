package com.isti.traceview.data;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceView;

import edu.iris.Fissures.Time;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import static edu.sc.seis.seisFile.sac.SacConstants.data_offset;

public class SourceFileSAC extends SourceFile implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SourceFileSAC.class);
	private static final SimpleDateFormat fissTime = new SimpleDateFormat("yyyyDDD'T'HH:mm:ss.SSS'Z'"); 
	static {
		fissTime.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	public SourceFileSAC(File file) {
		super(file);
		logger.debug("Created: " + this);
	}
	
	public FormatType getFormatType(){
		return FormatType.SAC;
	}

	public Set<RawDataProvider> parse(DataModule dataModule) {
		Set<RawDataProvider> ret = new HashSet<RawDataProvider>();
		try {
			SacTimeSeries sac = new SacTimeSeries();

			sac.read(getFile().getCanonicalPath());
			if (getFile().length() != sac.getHeader().getNpts() * 4 + data_offset) {
				throw new IOException(getFile().getName() + " does not appear to be a SAC file!");
			} 
			// sac.read(getFile());
			String loc="";
			if(sac.getHeader().getKinst().trim().equals("-12345")){
				loc = sac.getHeader().getKhole().trim();
			} else {
				loc = sac.getHeader().getKhole().trim();
			}
			RawDataProvider channel = dataModule.getOrAddChannel(sac.getHeader().getKcmpnm(), DataModule.getOrAddStation(sac.getHeader().getKstnm()), sac.getHeader().getKnetwk(), loc);
			ret.add(channel);
			Segment segment = new Segment(this, 0, new Date(getSACtime(sac)), sac.getHeader().getDelta() * 1000, sac.getHeader().getNpts(), 0);
			channel.addSegment(segment);
		} catch (IOException e) {
			logger.error("IO error: ", e);
		} 
		return ret;
	}
	
	public void load(Segment segment){
		int[] data = null;
		try {
			SacTimeSeries sac = new SacTimeSeries();
			sac.read(getFile());
			data = new int[segment.getSampleCount()];
			int i = 0;
			for (float val: sac.getY()) {
				data[i++]=new Float(val).intValue();
			} 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (int value: data) {
			segment.addDataPoint(value);
		}
	}
	
	public String toString() {
		return "SourceFileSAC: file " + (getFile() == null ? "absent" : getFile().getName()) + ";";
	}
	
	public static Time getFissuresTime(long time){
		return new Time(fissTime.format(new Date(time)), 0); 
	}

	private static long getSACtime(SacTimeSeries sac) {
		GregorianCalendar cal = new GregorianCalendar(TraceView.timeZone);
		cal.set(Calendar.YEAR, sac.getHeader().getNzyear());
		cal.set(Calendar.DAY_OF_YEAR, sac.getHeader().getNzjday());
		cal.set(Calendar.HOUR_OF_DAY, sac.getHeader().getNzhour());
		cal.set(Calendar.MINUTE, sac.getHeader().getNzmin());
		cal.set(Calendar.SECOND, sac.getHeader().getNzsec());
		cal.set(Calendar.MILLISECOND, sac.getHeader().getNzmsec());
		return cal.getTimeInMillis();
	}
}
