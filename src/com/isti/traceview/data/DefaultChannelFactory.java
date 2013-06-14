package com.isti.traceview.data;

/**
 * Default factory class to produce plot data providers
 */
import com.isti.traceview.common.Station;

public class DefaultChannelFactory implements IChannelFactory {

	public PlotDataProvider getChannel(String channelName, Station station, String networkName, String locationName) {
		return new PlotDataProvider(channelName, station, networkName, locationName);
	}

}
