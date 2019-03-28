package com.isti.xmax.common;

import com.isti.traceview.common.AbstractEvent;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.data.PlotDataProvider;
import java.util.Date;
import java.util.Set;

/**
 * This class holds information about Automated Quality Control issue, loaded from XML files.
 * 
 * @author Max Kokoulin
 */
public class QCIssue extends AbstractEvent implements IEvent {

	public QCIssue(Date time, long duration) {
		super(time, duration);
	}

	@Override
	public String getType() {
		return "QCISSUE";
	}

	private String message = "";

	private int priority;

	private Set<PlotDataProvider> channels;

	/**
	 * Getter of the property <tt>priority</tt>
	 * 
	 * @return Returns the priority.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Setter of the property <tt>priority</tt>
	 * 
	 * @param priority
	 *            The priority to set.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Getter of the property <tt>channels</tt>
	 * 
	 * @return Returns the channels.
	 */
	public Set<PlotDataProvider> getChannels() {
		return channels;
	}

	/**
	 * Setter of the property <tt>channels</tt>
	 * 
	 * @param channels
	 *            The channels to set.
	 */
	public void setChannels(Set<PlotDataProvider> channels) {
		this.channels = channels;
	}

	/**
	 * Getter of the property <tt>message</tt>
	 * 
	 * @return Returns the message.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Setter of the property <tt>message</tt>
	 * 
	 * @param message
	 *            The message to set.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}
