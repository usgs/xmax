package com.isti.traceview;

import java.util.LinkedList;

/**
 * Abstract class to represent a command to be executed by {@link CommandExecutor}
 * with undo possibility
 * 
 * @author Max Kokoulin
 */
public abstract class AbstractUndoableCommand extends AbstractCommand implements IUndoableCommand {

	/**
	 * @see Runnable#run()
	 */
	public void run() {
		super.run();
		TraceView.setUndoEnabled(canUndo());
	}

	/**
	 * @see IUndoableCommand#undo()
	 */
	public void undo() throws RuntimeException {
		//Descended classes should call this method in their undo()

		LinkedList<ICommand> history = CommandExecutor.getInstance().getCommandHistory();
		if (history.size() > 1) {
			history.remove(this);
			ICommand prevCommand = history.getLast();
			if (prevCommand instanceof IUndoableCommand) {
				IUndoableCommand up = (IUndoableCommand) prevCommand;
				TraceView.setUndoEnabled(up.canUndo());
			} else {
				TraceView.setUndoEnabled(false);
			}
		}
		else{
			TraceView.setUndoEnabled(false);
			//throw new RuntimeException("History list is empty");
		}
	}
	
	/**
	 * @see IUndoableCommand#canUndo()
	 */
	public abstract boolean canUndo();
}
