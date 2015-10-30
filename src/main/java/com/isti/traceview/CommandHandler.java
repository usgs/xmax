package com.isti.traceview;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

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

	private static Obs observable = null;

	private static CommandHandler instance = null; 

	private CommandHandler() {
		history = new LinkedList<ICommand>();
		observable = new Obs();
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

	/* 
	 * Alert observers after execution
	 */
	public void afterExecute() {
		observable.setChanged();
		notifyObservers();
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

	// From Observable
	public void addObserver(Observer o) {
		logger.debug("Adding observer");
		observable.addObserver(o);
	}

	public int countObservers() {
		return observable.countObservers();
	}

	public void deleteObserver(Observer o) {
		observable.deleteObserver(o);
	}

	public void notifyObservers() {
		logger.debug("Notify observers");
		observable.notifyObservers();
		observable.clearChanged();
	}

	public void notifyObservers(Object arg) {
		observable.notifyObservers(arg);
		observable.clearChanged();
	}

	class Obs extends Observable {
		public void setChanged() {
			super.setChanged();
		}

		public void clearChanged() {
			super.clearChanged();
		}
	}
}
