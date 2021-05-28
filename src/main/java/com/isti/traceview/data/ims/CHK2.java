package com.isti.traceview.data.ims;

import java.io.IOException;
import java.io.RandomAccessFile;
import org.apache.log4j.Logger;

public class CHK2 extends Block {
	private static final Logger logger = Logger.getLogger(CHK2.class);
	private int chksum;
	
	public CHK2(long startOffset){
		super(startOffset);
	}

	public int getChkSum() {
		return chksum;
	}

	public void read(RandomAccessFile input) throws IMSFormatException, IOException {
		logger.debug("Reading buffered random access file");
		header = input.readLine();
		if (!header.startsWith("CHK2")) {
			throw new IMSFormatException("Wrong check block header: " + header);
		}
		String[] lineParts = header.split("\\s+");
		chksum = Integer.parseInt(lineParts[1].trim());
	}

	public int checksum(DAT2 dat2) {
		int i_sample;
		int sample_value;
		int modulo;
		int checksum;
		int MODULO_VALUE = 100000000;

		checksum = 0;
		modulo = MODULO_VALUE;
		for (i_sample = 0; i_sample < dat2.getData().length; i_sample++) {
			/* check on sample value overflow */
			sample_value = dat2.getData()[i_sample];
			if (Math.abs(sample_value) >= modulo) {
				sample_value = sample_value - (sample_value / modulo) * modulo;
			}
		}
		return checksum;
	}
}
