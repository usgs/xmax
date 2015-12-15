package com.isti.traceview.data;

import com.isti.traceview.gui.ChannelView;

public class SelectionContainer {
	private int selectionLevel;
	private ChannelView channelviewObj; 
	public SelectionContainer(int level, ChannelView channel){
		this.selectionLevel = level;
		this.channelviewObj = channel;
	}
	
	public int getSelectionLevel(){
		return this.selectionLevel;
	}
	
	public ChannelView getChannelView(){
		return this.channelviewObj;
	}
}
