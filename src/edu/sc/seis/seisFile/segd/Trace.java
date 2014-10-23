package edu.sc.seis.seisFile.segd;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.BufferedRandomAccessFile;

public class Trace {
	
	public enum TraceEdit {
		NO_EDIT, 
		DEAD_CHANNEL, 
		INTENTIONALLY_ZEROED, 
		EDITED
	};
	
	public enum SensorType {
		NOT_DEFINED, 
		PRESSURE, 
		VELOCITY_VERTICAL, 
		VELOCITY_HORIZONTAL_INLINE, 
		VELOCITY_HORIZONTAL_CROSSLINE, 
		VELOCITY_HORIZONTAL_OTHER, 
		ACCELERATION_VERTICAL, 
		ACCELERATION_HORIZONTAL_INLINE, 
		ACCELERATION_HORIZONTAL_CROSSLINE, 
		ACCELERATION_HORIZONTAL_OTHER
	};
	
	private SegdRecord record = null;
	private int fileNumber = -1;
	private int scanTypeNumber = -1;
	private int channelSetNumber = -1;
	private int traceNumber = -1;
	private double timingWord = 0.0;
	private short traceHeaderExtensions_number = -1;
	private double sampleSkew_fraction = 0.0;
	private TraceEdit traceEdit = null;
	private double timeBreakWindow = 0.0;
	//-------1st ext header data----------
	private double receiverLineNumber = 0.0;
	private double receiverPointNumber = 0.0;
	private byte receiverPointIndex = -1;
	private int samplesNumber = -1;
	private SensorType sensorType = null;
	private long dataOffset = -1;
	//------------------------------------
	private float[] data = null;
	private float minDataValue;
	private float maxDataValue;
	
	public Trace(SegdRecord parent){
		this.record = parent;
		minDataValue = Float.MAX_VALUE;
		maxDataValue = Float.MIN_VALUE;
	}
	
	
	public int getFileNumber() {
		return fileNumber;
	}

	public int getScanTypeNumber() {
		return scanTypeNumber;
	}

	public int getChannelSetNumber() {
		return channelSetNumber;
	}

	public int getTraceNumber() {
		return traceNumber;
	}

	public double getTimingWord() {
		return timingWord;
	}

	public short getTraceHeaderExtensions_number() {
		return traceHeaderExtensions_number;
	}

	public double getSampleSkew_fraction() {
		return sampleSkew_fraction;
	}

	public TraceEdit getTraceEdit() {
		return traceEdit;
	}

	public double getTimeBreakWindow() {
		return timeBreakWindow;
	}

	public double getReceiverLineNumber() {
		return receiverLineNumber;
	}

	public double getReceiverPointNumber() {
		return receiverPointNumber;
	}

	public byte getReceiverPointIndex() {
		return receiverPointIndex;
	}

	public int getSamplesNumber() {
		return samplesNumber;
	}

	public SensorType getSensorType() {
		return sensorType;
	}

	public float[] getData() {
		return data;
	}
	public float getMinDataValue() {
		return minDataValue;
	}
	public float getMaxDataValue() {
		return maxDataValue;
	}
	
	public long getDataOffset(){
		return dataOffset;
	}
	
	public void setDataOffset(long offset){
		this.dataOffset = offset;
	}
	
	public TimeInterval getTimeRange(){
		long startTime = record.getDate().getTime() + new Double(timingWord).longValue();
		return new TimeInterval(startTime, startTime + new Double(samplesNumber*record.getBaseScanInterval()).longValue());
	}
	
