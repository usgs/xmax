package edu.sc.seis.seisFile.segd;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import com.isti.traceview.common.TimeInterval;

public class SegdRecord {

	public enum Format {
		BINARY_MULTIPLEXED_20_BIT, // (not implemented)
		BINARY_DEMUX_20_BIT, // (not implemented)
		QUATERNARY_MULTIPLEXED_8_BIT, // (not implemented
		QUATERNARY_MULTIPLEXED_16_BIT, // (not implemented)
		HEXADECIMAL_MULTIPLEXED_8_BIT, // (not implemented)
		HEXADECIMAL_MULTIPLEXED_16_BIT, // (not implemented)
		HEXADECIMAL_MULTIPLEXED_32_BIT, // (not implemented)
		COMPLIMENT_2_INTEGER_MULTIPLEXED_24_BIT, // (not implemented)
		COMPLIMENT_2_INTEGER_MULTIPLEXED_32_BIT, // (not implemented)
		COMPLIMENT_2_INTEGER_DEMUX_24_BIT, // (not implemented)
		COMPLIMENT_2_INTEGER_DEMUX_32_BIT, // (not implemented)
		QUATERNARY_DEMUX_8_BIT, // (not implemented)
		QUATERNARY_DEMUX_16_BIT, // (not implemented)
		HEXADECIMAL_DEMUX_8_BIT, // (not implemented)
		HEXADECIMAL_DEMUX_16_BIT, // (not implemented)
		HEXADECIMAL_DEMUX_32_BIT, // (not implemented)
		IEEE_MULTIPLEXED_32_BIT, IEEE_DEMUX_32_BIT
	};

	public enum Polarity {
		UNTESTED, ZERO, DEGREES_45, DEGREES_90, DEGREES_135, DEGREES_180, DEGREES_225, DEGREES_270, DEGREES_315, UNASSIGNED
	};

	public enum RecordType {
		TEST_RECORD, PARALLEL_CHANNEL_TEST, DIRECT_CHANNEL_TEST, NORMAL_RECORD, OTHER
	};

	// -----------General header 1 data------------
	private int fileNumber = -1;
	private Format format;
	private short[] general_constants = new short[12];
	private short additionalGeneralHeaderBlocksNumber;
	private Date date = null;
	private int manufacturer_code = -1;
	private int manufacturer_serial_num = -1;
	private double base_scan_interval = -1.0;
	private Polarity polarity;
	private RecordType recordType;
	private double recordLength = -1.0;
	private int scanTypes_per_record = -1;
	private int channelSets_per_scanType = -1;
	private int skewBlocks_number = -1;
	private int extendedHeaderBlocks_number = -1;
	private int externalHeaderBlocks_number = -1;
	private int generalTrailer_blocks = -1;
	// --------------------------------------------------------
	// ------------General header 2 data--------------
	private String segd_revision = null;
	private int generalHeaderBlock_number = -1;
	private int sequence_number = -1;
	private File file = null;

	// --------------------------------------------------------
	// ------------General header 3 data--------------

	// ---------------------------------------------------------

	private AdditionalGeneralHeader[] additionalGeneralHeaders = null;
	private ScanType[] scanTypes = null;
	
	//Extended header data
	private short[][] extendedHeadersData = null;
	//External header data
	private short[][] externalHeadersData = null;
	
	private FileInputStreamPositioned inputStream= null;

	public SegdRecord(File file) throws FileNotFoundException, IOException {
		this.file = file;
	}

	public int getFileNumber(){
		return fileNumber;
	}
	public Format getFormat(){
		return format;
	}
	public short[] getGeneralConstants(){
		return general_constants;
	}
	public short getAdditionalGeneralHeaderBlocksNumber(){
		return additionalGeneralHeaderBlocksNumber;
	}
	public Date getDate(){
		return date;
	}
	public int getManufacturerCode(){
		return manufacturer_code;
	}
	public int getManufacturerSerialNnum(){
		return manufacturer_serial_num;
	}
	public double getBaseScanInterval(){
		return base_scan_interval;
	}
	public Polarity getPolatity(){
		return polarity;
	}
	public RecordType getRecordtype(){
		return recordType;
	}
	public double getRecordLength(){
		return recordLength;
	}
	public int getScanTypesPerRecord(){
		return scanTypes_per_record;
	}
	public int getChannelSetsPerScanType(){
		return channelSets_per_scanType;
	}
	public int getSkewBlocksNumber(){
		return skewBlocks_number;
	}
	public int getExtendedHeaderBlocksNumber(){
		return extendedHeaderBlocks_number;
	}
	public int getExternalHeaderBlocksNumber(){
		return externalHeaderBlocks_number;
	}
	public int getGeneralTrailerBlocks(){
		return generalTrailer_blocks;
	}
	public String getSegdRevision(){
		return segd_revision;
	}
	public int generalHeaderBlock_number(){
		return generalHeaderBlock_number;
	}
	public int getSequenceNumber(){
		return sequence_number;
	}
	public AdditionalGeneralHeader[] getAdditionalGeneralHeaders(){
		return additionalGeneralHeaders;
	}
	public ScanType[] getScanTypes(){
		return scanTypes;
	}
	public short[][] getExtendedHeadersData(){
		return extendedHeadersData;
	}
	public short[][] getExternalHeadersData(){
		return externalHeadersData;
	}
	
