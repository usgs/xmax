package edu.sc.seis.seisFile.segd;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileInputStreamPositioned extends BufferedInputStream {
	private long position = 0;

	public FileInputStreamPositioned(InputStream in){
		super(in);
		position = 0;
	}
	public FileInputStreamPositioned(InputStream in, int size){
		super(in, size);
		position = 0;
	}
	public void close() throws IOException {
		super.close();
		position = 0;
	}

    public int 	read() throws IOException {
    	int ret = super.read();
    	if(ret != -1){
    		position = position+1;
    	}
    	return ret;
    }

    public int 	read(byte[] b) throws IOException {
    	int ret = super.read(b);
    	if(ret != -1){
    		position = position+ret;
    	}
    	return ret;
    }
 
    public int read(byte[] b, int off, int len) throws IOException {
    	int ret = super.read(b, off, len);
    	if(ret != -1){
    		position = position+ret;
    	}
    	return ret;
    }
    
    public long skip(long n) throws IOException {
    	long ret = super.skip(n);
    	position = position+ret;
    	return ret;
    }
    
    public long getPosition(){
    	return position;
    }
}
