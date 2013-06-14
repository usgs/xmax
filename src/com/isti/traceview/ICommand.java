package com.isti.traceview;

import java.util.Date;
/**
 * Interface to represent a command to be executed by {@link CommandExecutor}
 * @author Max Kokoulin
 */

public interface ICommand extends Runnable{


	/**
	 * @return priority of command.
	 */
	public int getPriority();

	/**
	 * @return returns starting time of command execution.
	 */
	public Date getStartTime();

	/**
	 * @return creation time of command.
	 */
	public Date getCreationTime();
	
}