	public void readHeaders() throws IOException{
		inputStream = new FileInputStreamPositioned(new FileInputStream(file));
		readHeaders(new DataInputStream(inputStream));
		inputStream.close();
	}
	
	private void readHeaders(DataInput inStream) throws IOException {
		readHeader1(inStream);
		readHeader2(inStream);
		additionalGeneralHeaders = new AdditionalGeneralHeader[additionalGeneralHeaderBlocksNumber-1];
		for(int i=1; i<additionalGeneralHeaderBlocksNumber; i++){
			AdditionalGeneralHeader additionalGeneralHeader = new AdditionalGeneralHeader();
			additionalGeneralHeader.read(inStream);
			additionalGeneralHeaders[i-1]=additionalGeneralHeader;
		}
		scanTypes = new ScanType[scanTypes_per_record];
		for(int i=0; i<scanTypes_per_record; i++){
			ScanType scanType = new ScanType(i, this);
			scanType.read(inStream);
			scanTypes[i]=scanType;
		}
		extendedHeadersData = new short[extendedHeaderBlocks_number][32];
		for(int i = 0; i < extendedHeaderBlocks_number; i++){
			readExtendedHeader(inStream, extendedHeadersData[i]);
		}
		externalHeadersData = new short[externalHeaderBlocks_number][32];
		for(int i = 0; i < externalHeaderBlocks_number; i++){
			readExternalHeader(inStream, externalHeadersData[i]);
		}
		//Read traces
		for(ScanType scanType: scanTypes){
			for(ChannelSet channelSet: scanType.getChannelSets()){
				for(int i = 0; i<channelSet.getChannels_in_set(); i++){
					//System.out.println("Trace " + i);
					Trace trace = new Trace(this);
					trace.readHeader(inStream);
					trace.setDataOffset(getPosition());
					trace.readData(inStream);
					//inStream.skipBytes(trace.getSamplesNumber()*4); //skip place for data
					channelSet.addTrace(trace);
				}
				
			}
		}
	}

