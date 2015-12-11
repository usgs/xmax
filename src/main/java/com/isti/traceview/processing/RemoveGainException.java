package com.isti.traceview.processing;

// Remove Gain Exception (replaces RuntimeException)
public class RemoveGainException extends Exception {
	private static final long serialVersionUID = 1L;

	public RemoveGainException() {
		super();
	}

	public RemoveGainException(String message) {
		super(message);
	}
}
