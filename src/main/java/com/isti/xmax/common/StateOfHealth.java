package com.isti.xmax.common;

import java.util.Date;

import com.isti.traceview.common.AbstractEvent;
import com.isti.traceview.common.IEvent;

/**
 * State of health (SOH) information for a trace.
 * 
 * @author Max Kokoulin
 */
public class StateOfHealth extends AbstractEvent implements IEvent {

	public StateOfHealth(Date time, long duration) {
		super(time, duration);
	}

	@Override
	public String getType() {
		return "SOH";

	}

}
