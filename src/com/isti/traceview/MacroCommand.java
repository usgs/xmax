package com.isti.traceview;

import java.util.ArrayList;

/**
 * <p>
 * Implements {@link IUndoableCommand}, but it does not contain its own code. MacroCommand contains
 * list of commands which are executed sequentially one after another.
 * </p>
 * <p>
 * It implements "Comparable" to define a default sorting order of commands in the queue. It does
 * not have properties since they are computed from included command list.
 * </p>
 * 
 * @author Max Kokoulin
 */

public class MacroCommand extends AbstractUndoableCommand implements IUndoableCommand, Comparable {

	/**
	 * @uml.property name="commands"
	 */
	private ArrayList<IUndoableCommand> commands = new ArrayList();

	public void undo() {
		// TODO Auto-generated method stub

	}

	public boolean canUndo() {
		return true;
	}

	public void run() {
		// TODO Auto-generated method stub

	}

	/**
	 * Getter of the property <tt>commands</tt>
	 * 
	 * @return Returns the list of commands.
	 */
	public ArrayList<IUndoableCommand> getCommands() {
		return commands;
	}

	/**
	 * Setter of the property <tt>commands</tt>
	 * 
	 * @param commands
	 *            The commands to set.
	 */
	public void setCommands(ArrayList<IUndoableCommand> commands) {
		this.commands = commands;
	}
}
