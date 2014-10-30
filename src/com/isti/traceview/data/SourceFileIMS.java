package com.isti.traceview.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.isti.traceview.data.ims.BlockSet;
import com.isti.traceview.data.ims.DAT2;
import com.isti.traceview.data.ims.DataType;
import com.isti.traceview.data.ims.DataTypeWaveform;
import com.isti.traceview.data.ims.IMSFile;
import com.isti.traceview.data.ims.IMSFormatException;
import com.isti.traceview.data.ims.STA2;
import com.isti.traceview.data.ims.WID2;

import edu.sc.seis.seisFile.sac.SacTimeSeries;
import gov.usgs.anss.cd11.CanadaException;

public class SourceFileIMS extends SourceFile {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SourceFileIMS.class);

	public SourceFileIMS(File file) {
		super(file);
		logger.debug("Created: " + this);
	}

	@Override
	public Set<RawDataProvider> parse(DataModule dataModule) {
		Set<RawDataProvider> ret = new HashSet<RawDataProvider>();
		BufferedRandomAccessFile dis = null;
		try {
			dis = new BufferedRandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
			if (getFile().length() > 0) {
				//long currentOffset = dis.getFilePointer();
				IMSFile ims = IMSFile.read(dis, true);
				for (DataType dataType : ims.getDataTypes()) {
					if (dataType instanceof DataTypeWaveform) {
						DataTypeWaveform dtw = (DataTypeWaveform) dataType;
						for (BlockSet bs : dtw.getBlockSets()) {
							RawDataProvider channel = dataModule.getOrAddChannel(bs.getWID2().getChannel(), DataModule.getOrAddStation(bs.getWID2().getStation()), "", "");
							ret.add(channel);
							Segment segment = new Segment(this, bs.getStartOffset(), bs.getWID2().getStart(), 1000.0/bs.getWID2().getSampleRate(), bs.getWID2().getNumSamples(), 0);
							channel.addSegment(segment);
						}
					}
				}
			} else {
				logger.error("File " + getFile().getCanonicalPath() + " has null length");
			}

		} catch (FileNotFoundException e) {
			logger.error("File not found: ", e);
		} catch (IOException e) {
			logger.error("IO error: ", e);
		} catch (IMSFormatException e) {
			logger.error("Wrong IMS file format: ", e);
		} catch (ParseException e) {
			logger.error("Parsing problems: ", e);
		} catch (CanadaException e) {
			logger.error("Canada decompression problems: ", e);
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
				logger.error("IOException:", e);	
			}
		}
		return ret;
	}

	public void load(Segment segment) {
		//int[] data = null;
		BufferedRandomAccessFile dis = null;
		try {
			dis = new BufferedRandomAccessFile(getFile().getCanonicalPath(), "r");
			dis.order(BufferedRandomAccessFile.BIG_ENDIAN);
			if (getFile().length() > 0) {
				dis.seek(segment.getStartOffset());
				WID2 wid2 = new WID2(segment.getStartOffset());
				wid2.read(dis);
				STA2 sta2 = new STA2(dis.getFilePointer());
				sta2.read(dis);
				DAT2 dat2 = new DAT2(dis.getFilePointer(), wid2);
				dat2.read(dis);
				for (int value : dat2.getData()) {
					segment.addDataPoint(value);
				}

			} else {
				logger.error("File " + getFile().getCanonicalPath() + " has null length");
			}

		} catch (FileNotFoundException e) {
			logger.error("File not found: ", e);
		} catch (IOException e) {
			logger.error("IO error: ", e);
		} catch (IMSFormatException e) {
			logger.error("Wrong IMS file format: ", e);
		} catch (ParseException e) {
			logger.error("Parsing problems: ", e);
		} catch (CanadaException e) {
			logger.error("Canada decompression problems: ", e);
		} finally {
			try {
				dis.close();
			} catch (IOException e) {
				logger.error("IOException:", e);	
			}
		}
	}

	@Override
	public FormatType getFormatType() {
		return FormatType.IMS;
	}

}
