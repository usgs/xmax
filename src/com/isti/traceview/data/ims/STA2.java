package com.isti.traceview.data.ims;

import java.io.DataInput;
import java.io.IOException;
import java.text.ParseException;

import org.apache.log4j.Logger;

import com.isti.traceview.data.BufferedRandomAccessFile;

public class STA2 extends Block {
	private static Logger lg = Logger.getLogger(STA2.class);
	
	private String network;		//6–14 a9 network identifier
	private double latitude;	//16–24 f9.5 latitude (degrees, South is negative)
	private double longitude;	//26–35 f10.5 longitude (degrees, West is negative)
	private String coordType;	//37–48 a12 reference coordinate system	(for example, WGS-84)
	private double elevation;	//50–54 f5.3 elevation (km)
	private double emplacement;	//56–60 f5.3 emplacement depth (km)
	
	public STA2(long startOffset){
		super(startOffset);
	}
	
	public String getNetwork() {
		return network;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getCoordType() {
		return coordType;
	}

	public double getElevation() {
		return elevation;
	}

	public double getEmplacement() {
		return emplacement;
	}
	
	public void read(BufferedRandomAccessFile input) throws IMSFormatException, IOException, ParseException {
		lg.debug("STA2.read begin");
		header = input.readLine();
		if(!header.startsWith("STA2")){
			throw new IMSFormatException("Wrong station block header: " + header);
		}
		network = getString(5,14);
		latitude = getDouble(15,24);
		longitude = getDouble(25,35);
		coordType = getString(36,48);
		elevation = getDouble(49,54);
		emplacement = getDouble(55,60);
		lg.debug("STA2.read end");
	}
}
