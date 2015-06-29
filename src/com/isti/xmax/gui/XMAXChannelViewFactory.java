package com.isti.xmax.gui;

import java.util.List;


import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.gui.IChannelViewFactory;

/**
 * Factory for customized {@link com.isti.traceview.gui.ChannelView}s
 * 
 * @author Max Kokoulin
 */
public class XMAXChannelViewFactory implements IChannelViewFactory {
	
	public int getInfoAreaWidth(){
		return 80;
	}

	public XMAXChannelView getChannelView(List<PlotDataProvider> channels) {
		return new XMAXChannelView(channels, getInfoAreaWidth(), true, null, null);
	}

	public XMAXChannelView getChannelView(PlotDataProvider channel) {
		return new XMAXChannelView(channel, getInfoAreaWidth(), true, null, null);
	}

}
