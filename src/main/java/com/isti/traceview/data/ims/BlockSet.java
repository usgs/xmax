package com.isti.traceview.data.ims;

import gov.usgs.anss.cd11.CanadaException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import org.apache.log4j.Logger;

public class BlockSet {
	private static final Logger logger = Logger.getLogger(BlockSet.class);

	private long startOffset;
	private WID2 wid2;
	private STA2 sta2;
	private DAT2 dat2;
	private CHK2 chk2;

	public long getStartOffset() {
		return startOffset;
	}

	public WID2 getWID2() {
		return wid2;
	}

	public STA2 getSTA2() {
		return sta2;
	}

	public DAT2 getDAT2() {
		return dat2;
	}

	public CHK2 getCHK2() {
		return chk2;
	}

	public void read(RandomAccessFile input, boolean parseOnly) throws IOException, IMSFormatException, ParseException, CanadaException {
		long filePointer = 0;
		String line = null;
		startOffset = input.getFilePointer();
		while (true) {
			filePointer = input.getFilePointer();
			line = input.readLine();
			if (line.startsWith("STOP") || line.startsWith("TIME_STAMP")) {
				input.seek(filePointer);
				break;
			} else if (line.startsWith("WID2")) {
				wid2 = new WID2(filePointer);
				input.seek(filePointer);
				wid2.read(input);
			} else if (line.startsWith("STA2")) {
				sta2 = new STA2(filePointer);
				input.seek(filePointer);
				sta2.read(input);
			} else if (line.startsWith("DAT2")) {
				dat2 = new DAT2(filePointer, wid2);
				input.seek(filePointer);
				dat2.read(input);
			} else if (line.startsWith("CHK2")) {
				chk2 = new CHK2(filePointer);
				input.seek(filePointer);
				chk2.read(input);
				break;
			} else {
				throw new IMSFormatException("Unrecognized line in the file: " + line);
			}
		}

	}

	public void check() throws IMSFormatException {
		if (chk2.getChkSum() != chk2.checksum(dat2)) {
			throw new IMSFormatException("Wrong waveform checksum");
		}
	}
}
