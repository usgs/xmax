package edu.sc.seis.seisFile.segd;

import java.io.DataInput;
import java.io.IOException;

public class ScanType {
	private int scanTypeNumber;
	private SegdRecord parent;
	private ChannelSet[] channelSets = null;
	
	public ScanType(int number, SegdRecord parent){
		this.scanTypeNumber = number+1;
		this.parent = parent;
	}
	
	public int getNumber(){
		return scanTypeNumber;
	}
	public ChannelSet[] getChannelSets(){
		return channelSets;
	}

	public void read(DataInput inStream) throws IOException{
		channelSets = new ChannelSet[parent.getChannelSetsPerScanType()];
		for(int i=0; i<parent.getChannelSetsPerScanType(); i++){
			ChannelSet channelSet = new ChannelSet();
			channelSet.read(inStream);
			channelSets[i] = channelSet;
		}
		//Space for skew blocks
		inStream.skipBytes(parent.getSkewBlocksNumber()*32);
	}

	public String toString(){
		String channelSetsStr = "";
		for(ChannelSet cs: channelSets){
			channelSetsStr = channelSetsStr+cs.toString();
		}
		return "\nScan Type # " + scanTypeNumber
		+ channelSetsStr;
	}
}
