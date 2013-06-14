package com.isti.xmax.data;

import com.isti.traceview.common.Station;
import com.isti.traceview.data.IChannelFactory;

/**
 * Factory for customized {@link com.isti.traceview.data.Channel}s
 * 
 * @author Max Kokoulin
 */
public class XMAXChannelFactory implements IChannelFactory {
	public XMAXChannel getChannel(String channelName, Station station, String networkName, String locationName) {
		return new XMAXChannel(channelName, station, networkName, locationName);
	}
}
