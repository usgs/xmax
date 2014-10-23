package com.isti.traceview.processing;

// HighPass Filter Exception (replaces RuntimeException)
public class HPFilterException
extends Exception 
{
	private static final long serialVersionUID = 1L;

	public HPFilterException()
	{
		super();
	}

	public HPFilterException(String message)
	{
		super(message);
	}
}
