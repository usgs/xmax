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

import java.net.*;
import java.io.*;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.text.DecimalFormat;
import gov.usgs.alarm.SendEvent;
/**
 * simpleSMTP.java - This class is used to send a simple mail message 
 (no attachements, etc. just subject and text body) to a SMTP server.  The
 *main thread does the sending and an inner class reads data back from the
 *smtp server.  A StringBuffer is maintained with details of the session so
 *a user could inspect or use it for debugging.  
 *
 * Created on August 20, 2003, 4:56 PM
 
 *
 * @author  ketchum
 */

public class SimpleSMTPThread extends Thread {
  public static String [] dow ={"NaN", "Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
  public static String [] months ={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
  public static DecimalFormat df2 = new DecimalFormat("00");
  public static boolean debugOverride=false;
  StringBuffer err;
  BufferedReader bin;
  String server;
  String to;
  String from;
  String subject;
  String body;
  boolean successful;
  boolean running;
  boolean dbg;
  Socket s;
  long connectTime;
  public static void setDebug(boolean t) {debugOverride=t;}
  /** Create a object and send the mail message to he server.  This creates a 
   *thread which sends the message and spawns an inner class which monitors and
   *records the responses of the SMTP server.  If subject line contains 'DBG' then
   * debugging output of the thread will be turned on.
   *@param serverin The SMTP server machine to use (port 25)
   *@param to_in The email address of the recipient
   *@param from_in The email address of the sender
   *@param subject_in The subject line
   *@param body_in The main body of the message
   */
  public SimpleSMTPThread(String serverin, String to_in, String from_in, String subject_in, String body_in) 
  { dbg=debugOverride;
    if(serverin == null || serverin.equals("")) server = Util.getProperty("SMTPServer");
    else server = serverin;
    if(subject_in.indexOf("DBG") >=0) dbg=true;
    to = to_in.trim();
    from=from_in.trim();
    subject=subject_in.trim();
    body=body_in.trim();
    running=true;
    if(dbg) Util.prt("SMTPT: serv="+server+"HostIP="+Util.getProperty("HostIP")+
            " to "+to+" from "+from+"\nsub="+subject);
    successful=false;
    err= new StringBuffer(1000);
    start();
  }
  /** this is the main body of the sending thread.  It interacts with the SMTP
   *server in the normal manner and monitors responses of the server via a GetSession
   *inner class object.  Beware per http://pobox.com/~djb/docs/smtplf.html sending linefeeds
   *without preceding carriage returns can result in rejections of mail by some servers
   *per a RFP spec.  We spent much time running this down!
   */
  @Override
  public void run() {
    
    try
    { if(err.length() > 0) err.delete(0, err.length()-1);
      if(dbg) Util.prta("SMTPT:Open socket to "+server);
      connectTime=System.currentTimeMillis();
      try {
        s = new Socket(server, 25);
      }
      catch (java.io.IOException e)
      { if(e.getMessage() != null) {
          if(e.getMessage().indexOf("Connection timed out") >=0) {
            Util.prta("SMTPT: 1st Connection timed out to server="+server+" to "+to+
                    " subj="+subject+" "+(System.currentTimeMillis() - connectTime));
            connectTime=System.currentTimeMillis();
            s = new Socket(server, 25);
          }
          else {
            Util.prta("SMTPT: IOError"+e+" server="+server+" to "+to+" subj="+subject+" "+
                    (System.currentTimeMillis() - connectTime));
            SendEvent.debugEvent("SMTPThrErr","IOError e="+e,this);
            return;
          }
        }
        else {
          Util.prt("SMTPT: null IOexception="+e.toString()+" server="+server+" to "+to+" subj="+subject);
          err.append("SimpleSMTPThread exception : "+e.toString()+" server="+server);
          SendEvent.debugEvent("SMTPThrErr","IOException e="+e, this);
          running=false;
          return;
       }
      }
      //err.append("SimpleMail :Srv="+server+" to:"+to+" frm:"+from+" subj:"+subject+"\n");
      //Util.prt(err.toString());
      // connect with the mail server
     // get the input stream from the socket and wrap it in a BufferedReader
      InputStream in = s.getInputStream();
      bin = new BufferedReader(new InputStreamReader(in));
      GetSession get = new GetSession(bin, err);   // Thread listening for input!
      

      // get the output stream and wrap it in a PrintWriter
      //PrintWriter pout = new PrintWriter(s.getOutputStream(), true);
      OutputStream pout = s.getOutputStream();


      // say Hello back
      successful=true;
      String str="";
      int i = server.indexOf('.');
      str = "HELO "+server.substring(i);
      if(Util.getProperty("HostIP") != null)
        if(Util.getProperty("HostIP").length() > 7) str="HELO "+Util.getProperty("HostIP");

      if(dbg) System.out.println("SMTPT:"+str);	// display what we're sending
      err.append("Send:"+str+"\n");
      //pout.println(str+"\r");		// send it   
      pout.write((str+"\r\n").getBytes());

      // the message header
      str = "MAIL FROM:\"SMTPThread\" <"+from.trim()+">";  // from
      if(dbg)System.out.println("SMTPT:"+str+"|");
      err.append("Send:"+str+"\n");
      //pout.print(str+"\r\f");
      pout.write((str+"\r\n").getBytes());
      if(!get.chkFor("Sender","ok")) {
        err.append("NO 'Sender ok' abort...\n"); successful=false;}
      else {
        str = "RCPT TO: <"+to+">";  // to
        if(dbg)System.out.println("SMTPT:"+str+"|");
        err.append("Send:"+str+"\n");
        //pout.println(str+"\r");
        pout.write((str+"\r\n").getBytes());
        if(!get.chkFor("Recipient","ok")) {
          err.append("NO 'Recipient ok' abort...\n"); successful=false;
          if(dbg) System.out.println("SMTPT:"+"No recipient o.k.");
        } 
        else {

          // send the DATA message
          str = "DATA";
          if(dbg) System.out.println("SMTPT:"+str);
          err.append("Send:"+str+"\n");
          //pout.println(str+"\r");
          pout.write((str+"\r\n").getBytes());
          if(!get.chkFor("354")) {
            err.append("NO 'go ahead or start input' abort...\n");  successful=false; 
            if(dbg) System.out.println("SMTPT:"+str);
          }
          else {

            // subject line
            
            //pout.println("SUBJECT:"+subject+"\r");
            pout.write(("SUBJECT:"+subject+"\r\n").getBytes());
            err.append("Send:SUBJECT:"+Util.asctime()+" "+subject+"\n");
            
            //pout.println("From:SMTPThread <ketchum@usgs.gov>\r");
            pout.write(("From:SMTPThread <ketchum@usgs.gov>\r\n").getBytes());
            GregorianCalendar now = new GregorianCalendar();
            //String d = now.getTime().toString();
            //Util.prt("now toString="+now.getTime().toString()+"day="+now.get(GregorianCalendar.DAY_OF_WEEK)+
            //" zone offset="+now.get(GregorianCalendar.ZONE_OFFSET));
            //pout.println("Date:"+d.substring(0,3)+", "+d.substring(8,10)+" "+
            //  d.substring(4,7)+" "+d.substring(24,28)+" "+d.substring(11,19)+" -0"+
            //  -now.get(GregorianCalendar.ZONE_OFFSET)/36000+"\r");
            String d = "Date:"+dow[now.get(Calendar.DAY_OF_WEEK)]+", "+df2.format(now.get(Calendar.DAY_OF_MONTH))+" "+
                months[now.get(Calendar.MONTH)]+" "+now.get(Calendar.YEAR)+" "+
                df2.format(now.get(Calendar.HOUR_OF_DAY))+":"+df2.format(now.get(Calendar.MINUTE))+":"+
                df2.format(now.get(Calendar.SECOND))+" -0"+(-now.get(GregorianCalendar.ZONE_OFFSET)/36000)+"\r\n";
            pout.write(d.getBytes());
            /*pout.write(("Date:"+d.substring(0,3)+", "+d.substring(8,10)+" "+
              d.substring(4,7)+" "+d.substring(24,28)+" "+d.substring(11,19)+" -0"+
              -now.get(GregorianCalendar.ZONE_OFFSET)/36000+"\r\n").getBytes());*/
            
            // blank line indicates beginning of the message body.
            //pout.println("\r");
            pout.write("\r\n".getBytes());
            
            // message
            //pout.print(convert(body));
            //pout.print("\r\n.\r\n");
            pout.write(convert(body).getBytes());
            pout.write("\r\n.\r\n".getBytes());
            
            err.append("Send:Body:"+body+"\n.\n");
           
            // send the QUIT message
            str = "QUIT";
            err.append("Send:"+str+"\n");
            //pout.println(str+"\r");
            pout.write((str+"\r\n").getBytes());
            if(dbg) System.out.println("SMTPT:"+str);
            //try{sleep(2000L);} catch(InterruptedException e) {}
            if(!get.chkFor("250")) {
              err.append("no 'Message Accepted' abort...\n");  successful=false;
            }
          }
        }
      }
      if(dbg || !successful) Util.prta("SMTPT:----- Error list --------\n"+err.toString());
      if(dbg || !successful) Util.prta("SMTPT:------Session --------- \n"+get.getSession());
          
      // close the connection
      get.setBin(null);
      s.close();
    }
    catch (java.io.IOException e) 
    { if(e.getMessage() != null) {
        if(e.getMessage().indexOf("Connection timed out") >=0) {
          Util.prta("SMTPT: 2nd Connection timed out to server="+server+" to "+to+
                  " subj="+subject+" "+(System.currentTimeMillis() - connectTime));
          SendEvent.debugEvent("SMTPThrErr", "Connection2 timed out to "+server, this);
        } 
        else {
          Util.prta("SMTPT: IOError="+e+" to server="+server+" to "+to+
                  " subj="+subject+" "+(System.currentTimeMillis() - connectTime));
          SendEvent.debugEvent("SMTPThrErr", "IOerror2="+e+" server="+server, this);
        }
      }
      else {
        Util.prt("SMTPT: exception2="+e.toString()+" server="+server);
        err.append("SimpleSMTPThread exception : "+e.toString()+" server="+server);
        SendEvent.debugEvent("SMTPThrErr","IOException2 e="+e, this);
      }
    }
    if(dbg) Util.prt("SMTPT: exiting");
    running=false;
    return;
  }
  private String convert(String body) {
     StringBuffer sb = new StringBuffer(body.length()+100);
     for(int i=0; i<body.length(); i++) 
       if(body.charAt(i) == '\n') sb.append("\r\n");
       else sb.append(body.charAt(i));
     return sb.toString();
  }
  /** wait for the SMTP mail session to complete.  This will sleep the caller until
   *the SMTP session has completed.  At the time of the return the session text will
   *be complete
   */
  public void waitForDone() {while(running) {try{sleep(10);} catch (InterruptedException e) {}}}
  /** Return wether the mail message appeared to go out successfully to the server
   *@return If true, the message was sent
   */
  public boolean wasSuccessful(){return successful;}
  /** Return a String with a record of the SMTP session.  Normally only used for debugging
   *the send.
   *@return The session string
   */
  public String getSendMailError() {
    return err.toString();
  }
  
  /** This inner class monitors the input side of the SMTP socket and adds any
   *text received from the server to the session log.
   */
    
  class GetSession extends Thread {
    String lastLine;
    BufferedReader bin;
    StringBuffer err;
    public String getSession() {return err.toString();}
    /** Construct a listener on the SMTP socket
    *@param bb is the input side of the socket
     *@param e The session log to append received lines to
     */
    public GetSession(BufferedReader b,StringBuffer e) {
      bin=b; err=e;
      start();
    }
    /** return the last line of input received from theSMTP server
     */
    public String getLastLine(){return lastLine;}
    /** look for a string in the last line received.  Used by the SMTP sender to
     *insure good SMTP responses to output come back (case insensitive)
     *@aparm str to look for.
     *@return true if string is found on last line
     */
    public boolean chkFor(String str) {
      //Util.prt("ChkFor="+str);
      for(int i=0; i<100; i++) {
        if(err.toString().toLowerCase().indexOf(str.toLowerCase()) < 0) {
          try {sleep(100L);} catch (InterruptedException e) {}
        } else return true;
      }   
      //Util.prt("Chkfor not found="+lastLine);
      return false;
    }
    /** look for two string on the last line (like "recipient", "ok") case insensitive.
     *@param str1 first string to look for
     *@param str2 2nd string to look for
     *@return true of the strings are both found */
    public boolean chkFor(String str1, String str2) {
      if(chkFor(str1)) 
        if(chkFor(str2)) return true;
      return false;
      
    }
    /** conveniently clear the receive buffer input which shutsdown the thread */
    public void setBin(BufferedReader b) {bin=b;}
    
    @Override
    public void run() {
      String line=null;
      while( bin != null) {
        try {line = bin.readLine();} catch (IOException e) {
          //Util.IOErrorPrint(e,"SMTPThread: IOerror");
          bin=null;
        }
        if(line == null) {
          try{sleep(10L);} catch (InterruptedException e) {}
        }
        else {
          lastLine=line;
          err.append("resp:"+line+"\n");
          //Util.prt("GetSession l="+line);
        }
      }
      //Util.prt("SMTPThread:GetSession: bin is null exit");
    }
  }
  
 public static SimpleSMTPThread email(String to, String subject, String body) {
   String t = to;
   if(t == null) {
     Util.prta(" **** SimpleSMTPThread email() to is null or blank.  Use ketchum as a backup");
     t = "ketchum@usgs.gov";
   }
   if(t.equals("")) {
     t = "ketchum@usgs.gov";
     Util.prta(" **** SimpleSMTPThread email() to is null or blank.  Use ketchum as a backup");
   }
   if(to.indexOf("@") < 0) t += "@usgs.gov";
   return new SimpleSMTPThread("",t, "ketchum@usgs.gov",subject, body);
 }
 /*public static SimpleSMTPThread pageDave(String subject, String body) {
   String killpages = Util.getProperty("KillPages");
   if(killpages != null) {
     Util.prta("SMTPT: KillPages is "+killpages);
     if(killpages.equals("true")) {
       Util.prta("SMTPT: Pagekilled subj="+subject+" body="+body);
       return email("ketchum","PAGE KILLED:"+subject, body);
     }
   }
   String override = Util.getProperty("KetchumPageOverride");
   if(override != null) {
     Util.prta("SMTPT: KetchumPageOverride="+override+" msg="+subject);
     if(override.length() > 2) {
       Util.prt("Pager override="+override+" sub="+subject+" body="+body);
       email(override,subject.substring(0,Math.min(subject.length(),100)), 
           body.substring(0,Math.min(body.length(),5)));
     }
   }
   return email("3035205840@vtext.com",subject.substring(0,Math.min(subject.length(),100)), 
       body.substring(0,Math.min(body.length(),5)));
 }*/
 /** A test for the SimpleSMTPThread class
     @param args the command line arguments
  */
  public static void main(String[] args) {
    Util.init();
    Util.loadProperties("edge.prop");
    String subject=null;
    String filename=null;
    int istart=0;
    for(int i=0; i<args.length; i++) {
      if(args[i].equals("-s")) {
        subject = args[i+1];
        istart=i+2;
      }
      else if(args[i].equals("-b")) {
        filename=args[i+1];
        istart=i+2;
      }
      else if(args[i].equals("-dbg")) SimpleSMTPThread.setDebug(true);
    }
    if(subject != null) {
      StringBuilder sb = new StringBuilder(1000);
      sb.append("BEGIN\n");
      try {
        BufferedReader in = null;
        if(filename == null) in = new BufferedReader(new InputStreamReader(System.in));
        else in = new BufferedReader(new FileReader(filename));
        String line = "";
        while( (line = in.readLine()) != null) {
          sb.append(line+"\n");
        }
        in.close();
        sb.append("END\n");
        for(int j=istart; j<args.length; j++) {
          SimpleSMTPThread thr = SimpleSMTPThread.email(args[j], subject, sb.toString());
          thr.waitForDone();
          if(thr.wasSuccessful()) {
            Util.prt("Your email was sent to "+args[j]);
          }
          else {
            Util.prt("Email failed to "+args[j]+" "+thr.getSendMailError());
          }

        }
      }
      catch(IOException e) {
        Util.IOErrorPrint(e,"Email I/O error");
      }

      System.exit(0);
    }

    Util.prt("SMTPServer="+Util.getProperty("SMTPServer"));
    /*SimpleSMTPThread ab = email("ejhaug07@att.blackberry.net","Test page to Eric","This message had the Date changed to two digit year.\n");
    ab.waitForDone();
    Util.prt("ejh="+ab.getSendMailError());*/
    
    SimpleSMTPThread a = email("3035205840@vtext.com", "DBG:Test page "+Util.asctime(),"body1");
    a.waitForDone();
    Util.prta("1st="+a.getSendMailError());
    a = email("ketchum", "DBG:subject no @usgs.gov","body1");
    a.waitForDone();
    Util.prta("1st="+a.getSendMailError());
    a = email("ketchum@usgs.gov","DBG:subject with usgs.gov "+Util.asctime(),"body2");
    a.waitForDone();
    Util.prta("2ND="+a.getSendMailError());
    //a = pageDave("Test Paging subject "+Util.asctime(), "Test Page BODY");
    //a.waitForDone();
    Util.prta("Page="+a.getSendMailError());
    System.exit(0);
    Util.prt("SimpleSMTPThread test start");

  }
}


