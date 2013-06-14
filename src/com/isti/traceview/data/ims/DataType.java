package com.isti.traceview.data.ims;

import gov.usgs.anss.cd11.CanadaException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.isti.traceview.data.BufferedRandomAccessFile;

/**
 * 	data_type type of data that follows; typical examples  
	are WAVEFORM, BULLETIN, and RESPONSE
 * @author Max Kokoulin
 *
 */
public abstract class DataType {

	private static Pattern dataTypePattern = Pattern.compile("^DATA_TYPE\\s+(\\S+)\\s+(\\S+)\\s*$");
	private String subtype;		//subtype to use with this data type. subtype is used  primarily for ARRIVAL data types.
	private String format;		//general format of the data (IMS1.0)
	private String subformat;	//internal format to use with this data type. sub_format is used primarily for BULLETIN and WAVEFORM data types.
	protected Date beginTimeStamp = null;
	protected Date endTimeStamp = null;
	protected long startOffset;
	
	
	public String getSubtype() {
		return subtype;
	}

	public String getFormat() {
		return format;
	}

	public String getSubformat() {
		return subformat;
	}

	public long getStartOffset() {
		return startOffset;
	}
	
	public Date getBeginTimeStamp() {
		return beginTimeStamp;
	}

	public Date getEndTimeStamp() {
		return endTimeStamp;
	}
	
	public DataType(long startOffset){
		this.startOffset = startOffset;
	}
	
	public static DataType readHeader(BufferedRandomAccessFile input) throws IMSFormatException, IOException {
		DataType ret = null;
		long filePointer = input.getFilePointer();
		String line = input.readLine();
		Matcher m = dataTypePattern.matcher(line);
		if(m.matches()){
			String typeString = m.group(1);
			String[] types = typeString.split(":");
			if(types[0].equals("WAVEFORM")){
				ret = new DataTypeWaveform(filePointer);
				if(types.length>1){
					ret.subtype = types[1];
				}
//			} else if(types[0].equals("BULLETIN")){
//				;
//			} else if(types[0].equals("RESPONSE")){
//				;
			} else {
				throw new IMSFormatException("Unsupported DATA_TYPE: " + types[0]);
			}
			String formatString = m.group(2);
			String[] formats = formatString.split(":");
			ret.format = formats[0];
			if(formats.length>1){
				ret.subformat = formats[1];
			}
		} else {
			throw new IMSFormatException("Wrong DATA_TYPE header: " + line);
		}
		return ret;
	}
	
	public abstract void read(BufferedRandomAccessFile input, boolean parseOnly) throws IOException, IMSFormatException, ParseException, CanadaException;
	public abstract void check() throws IMSFormatException;

}
