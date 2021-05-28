package com.isti.traceview.data;

import com.isti.traceview.common.Station;

/**
 * Default factory class to produce plot data providers
 */
public class DefaultChannelFactory implements IChannelFactory {

	public PlotDataProvider getChannel(String channelName, Station station, String networkName, String locationName) {
		return new PlotDataProvider(channelName, station, networkName, locationName);
	}
}