	public void readHeader(DataInput inStream) throws IOException{
		try {
			short[] check = { 0xFF, 0xFF };
			fileNumber = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, check), 4), 10);
		} catch (CheckFailedException e) {
			// Does nothing
		}
		try {
			short[] check = { 0xFF };
			scanTypeNumber = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 1, check), 4), 10);
		} catch (CheckFailedException e) {
			// Does nothing
		}
		try {
			short[] check = { 0xFF };
			channelSetNumber = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 1, check), 4), 10);
		} catch (CheckFailedException e) {
			// Does nothing
		}
		try {
			traceNumber = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10);
		} catch (CheckFailedException e) {
			// Does nothing
		}
		try {
			int timeWordInteger = SegdRecord.readShorts(inStream, 1)[0];
			short timeWordFraction = SegdRecord.readBytes(inStream, 1, null)[0];
			timingWord = timeWordInteger + timeWordFraction/256.0;
		} catch (CheckFailedException e) {
		}
		try {
			traceHeaderExtensions_number = SegdRecord.readBytes(inStream, 1, null)[0];
		} catch (CheckFailedException e) {
		}
		try {
			sampleSkew_fraction = SegdRecord.readBytes(inStream, 1, null)[0]/256;
			//?????????????????????????????????????????????????????????????
		} catch (CheckFailedException e) {
		}
		try {
			short byte12 = SegdRecord.readBytes(inStream, 1, null)[0];
			switch (byte12) {
			case 0:
				traceEdit = TraceEdit.NO_EDIT;
				break;
			case 1:
				traceEdit = TraceEdit.DEAD_CHANNEL;
				break;
			case 2:
				traceEdit = TraceEdit.INTENTIONALLY_ZEROED;
				break;
			case 3:
				traceEdit = TraceEdit.EDITED;
				break;
			default:
				traceEdit = null;
			}
		} catch (CheckFailedException e) {
		}
		try {
			int timeBreakInteger = SegdRecord.readShorts(inStream, 1)[0];
			short timeBreakFraction = SegdRecord.readBytes(inStream, 1, null)[0];
			timeBreakWindow = timeBreakInteger + timeBreakFraction/256.0;
		} catch (CheckFailedException e) {
		}
		//if(channelSetNumber == -1){
			channelSetNumber = SegdRecord.readShorts(inStream, 1)[0];
		//} else {
		//	inStream.skipBytes(2);
		//}
		if(fileNumber == -1){
			fileNumber = SegdRecord.readUnsignedTriple(inStream);
		} else {
			inStream.skipBytes(3);
		}
		readExtHeaders(inStream);
	}
	
	private void readExtHeaders(DataInput inStream) throws IOException {
		if(traceHeaderExtensions_number>0){
			int receiverLineNumberInt = SegdRecord.readSignedTriple(inStream);
			int receiverPointNumberInt = SegdRecord.readSignedTriple(inStream);
			receiverPointIndex = inStream.readByte();
			samplesNumber = SegdRecord.readUnsignedTriple(inStream);
			if (receiverLineNumberInt == 0xFFFFFF) {
				receiverLineNumber = readSignedFractionedFive(inStream);
			} else {
				receiverLineNumber = receiverLineNumberInt;
				inStream.skipBytes(5);
			}
			if (receiverPointNumberInt == 0xFFFFFF) {
				receiverPointNumber = readSignedFractionedFive(inStream);
			} else {
				receiverPointNumber = receiverPointNumberInt;
				inStream.skipBytes(5);
			}
			try {
				short byte21 = SegdRecord.readBytes(inStream, 1, null)[0];
				switch (byte21) {
				case 0:
					sensorType = SensorType.NOT_DEFINED;
					break;
				case 1:
					sensorType = SensorType.PRESSURE;
					break;
				case 2:
					sensorType = SensorType.VELOCITY_VERTICAL;
					break;
				case 3:
					sensorType = SensorType.VELOCITY_HORIZONTAL_INLINE;
					break;
				case 4:
					sensorType = SensorType.VELOCITY_HORIZONTAL_CROSSLINE;
					break;
				case 5:
					sensorType = SensorType.VELOCITY_HORIZONTAL_OTHER;
					break;
				case 6:
					sensorType = SensorType.ACCELERATION_VERTICAL;
					break;
				case 7:
					sensorType = SensorType.ACCELERATION_HORIZONTAL_INLINE;
					break;
				case 8:
					sensorType = SensorType.ACCELERATION_HORIZONTAL_CROSSLINE;
					break;
				case 9:
					sensorType = SensorType.ACCELERATION_HORIZONTAL_OTHER;
					break;
				default:
					sensorType= null;
				}
			} catch (CheckFailedException e) {
			}
			inStream.skipBytes(11);//not used
		}
		//Skip all subsequent ext headers
		inStream.skipBytes(32*(traceHeaderExtensions_number-1));
	}
	
	public void readData(DataInput inStream)throws IOException{
		if(data == null) {
			data = new float[samplesNumber];
		}
		for(int i = 0; i<samplesNumber; i++){
			float val = inStream.readFloat();
			data[i] = val;
			if(val>maxDataValue){
				maxDataValue = val;
			}
			if(val<minDataValue){
				minDataValue = val;
			}
		}
	}
	
	public static float[] getData(File file, long offset, int samplesCount) throws IOException{
		float[] ret = new float[samplesCount];
		DataInputStream strm = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		strm.skipBytes((int)offset);
		//BufferedRandomAccessFile strm = new BufferedRandomAccessFile(file.getCanonicalPath(), "r");
		//strm.seek(offset);
		for(int i = 0; i<samplesCount; i++){
			ret[i] = strm.readFloat();
		}
		strm.close();
		return ret;
	}
	
	static double readSignedFractionedFive(DataInput inStream)	throws IOException {
		int intPart = SegdRecord.readSignedTriple(inStream);
		double fractionPart = SegdRecord.readShorts(inStream, 1)[0] / 65536.0;
		return intPart + fractionPart;
	}
	
	public String toString(){
		return "\n\t\tTrace # " + traceNumber
		+"\n\t\t\tFile number: " + fileNumber
		+"\n\t\t\tScan Type number: " + scanTypeNumber
		+"\n\t\t\tChannel Set number: " + channelSetNumber
		+"\n\t\t\tTiming word, ms: " + timingWord
		+"\n\t\t\tTrace Header Extensions number: " + traceHeaderExtensions_number
		+"\n\t\t\tSample Skew (fraction part): " + sampleSkew_fraction
		+"\n\t\t\tTrace edit status: " + traceEdit
		+"\n\t\t\tTime break window, ms: " + timeBreakWindow
		+ "\n\t\t\tReceiver Line number: " + receiverLineNumber
		+ "\n\t\t\tReceiver Point number: " + receiverPointNumber
		+ "\n\t\t\tReceiver Point index: " + receiverPointIndex
		+ "\n\t\t\tSamples number: " + samplesNumber
		+ "\n\t\t\tSensor type: " + sensorType
		+ "\n\t\t\tData offset: " + dataOffset;
	}
}
