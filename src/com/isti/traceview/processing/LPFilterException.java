package com.isti.traceview.processing;

// LowPass Filter Exception (replaces RuntimeException)
public class LPFilterException
extends Exception 
{
	private static final long serialVersionUID = 1L;

	public LPFilterException()
	{
		super();
	}

	public LPFilterException(String message)
	{
		super(message);
	}
}
