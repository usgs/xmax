package com.isti.traceview.data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Socket data source
 * 
 * @author Max Kokoulin
 */
public abstract class SourceSocket implements ISource, Serializable {
	private static Logger lg = Logger.getLogger(SourceSocket.class);

	public SourceType getSourceType() {
		return SourceType.SOCKET;
	}

	public abstract FormatType getFormatType();

	public Set<RawDataProvider> parse(DataModule dataModule) {
		return null;
	}

	public int[] load(long offset, int sampleCount) {
		return null;
	}

	public String getName() {
		return "SocketRawDataProvider";
	}

	public String toString() {
		return "SocketRawDataProvider";
	}
}
