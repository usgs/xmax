package com.isti.traceview;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
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
		System.out.println("ExecuteCommand( " + command.getClass() + " )");	
		this.executor = Executors.newSingleThreadExecutor();
		this.handler = CommandHandler.getInstance();
		this.command = command;	
	}

	/**
	 * Initializes CommandHandler for Runnable class
	 */
	public void initialize() {
		System.out.println("ExecuteCommand().intialize() --> Handler.beforeExecute()");	
		handler.beforeExecute(command);	
	}

	/**
	 * Execute runnable task
	 */
	public void start() {
		System.out.println("ExecuteCommand().start() --> Execute()");	
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
			System.out.println("ExecuteCommand.shutdown() --> Handler.afterExecute()");	
			handler.afterExecute();
			System.out.println("ExecuteCommand.shutdown(): COMPLETE!!!\n");	
		} catch (InterruptedException e) {
			logger.error("InterruptedException:", e);	
		}
	}
}
