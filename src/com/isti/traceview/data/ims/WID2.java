package com.isti.traceview.data.ims;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.isti.traceview.data.BufferedRandomAccessFile;

public class WID2 extends Block {
	private static final Logger logger = Logger.getLogger(WID2.class);
	public enum Compression {
		INT, CM6, CM8, CSF
	}
	
	
	//private static Pattern headerPattern = Pattern.compile("(\\w.)\\s.(\\S.)\\s.(\\S.)\\s.");
	
	private Date start;			//6-15 i4,a1,i2,a1,i2 date of the first sample (yyyy/mm/dd)
								//17-28 i2,a1,i2,a1,f6.3 time of the first sample (hh:mm:ss.sss)
	private String station;		//30-34 a5 station code
	private String channel;		//36-38 a3 FDSN channel code
	private String aux_id;		//40-43 a4 auxiliary identification code
	private Compression csf;	//45-47 a3 INT, CMn, or CSF. 
								//INT is free-format integers as ASCII characters. 
								//CM denotes compressed data, and n is either 6 (6-bit compression), or 8 (8-bit binary compression)
								//CSF is a signed format
	private int numSamples;		//49-56 i8 number of samples
	private double sampleRate;	//58-68 f11.6 data sampling rate (Hz)
	private double sensitivity;	//70-79 e10.2 system sensitivity (nm/count) at the calibration reference period, the ground motion in nanometers per digital count at calibration period (calper)
	private double period;		//81-87 f7.3 calibration reference period; the period in seconds 
								//at which the system sensitivity is valid; calper should be near 
								//the flat part of the response curve (in most cases, 1 second)
	private String instType;	//89-94 a6 instrument type (from Table A-5 on page A17)
	private double orientHor;	//96-100 f5.1 horizontal orientation of sensor, measured in positive degrees clockwise from North (�1.0 if vertical)
	private double orientVer;	//102-105 f4.1 vertical orientation of sensor, measured in degrees from vertical (90.0 if horizontal)
	
								
	public WID2(long startOffset){
		super(startOffset);

	}

	public Date getStart() {
		return start;
	}

	public String getStation() {
		return station;
	}

	public String getChannel() {
		return channel;
	}

	public String getAux_id() {
		return aux_id;
	}

	public Compression getCsf() {
		return csf;
	}

	public int getNumSamples() {
		return numSamples;
	}

	/**
	 * Sample rate in Hz
	 * @return
	 */
	public double getSampleRate() {
		return sampleRate;
	}

	public double getSensitivity() {
		return sensitivity;
	}

	public double getPeriod() {
		return period;
	}

	public String getInstType() {
		return instType;
	}

	public double getOrientHor() {
		return orientHor;
	}

	public double getOrientVer() {
		return orientVer;
	}

	public void read(BufferedRandomAccessFile input) throws IMSFormatException, IOException, ParseException{
		logger.debug("== START WID2 Read");
		header = input.readLine();
		if(!header.startsWith("WID2")){
			throw new IMSFormatException("Wrong waveform block header: " + header);
		}
		start = getDate(5,28);
		station = getString(29,34);
		channel = getString(35,38);
		aux_id = getString(39,43);
		String csft = getString(44,47);
		if(csft.equals("INT")){
			csf = Compression.INT;
		} else if(csft.equals("CM6")){
			csf = Compression.CM6;			
		} else if(csft.equals("CM8")){
			csf = Compression.CM8;			
		} else if(csft.equals("CSF")){
			csf = Compression.CSF;			
		}
		numSamples = getInt(48,56);
		sampleRate = getDouble(57,68);
		sensitivity = getDouble(69,79);
		period = getDouble(80,87);
		instType = getString(88,94);
		orientHor = getDouble(95,100);
		orientVer = getDouble(101,105);
	}
}
