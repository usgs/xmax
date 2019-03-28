package com.isti.traceview.data.ims;

import gov.usgs.anss.cd11.CanadaException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public abstract class Block {
	private static SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
	protected String header = null;
	protected long startOffset;
	
	public static boolean isBlockEnded(String line){
		return (line==null || line.length()==0 || line.startsWith("CHK2 ") || line.startsWith("WID2 ") || line.startsWith("STA2 ") || line.startsWith("DAT2 ")  || line.startsWith("TIME_STAMP") || line.equals("STOP"));
	}
	
	public Block(long startOffset){
		this.startOffset = startOffset;
	}
	
	public String getHeader(){
		return header;
	}
	
	public long getStartOffset(){
		return startOffset;
	}
	
	public String getString(int begin, int end){
		return header.substring(begin, end);
	}
	
	public Date getDate(int begin, int end) throws ParseException {
		Date ret = null;
		ret = df.parse(getString(begin, end));
		return ret;
	}
	
	public int getInt(int begin, int end) throws ParseException {
		int ret;
		ret = Integer.parseInt(getString(begin, end).trim());
		return ret;
	}
	
	public double getDouble(int begin, int end) throws ParseException {
		double ret;
		ret = Double.parseDouble(getString(begin, end).trim());
		return ret;
	}
	
	public abstract void read(RandomAccessFile input) throws IMSFormatException, IOException, ParseException, CanadaException;

}
