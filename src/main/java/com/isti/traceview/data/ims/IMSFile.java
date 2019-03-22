package com.isti.traceview.data.ims;

import gov.usgs.anss.cd11.CanadaException;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class IMSFile {

	public enum MessageType {
		DATA, REQUEST, SUBSCRIPTION
	};

	private static final Logger logger = Logger.getLogger(IMSFile.class);
	//private static Pattern wid2Pattern = Pattern.compile("(\\w.)\\s.(\\S.)\\s.(\\S.)\\s.");
	private static Pattern msgTypePattern = Pattern.compile("^MSG_TYPE\\s+(\\S+)$");
	private static Pattern msgIdPattern = Pattern.compile("^MSG_ID\\s+(\\S+\\.*)$");
	private static Pattern refIdPattern = Pattern.compile("^REF_ID\\s+(\\S+\\.*)$");
	private static Pattern prodIdPattern = Pattern.compile("^PROD_ID\\s+(\\S+\\.*)$");
	@SuppressWarnings("unused")
	private String format_version = null;
	private MessageType msg_type = null;
	@SuppressWarnings("unused")	
	private String msg_id = null;
	@SuppressWarnings("unused")	
	private String ref_id = null; // msg_id of referenced message
	@SuppressWarnings("unused")	
	private String prod_id = null;
	private List<DataType> dataTypes = new ArrayList<>();

	private IMSFile() {

	}

	public List<DataType> getDataTypes() {
		return dataTypes;
	}

	public static IMSFile read(DataInput inStream, boolean parseOnly) throws IOException, IMSFormatException, ParseException, CanadaException {
		IMSFile imsFile = new IMSFile();
		RandomAccessFile input = (RandomAccessFile) inStream;
		try {
			while (true) {
				long filePointer = input.getFilePointer();
				String line = input.readLine();
				if((line == null) || (line.startsWith("STOP"))){
					break;
				}
				String[] firstLineParts = line.split("\\s.");
				String tag = firstLineParts[0].toUpperCase();
				if (tag.equals("BEGIN")) {
					imsFile.format_version = firstLineParts[1].trim();
					imsFile.readMessageHeader(input, filePointer);
					if (imsFile.msg_type == MessageType.DATA) {
						imsFile.readDataTypes(parseOnly, input, filePointer);
					} else if (imsFile.msg_type == MessageType.REQUEST) {
						;
					} else if (imsFile.msg_type == MessageType.SUBSCRIPTION) {
						;
					}
				} else if (tag.equals("DATA_TYPE")) {
					input.seek(filePointer);
					imsFile.readDataTypes(parseOnly, input, filePointer);
				} else if (tag.equals("WID2")) {
					input.seek(filePointer);
					DataType dt = new DataTypeWaveform(filePointer);
					dt.read(input, parseOnly);
					imsFile.dataTypes.add(dt);
				}
			}
		} catch (EOFException e) {
			// Do nothing
			logger.error("EOFException:", e);	
		}
		if(imsFile.dataTypes.size()==0){
			throw new IMSFormatException("Data not found");
		}
		return imsFile;
	}

	private void readMessageHeader(RandomAccessFile input, long startPointer) throws IOException, IMSFormatException, ParseException {
		long filePointer = 0;
		while (true) {
			filePointer = input.getFilePointer();
			String line = input.readLine();
			Matcher m = msgTypePattern.matcher(line);
			if (m.matches()) {
				String msg_type_str = m.group(1);
				if(msg_type_str.equals("DATA")){
					msg_type = MessageType.DATA;
				} else if(msg_type_str.equals("REQUEST")){
					msg_type = MessageType.REQUEST;
				} else if(msg_type_str.equals("SUBSCRIPTION")){
					msg_type = MessageType.SUBSCRIPTION;
				}
				continue;
			}
			m = msgIdPattern.matcher(line);
			if (m.matches()) {
				msg_id = m.group(1);
				continue;
			}
			m = refIdPattern.matcher(line);
			if (m.matches()) {
				ref_id = m.group(1);
				continue;
			}
			m = prodIdPattern.matcher(line);
			if (m.matches()) {
				prod_id = m.group(1);
				continue;
			}
			if (line.toUpperCase().startsWith("DATA_TYPE") || line.toUpperCase().startsWith("STOP")) {
				input.seek(filePointer);
				break;
			}
		}
	}

	private void readDataTypes(boolean parseOnly, RandomAccessFile input, long startPointer) throws IOException, IMSFormatException,
			ParseException, CanadaException {
		long filePointer = 0;
		try {
			while (true) {
				filePointer = input.getFilePointer();
				String line = input.readLine();
				if ((line == null) || line.toUpperCase().startsWith("STOP")) {
					break;
				}
				if (line.toUpperCase().startsWith("DATA_TYPE")) {
					input.seek(filePointer);
					DataType dt = DataType.readHeader(input);
					dt.read(input, parseOnly);
					dataTypes.add(dt);
				}
			}
		} catch (EOFException e) {
			// Do nothing
			logger.error("EOFException:", e);	
		}
	}
}
