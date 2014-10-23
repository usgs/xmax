package edu.sc.seis.seisFile.segd;

import java.io.DataInput;
import java.io.IOException;
import edu.sc.seis.seisFile.segd.SegdRecord;

public class ChannelSet {
	
	public enum ChannelType {
		OTHER, 
		EXTERNAL_DATA, 
		TIME_COUNTER, 
		WATER_BREAK, 
		UP_HOLE,
		TIME_BREAK,
		SEIS,
		UNUSED,
		SUGNATURE_UNFILTERED,
		SIGNATURE_FILTERED,
		AUXILIARY_DATA_TRAILER
	};
	
	public enum GainMode {
		INDIVIDUAL_AGC, 
		GANGED_AGC, 
		FIXED_GAIN, 
		PROGRAMMED_GAIN, 
		BINARY_GAIN_CONTROL,
		IFP_GAIN_CONTROL
	};
	
	public enum ArrayForming {
		NO_ARRAY_FORMUNG, 
		SUMMED_2_GROURS_NO_WEIGHTING, 
		SUMMED_3_GROURS_NO_WEIGHTING, 
		PROGRAMMED_GAIN, 
		BINARY_GAIN_CONTROL,
		IFP_GAIN_CONTROL
	};
	
	
	private int scanTypeNumber = -1;
	private int channelSetNumber = -1;
	private int startTime = -1;
	private int endTime = -1;
	private double mpFactor = 0.0;
	private int channels_in_set = -1;
	private ChannelType channelType = null;
	private GainMode gainMode = null;
	private int subscans_number = -1;
	private int alias_filter_frequency = -1;
	private int alias_filter_slope = -1;
	private int lowCut_filter_frequency = -1;
	private int lowCut_filter_slope = -1;
	private float notchFilter_first = 0.0f;
	private float notchFilter_second = 0.0f;
	private float notchFilter_third = 0.0f;
	private short extended_header_flag = -1;
	private short trace_header_extensions = -1;
	private short vertical_stack = -1;
	private short streamer_number = -1;
	private boolean array_weighted = false;
	private short   array_summed_groups = -1;
	private Trace[] traces = null;
	private int tracesAdded;
	
	public ChannelSet(){
		tracesAdded = 0;
	}
	
	public int getScanTypeNumber() {
		return scanTypeNumber;
	}
	public int getChannelSetNumber() {
		return channelSetNumber;
	}
	public int getStartTime() {
		return startTime;
	}
	public int getEndTime() {
		return endTime;
	}
	public double getMpFactor() {
		return mpFactor;
	}
	public int getChannels_in_set() {
		return channels_in_set;
	}
	public ChannelType getChannelType() {
		return channelType;
	}
	public GainMode getGainMode() {
		return gainMode;
	}
	public int getSubscans_number() {
		return subscans_number;
	}
	public int getAlias_filter_frequency() {
		return alias_filter_frequency;
	}
	public int getAlias_filter_slope() {
		return alias_filter_slope;
	}
	public int getLowCut_filter_frequency() {
		return lowCut_filter_frequency;
	}
	public int getLowCut_filter_slope() {
		return lowCut_filter_slope;
	}
	public float getNotchFilter_first() {
		return notchFilter_first;
	}
	public float getNotchFilter_second() {
		return notchFilter_second;
	}
	public float getNotchFilter_third() {
		return notchFilter_third;
	}
	public short getExtended_header_flag() {
		return extended_header_flag;
	}
	public short getTrace_header_extensions() {
		return trace_header_extensions;
	}
	public short getVertical_stack() {
		return vertical_stack;
	}
	public short getStreamer_number() {
		return streamer_number;
	}
	public boolean isArray_weighted() {
		return array_weighted;
	}
	public short getArray_summed_groups() {
		return array_summed_groups;
	}
	public Trace[] getTraces() {
		return traces;
	}
	
	public void addTrace(Trace trace){
		traces[tracesAdded++] = trace;
	}



