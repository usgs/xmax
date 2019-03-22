package com.isti.traceview;

import java.util.Date;
/**
 * Interface to represent a command to be executed by {@link CommandHandler}
 * 
 * @author Max Kokoulin
 */

public interface ICommand extends Runnable {


	/**
	 * @return priority of command.
	 */
  int getPriority();

	/**
	 * @return returns starting time of command execution.
	 */
  Date getStartTime();

	/**
	 * @return creation time of command.
	 */
  Date getCreationTime();
	
}
