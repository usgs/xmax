package com.isti.traceview.processing;

// BandPass Filter Exception (replaces RuntimeException)
public class BPFilterException
extends Exception 
{
	private static final long serialVersionUID = 1L;

	public BPFilterException()
	{
		super();
	}

	public BPFilterException(String message)
	{
		super(message);
	}
}
