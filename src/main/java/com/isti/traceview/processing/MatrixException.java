package com.isti.traceview.processing;

// Special exception for matrix arithmetic 
public class MatrixException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public MatrixException() {
		super();
	}

	public MatrixException(String str) {
		super(str);
	}
}
