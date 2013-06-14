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

	/**
	 * @uml.property name="message"
	 */
	private String message = "";

	/**
	 * @uml.property name="priority"
	 */
	private int priority;

	/**
	 * @uml.property name="channels"
	 */
	private Set<PlotDataProvider> channels;

	/**
	 * Getter of the property <tt>priority</tt>
	 * 
	 * @return Returns the priority.
	 * @uml.property name="priority"
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Setter of the property <tt>priority</tt>
	 * 
	 * @param priority
	 *            The priority to set.
	 * @uml.property name="priority"
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Getter of the property <tt>channels</tt>
	 * 
	 * @return Returns the channels.
	 * @uml.property name="channels"
	 */
	public Set<PlotDataProvider> getChannels() {
		return channels;
	}

	/**
	 * Setter of the property <tt>channels</tt>
	 * 
	 * @param channels
	 *            The channels to set.
	 * @uml.property name="channels"
	 */
	public void setChannels(Set<PlotDataProvider> channels) {
		this.channels = channels;
	}

	/**
	 * Getter of the property <tt>message</tt>
	 * 
	 * @return Returns the message.
	 * @uml.property name="message"
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Setter of the property <tt>message</tt>
	 * 
	 * @param message
	 *            The message to set.
	 * @uml.property name="message"
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}
