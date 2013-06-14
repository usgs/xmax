package com.isti.traceview.data;

import com.isti.traceview.common.IEvent;

/**
 * Wrapper for IEvent. Contains IEvent itself and flag is it first(front) point of continious event. Used for drawing events.
 * @author Max Kokoulin
 */

public class EventWrapper implements Comparable {
	private IEvent event = null;
	private boolean isFront = false;
	
	public EventWrapper(IEvent event, boolean isFront){
		this.event = event;
		this.isFront = isFront;
	}
	
	public IEvent getEvent(){
		return event;
	}
	
	public boolean isFront(){
		return isFront;
	}
	
	public int compareTo(Object o) {
		if ((o instanceof EventWrapper)) {
				return event.compareTo(((EventWrapper)o).getEvent());
		} else {
			return -1;
		}
	}
}