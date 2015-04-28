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
	public void undo() throws UndoException;
	
	/**
	 * @return flag this command can be undone
	 */
	public boolean canUndo();


}
