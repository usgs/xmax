package com.isti.traceview;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

/**
 * <p>
 * Class performs initialization and tuning of internal Java ThreadPoolExecutor class. GUI classes
 * interact CommandExecutor: they create command objects and pass the to execute() method of
 * ThreadPoolExecutor. Commands will be queued and executed according to its priority in thread pool
 * environment.
 * </p>
 * <p>
 * The class is implemented as a Singleton pattern.
 * </p>
 * 
 * @author Max Kokoulin
 */
public class CommandExecutor extends ThreadPoolExecutor {
	private static Logger lg = Logger.getLogger(CommandExecutor.class);
	/*
	 * the number of threads to keep in the pool, even if they are idle
	 */
	private static final int corePoolSize = 3;

	/*
	 * the maximum number of threads to allow in the pool.
	 */
	private static final int maximumPoolSize = 10;

	/*
	 * when the number of threads is greater than the core, this is the maximum time that excess
	 * idle threads will wait for new tasks before terminating
	 */
	private static final long keepAliveTime = 10000;

	/**
	 * @uml.property name="history"
	 */
	private LinkedList<ICommand> history;

	private Obs observable = null;

	private static CommandExecutor instance = null;

	private CommandExecutor() {
		super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, new PriorityBlockingQueue());
		history = new LinkedList<ICommand>();
		observable = new Obs();
	}

	private boolean isPaused;

	private ReentrantLock pauseLock = new ReentrantLock();

	private Condition unpaused = pauseLock.newCondition();

	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		lg.debug("Executing " + r.toString());
		if (r instanceof IUndoableCommand) {
			IUndoableCommand uc = (IUndoableCommand) r;
			if (uc.canUndo()) {
				history.add(uc);
			}
		}
		pauseLock.lock();
		try {
			while (isPaused)
				unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}

	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		// notify observers that all tasks were executed and rest nothing
		if (getQueue().size() == 0) {
			observable.setChanged();
			notifyObservers();
		}
	}

	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
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
	public static CommandExecutor getInstance() {
		if (instance == null) {
			instance = new CommandExecutor();
		}
		return instance;
	}

	// From Observable
	public void addObserver(Observer o) {
		lg.debug("CommandExecutor: adding observer");
		observable.addObserver(o);
	}

	public int countObservers() {
		return observable.countObservers();
	}

	public void deleteObserver(Observer o) {
		observable.deleteObserver(o);
	}

	public void notifyObservers() {
		lg.debug("CommandExecutor: notify observers");
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
