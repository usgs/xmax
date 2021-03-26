package com.isti.traceview;

import java.util.LinkedList;
import org.apache.log4j.Logger;

/**
 * <p>
 * Class handles action command history. This will replace
 * CommandExecutor.java and the need for external threads for simple
 * button actions.
 * </p>
 *
 * @author Alejandro Gonzales
 */
public class CommandHandler {
	private static final Logger logger = Logger.getLogger(CommandHandler.class);

	/*
	 * history list of previous and current commands
	 */
	private static LinkedList<ICommand> history = null;


	private static CommandHandler instance = null; 

	private CommandHandler() {
		history = new LinkedList<>();
	}

	/*
	 * Intializer for Runnable class
	 */
	public void beforeExecute(Runnable r) {
		if (r instanceof IUndoableCommand) {
			IUndoableCommand uc = (IUndoableCommand) r;
			if (uc.canUndo()) {
				history.add(uc);
			}
		}
	}

	/**
	 */
	public LinkedList<ICommand> getCommandHistory() {
		return history;
	}

	public void clearCommandHistory() {
		history.clear();
	}

	/**
	 */
	public static CommandHandler getInstance() {
		if (instance == null) {
			instance = new CommandHandler();
		}
		return instance;
	}
}
