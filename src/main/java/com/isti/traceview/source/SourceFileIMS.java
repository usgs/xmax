package com.isti.traceview.source;

import com.isti.traceview.data.DataModule;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.data.ims.BlockSet;
import com.isti.traceview.data.ims.DAT2;
import com.isti.traceview.data.ims.DataType;
import com.isti.traceview.data.ims.DataTypeWaveform;
import com.isti.traceview.data.ims.IMSFile;
import com.isti.traceview.data.ims.IMSFormatException;
import com.isti.traceview.data.ims.STA2;
import com.isti.traceview.data.ims.WID2;
import gov.usgs.anss.cd11.CanadaException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

public class SourceFileIMS extends SourceFile {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SourceFileIMS.class);

	public SourceFileIMS(File file) {
		super(file);
		logger.debug("Created: " + this);
	}

	@Override
	public Set<PlotDataProvider> parse() {
		Set<PlotDataProvider> ret = new HashSet<>();
		RandomAccessFile dis = null;
		try {
			dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
			if (getFile().length() > 0) {
				//long currentOffset = dis.getFilePointer();
				IMSFile ims = IMSFile.read(dis, true);
				for (DataType dataType : ims.getDataTypes()) {
					if (dataType instanceof DataTypeWaveform) {
						DataTypeWaveform dtw = (DataTypeWaveform) dataType;
						for (BlockSet bs : dtw.getBlockSets()) {
							PlotDataProvider channel = new PlotDataProvider(bs.getWID2().getChannel(), DataModule
                  .getOrAddStation(bs.getWID2().getStation()), "", "");
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
		RandomAccessFile dis = null;
		try {
			dis = new RandomAccessFile(getFile().getCanonicalPath(), "r");
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
