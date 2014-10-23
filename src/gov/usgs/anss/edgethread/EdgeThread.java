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
/*
 * EdgeThread.java
 *
 * Created on June 16, 2005, 3:15 PM

 */

package gov.usgs.anss.edgethread;
//import gov.usgs.anss.edgemom.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.GregorianCalendar;
import gov.usgs.anss.util.*;
/** 
 *
 * This sets up the required /commonn stuff for a EdgeMom controlled Thread class.
 * It provides the common stuff like output, annotation, and control needed by EdgeMom.
 * Any subclass should set logname if it wants indiviual logging files for this thread,
 * if logfile is null, then its logging will be merged with the EdgeMom log preceded 
 * the tag.  Each thread shoud then use prt() or prta() to record any output.
 * <p>
 * 1) Each Subclass of this must have a constructor(String line, String tg) where line
 * is parsed for command line arguments, and then call super(line,tg) 1st thing.  This sets
 * up the command elements for the EdgeThread (sets the tag, opens log files after > or >>).
 * Don't forget to strip the > or >> section if your code will gag on it.
 *<p>
 * 2) Each subclass should set running=true when its run method is started and set
 *  it false if it exits.
 *<p>
 * 3) Each subclass should terminate its main thread if terminate is found true,
 *   it should set terminate false at completion of the thread.
 *<p>
 * 4)  Create all abstract methods specified below :
 *    getConsoleOutput() (any output not written via the prt() or prta() function.
 *    Normally this is stdout or stderr for real subprocesses created using Subprocess
 *    class.  This output will be sent to the appropriate log (thread log or
 *    edgemom log) depending on the logname setting. By doConsoleLog().
 *<p>
 *    terminate() this will start a termination of the thread. At a minimum the terminate
 *    variable in the base class should be set true, and then set false when the thread
 *    exits.  This gives a change to the user to cause blocked threads to terminate 
 *    quickly and gracefully especially by closing I/O channels, calling "interrupt()"
 *    to get out of sleeps and waits, etc.  Care should be taken that a thread that
 *    has this called will quit (preferably soon, but certainly surely!).
 *<p>
 *    getStatusString() return something to display on the status lines by process
 *<p>
 * 5) after calling super() the thread can add stuff to its "tag".  InitThread()
 *   will make a tag like "uniqueTag[Thread-nn]" typically add a "type" on as a two
 *   character string +":".  So for LISS client tag += "LC:"
 *<p>
 * 6) It is important that the thread catch RuntimeException and cause the thread to exit
 *    as if its "terminate" flag was set.  The EdgeMom thread should then be able to 
 *    restart the thread and processing will continue *
 * @author davidketchum
 */
