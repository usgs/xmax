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

import javax.swing.JOptionPane;
/**
* Simple utility for testing program output. Intercepts
* System.out to print both to the console and a buffer.
* From 'Thinking in Java, 3rd ed.' (c) Bruce Eckel 2002
 *
 *Adapted by D.Ketchum to simultaneously write the SESSION.OUT file and to
 *put output on standard out.  Added ring buffer of output which can be checked
 *for errors and then trigger dialog box to send the ring buffer of output
 *by mail to D. Ketchum
 *
 */
public class TestStream extends PrintStream {
  static boolean exceptionOccurred=false;
  static boolean noConsole;
  static boolean noInteractive;
  static PrintStream console=System.out;    // save original output (console)
  static PrintStream err = System.err;
  protected int numOfLines;
  private static StringBuffer sb;
  private PrintStream fout;
  // To store lines sent to System.out or err
  private String className;
  public void setConsole() {
    fout=console;
  }

  public TestStream(String className) {
    
    super(System.out, true); // Autoflush
    //System.out.println("About to change SYstem.out");
    System.setOut(this);
    System.setErr(this);
    sb = new StringBuffer(20000);
    noConsole=false;
    noInteractive=false;
    this.className = className;

    openOutputFile(false);
  }
  public TestStream(String className, boolean append) {
    super(System.out, true); // Autoflush
    System.out.println("About to change out/err to "+className.toString()+" with append="+append);
    System.setOut(this);
    System.setErr(this);
    sb = new StringBuffer(20000);
    noConsole=false;
    noInteractive=false;
    this.className = className;
    openOutputFile(append);
  }
  /** when set true, no output goes to console print stream and no dialog boxes are put up
   * for mailing when an excption is detected
   *@param t If true, ture n off console print stream and disable exception based mail via dialog
   */
  public static void setNoConsole(boolean t) {noConsole=t;}
  /** when set true, this make the "append" function which keeps the last 10000 characters
   * available will not do dialog boxes if SQLException or EventQueueExceptoinHander are found.
   * It also disables dialog boxes which would be checked if noConsole was false.
   *@param t if true, set noInteractive mode
   */
  public static void setNoInteractive(boolean t) {noInteractive=t;}
  public void suppressFile() {fout=null;}
  // public PrintStream getConsole() { return console; }
  public synchronized void dispose() {
    System.setOut(console);
    System.setErr(err);
  }
  // This will write over an old Output.txt file:
  public void openOutputFile(boolean append) {
    try {
      fout = new PrintStream(new FileOutputStream(
        new File(className), append));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  /** Add this string to the buffer and check for appearance of magic strings
   *which cause the exceptionOccurred flag to be set and a timed dialog to be
   *triggered off.
   */
  boolean ignoreSQLDialog;
  public void setIgnoreSQLDialog(boolean t) {ignoreSQLDialog=t;}
  private synchronized void append(String s){
      sb.append(s);
      if(s.indexOf("EventQueueExceptionHandler") >= 0) {
        exceptionOccurred=true;
        TimedDialog tm = new TimedDialog(sb,"EventQueueException",1000);
      }
      else if(s.indexOf("SeedLinkException") >=0) {
        if(s.indexOf("failed to decode") >=0 && s.indexOf("NumberFormatExc") >= 0) return;    // These are not worth the e-mail
        exceptionOccurred=true;
        TimedDialog tm = new TimedDialog(sb,"SeedLinkException ",3000);
      }
      else if(s.indexOf("SQLException") >=0) {
        int i = s.indexOf("e=");
        String extra="";
        if(i > 0) extra=s.substring(0,i-2);
        else extra="";
        exceptionOccurred=true;
        if(!ignoreSQLDialog) {
          TimedDialog tm = new TimedDialog(sb,"SQLException "+extra,3000);
        }
      }
      else if(s.indexOf("SeedLinkException") >= 0) { 
        exceptionOccurred=true;
        int pos = s.indexOf("SeedLinkException");
        pos = s.indexOf("[", pos);
        String extra="Unknown SeedlinkException";
        if(pos >=0) {
          int end = s.indexOf("\n", pos);
          if(end >=0 && end > pos) extra=s.substring(pos,end);
        }
        TimedDialog tm = new TimedDialog(sb,"SeedLinkException "+extra, 3000);
      }
      else if(s.indexOf("RuntimeException") >=0) {
        exceptionOccurred=true;
        int i = s.indexOf("e=");
        String extra="";
        if(i > 0) extra=s.substring(0,i-2);
        else extra="";
        TimedDialog tm = new TimedDialog(sb,"RuntimeException "+extra,3000);
      }
      else if(s.indexOf("duplicate entry") > 0) {
        exceptionOccurred=true;
        int i= s.indexOf("entry Duplicate entry");
        String extra = "";
        if(i > 0) extra=s.substring(i+20);
        TimedDialog tm = new TimedDialog(sb,"DuplicateException"+extra, 3000);
      }
      else if(s.indexOf("Exception") >= 0) {
        exceptionOccurred=true;
        int i = s.indexOf("e=");
        String extra="";
        if(i > 0) extra=s.substring(0,i-2);
        else extra="";
        TimedDialog tm = new TimedDialog(sb,"Exception "+extra, 3000);
      }
    if(sb.length() > 10000) sb.delete(0,sb.length()-10000);
  }
  public static void setExceptionOccurred(boolean b) {exceptionOccurred=true;}
  public static boolean getExceptionOccurred(){ return exceptionOccurred;}
  public static StringBuffer getText() {return sb;}
  
  // Override all possible print/println methods to send
  // intercepted console output to both the console and
  // the Output.txt file:
  @Override
  public synchronized void print(boolean x) {
    append(String.valueOf(x));
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(boolean x) {
    append(String.valueOf(x)+"\n");
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void print(char x) {
    append(String.valueOf(x));
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(char x) {
    append(String.valueOf(x)+"\n");
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void print(int x) {
    append(String.valueOf(x));
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(int x) {
    append(String.valueOf(x)+"\n");
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void print(long x) {
    append(String.valueOf(x));
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(long x) {
    append(String.valueOf(x)+"\n");
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void print(float x) {
    append(String.valueOf(x));
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(float x) {
    append(String.valueOf(x)+"\n");
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void print(double x) {
    append(String.valueOf(x));
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(double x) {
    append(String.valueOf(x)+"\n");
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void print(char[] x) {
    append(new String(x));
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(char[] x) {
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void print(String x) {
    append(x);
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(String x) {
    append(x+"\n");
    numOfLines++;
    if(!noConsole) {
      console.println(x);
      console.flush();
    }
    if(fout != null) 
      fout.println(x);
  }
  @Override
  public synchronized void print(Object x) {
    append(x.toString());
    if(!noConsole) console.print(x);
    if(fout != null) fout.print(x);
  }
  @Override
  public synchronized void println(Object x) {
    append(x.toString()+"\n");
    numOfLines++;
    if(!noConsole) console.println(x);
    if(fout != null) fout.println(x);
  }
  @Override
  public synchronized void println() {
    append("\n");
    if(false) if(!noConsole) console.print("println");
    numOfLines++;
    if(!noConsole) console.println();
    if(fout != null) fout.println();
  }
  @Override
  public synchronized void write(byte[] buffer, int offset, int length) {
    
    for(int i=offset; i<offset+length; i++) append(String.valueOf(buffer[i]));
    if(!noConsole) console.write(buffer, offset, length);
    fout.write(buffer, offset, length);
  }
  @Override
  public synchronized void write(int b) {
    if(!noConsole) console.write(b);
    fout.write(b);
  }
  
  /** This class is instantiated to cause a slight pause before a dialog box is
   *displayed.  If he user answers yes, the ring buffer of output is sent to dave
   */
  static int numberHeadless=0;
  static int numberExceptions=0;
  static long lastException;
  static int skippedExceptions=0;
  class TimedDialog extends Thread {
    int timeout;
    String title;
    StringBuffer sb;
    public TimedDialog(StringBuffer sbin, String titlein, int ms) {
      sb=sbin;
      title=titlein;
      timeout=ms;
      //System.out.println("TimedDialogLaunch title="+titlein+" sb="+sbin+" noInteractive="+noInteractive+" noconsole="+noConsole);
      if(noConsole || noInteractive) return;
      start();
    }
    @Override
    public void run() {
      try {sleep(timeout);} catch (InterruptedException e) {}
      int count=8;
      int i=0;
      int start=sb.indexOf(title);
      if(start > 0) {
        for(i=start;i<sb.length(); i++) {
          if(sb.charAt(i) == '\n') count--;
          if(count == 0) {start=i;break;}
        }
      } else start=sb.length();
      count=20;
      for(i=start-1; i> 0; i--) {
         if(sb.charAt(i) == '\n') count--; 
         if(count == 0) break;
      }
      
      int ans =0;
      // If this is a interactive application, show a dialog and let the user decide
      try {
        if(!noConsole  && !noInteractive) ans=JOptionPane.showConfirmDialog(null,
        sb.substring(i,start)+"\n\n       *******Mail this to Dave Ketchum? ******", 
        title, JOptionPane.YES_NO_OPTION);
      } 
      catch(RuntimeException e ) {
        Util.prt("Runtime exception in TestStreamd e="+e);
        if(e.toString().indexOf("HeadlessException") >=0) {
          numberHeadless++;
          Util.prt("Got a HeadlessException in "+Util.getProcess()+" setting noInteractive "+numberHeadless);
          noInteractive=true;
          e.printStackTrace();
        }
        else {
          e.printStackTrace();
        }
        if(numberHeadless %100 == 0) System.exit(1);
        return;
      }
      catch(Exception e) {
        Util.prt("Exception in TestStream e="+e);
        if(e.toString().indexOf("HeadlessException") >=0 ) {
          numberHeadless++;
          Util.prt("Got a HeadlessException2 in "+Util.getProcess()+" setting noInteractive "+numberHeadless);
          noInteractive=true;
          e.printStackTrace();
        }
        else {
          e.printStackTrace();
        }
        if(numberHeadless % 100 == 0) System.exit(1);
        return;
      }
      // if not interactive or user said yes, send e-mail.
      if(ans == 0 || ans == JOptionPane.YES_OPTION) {
        numberExceptions++;
        if(numberExceptions > 100) {
          if(System.currentTimeMillis() - lastException < 10000) {
            skippedExceptions++;
            Util.prt("Skip Excpt ="+skippedExceptions+" of "+numberExceptions+" "+
                Util.getNode()+" "+User.getUser()+" "+title+"\n"+
                Util.getProcess()+" "+System.getProperty("user.home")+" "+System.getProperty("user.dir")+" "+Util.getNode()+
                " "+Util.ascdate()+" "+Util.asctime()+"\n"+sb.toString());
            return;
          }
          else  title="STORM "+title;
        }
        if(System.currentTimeMillis() - lastException > 600000) numberExceptions=0; // rearm the storm delayer
        lastException = System.currentTimeMillis();
        SimpleSMTPThread.email(Util.getProperty("emailTo"),
            "_Exception:"+User.getUser()+" "+title,                 // subject
            Util.ascdate()+" "+Util.asctime()+" "+User.getUser()+" "+Util.getIDText()+
                "\n"+sb.toString()+"\n");
      }
    }
  }
}