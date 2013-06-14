package com.isti.traceview.data;

import java.io.DataInput;
import java.io.IOException;

import edu.sc.seis.seisFile.mseed.ControlHeader;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;

/**
 * 
 * @author Max Kokoulin
 * Adds synchronization to SeedRecord.read - to avoid messed traces in concurrent environment
 */

public class SynchronizedSeedRecord extends SeedRecord {

	public SynchronizedSeedRecord(ControlHeader header) {
		super(header);
	}
	
    public synchronized static SeedRecord read(DataInput inStream, int defaultRecordSize) throws IOException, SeedFormatException {
    	return SeedRecord.read(inStream, defaultRecordSize);
    }
}