	public void readHeader1(DataInput inStream) throws IOException {
		// BYTES 1 -2 file number; if 'FFFF', see general header block #3
		try {
			short[] check = { 0xFF, 0xFF };
			fileNumber = getDataValue(getSections(
					readBytes(inStream, 2, check), 4), 10);
		} catch (CheckFailedException e) {
			// Does nothing
		}
		try {
			int formatCode = getDataValue(getSections(readBytes(inStream, 2,
					null), 4), 10);
			switch (formatCode) {
			case 15:
				format = Format.BINARY_MULTIPLEXED_20_BIT;
				break;
			case 22:
				format = Format.QUATERNARY_MULTIPLEXED_8_BIT;
				break;
			case 24:
				format = Format.QUATERNARY_MULTIPLEXED_16_BIT;
				break;
			case 36:
				format = Format.COMPLIMENT_2_INTEGER_MULTIPLEXED_24_BIT;
				break;
			case 38:
				format = Format.COMPLIMENT_2_INTEGER_MULTIPLEXED_32_BIT;
				break;
			case 42:
				format = Format.HEXADECIMAL_MULTIPLEXED_8_BIT;
				break;
			case 44:
				format = Format.HEXADECIMAL_MULTIPLEXED_16_BIT;
				break;
			case 48:
				format = Format.HEXADECIMAL_MULTIPLEXED_32_BIT;
				break;
			case 58:
				format = Format.IEEE_MULTIPLEXED_32_BIT;
				break;
			case 8015:
				format = Format.BINARY_DEMUX_20_BIT;
				break;
			case 8022:
				format = Format.QUATERNARY_DEMUX_8_BIT;
				break;
			case 8024:
				format = Format.QUATERNARY_DEMUX_16_BIT;
				break;
			case 8036:
				format = Format.COMPLIMENT_2_INTEGER_DEMUX_24_BIT;
				break;
			case 8038:
				format = Format.COMPLIMENT_2_INTEGER_DEMUX_32_BIT;
				break;
			case 8042:
				format = Format.HEXADECIMAL_DEMUX_8_BIT;
				break;
			case 8044:
				format = Format.HEXADECIMAL_DEMUX_16_BIT;
				break;
			case 8048:
				format = Format.HEXADECIMAL_DEMUX_32_BIT;
				break;
			case 8058:
				format = Format.IEEE_DEMUX_32_BIT;
				break;
			default:
				format = null;
				throw new RuntimeException("Wrong format code: " + formatCode);
			}
		} catch (CheckFailedException e) {
		}
		try {
			general_constants = getSections(readBytes(inStream, 6, null), 4);
			int year = getDataValue(getSections(readBytes(inStream, 1, null), 4), 10);
			
			short byte12 = readBytes(inStream, 1, null)[0];
			additionalGeneralHeaderBlocksNumber = (short) (byte12 >>> 4);
			short[] day_digits = new short[3];
			day_digits[0] = (short) (byte12 & 0xF);
			short[] day_digits_byte13 = getSections(readBytes(inStream, 1, null), 4);
			day_digits[1] = day_digits_byte13[0];
			day_digits[2] = day_digits_byte13[1];
			int jDay = getDataValue(day_digits, 10);
			if(jDay>366) throw new RuntimeException("Wrong Julian day: " + jDay);
			int hour = getDataValue(getSections(readBytes(inStream, 1, null), 4), 10);
			if(hour>24) throw new RuntimeException("Wrong hour: " + hour);
			int minute = getDataValue(getSections(readBytes(inStream, 1, null),	4), 10);
			if(minute>60) throw new RuntimeException("Wrong minute: " + minute);
			int second = getDataValue(getSections(readBytes(inStream, 1, null), 4), 10);
			if(second>60) throw new RuntimeException("Wrong second: " + second);
			date = new Date(TimeInterval.getTime(year > 50 ? 1900 + year: 2000 + year, jDay, hour, minute, second, 0));
			manufacturer_code = getDataValue(getSections(readBytes(inStream, 1,	null), 4), 10);
			manufacturer_serial_num = getDataValue(getSections(readBytes(inStream, 2, null), 4), 10);
			inStream.skipBytes(3); // not used
			base_scan_interval = readBytes(inStream, 1, null)[0] / 16;
			int byte24 = readBytes(inStream, 1, null)[0];
			byte24 = byte24 >>> 4;
			switch (byte24) {
			case 0:
				polarity = Polarity.UNTESTED;
				break;
			case 1:
				polarity = Polarity.ZERO;
				break;
			case 2:
				polarity = Polarity.DEGREES_45;
				break;
			case 3:
				polarity = Polarity.DEGREES_90;
				break;
			case 4:
				polarity = Polarity.DEGREES_135;
				break;
			case 5:
				polarity = Polarity.DEGREES_180;
				break;
			case 6:
				polarity = Polarity.DEGREES_225;
				break;
			case 7:
				polarity = Polarity.DEGREES_270;
				break;
			case 8:
				polarity = Polarity.DEGREES_315;
				break;
			case 12:
				polarity = Polarity.DEGREES_315;
				break;
			default:
				polarity = null;
				throw new RuntimeException("Wrong polarity: " + polarity);
			}
			inStream.skipBytes(1); // not used
		} catch (CheckFailedException e) {
		}
		try {
			short byte26 = readBytes(inStream, 1, null)[0];
			short recordTypeCode = (short) (byte26 >>> 4);
			switch (recordTypeCode) {
			case 2:
				recordType = RecordType.TEST_RECORD;
				break;
			case 4:
				recordType = RecordType.PARALLEL_CHANNEL_TEST;
				break;
			case 6:
				recordType = RecordType.DIRECT_CHANNEL_TEST;
				break;
			case 8:
				recordType = RecordType.NORMAL_RECORD;
				break;
			case 1:
				recordType = RecordType.OTHER;
				break;
			default:
				recordType = null;
				throw new RuntimeException("Wrong record type: " + recordType);
			}
			short[] record_length_digits = new short[3];
			record_length_digits[0] = (short) (byte26 & 0xF);
			short[] record_length_digits_byte27 = getSections(readBytes(inStream, 1, null), 4);
			record_length_digits[1] = record_length_digits_byte27[0];
			record_length_digits[2] = record_length_digits_byte27[1];
			if(record_length_digits[0]==0xF && record_length_digits[1]==0xF && record_length_digits[2]==0xF){
				throw new CheckFailedException();
			}
			recordLength = getDataValue(record_length_digits, 10)/10.0;
		} catch (CheckFailedException e) {
			//see Extended Record length, bytes 15-17 General Header block #2
		}
		try {
			scanTypes_per_record = getDataValue(getSections(readBytes(inStream, 1, null), 4), 10);
			short[] check = {0xFF};
			channelSets_per_scanType = getDataValue(getSections(readBytes(inStream, 1, check), 4), 10);
		}catch (CheckFailedException e) {
			//see Extended channelSet/ScanTypes
		}
		try {
			skewBlocks_number = getDataValue(getSections(readBytes(inStream, 1, null), 4), 10);
			short[] check = {0xFF};
			extendedHeaderBlocks_number = getDataValue(getSections(readBytes(inStream, 1, check), 4), 10);
		}catch (CheckFailedException e) {
			//see bytes 6-7 of General Header block #2
		}
		try {
			short[] check = {0xFF};
			externalHeaderBlocks_number = getDataValue(getSections(readBytes(inStream, 1, check), 4), 10);
		}catch (CheckFailedException e) {
			//see bytes 8-9 of General Header block #2
		}
	}
	
