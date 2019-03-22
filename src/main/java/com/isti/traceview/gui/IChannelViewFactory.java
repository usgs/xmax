package com.isti.traceview.gui;

import java.util.List;

import com.isti.traceview.data.PlotDataProvider;

/**
 * Factory for {@link ChannelView}. Library users can create factory for their own, customized
 * ChannelViews
 * 
 * @author Max Kokoulin
 */
public interface IChannelViewFactory {
	int getInfoAreaWidth();
	
	ChannelView getChannelView(List<PlotDataProvider> channels);

	ChannelView getChannelView(PlotDataProvider channel);

}
