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
	private static final Logger logger = Logger.getLogger(CommandExecutor.class);
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
			
		super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>());
		history = new LinkedList<ICommand>();
		observable = new Obs();
	}

	private boolean isPaused;

	private ReentrantLock pauseLock = new ReentrantLock();

	private Condition unpaused = pauseLock.newCondition();

	/*
	 * Launching threads for buttons doesn't make sense. Unless the user has
	 * the ability to submit multiple commands at once (which they can't).
	 * Consider adding methods for LinkList<ICommand> history and notifying
	 * Observers when threads have finished executing
	 *
	 * NOTE: Threads should only be used for process intensive code
	 */	
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		System.out.println("CommandExecutor.beforeExecute():"); 
		if (r instanceof IUndoableCommand) {
			IUndoableCommand uc = (IUndoableCommand) r;
			if (uc.canUndo()) {
				System.out.println("history.add[ " + uc.toString() + " ]");	
				history.add(uc);
			}
		}
		
		pauseLock.lock();
		try {
			long start = System.nanoTime();
			while (isPaused)
				unpaused.await();
			long endl = System.nanoTime() - start;
			double end = endl * Math.pow(10, -9);
			System.out.format("CommandExecutor: unpaused.await() execution time = %.9f sec\n", end);
		} catch (InterruptedException ie) {
			t.interrupt();
			logger.error("InterruptedException:", ie);	
		} finally {
			System.out.println("CommandExecutor: pauseLock.unlock()\n");
			pauseLock.unlock();
		}
	}

	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		// notify observers that all tasks were executed and rest nothing
		if (getQueue().size() == 0) {
			System.out.println("CommandExecutor.afterExecute(): observable.setChanged(), notifyObservers()\n");	
			observable.setChanged();
			notifyObservers();
		}
	}

	public void finalize() {
		super.finalize();
		System.out.println("CommandExecutor.finalize() --> super.shutdown()");	
		try {	// try to terminate and force shutdown	
			super.awaitTermination(10, TimeUnit.SECONDS);	
			if (!super.isTerminated())
				super.shutdownNow();
			System.out.println("CommandExecutor.getCompletedTaskCount() = " + 
				super.getCompletedTaskCount());
			System.out.println("CommandExecutor.shutdown(): COMPLETE!\n");	
		} catch (InterruptedException e) {
			//logger.error	
			System.out.println("CommandExecutor: InterruptedException:" + e);
		}	
	}

	public void pause() {
		System.out.println("CommandExecutor.pause(): pauseLock.lock()\n");
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
		/** NOTE: Need a way to determine if pool is TERMINATED, if
		 * 	  TRUE, then create new POOL of WORKERS
		 **/
		if (instance == null) {
			System.out.println("CommandExecutor.getInstance: instance = NULL\n");	
			instance = new CommandExecutor();
		} else {
			System.out.println("CommandExecutor.getInstance: instance = " + 
				instance + "\n");
			if (instance.isTerminated()) {
				System.out.println("CommandExecutor: POOL == TERMINATED!!\n");	
			}
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