	private void readHeader2(DataInput inStream) throws IOException {
		try {
			if(fileNumber == -1){
				fileNumber = getDataValue(getSections(readBytes(inStream, 3, null), 1), 2);
			} else {
				inStream.skipBytes(3);
			}
			if(channelSets_per_scanType == -1){
				channelSets_per_scanType = readShorts(inStream, 1)[0];
			} else {
				inStream.skipBytes(2);
			}
			if(extendedHeaderBlocks_number == -1){
				extendedHeaderBlocks_number = readShorts(inStream, 1)[0];
			} else {
				inStream.skipBytes(2);
			}
			if(externalHeaderBlocks_number == -1){
				externalHeaderBlocks_number = readShorts(inStream, 1)[0];
			} else {
				inStream.skipBytes(2);
			}
			inStream.skipBytes(1); //not used
			segd_revision = readBytes(inStream, 1, null)[0] + "." + readBytes(inStream, 1, null)[0];
			generalTrailer_blocks = readShorts(inStream, 1)[0];
			if(recordLength == -1){
				recordLength = getDataValue(getSections(readBytes(inStream, 3, null), 1), 2);
			} else {
				inStream.skipBytes(3);
			}
			inStream.skipBytes(1); //not used
			generalHeaderBlock_number = readBytes(inStream, 1, null)[0];
			inStream.skipBytes(1); //not used
			sequence_number = readShorts(inStream, 1)[0];
			inStream.skipBytes(10); //not used
		} catch (CheckFailedException e) {
			//Do nothing
		}
	}
	
	private void readExtendedHeader(DataInput inStream, short[] data) throws IOException{
		try {
			data=readBytes(inStream, 32, null);
		} catch (CheckFailedException e) {
		}
	}

	private void readExternalHeader(DataInput inStream, short[] data) throws IOException{
		try {
			data=readBytes(inStream, 32, null);
		} catch (CheckFailedException e) {
		}
	}
	
	public long getPosition(){
		return inputStream.getPosition();
	}
	
	static short[] readBytes(DataInput inStream, int byteCount, short[] check)
			throws IOException, CheckFailedException {
		short[] ret = new short[byteCount];
		for (int i = 0; i < byteCount; i++) {
			ret[i] = (short)inStream.readUnsignedByte();
		}
		if (check != null) {
			if (check.length != ret.length) {
				throw new RuntimeException(
						"Check array size should be equal tested array size");
			}
			for (int j = 0; j < ret.length; j++) {
				if (ret[j] != check[j]) {
					return ret;
				}
			}
			throw new CheckFailedException();
		}
		return ret;
	}
	
	static int[] readShorts(DataInput inStream, int shortCount) throws IOException  {
		int[] ret = new int[shortCount];
		for (int i = 0; i < shortCount; i++) {
			ret[i] = inStream.readUnsignedShort();
		}
		return ret;
	}
	
