package com.isti.traceview.gui;

import com.isti.traceview.data.PlotDataProvider;
import java.util.List;

/**
 * Default channel view factory, generates ordinary {@link ChannelView}
 * 
 * @author Max Kokoulin
 */
public class DefaultChannelViewFactory implements IChannelViewFactory {
	
	public int getInfoAreaWidth(){
		return 80;
	}

	public ChannelView getChannelView(List<PlotDataProvider> channels) {
		return new ChannelView(channels);
	}

	public ChannelView getChannelView(PlotDataProvider channel) {
		return new ChannelView(channel);
	}

}