abstract public class EdgeThread extends Thread {
  public boolean terminate;
  public boolean running;
  public String tag;
  public PrintStream out;
  static TestStream staticout;
  static long staticlastday;
  static boolean useConsole;
  static String  staticlogname="edgemom";
  String logname;
  long lastday;                 // track days for log file opens
  public static void setUseConsole(boolean t) {useConsole=t;
  Util.prt("UseConsole is "+t);}
  public abstract void terminate();          // starts a termination on the thread
  public abstract String getConsoleOutput();        // must return any output from the thread
  public abstract String getStatusString();         // Some sort of status on the thread
  public abstract String getMonitorString();          // Key value pairs to return to a monitor process like Nagios
  public  boolean getTerminate() {return terminate;}    // returns the terminate variable
  public boolean isRunning() {return running;}
  public String getTag() {return tag;}
  protected void setPrintStream(PrintStream o) {out=o;}
  public PrintStream getPrintStream() { if(out == null) return System.out; else return out;}
  public long getBytesIn() {return -1;}
  public long getBytesOut() {return -1;}
  public void closeLog() {
    if(staticout != null)staticout.close();
    if(out != null) out.close();
  }
  public static void setMainLogname(String s) {staticlogname=s;}
  /** print a string with the time prepended.  Ends up in the file based on thread creation 
   *@param s The string to output.*/
  public static void staticprta(String s) {
    staticprt(Util.asctime()+" "+s); 
  }
  /** Oupput a string.  This also causes new log files to be crated as days roll
   * over based on the julian day of the system time.  The output unit is chosen based
   * on the redirect .
   *@param s The string to print
   */
  public static void staticprt(String s) {
    if(!useConsole) {
      if((staticout == null || staticlastday != System.currentTimeMillis()/86400000L )) {
        if(staticout != null) {
          staticout.println(Util.ascdate()+" "+Util.asctime()+" Closing day file.");
          staticout.close();
        }
        staticlastday = System.currentTimeMillis()/86400000L;
        boolean append=false;
        if(staticout == null) append=true;
        staticout = new TestStream((Util.getProperty("logfilepath") == null? "":Util.getProperty("logfilepath"))+
          staticlogname+".log"+EdgeThread.EdgeThreadDigit(),  append);// append if out is null
        TestStream.setNoConsole(!useConsole);
        TestStream.setNoInteractive(true);
        Util.setOutput(staticout);    // this forces err/std exceptions to the file
        staticout.println("\n"+Util.ascdate()+" "+Util.asctime()+
          " %%%% Opening day file :"+Util.getProperty("logfilepath")+
          staticlogname+".log"+EdgeThread.EdgeThreadDigit()+" append="+append);
      }
    }
    Util.prt(s);
  }
  /** process the initialization for the EdgeThread.  Set the tag and open the
   * log file if the command line has ">" or ">>" on it.
   *@param line The command line parameters (parse for > or >> to set log name)
   *@param tg The unique tag which will appear on any lines header for EdgeMom.prt()
   */
  public EdgeThread(String line, String tg) { 
    if(tg.equals("") || useConsole) {out=System.out;}
    if(tg.length() > 4) tag=tg;
    else tag=(tg+"    ").substring(0,4);
    StringTokenizer tk = new StringTokenizer(getName(), "-");
    if(tk.countTokens() >=2) {
      tk.nextToken();
      tag = tag +"["+tk.nextToken()+"]";
    } else tag=tag+"["+getName()+"]";
    //prt("EdgeThread set tag to "+tag);
    line=line.replaceAll("  "," ");
    line=line.replaceAll("  "," ");
    tk = new StringTokenizer(line,">");
    if(tk.countTokens() >= 2) {
      tk.nextToken();         // skip the parameters
      logname=tk.nextToken();
      // if its >> then use append mode and get name
      boolean append=false;
      if(line.indexOf(">>") >= 0) append=true;
      logname=logname.trim();           // no leading or trailing whitespace please!
      try {
        lastday = System.currentTimeMillis()/86400000L;
        
        out = new PrintStream(
            new FileOutputStream(
            Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit(), append));
        prta("Open log file "+ Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit()+" append="+append);
      }
      catch (FileNotFoundException e) {
        Util.IOErrorPrint(e,"Cannot open log file "+
            Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit());
        out = System.out;           // emergency, send it to standard out
      }
    }
    if(tg.indexOf("TEST") < 0) Util.prta("new ThreadEdge "+getName()+" is "+tg+":"+getClass().getSimpleName()+"log="+logname+" args="+line);


  }
  public void doConsoleLog() {
    String s2 = getConsoleOutput();
    String s;
    try {
      if(!s2.equals("")) {
        BufferedReader br= new BufferedReader(new StringReader(s2));
        while ( (s=br.readLine()) != null) {
          prta(s);
        }
      }
    }
    catch(IOException e) {
      prt(tag+" cannot read console input into lines.<"+tag+">"+s2+"</"+tag+">");
    }
  }
  /** print the string on 1) My local output file if defined, 2) The EdgeMom log
   * with tag privix if not
   *@param s The string to print
   */
  public void prt(String s) {
    if(!useConsole) {
      // If we have a log name, but it is not open, or its a new day - open it
      if(logname != null && (out == null || lastday != System.currentTimeMillis()/86400000L) &&
          out != System.out){
        if(out != null) out.close();
        lastday = System.currentTimeMillis()/86400000L;
        try {
          out = new PrintStream(
              new FileOutputStream(
              Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit()));
        }
        catch (FileNotFoundException e) {
          Util.IOErrorPrint(e,"Cannot open log file "+
              Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit());
          out = System.out;           // emergency, send it to standard out
        }
      }
    }
    if(out != null) out.println(s);
    else staticprt(tag+" "+s); 
  }
  public void prta(String s) {
    prt(Util.asctime()+" "+s);
  }
  public void setNewLogName(String s) {
    logname=s; 
   staticprt("Changing log file to "+Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit());
   if(!useConsole) {
      if(out != null) out.close();
      lastday = System.currentTimeMillis()/86400000L;
      try {
        out = new PrintStream(
            new FileOutputStream(
            Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit()), true);
      }
      catch (FileNotFoundException e) {
        Util.IOErrorPrint(e,"Cannot open log file "+
            Util.getProperty("logfilepath")+logname+".log"+EdgeThreadDigit());
        out = System.out;           // emergency, send it to standard out
      }
   }
 }
  public static String EdgeThreadDigit() {
    int doy = SeedUtil.doy_from_ymd( SeedUtil.fromJulian(SeedUtil.toJulian(new GregorianCalendar())));
    return Character.toString((char) ('0'+(doy%10))); 
  } 
}
