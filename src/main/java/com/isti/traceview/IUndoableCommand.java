package com.isti.traceview;

/**
 * Interface to represent a command to be executed by {@link CommandHandler}
 * with undo possibility
 * 
 * @author Max Kokoulin
 */

public interface IUndoableCommand extends ICommand {

	/**
	 * Undo this command and restore state before it's execution
	 */
  void undo();
	
	/**
	 * @return flag this command can be undone
	 */
  boolean canUndo();


}
