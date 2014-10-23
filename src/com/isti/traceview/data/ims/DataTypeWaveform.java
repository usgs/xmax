package com.isti.traceview.data.ims;

import gov.usgs.anss.cd11.CanadaException;

import java.io.EOFException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.isti.traceview.data.BufferedRandomAccessFile;

public class DataTypeWaveform extends DataType {
	private static Logger lg = Logger.getLogger(DataTypeWaveform.class);

	private List<BlockSet> channels = null;

	public DataTypeWaveform(long startOffset) {
		super(startOffset);
		channels = new ArrayList<BlockSet>();
	}
	
	public List<BlockSet> getBlockSets(){
		return channels;
	}
	
	public void read(BufferedRandomAccessFile input, boolean parseOnly) throws IOException, IMSFormatException, ParseException, CanadaException {
		lg.debug("DataTypeWaveform.read begin");
		long filePointer = 0;
		try {
			while (true) {
				filePointer = input.getFilePointer();
				String line = input.readLine();
				if(line == null){
					break;
				}
				if (line.startsWith("STOP")) {
					break;
				}
				if (line.startsWith("TIME_STAMP")) {
					continue;
				}
				input.seek(filePointer);
				BlockSet bs = new BlockSet();
				bs.read(input, parseOnly);
				if(channels.size()>0){
					if(!bs.getWID2().getStation().equals(channels.get(0).getWID2().getStation())){
						throw new IMSFormatException("Different stat");
					}
				}
				channels.add(bs);
			}
		} catch (EOFException e) {
			// Do nothing
		}

		lg.debug("DataTypeWaveform.read end");
	}

	@Override
	public void check() throws IMSFormatException {
		int i = 1;
		for (BlockSet set : channels) {
			if (set.getCHK2().getChkSum() != set.getCHK2().checksum(set.getDAT2()))
				throw new IMSFormatException("Wrong checksum, dataset number " + i);
			i++;
		}
	}
}

