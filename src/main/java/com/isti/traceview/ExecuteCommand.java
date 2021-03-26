package com.isti.traceview;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 * Customized class for executing commands
 */
public class ExecuteCommand {
	private static final Logger logger = Logger.getLogger(ExecuteCommand.class);

	private ExecutorService executor = null;
	private CommandHandler handler = null;
	private Runnable command = null;

	public ExecuteCommand(Runnable command) {
		this.executor = Executors.newSingleThreadExecutor();
		this.handler = CommandHandler.getInstance();
		this.command = command;	
	}

	/**
	 * Initializes CommandHandler for Runnable class
	 */
	public void initialize() {
		handler.beforeExecute(command);	
	}

	/**
	 * Execute runnable task
	 */
	public void start() {
		executor.execute(command);
	}

	/**
	 * Shutdown ExecutorService and notify
	 */
	public void shutdown() {
		try {
			executor.shutdown();	// now new threads submitted
			executor.awaitTermination(10, TimeUnit.SECONDS);
			if (!executor.isTerminated())
				executor.shutdownNow();
			// handler.afterExecute(); // this called unused code related to the observer interface
		} catch (InterruptedException e) {
			logger.error("InterruptedException:", e);	
		}
	}
}
