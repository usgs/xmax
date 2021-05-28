package com.isti.traceview.data.ims;

import gov.usgs.anss.cd11.CanadaException;
import gov.usgs.anss.cd11.ChannelSubframe;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

public class DAT2 extends Block {
	private static final Logger logger = Logger.getLogger(DAT2.class);
	private int[] data;
	private WID2 wid2;

	public DAT2(long startOffset, WID2 wid2) {
		super(startOffset);
		this.wid2 = wid2;
		data = new int[wid2.getNumSamples()];
	}

	public int[] getData() {
		return data;
	}

	public void read(RandomAccessFile input) throws IMSFormatException, IOException, ParseException, CanadaException {
		header = input.readLine();
		if (!header.startsWith("DAT2")) {
			throw new IMSFormatException("Wrong data block header: " + header);
		}
		int numSamples = 0;
		StringBuilder line = new StringBuilder();
		while (true) {
			long filePointer = input.getFilePointer();
			if (wid2.getCsf() == WID2.Compression.CSF) {
				String l = input.readLine();
				if (isBlockEnded(l)) {
					numSamples = decodeCSF(numSamples, line.toString());
					input.seek(filePointer);
					break;
				}
				line.append(l);
				if (line.toString().endsWith("=")) {
					numSamples = decodeCSF(numSamples, line.toString());
					line = new StringBuilder();

				}

			} else if (wid2.getCsf() == WID2.Compression.INT) {
				line = new StringBuilder(input.readLine());
				// lg.debug(line);
				if (isBlockEnded(line.toString())) {
					input.seek(filePointer);
					break;
				}
				String[] lineParts = line.toString().split("\\s+");
				if (lineParts.length > 0) {
					try {
						for (String part : lineParts) {
							if (part.length() > 0) {
								data[numSamples] = Integer.parseInt(part.trim());
								numSamples++;
							}
						}
					} catch (NumberFormatException e) {
						input.seek(filePointer);
						logger.error("NumberFormatException:", e);	
						break;
					}
				}
			} else {
				throw new IMSFormatException("Unknown compression type");
			}
		}
		if (numSamples != wid2.getNumSamples()) {
			throw new IMSFormatException("Wrong samples count in data block: read " + numSamples + ", should be " + wid2.getNumSamples());
		}
	}

	private int decodeCSF(int numSamples, String line) throws CanadaException {
		List<ChannelSubframe> csfList = new ArrayList<>();
		byte[] csfs = Base64.decodeBase64(line);
		ByteBuffer bb = ByteBuffer.wrap(csfs);
		while (bb.position() < bb.capacity()) {
			csfList.add(new ChannelSubframe(bb));
		}
		for (ChannelSubframe csf : csfList) {
			int[] samples = new int[csf.getNsamp()];
			csf.getSamples(samples);
			System.arraycopy(samples, 0, data, numSamples + 0, samples.length);
			numSamples += csf.getNsamp();
		}
		return numSamples;
	}
}
