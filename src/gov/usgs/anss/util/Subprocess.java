/*
 * Copyright 2010, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package gov.usgs.anss.util;
import java.io.*;
//import java.lang.Thread;
/*
 * Subprocess.java 
 *
 * Created on December 1, 2004, 12:21 PM
 */

/** This class really wraps the Process class providing simple err and output
 * processing.  It excecutes a command using the Runtime.getRuntime().exec() 
 * function as a subprocess which could still be running after constuction.
 * It facilitates getting the returned stderr and stdout as strings and allows
 * for a waitFor().  It mainly is used to hide the details of getting the err and
 * output from the users and the details of process control.  It prevents OS 
 * dependent lock ups if stdout and stderr have lots of output and need to be
 * releived.  This Class extends thread and the run() method is emptying the
 * stderr and stdout every 0.1 seconds and exiting when it discovers the subprocess
 * has exited.  The waitFor() not only waits for the subprocess to exit, but insures
 * the run() has exited insuring the stdout and stdin buffers are complete before
 * returning.
 *
 * @author  davidketchum
 */
public class Subprocess extends Thread  {
  InputStreamReader err;       // the error output
  //InputStreamReader out;        // the stdout
  BufferedReader out;        // the stdout
  final StringBuffer errText= new StringBuffer(200);       // Build the error output here
  final StringBuffer outText = new StringBuffer(1000);       // Build the stdout here
  Process proc;               // The subprocess object
  String cmd;
  char [] c ;
  static boolean dbg;
  boolean terminate;
  boolean blockInput;
  public static void setDebug(boolean t) {dbg=t;}
  public void terminate() {terminate=true;}
  public boolean getTerminate() {return terminate;}
  /** Creates a new instance of Subprocess 
   @param cin A command string to process
   *@throws IOException If reading stderr or stdout gives an error
   */
  public Subprocess(String cin) throws IOException {
    cmd=cin;
    terminate=false;
    blockInput=false;
    proc = Runtime.getRuntime().exec(cmd);
    err = /*new BufferedReader(*/new InputStreamReader(proc.getErrorStream());
    out = new BufferedReader(new InputStreamReader(proc.getInputStream()));

    c = new char[100];
    start();
    
  }

  /** This is implemented as a Thread so that it will keep reading any stderr or
   * stdout input into the StringBuffers so that they cannot block the operating
   * system from running the subprocess.  Read any input each 1/10th second and
   * exits when the subprocess has exited.
   */
  @Override
  public void run() {
    
    int val=-99;
    long loop=0;
    while(val == -99) {
      if(terminate) break;
      getInput();
      /* it is possible but unlikely that input comes in after being emptied above
       *but then finding that the subprocess has exited.
       */
      if(loop ++ % 2 == 0) val=exitValue();
      //Util.prta("Run out="+outText.toString()+"\nRun err="+errText.toString());
      try{sleep(100L);} catch(InterruptedException e2) {}
    }
    getInput();
    terminate=false;
  }
  private void getInput() {
    int l;
    if(blockInput) return;    // user is reading it himself
    try {
      l=1;
      while( l > 0) {
        if(out.ready()) l=out.read(c);    // if output is ready, get it, if not go on
        else l=0;
        if(l > 0) {
          synchronized(outText) {outText.append(c,0,l); } // append up to 100 chars to outText
        }
        if(dbg) Util.prta("getinput l="+l);
      }
      l=1;                                // Read in all stderr input
      while( l > 0) {
        if(err.ready()) l=err.read(c);    // read upto 100 chars
        else l=0;
        if(l>0) {
          synchronized(errText) {errText.append(c,0,l);}    // append it
        }
      }
    }
    catch (IOException e) {
      Util.IOErrorPrint(e,"IOerror on subprocess:"+cmd);
    }

  }
  /** return the link to the stdout from this process
   *@return the InputStream which gets the output
   */
  public InputStream getOutputStream() {
    blockInput=true;
    return proc.getInputStream();
  }
  /** Wait for this subprocess to exit.  This suspends the current thread until
   * the subprocess is done so be careful that the subprocess does not dead lock
   * or run continuously.
   *@return Exit value of process, zero is success.
   *@throws InterruptedException Means wait did not complete but was interrupted
   */
  public int  waitFor() throws InterruptedException {
    int i=0;
    if(dbg) Util.prt("Subprocess : waiting "+cmd);
    i = proc.waitFor();
    getInput();     // insure the buffers are complete
    return i;
  }

  /** Returns the exit status of the subprocess.  If the subprocess is still running
   * this should return -99 to indicate it is still running and so can be used to 
   * test for the process still running without blocking the thread's execution
   *@return The exit status of the subprocess or -99 if the subprocess is still running
   */
  public int exitValue() {
    try {
      return proc.exitValue();
    }
    catch (IllegalThreadStateException e) {
      return -99;
    }
  }
  /** return the output of the subprocess as a string.  If the subprocess is still
   *running, it may return some portion of the stdout to this point, but will not block.
   *@return String with the contents of stdout
   */
  public String getOutput() {
    return outText.toString();
  }
  /** return the output of the subprocess as a string.  If the subprocess is still
   *running, it may return some portion of the stdout to this point, but will not block.
   * if flag is true, clear the output buffer 
   *@param clear If true, clear the output buffer after getting the string
   *@return String with the contents of stdout
   */
  public String getOutput(boolean clear) {
    String s;
    synchronized (outText) {
      s = outText.toString();
      if(clear) outText.delete(0, outText.length());
    }
    return s;
  }
  /** return error output to date of the subprocess.  If the subprocess is still 
   * running, it may return some portion of the stderr to this point, but will not block.
   
   *@return String with contents of stderr
   
   */
  public String getErrorOutput() {//throws IOException {
    return errText.toString();
  }
  /** return error output to date of the subprocess.  If the subprocess is still 
   * running, it may return some portion of the stderr to this point, but will not block.
   * @param clear If true, clear the text field after returning it
   *@return String with contents of stderr
   
   */
  public String getErrorOutput(boolean clear) {//throws IOException {
    String s;
    synchronized(errText) {
      s = errText.toString();
      if(clear) errText.delete(0, errText.length());
    }
    return s;
  }

  /** unit test routine
   *@param args command line args
   */
  public static void main(String[] args) {
    Util.init();
    try {
      //Process p = Runtime.getRuntime().exec("ifconfig -a");
      Util.prta("Issue command");
      //Subprocess p = new Subprocess("ping -c 1 69.19.65.251");
      Subprocess p = new Subprocess("netstat -nrv");
      
      Util.prta("return from command.");

      Util.prta("Out immediate="+p.getOutput());
      Util.prta("Err immediate="+p.getErrorOutput());
      Util.prta("immediate return="+p.exitValue());
      p.waitFor();
      Util.prta("Waitfor done");
      Util.prta("Out="+p.getOutput());
      Util.prta("Err="+p.getErrorOutput());
      Util.prt("error return="+p.exitValue());      
     
    } 
    catch (IOException e) {
      Util.IOErrorPrint(e,"reading file :");
    }
    catch (InterruptedException e) {
      Util.prt("Interrupted waiting for command");
    }
  
  }
}