	static int readUnsignedTriple(DataInput inStream) throws IOException  {
		int ret = 0;
		try {
			short[] read = readBytes(inStream, 3,null);
			ret = read[2]|(read[1]<<8)|(read[0]<<16);
		} catch (CheckFailedException e) {
		}
		return ret;
	}
	
	static int readSignedTriple(DataInput inStream) throws IOException  {
		int[] read = new int[3];
		int ret = 0;
		read[0] = inStream.readByte();
		read[1] = inStream.readUnsignedByte();
		read[2] = inStream.readUnsignedByte();
		ret = read[2]|(read[1]<<8)|(read[0]<<16);
		return ret;
	}
	
	static short[] getSections(short[] bytes, int blockLength) {
		int blocksOnByte = 8 / blockLength;
		int arrayLength = bytes.length * blocksOnByte;
		int mask = 1;
		for (int i = 1; i < blockLength; i++) {
			mask = mask + (int) Math.pow(2, i);
		}
		short[] ret = new short[arrayLength];
		for (int i = 0; i < bytes.length; i++) {
			int byteValue = bytes[i];
			for (int j = 0; j < blocksOnByte; j++) {
				ret[(i * blocksOnByte) + blocksOnByte - j - 1] = new Integer(
						byteValue & mask).byteValue();
				byteValue = byteValue >>> blockLength;
			}
		}
		return ret;
	}

	static int getDataValue(short[] digits, int base) {
		int ret = 0;
		for (int i = 0; i < digits.length; i++) {
			if (digits[i] >= base) {
				System.out.println("digit value is more than base");
				return -1;
			}
			ret = ret + digits[i]
					* (int) Math.pow(base, (digits.length - i - 1));
		}
		return ret;
	}
	

	public String toString() {
		String additionalHeadersStr = "";
		for(AdditionalGeneralHeader agh: additionalGeneralHeaders){
			additionalHeadersStr = additionalHeadersStr+agh.toString();
		}
		String scanTypesStr = "";
		for(ScanType st: scanTypes){
			scanTypesStr = scanTypesStr+st.toString();
		}
		String extHeadersStr = "";
		if(extendedHeadersData.length>0){
			extHeadersStr = extHeadersStr+ "\nExtended headers:";
			for(short[] extendedHeaderData: extendedHeadersData){
				extHeadersStr = extHeadersStr+ "\n"+Arrays.toString(extendedHeaderData);
			}
		}
		if(externalHeadersData.length>0){
			extHeadersStr = extHeadersStr+ "\nExternal headers:";
			for(short[] externalHeaderData: externalHeadersData){
				extHeadersStr = extHeadersStr+ "\n"+Arrays.toString(externalHeaderData);
			}
		}
		return "File number: " + fileNumber + "\nFormat: " + format
				+"\nGeneral constants: " + Arrays.toString(general_constants)
				+"\nNumber of additional General Header Blocks: "	+ additionalGeneralHeaderBlocksNumber 
				+"\nDate: "	+ (date == null ? "null" : date) 
				+"\nManufacturer code: "	+ manufacturer_code 
				+"\nManufacturer serial number: "+ manufacturer_serial_num 
				+"\nBase scan interval, msec: " + base_scan_interval 
				+"\nPolarity: " + polarity
				+"\nRecord type: " + recordType 
				+"\nRecord length, in 0.512 sec intervals: " + recordLength
				+"\nScan Types number per record: "+ scanTypes_per_record
				+"\nChannel Sets per Scan Type number: " + channelSets_per_scanType
				+"\nNumber of 32-bytes skew blocks: " + skewBlocks_number
				+"\nExtended header length, in 32 bytes blocks: " + extendedHeaderBlocks_number
				+"\nExternal header length, in 32 bytes blocks: " + externalHeaderBlocks_number
				+"\nSEGD revision: " + segd_revision
				+"\nGeneral trailer length, in 32 bytes blocks: " + generalTrailer_blocks
				+"\nGeneral header block number: " + generalHeaderBlock_number
				+"\nSequence number: " + sequence_number
				+additionalHeadersStr + extHeadersStr + scanTypesStr ;
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java SegdRecord [file_name]");
			System.exit(1);
		}
		try {
			File file = new File(args[0]);
			SegdRecord rec = new SegdRecord(file);
			rec.readHeaders();
			System.out.println(rec.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

class CheckFailedException extends Exception {
	private static final long serialVersionUID = 1L;

	public CheckFailedException() {
		super();
	}

	public CheckFailedException(String str) {
		super(str);
	}

}
