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
	public int getInfoAreaWidth();
	
	public ChannelView getChannelView(List<PlotDataProvider> channels);

	public ChannelView getChannelView(PlotDataProvider channel);

}
