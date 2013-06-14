package com.isti.traceview;

import java.util.Date;

/**
 * Abstract base class to represent a command to be executed by {@link CommandExecutor}
 * 
 * @author Max Kokoulin
 */

public abstract class AbstractCommand implements ICommand, Comparable {
	/**
	 * Priority of command
	 * 
	 * @uml.property name="priority"
	 */
	private int priority;

	/**
	 * Time of execution start
	 * 
	 * @uml.property name="startTime"
	 */
	private Date startTime = null;

	/**
	 * Creation time of command
	 * 
	 * @uml.property name="creationTime"
	 */
	private Date creationTime = null;


	public AbstractCommand() {
		this.creationTime = new Date();
	}
	
	/**
	 * @see Runnable#run()
	 */
	public void run() {
		this.startTime = new Date();
	}

	/**
	 * Getter of the property <tt>startTime</tt>
	 * 
	 * @return returns the startTime.
	 * @see ICommand#getStartTime()
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * Getter of the property <tt>creationTime</tt>
	 * 
	 * @return creation time of command.
	 * @see ICommand#getCreationTime()
	 */
	public Date getCreationTime() {
		return creationTime;
	}

	/**
	 * Getter of the property <tt>priority</tt>
	 * 
	 * @return priority of command.
	 * @see ICommand#getPriority()
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
	 * Standard comparator - by priority-creation time
	 * 
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(Object o) {
		if ((o instanceof AbstractCommand)) {
			AbstractCommand aco = (AbstractCommand) o;
			if (this.getPriority() == aco.getPriority()) {
				return this.getCreationTime().compareTo(aco.getCreationTime());
			} else {
				if (this.getPriority() > aco.getPriority()) {
					return 1;
				} else {
					return -1;
				}
			}
		} else {
			return -1;
		}
	}
}