	public void read(DataInput inStream) throws IOException{
		
		try {
			scanTypeNumber = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 1, null),	4), 10);
			short[] check = {0xFF}; 
			channelSetNumber = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 1, check),	4), 10);
		} catch (CheckFailedException e) {
			//See extended channel set number, bytes 27-28
		}
		try {
			startTime = SegdRecord.readShorts(inStream, 1)[0]*2;
			endTime = SegdRecord.readShorts(inStream, 1)[0]*2;
			short byte7 = inStream.readByte();
			short byte8 = SegdRecord.readBytes(inStream, 1, null)[0];
			int integerPart = byte7>>2;
			int fractionPart = ((byte7&0x3)<<8) | byte8;
			mpFactor = Math.pow(2, new Double(integerPart+"."+fractionPart));
			channels_in_set = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null),	4), 10);
			short byte11 = SegdRecord.readBytes(inStream, 1, null)[0];
			switch (byte11>>>4) {
			case 7:
				channelType = ChannelType.OTHER;
				break;
			case 6:
				channelType = ChannelType.EXTERNAL_DATA;
				break;
			case 5:
				channelType = ChannelType.TIME_COUNTER;
				break;
			case 4:
				channelType = ChannelType.WATER_BREAK;
				break;
			case 3:
				channelType = ChannelType.UP_HOLE;
				break;
			case 2:
				channelType = ChannelType.TIME_BREAK;
				break;
			case 1:
				channelType = ChannelType.SEIS;
				break;
			case 0:
				channelType = ChannelType.UNUSED;
				break;
			case 8:
				channelType = ChannelType.SUGNATURE_UNFILTERED;
				break;
			case 9:
				channelType = ChannelType.SIGNATURE_FILTERED;
				break;
			case 12:
				channelType = ChannelType.AUXILIARY_DATA_TRAILER;
				break;
			default:
				channelType = null;
			}
			short[] byte12 = SegdRecord.getSections(SegdRecord.readBytes(inStream, 1, null), 4);
			subscans_number = (int) Math.pow(2, byte12[0]);
			switch (byte12[1]) {
			case 1:
				gainMode = GainMode.INDIVIDUAL_AGC;
				break;
			case 2:
				gainMode = GainMode.GANGED_AGC;
				break;
			case 3:
				gainMode = GainMode.FIXED_GAIN;
				break;
			case 4:
				gainMode = GainMode.PROGRAMMED_GAIN;
				break;
			case 8:
				gainMode = GainMode.BINARY_GAIN_CONTROL;
				break;
			case 9:
				gainMode = GainMode.IFP_GAIN_CONTROL;
				break;
			default:
				gainMode = null;
			}
			alias_filter_frequency = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10);
			alias_filter_slope = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10);
			lowCut_filter_frequency = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10);
			lowCut_filter_slope = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10);
			//inStream.skipBytes(2);  //WRONG NUMBERS!!!!!!!
			notchFilter_first = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10)/10.0f;
			notchFilter_second = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10)/10.0f;
			notchFilter_third = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 2, null), 4), 10)/10.0f;
			if(channelSetNumber == -1){
				channelSetNumber = SegdRecord.readShorts(inStream, 1)[0];
			} else {
				inStream.skipBytes(2);
			}
			short byte29 = SegdRecord.readBytes(inStream, 1, null)[0];
			extended_header_flag = (short) (byte29>>>4);
			trace_header_extensions = (short)(byte29 & 0xF);
			vertical_stack = SegdRecord.readBytes(inStream, 1, null)[0];
			streamer_number = SegdRecord.readBytes(inStream, 1, null)[0];
			short byte32 = SegdRecord.readBytes(inStream, 1, null)[0];
			array_weighted = ((byte32>>>4)==1);
			array_summed_groups = (short) (byte32&0xF);
			traces = new Trace[channels_in_set];
		} catch (CheckFailedException e) {
		}
		
		
	}
	
	public String toString(){
		String tracesStr = "";
		for(Trace trace: traces){
			tracesStr = tracesStr+trace.toString();
		}
		return "\n\tChannel set # " + channelSetNumber
		+"\n\t\tScanType #: " + scanTypeNumber
		+"\n\t\tStart time, ms: " + startTime
		+"\n\t\tEnd time, ms: " + endTime
		+"\n\t\tMP factor: " + mpFactor
		+"\n\t\tChannels in set: " + channels_in_set
		+"\n\t\tChannel type: " + channelType
		+"\n\t\tSubscans number: " + subscans_number
		+"\n\t\tGain mode: " + gainMode
		+"\n\t\tAlias filter frequency, hz: " + alias_filter_frequency
		+"\n\t\tAlias filter slope, db per octave: " + alias_filter_slope
		+"\n\t\tLow Cut filter frequency, hz: " + lowCut_filter_frequency
		+"\n\t\tLow Cut filter slope, db per octave: " + lowCut_filter_slope
		+"\n\t\tFirst notch filter, hz: " + notchFilter_first
		+"\n\t\tSecond notch filter, hz: " + notchFilter_second
		+"\n\t\tThird notch filter, hz: " + notchFilter_third
		+"\n\t\tExtended header flag: " + extended_header_flag
		+"\n\t\tTrace header extensions: " + trace_header_extensions
		+"\n\t\tVertical stack: " + vertical_stack
		+"\n\t\tStreamer number: " + streamer_number
		+"\n\t\tArray weighted: " + array_weighted
		+"\n\t\tArray groups summed: " + array_summed_groups
		+ tracesStr;
	}
}
