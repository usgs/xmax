package com.isti.traceview.common;

import java.util.Date;

/**
 * Concrete class for very simple default event
 * @author Max Kokoulin
 *
 */
public class DefaultEvent extends AbstractEvent implements IEvent {

	/**
	 * 
	 * @param time event start time
	 */
	public DefaultEvent(Date time) {
		super(time, 0);
	}

	public String getType() {
		return "DEFAULT";
	}

}
