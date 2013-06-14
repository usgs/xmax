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
/**
 * Util.java contains various static methods needed routinely in many places.
		*Its purpose is to hold all of these little helper functions in one place 
		*rather that creating a lot of little classes whose names would have to be 
		*remembered.
 *
 * Created on March 14, 2000, 3:58 PMt
 */
 
import java.sql.*;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.Properties;

//import java.lang.NumberFormatException;

/** 
 *
 * @author  David Ketchum, 17326 Rimrock Dr.,Golden, CO 80401 ketchum@stw-software.com
 *r
 * @version 1.00 (Baseline) 
 */
import java.util.TimeZone;
public class Util extends Object {
  static String process="UNSET";
  static PrintStream stdout=System.out;
  static PrintStream stderr=System.err;
  private static final String TRUSTSTORE_FILENAME =
          new File(System.getProperty("user.home"), ".keystore").getPath();

  private static boolean debug_flag=false;
  private static boolean traceOn = false;
  static TestStream out;
  static PrintStream lpt = null;
  static String OS="";
  static String userdir;
  static String userhome;
  static String localHostIP;
  static String printerFile;
  static String device;
  static String node;
  static String username;
  private static boolean isApplet = true;
  public static void setOutput(TestStream o) {if(out != null) out.close(); out = o;}
  public static TestStream getOutput() {return out;}
  public static void suppressFile() {out.suppressFile();}
  public static void setProcess(String s) {process=s;}
  public static String getProcess() {return process;}
  
  /** return the string representing the OS
   *@return A String with OS in it */
  public static String getOS() {return OS;}
  /** this initializes the Util package using the UC.getPropertyFilename()*/
  public static void init() {
    init(UC.getPropertyFilename());
  }
  public static boolean getNoInteractive() {
    if(out != null) return TestStream.noInteractive;
    return false;
  }
  /** set value of nointeractive flag (eliminates output dialogs on error if true)
   *@param t If true, kill interactive dialogs on error */
  public static void setNoInteractive(boolean t) {
    if(out != null) TestStream.setNoInteractive(t); 
  }
  /** Set the noConsoleFlag for output via TestStream. Also diables all dialog boxes
   *@param t If true, set noConsoleflag in TestStream
   */
  public static void setNoconsole(boolean t) {
    if(out != null) TestStream.setNoConsole(t);
  }
  /** Print out a summary of running threads. Use tag for identification of caller
   *@param tag The tag to use in output to identify caller */
  public static void showShutdownThreads (String tag) {
      System.err.println(Util.asctime()+" "+tag+" Shutdown is complete.  Thread.activeCount="+
      Thread.activeCount());

    // If there are any threads alive, we need more data on how to kill them!
    if(Thread.activeCount() > 0) {
      Thread [] thrs = new Thread[Thread.activeCount()];
      Thread.enumerate(thrs);       // GEt the list of threads.
      for(int i=0; i<thrs.length; i++) {
        if(thrs[i] != null) System.err.println(i+" nm="+thrs[i].getName()+" toStr="+thrs[i].toString()+
            " alive="+thrs[i].isAlive()+" class="+thrs[i].getClass().toString());
      }
      for(int i=0; i<thrs.length; i++) if(thrs[i] != null) thrs[i].interrupt();

      try { Thread.sleep(5000L);} catch(InterruptedException e) {}
      System.err.println(Util.asctime()+" "+tag+" Shutdown is complete.  Thread.activeCount="+
          Thread.activeCount());
    }
  }
  /** returns a String with on line per active thread
   *@return The list of current Threads */
  public static String getThreadsString () {
    StringBuilder sb = new StringBuilder(200);
    sb.append(Util.asctime()+" Thread.activeCount="+
      Thread.activeCount()+"\n");

    // If there are any threads alive, we need more data on how to kill them!
    if(Thread.activeCount() > 0) {
      Thread [] thrs = new Thread[Thread.activeCount()];
      if(thrs != null) {
        Thread.enumerate(thrs);       // GEt the list of threads.
        for(int i=0; i<thrs.length; i++) {
          if(thrs[i] != null) sb.append(i+" nm="+thrs[i].getName()+" toStr="+thrs[i].toString()+
              " alive="+thrs[i].isAlive()+" class="+thrs[i].getClass().toString()+"\n");
        }
      }
    }
    return sb.toString();
  }
  /** initialize the Util packages using the given property filename 
  * @param propfile The string with file name in user account to use for properties.*/
  public static void init(String propfile) {
    if(userhome != null) return;
    OS = System.getProperty("os.name");
    userdir = System.getProperty("user.dir");
    userhome = System.getProperty("user.home");
    username = System.getProperty("user.name");
    try {
      localHostIP = InetAddress.getLocalHost().toString();
    }
    catch(UnknownHostException e) {
      localHostIP = "UnknownLocalIP";
    }
    if(debug_flag) 
      System.out.println("  *** init() "+OS+" "+userdir+" "+userhome);
    //out = System.out;
    if(OS.indexOf("Windows") >= 0) {
      addDefaultProperty("PrinterCommand","print /D:\\\\pilgrim\\shaky2");
    }
    else if(OS.indexOf("SunOS") >= 0) {
      addDefaultProperty("PrinterCommand","lpr -P shaky2");
    }
    else if(OS.indexOf("Mac") >= 0) {
      addDefaultProperty("PrinterCommand", "lpr -P shaky2");
    }
    else {
      addDefaultProperty("PrinterCommand","");
    }
    addDefaultProperty("PrinterFile",System.getProperty("user.home")+System.getProperty("file.separator")+"anss.lpt");
    addDefaultProperty("SessionFile",System.getProperty("user.home")+System.getProperty("file.separator")+"SESSION.OUT");
    addDefaultProperty("MySQLServer","136.177.24.92");
    addDefaultProperty("SMTPServer","gscodens03.cr.usgs.gov");
    
    if(OS.indexOf("Windows") < 0) {   // All systems but Windows!
      try {
        String c = "/usr/sbin/ifconfig -a";
        String adrstring="inet";
        String netmaskstring="netmask";
        if(OS.indexOf("Linux") >=0) {
          c="/sbin/ifconfig -a";
          adrstring="inet addr:";
          netmaskstring="Bcast";
        }
        if(OS.indexOf("Mac") >=0) c="/sbin/ifconfig -a";
        Subprocess sp = new Subprocess(c);
        int val = sp.waitFor();
        String s = sp.getOutput();
        //System.out.println("ifconfi output= "+val+"\n"+s);
        BufferedReader in = new BufferedReader(new StringReader(s));
        String line="";
        while ( (line = in.readLine()) != null) {
          if(line.indexOf(adrstring) >=0 && line.indexOf(netmaskstring) > 0) {
            String addr = line.substring(line.indexOf(adrstring)+adrstring.length()+1, line.indexOf(netmaskstring)-1).trim();
            if(addr.length() > 7) {
              if(addr.substring(0,7).equals("136.177")) {
                addDefaultProperty("HostIP", addr);
                break;
              }
            }
          }
        }
      }
      catch(IOException e) {
        System.out.println("IOException trying to get IP of local host to public internet"+e);
      }
      catch(InterruptedException e) {
        System.out.println("InterruptedException trying to get IP of local host to public internet"+e);
      }
    }
    else {    // For windows do this
     try {
       System.out.println("Windows IP="+InetAddress.getLocalHost().getHostAddress());
       addDefaultProperty("HostIP", InetAddress.getLocalHost().getHostAddress());
     }
     catch(UnknownHostException e) {
       System.out.println("Could not load host IP for windows.");
     }
    }
    
    //if(out == null) defprops.list(System.out);
    //else defprops.list(out);
    loadProperties(propfile);
    if(Util.getProperty("HostIP") != null) 
      if(Util.getProperty("HostIP").length() < 7) addDefaultProperty("HostIP", "136.177.24.92");
    if(debug_flag) System.out.println(" prop size="+prop.size());
    
    // Set up the TestStream object to control the output log file and console output
    if(out == null) {
      String filename=prop.getProperty("SessionFile");
      if(filename == null) filename="SESSION.OUT";
      if(debug_flag) System.out.println("  **** Setting up the TestStream for out file="+filename);
      if(debug_flag) System.out.println("  **** Opening session file ="+filename);
      out=new TestStream(filename); 
    }
    if(debug_flag) {
      if(out == null) prop.list(System.out);
      else prop.list(out);
    }
    if(debug_flag) prtinfo();

    /* Identify the truststore used for SSL. */
    System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILENAME);
    /* It seems we don't need a password when we're not getting private keys out
       of the truststore. If it later becomes necessary to provide a password,
       here's how to do it. */
    // System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);
  }
  public static void setTestStream(String filename) {if(out != null) out.close(); out = new TestStream(filename);}
	
	/** return the static member indicating whether this is a application or 
	 *applet.  This static member controls other Util about whether certain
	 *thing can be done.  For instance, Util.prt will put output in a 
	 *SESSION.OUT file if its an application, but it cannot do this as an 
	 *applet because file access is not allowed by applets via the sandbox.
   *@return The current state of isApplet variable;
	 */
	
	public static boolean getIsApplet(){ return isApplet;}
  
	/** send txt to the locally attached printer.  This only make sense in a 
	 *application.  The lpt unit is opened automatically 
   *@param filename This file is read and formatted for output on a printer
	 */
  public static void lptText(String filename) {
    String data;
    try {
      if(debug_flag) Util.prt("lptText lpt="+lpt);
      if(lpt == null) lptOpen();
      if(debug_flag) Util.prt("lptText lpt now="+lpt);
      BufferedReader r = new BufferedReader(
        new StringReader(filename));
      while ( (data = r.readLine()) != null ) {
        if(data.equals("")) {
                data = " ";
        }
        if(debug_flag) Util.prt("Line="+" "+data);
        lpt.println("    | "+data);
      }
      lpt.print("\f");
      lpt.flush();
			lpt.close();
			lpt=null;
    } catch (IOException E) {
      Util.prt("IO exception caught!!!!");
    }
  }
  
	/** send txt to the LPT output unit.  It will be indented with spaces and
	 *the LPT will use lptOpen() if it is not yet opended 
   * @param t A string buffer to format for output on lpt unit
   */
  public static void lpt(StringBuffer t) {lpt(t.toString());}
	/** send txt to the LPT output unit.  It will be indented with spaces and
	 *the LPT will use lptOpen() if it is not yet opended 
   * @param txt A string buffer to format for output on lpt unit
   */
  public static void lpt(String txt) {
    if(lpt == null) lptOpen();
    //lpt.println(txt); // the following was needed so PCs would put in CRLF
    try {
      BufferedReader in = new BufferedReader(
          new StringReader(txt));
      String s=null;
      while ((s = in.readLine()) != null) lpt.println(s);
    }
    catch(IOException e) {
      Util.prt("lpt() threw IOError="+e);
    }
  }
  static public String getAccount() {
    return username;
  }
  static public String getLocalHostIP() {return localHostIP;}
  static public String getIDText() {return node+"/"+username+" "+process+" "+localHostIP+" "+OS+" "+userhome+" "+userdir;}
  static String [] roles;
  static long lastRoles;
  /** return the roles currently running on this computer according to the /vdl/home/roles_NODE
   * file
   * @param node If null or empty, use the current node returned by Util.getNode(), else the node to get
   * @return Array of strings with each role currently on this node.
   */
  static public String [] getRoles(String node) {
    if(node == null) node = Util.getNode();
    if(node.equals("")) node = Util.getNode();
    // on non-edge/gaux node, just use the system node name
    if(node.indexOf("edge") < 0 && node.indexOf("gaux") < 0) {  // Not an edge type node use system name
      if(roles == null) {
        roles = new String[1];
        roles[0] = node;
      }
      return roles;
    }
    if(System.currentTimeMillis() - lastRoles < 120000) return roles;
    lastRoles=System.currentTimeMillis();

    //Util.prt("call getroles returns="+System.getenv("VDL_ROLES"));
    String line="";
    try {
      BufferedReader in = new BufferedReader(new FileReader("/home/vdl/roles_"+node.trim()));
      while( (line = in.readLine()) != null) {
        if(line.length() > 9)
          if(line.substring(0,9).equals("VDL_ROLES")) {
            line =line.substring(11).replaceAll("\"","");
            roles = line.split("[\\s,]");
            for(int i=0; i<roles.length; i++) {
              roles[i] = roles[i].trim();
              //Util.prt("get roles read files returns roles "+i+" "+roles[i]);
            }
            break;
          }
      }
      in.close();
    }
    catch(FileNotFoundException e) {
      Util.IOErrorPrint(e,"Trying to read /home/vdl/roles_"+node.trim());
    }
    catch(IOException e) {
      Util.IOErrorPrint(e,"Trying to read /home/vdl/roles_"+node.trim());
    }
    return roles;
  }
  /** When you want the basic data base reports with "|" to have
  * boxes around the fields.  This takes the string in txt (usually returned
	 *as a print from a result set and sends it to the printer 
   * @param t The string buffer to format in box mode
   */
  public static void lptBox(StringBuffer t) {lptBox(t.toString());}
   /** When you want the basic data base reports with "|" to have
  * boxes around the fields.  This takes the string in txt (usually returned
	 *as a print from a result set and sends it to the printer 
   * @param txt The string to format in box mode
   */
 public static void lptBox(String txt) {
    String s;
    String header;
    String line="";
    int iline;
    if(lpt == null) lptOpen();
    try {
      BufferedReader in = new BufferedReader(
          new StringReader(txt));
      
      // get "title" line and build the - and + box line
			if(debug_flag) Util.prt("Starting Print");
      s = in.readLine();
      int lastcol =0;
      for(int i=0; i<s.length(); i++) {
        if(s.charAt(i) == '|') {
          if( i < 80) lastcol= i;
          line += "+";
        }
        else line += "-";
      }  
      header = s;
      iline=100;
      lastcol++;
      
      // Read in a database line, convert it
      while ( (s = in.readLine()) != null) {
				if(debug_flag) Util.prt("Readln="+s);
        if(iline > 55) {
          if(iline < 100) lpt.print("\f");
          if(lastcol <= 80) {
            lpt.println(" "+header);
            lpt.println(" "+line);  
            iline = 2;
          } else {
            lpt.println(" "+header.substring(0,lastcol));
            lpt.println(" "+line.substring(0,lastcol));
            lpt.println("           |"+header.substring(lastcol));
            lpt.println("           |"+line.substring(lastcol));
            iline=4;
          }
        }
        if(lastcol <= 80) {
          lpt.println(" "+s);
          lpt.println(" "+line);
          iline+=2;
        } else {
          lpt.println(" "+s.substring(0,lastcol));
          String tmp = s.substring(lastcol);
          tmp = tmp.replace('|', ' ');
          tmp = tmp.trim();
          if( !tmp.equals("")) {
            s = s.replace('|', ' ');
            lpt.println("            "+s.substring(lastcol));
            iline++;
          }
          lpt.println(" "+line.substring(0,lastcol));
//          lpt.println("           |"+line.substring(lastcol));
          iline+=2;
        }

      }
      in.close();
      lpt.println("\f");
      lpt.flush();
			lpt.close();
			lpt = null;
    } catch (IOException E) {
      Util.prt("IO exception creating print job");
    }
  }
  /** lptPrint() causes the currently open printer file to be printed based
   * on the "PrintCommand" property
   */
  public static void lptSpool() {
    if(lpt != null) {
      lpt.flush();
      lpt.close();
      lpt=null;
    }
    String cmd = prop.getProperty("PrinterCommand")+" "+prop.getProperty("PrinterFile");
    if(debug_flag) Util.prt("Print command = "+cmd);
    try {
      Process p = Runtime.getRuntime().exec(cmd);
      Util.prt("Runtime executed!");
      p.waitFor();
    }
    catch(IOException e) {
      IOErrorPrint(e,"Trying to print with cmd="+cmd);
    }
    catch (InterruptedException e) {
      Util.prt("Interrupted exception doing runtime.exec()");
    }
  }
	/** lptOpen opens the line printer.  If the user wants to redirect this
	 *to some device/file other than LPT1:, create a file C:\Ops\PRINTER.DAT
	 *and put the desired device or file on the first line.  This will open
	 *an outputStream to the device */
  public static void lptOpen() {

    device = prop.getProperty("PrinterFile");
    try {

      if(debug_flag) Util.prt("Attempt Connect to |"+device+"| unit");
      lpt = new PrintStream(
                 device);
      /*lpt = new PrintStream(
                 new BufferedOutputStream(
                   new FileOutputStream(device)));*/
     }
     catch (FileNotFoundException E) {
      Util.prt("Printer did not open "+device);
      Util.prt("Message : "+E.toString());
      E.printStackTrace(out);
      Util.prt("Open LPT.OUT for print compatibility");
      exit("Printer file would not open.");
    }
    if(debug_flag) Util.prt("LPT is now "+lpt);
  }
  
	/** set whether this entity should be treated as an applet or not 
   * @param b If true this should be treated as an applet*/
  public static void setApplet(boolean b)
  { isApplet = b;
    init();
    if(debug_flag) Util.prt("setApplet="+b);

    return;
  }
  
  /** return a line of user input (from System.in)
   *@return A String with input without the newline */
  public static String getLine() {
    StringBuilder sb = new StringBuilder(200);
    byte [] b = new byte[50];
    boolean done = false;
    while (!done) {
      try {
        int len = System.in.read(b);
        for(int i=0; i<len; i++) {
          if(b[i] == '\n') {done=true; break;}
          sb.append( (char) b[i]);
        }
      } catch (IOException e) {
        Util.IOErrorPrint(e,"Getting console input");
      }
    }

    return sb.toString();
  }
  
  /** turn trace output on or off
   *@param t If true, start printing trace output */
  public static void setTrace(boolean t) { traceOn=t;}
  /** if Trace is on, print out information on object and the given string
   *@param obj A object to print its class name
   *@param txt Some text to add to class information
   */
  public static void trace(Object obj,String txt) {
    if(traceOn) 
       Util.prt("TR:"+obj.getClass().getName()+":"+txt);
  }
/**
   * Use this to override the target of the prt and prta() methods from the console
   * or session.out
   */
  //public static void setOut(PrintStream o) { out = o;}
  /** prt takes the input text and prints it out.  It might go into the MSDOS
	 window or to the SESSION.OUT file depending on the state of the debug flag.
	 The "main" should decide on debug or not, set the flag and then all of the
	 output will be available on the window or in the file.  The file is really
	 useful when something does not work because the user can e-mail it to us
	 and a full debug listing is available for postmortem
   * @param out The output PrintStream to send output,
   * @param txt The output text
   */
  public static void prt(PrintStream out, String txt) {out.println(txt);}
  /** prta adds time stamp to output of prt().
   * takes the input text and prints it out.  It might go into the MSDOS
	 window or to the SESSION.OUT file depending on the state of the debug flag.
	 The "main" should decide on debug or not, set the flag and then all of the
	 output will be available on the window or in the file.  The file is really
	 useful when something does not work because the user can e-mail it to us
	 and a full debug listing is available for postmortem
   * @param out The output PrintStream to send output,
   * @param txt The output text
   */
  public static void prta(PrintStream out, String txt) {out.println(Util.asctime()+" "+txt);}
  /** prta adds time stamp to output of prt().
   * takes the input text and prints it out.  It might go into the MSDOS
	 window or to the SESSION.OUT file depending on the state of the debug flag.
	 The "main" should decide on debug or not, set the flag and then all of the
	 output will be available on the window or in the file.  The file is really
	 useful when something does not work because the user can e-mail it to us
	 and a full debug listing is available for postmortem
   * @param txt The output text
   */
  public static void prta(String txt) {Util.prt(Util.asctime()+" "+txt);}
  /** prt takes the input text and prints it out.  It might go into the MSDOS
	 window or to the SESSION.OUT file depending on the state of the debug flag.
	 The "main" should decide on debug or not, set the flag and then all of the
	 output will be available on the window or in the file.  The file is really
	 useful when something does not work because the user can e-mail it to us
	 and a full debug listing is available for postmortem

   * @param txt The output text
   */
  public static void prt(String txt) {
    //System.out.println("OS="+OS+" Debug="+debug_flag+" isApplet="+isApplet+" txt="+txt+" out="+out);
    if(userhome == null) init();
    out.println(txt);
    out.flush();
  }
  /** dump a buch of config info
   */
  public static void prtinfo() {
    Util.prt("Environment : OS="+OS+" Arch="+System.getProperty("os.arch")+" version="+System.getProperty("os.version")+
      " user name="+System.getProperty("user.name")+" hm="+System.getProperty("user.home")+
      " current dir="+System.getProperty("user.dir")+
      "Separators file="+System.getProperty("file.separator")+
      " path="+System.getProperty("path.separator"));
    Util.prt("Java compiler="+System.getProperty("java.compiler")+
      " JRE version="+System.getProperty("java.version")+
      " JRE Manuf="+System.getProperty("java.vendor")+
      " Install directory="+System.getProperty("java.home")+
      " JRE URL="+System.getProperty("java.url"));
    Util.prt("VM implementation version="+System.getProperty("java.vm.version")+
      " vendor="+System.getProperty("java.vm.vendor")+
      " name="+System.getProperty("java.vm.name"));
    Util.prt("VM Specification version="+System.getProperty("java.vm.specification.version")+
      " vendor="+System.getProperty("java.vm.specification.vendor")+
      " name="+System.getProperty("java.vm.specification.name"));
    Util.prt("Class version="+System.getProperty("java.class.version")+
      "\nclass path="+System.getProperty("java.class.path")+
      "\nlibrary path="+System.getProperty("java.library.path"));
  }
  
	/** set value of debug flag and hence whether Util.prt() generates output 
	 *to string.  If false, output will go to SESSION.OUT unless an applet 
   * @param in if true, set debug flag on*/
  public static void debug(boolean in) {
    debug_flag = in;
    if(debug_flag) prtinfo();
    return;
  }
	
	/** get state of debug flag 
   *@return Current setting of debug flag*/
  public static boolean isDebug() { return debug_flag;}

  /**
   This routine dumps the meta data and the current values from a 
   ResultSet.  Note: the values will always return NULL if the RS
   is on the insertRow, even if the insertRow columns have been updated
   *@param rs The resultset to print
  */
  public static void printResultSetMetaData(ResultSet rs) {
    try {
      ResultSetMetaData md = rs.getMetaData();
      Util.prt("Insert row columns= " + md.getColumnCount());
      for(int i=1; i<=md.getColumnCount(); i++) {
        String column = md.getColumnName(i);
        String type = md.getColumnTypeName(i);
        String txt = "" + i + " " + type + " nm: " + column + " NullOK :" +
          md.isNullable(i) + "value=";
        if(type.equals("CHAR")) txt = txt + rs.getString(column);
        if(type.equals("LONG")) txt = txt + rs.getInt(column);
        Util.prt(txt);
      }
    } catch (SQLException e) {
     Util.SQLErrorPrint(e,"MetaData access failed");
    }
  }
  
  
  /** Clear all of the fields in a ResultSet. Used the by the New record
   objects to insure everything is cleared for an InsertRow so the
   dumb thing will actually insert something!  This uses the Result set 
	 meta data to
	 *get the desciptions and types of the columns
   *@param rs The resultset to clear.
	 */
  public static void clearAllColumns(ResultSet rs) {
    try {
      ResultSetMetaData md = rs.getMetaData();
//      Util.prt("ClearAllColumns= " + md.getColumnCount());
      for(int i=1; i<=md.getColumnCount(); i++) {
        String column = md.getColumnName(i);
        String type = md.getColumnTypeName(i);
//        Util.prt("" + i + " " + type + " nm: " + column + " NullOK :" +md.isNullable(i));
//        String txt = "" + i + " " + type + " nm: " + column + " NullOK :" +
//          md.isNullable(i) ;
        // For each data type add an ELSE here
        int j = type.indexOf(" UNSIGNED");
        if(j > 0) {
          type = type.substring(0,j);
          //Util.prta("handle unsigend="+type);
        }
        if(type.equals("CHAR")) rs.updateString(column,"");
        else if(type.equals("FLOAT")) rs.updateFloat(column,(float) 0.);
        else if(type.equals("DOUBLE")) rs.updateDouble(column,(double) 0.);
        else if(type.equals("LONGLONG")) rs.updateLong(column,(long) 0);
        else if(type.equals("INTEGER")) rs.updateInt(column,(int) 0);
        else if(type.equals("BIGINT")) rs.updateInt(column,(int) 0);
        else if(type.equals("LONG")) rs.updateInt(column,0);
        else if(type.equals("SHORT")) rs.updateShort(column,(short)0);
        else if(type.equals("SMALLINT")) rs.updateShort(column,(short)0);
        else if(type.equals("TINY")) rs.updateByte(column,(byte) 0);
        else if(type.equals("TINYINT")) rs.updateByte(column,(byte) 0);
        else if(type.equals("BYTE")) rs.updateByte(column,(byte) 0);
        else if(type.equals("VARCHAR")) rs.updateString(column,"");
        else if(type.equals("DATE")) rs.updateDate(column, new java.sql.Date((long) 0));
        else if(type.equals("DATETIME")) rs.updateDate(column, new java.sql.Date((long) 0));
        else if(type.equals("TIME")) rs.updateTime(column, new Time((long) 0));
        else if(type.equals("TINYBLOB")) rs.updateString(column,"");
        else if(type.equals("BLOB")) rs.updateString(column,"");
        else if(type.equals("MEDIUMBLOB")) rs.updateString(column,"");
        else if(type.equals("LONGBLOB")) rs.updateString(column,"");
        else if(type.equals("TINYTEXT")) rs.updateString(column,"");
        else if(type.equals("TEXT")) rs.updateString(column,"");
        else if(type.equals("MEDIUMTEXT")) rs.updateString(column,"");
        else if(type.equals("LONGTEXT")) rs.updateString(column,"");
        else if(type.equals("TIMESTAMP")) {
          java.util.Date now = new java.util.Date();
          rs.updateTimestamp(column, new Timestamp(now.getTime()));
        }
        else {
          System.err.println("clearAllColumn type not handled!=" + type+
             " Column=" + column);
          System.exit(0);
        }
//        Util.prt(txt);
      }
    } catch (SQLException e) {
     Util.SQLErrorPrint(e,"MetaData access failed");
    }
  }
     
	
	/** given and SQLException and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(SQLException) clauses to implement standard reporting.
   *@param E The esction
   *@param msg The user supplied text to add
	 */
  public static void SQLErrorPrint(SQLException E, String msg) {
    System.err.println(asctime()+" "+msg);
    System.err.println("SQLException : " + E.getMessage());
    System.err.println("SQLState     : " + E.getSQLState());
    System.err.println("SQLVendorErr : " + E.getErrorCode());
    return;
  }
	/** given and SQLException and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(SQLException) clauses to implement standard reporting.
   *@param E The esction
   *@param msg The user supplied text to add
   *@param out The printstream to use for outputing this exception
	 */
  public static void SQLErrorPrint(SQLException E, String msg, PrintStream out) {
    if(out == null ) {SQLErrorPrint(E, msg); return;}
    out.println(asctime()+" "+msg);
    out.println("SQLException : " + E.getMessage());
    out.println("SQLState     : " + E.getSQLState());
    out.println("SQLVendorErr : " + E.getErrorCode());
    return;
  }
	/** given and IOException from a Socket IO and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(SocketException) clauses to implement standard reporting.
   *@param e The esction
   *@param msg The user supplied text to add
	 */
  public static void SocketIOErrorPrint(IOException e, String msg) {
    SocketIOErrorPrint(e,msg, null);
    return;
  }
	/** given and IOException from a Socket IO and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(SocketException) clauses to implement standard reporting.
   *@param e The esction
   *@param msg The user supplied text to add
   *@param ps The PrintStream to use to output this exception
	 */
  public static void SocketIOErrorPrint(IOException e, String msg, PrintStream ps) {
    if(ps == null) ps = System.out;
    if(e != null) {
      if(e.getMessage() != null) {
        if(e.getMessage().indexOf("Broken pipe") >=0) ps.println("Broken pipe "+msg);
        else if(e.getMessage().indexOf("Connection reset") >=0) ps.println("Connection reset "+msg);
        else if(e.getMessage().indexOf("Connection timed") >=0) ps.println("Connection timed "+msg);
        else if(e.getMessage().indexOf("Socket closed") >=0) ps.println("Socket closed "+msg);
        else if(e.getMessage().indexOf("Stream closed") >=0) ps.println("Socket Stream closed "+msg);
        else if(e.getMessage().indexOf("Operation interrupt") >=0) ps.println("Socket interrupted "+msg);
        else Util.IOErrorPrint(e,msg, ps);
      }
    }
    else Util.IOErrorPrint(e,msg, ps);
    return;
  }
	/** given and SocketException and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(SocketException) clauses to implement standard reporting.
   *@param E The esction
   *@param msg The user supplied text to add
	 */
  public static void SocketErrorPrint(SocketException E, String msg) {
    System.err.println(asctime()+" "+msg);
    System.err.println("SocketException : " + E.getMessage());
    System.err.println("SocketCause     : " + E.getCause());
    E.printStackTrace();
    return;
  }
	/** given and IOException and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(IOException) clauses to implement standard reporting.
   *@param E The esction
   *@param msg The user supplied text to add
	 */
  public static void IOErrorPrint(IOException E, String msg) {
    System.err.println(asctime()+" "+msg);
    System.err.println("SocketException : " + E.getMessage());
    System.err.println("SocketCause     : " + E.getCause());
    E.printStackTrace();
    return;
  }
	/** given and IOException and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(IOException) clauses to implement standard reporting.
   *@param E The esction
   *@param msg The user supplied text to add
   *@param out The PrintStream to use to output this exception
	 */
  public static void IOErrorPrint(IOException E, String msg, PrintStream out) {
    if(out == null) {IOErrorPrint(E, msg); return;}
    out.println(asctime()+" "+msg);
    out.println("SocketException : " + E.getMessage());
    out.println("SocketCause     : " + E.getCause());
    E.printStackTrace(out);
    return;
  }
	/** given and UnknownHostException and local message string, dump the exception
	 *and system state at the time of the exception.  This routine should be 
	 *called by all "catch(UnknownHostException) clauses to implement standard reporting.
   *@param E The esction
   *@param msg The user supplied text to add
	 */
  public static void UnknownHostErrorPrint(UnknownHostException E, String msg) {
    System.err.println(asctime()+" "+msg);
    System.err.println("SocketException : " + E.getMessage());
    System.err.println("SocketCause     : " + E.getCause());
    E.printStackTrace();
    return;
  }
  /** sleep the give number of milliseconds
   *@param ms THe number of millis to sleep */
  public static void sleep(int ms) {
    try {Thread.sleep(Math.max(1,ms));} catch(InterruptedException e) {}
  }
  /**
   * Escape a string for use in an SQL query. The string is returned enclosed in
   * single quotes with any dangerous characters escaped.
   *
   * This is modeled after escape_string_for_mysql from the MySQL API. Beware
   * that the characters '%' and '_' are not escaped. They do not have special
   * meaning except in LIKE clauses.
   *
   * @param s The string to be escaped
   * @return The escaped string
   */
  public static String sqlEscape(String s)
  {
    StringBuilder result;
    int i;
    char c;

    result = new StringBuilder();
    result.append('\'');
    for (i = 0; i < s.length(); i++) {
      c = s.charAt(i);
      switch (c) {
        case '\0':
          result.append("\\0");
          break;
        case '\n':
          result.append("\\n");
          break;
        case '\r':
          result.append("\\r");
          break;
        case '\032':
          result.append("\\Z");
        case '\\':
        case '\'':
        case '"':
          result.append("\\" + c);
          break;
        default:
          result.append(c);
          break;
      }
    }
    result.append('\'');

    return result.toString();
  }
	
  /**
   * Escape an int for use in an SQL query.
   *
   * @param i The int to be escaped
   * @return The escaped string
   */
  public static String sqlEscape(int i)
  {
    return Integer.toString(i);
  }
	
  /**
   * Escape a long for use in an SQL query.
   *
   * @param l The long to be escaped
   * @return The escaped string
   */
  public static String sqlEscape(long l)
  {
    return Long.toString(l);
  }

  /**
   * Escape a Date for use in an SQL query. The string returned is in the form
   * "{d 'yyyy-MM-dd'}".
   *
   * @param d The Date to be escaped
   * @return The escaped string
   * @see <a href="http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html">http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html</a>
   */
  public static String sqlEscape(java.sql.Date d)
  {
    return "{d " + sqlEscape(d.toString()) + "}";
  }

  /**
   * Escape a Time for use in an SQL query. The string returned is in the form
   * "{t 'hh:mm:ss'}".
   *
   * @param t The Time to be escaped
   * @return The escaped string
   * @see <a href="http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html">http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html</a>
   */
  public static String sqlEscape(Time t)
  {
    return "{t " + sqlEscape(t.toString()) + "}";
  }

  /**
   * Escape a Timestamp for use in an SQL query. The string returned is in the
   * form "{ts 'yyyy-MM-dd hh:mm:ss.ffffffff'}".
   *
   * @param ts The Timestamp to be escaped
   * @return The escaped string
   * @see <a href="http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html">http://incubator.apache.org/derby/docs/10.0/manuals/reference/sqlj230.html</a>
   */
  public static String sqlEscape(Timestamp ts)
  {
    return "{ts " + sqlEscape(ts.toString()) + "}";
  }

	/** ResultSets often come with "NULL" and return nulls, we preferr
   actual objects with appropriate values.  Return a "" if result is null.
   *@param rs The ResultSet to get this String column from
   *@param column The name of the column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
	 */
  public static String getString(ResultSet rs, String column)
   throws SQLException {
//    try {
    String t = rs.getString(column);
//    Util.prt("Util.getString for " + column + "=" +t + "| wasnull=" +rs.wasNull());
    if( rs.wasNull()) t="";
//    } catch (SQLException e) {throw e};
    return t;
  }
	/** ResultSets often come with "NULL" and return nulls, we preferr
   actual objects with appropriate values.  Return a "" if result is null.
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
	 */
  public static String getString(ResultSet rs, int column)
   throws SQLException {
//    try {
    String t = rs.getString(column);
//    Util.prt("Util.getString for " + column + "=" +t + "| wasnull=" +rs.wasNull());
    if( rs.wasNull()) t="";
//    } catch (SQLException e) {throw e};
    return t;
  }
   
	
	/** get and integer from ResultSet rs with name 'column' 
    *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
  */
  public static int getInt(ResultSet rs, String column)
  throws SQLException{
//    try {
      int i = rs.getInt(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  } 
  
	/** get and integer from ResultSet rs with name 'column' 
    *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
  */
  public static int getInt(ResultSet rs, int column)
  throws SQLException{
//    try {
      int i = rs.getInt(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  } 

   
	/** get a long from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
  */
  public static long getLong(ResultSet rs, String column)
  throws SQLException{
//    try {
      long i = rs.getLong(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  } 

	/** get a long from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
  */
  public static long getLong(ResultSet rs, int column)
  throws SQLException{
//    try {
      long i = rs.getLong(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  } 

   
	/** get a short from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
  */
  public static short getShort(ResultSet rs, String column)
  throws SQLException{
//    try {
      short i = rs.getShort(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  } 
  
	/** get a short from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
  */
  public static short getShort(ResultSet rs, int column)
  throws SQLException{
//    try {
      short i = rs.getShort(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  } 
  
	/** get a byte from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQLExceptions
  */  public static byte getByte(ResultSet rs, int column)
  throws SQLException{
//    try {
      byte i = rs.getByte(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  }
  
	/** get a double from ResultSet rs with name 'column' 
   	/** get a short from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static double getDouble(ResultSet rs, String column)
  throws SQLException{
//    try {
      double i = rs.getDouble(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  }
  
	/** get a double from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
   */
  public static double getDouble(ResultSet rs, int column)
  throws SQLException{
//    try {
      double i = rs.getDouble(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  }
  
	/** get a float from ResultSet rs with name 'column' 
      *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static float getFloat(ResultSet rs, String column)
  throws SQLException{
//    try {
      float i = rs.getFloat(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  }
  /** get a float from ResultSet rs with name 'column' 
    *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static float getFloat(ResultSet rs, int column)
  throws SQLException{
//    try {
      float i = rs.getFloat(column);
      if( rs.wasNull()) i = 0;
//    } catch (SQLException e) { throw e}
    return i;
  }
     
	/** get a Timestamp from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static Timestamp getTimestamp(ResultSet rs, String column)
  throws SQLException {
      Timestamp i = rs.getTimestamp(column);
      if( rs.wasNull()) i = new Timestamp(0);
    return i;
  }
  
	/** get a Timestamp from ResultSet rs with name 'column' 
   *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static Timestamp getTimestamp(ResultSet rs, int column)
  throws SQLException {
      Timestamp i = rs.getTimestamp(column);
      if( rs.wasNull()) i = new Timestamp(0);
    return i;
  }
  
	/** get a date from ResultSet rs with name 'column' 
      *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static java.sql.Date getDate(ResultSet rs, String column)
  throws SQLException {
      java.sql.Date i = rs.getDate(column);
      if( rs.wasNull()) i = new java.sql.Date((long) 0);
    return i;
  }
  
	/** get a date from ResultSet rs with name 'column' 
      *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static java.sql.Date getDate(ResultSet rs, int column)
  throws SQLException {
      java.sql.Date i = rs.getDate(column);
      if( rs.wasNull()) i = new java.sql.Date((long) 0);
    return i;
  }
  
	/** get a Time from ResultSet rs with name 'column' 
      *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static Time getTime(ResultSet rs, String column)
  throws SQLException {
      Time i = rs.getTime(column);
      if( rs.wasNull()) i = new Time(0);
    return i;
  }
  
	/** get a Time from ResultSet rs with name 'column' 
      *@param rs The ResultSet to get this column from
   *@param column The  column to get
   *@return The target column value
   *@throws SQLException if column does not exist or other SQL error
  */
  public static Time getTime(ResultSet rs, int column)
  throws SQLException {
      Time i = rs.getTime(column);
      if( rs.wasNull()) i = new Time(0);
    return i;
  }
  
 
  /** We will represent Times as hh/dd and trade them back and forth with 
	 *sister routine stringToTime() 
   *@param t The time to convert to a String
   *@return The String with time in hh:mm am/pm
	 */
  public static String timeToString(Time t) {
    String s= t.toString();
    s=s.substring(0,5);
    if(s.substring(0,2).compareTo("12") > 0) {
      int ih=Integer.parseInt(s.substring(0,2))-12;
      s= "" + ih + s.substring(2,5);
      s = s + " pm";
    } else {
      if(s.substring(0,2).equals("12")) s = s + " pm";
      else s = s + " am";
    }
    return s;
  }
	
	/** convert string to a time in the normal form hh:mm to SQL Time type 
   *@param s The String to convert to a time
   *@return The Time
   */
  public static Time stringToTime(String s) {
    int ih,im;
    String ampm;
    StringTokenizer tk = new StringTokenizer(s,": ");
    if(tk.countTokens() < 2) {
      Util.prt("stringToTime not enough tokens s="+s+" cnt="+
        tk.countTokens());
      return new Time((long) 0);
    }
    String hr=tk.nextToken();
    String mn=tk.nextToken();
		if(debug_flag) Util.prt("time to String hr="+hr+" min="+mn);
    try {
      ih=Integer.parseInt(hr);
      im=Integer.parseInt(mn);
    } catch(NumberFormatException e) {
      Util.prt("Time: not a integers "+hr+":"+mn+ " string="+s);
      return new Time((long) 0);
    }
    if(tk.hasMoreTokens()) {
      ampm=tk.nextToken();
			if(debug_flag) Util.prt("timeToString ampm="+ampm+" is pm="+ampm.equalsIgnoreCase("pm"));
      if(ampm.equalsIgnoreCase("pm")&& ih != 12) ih+=12;
      else if(ampm.equalsIgnoreCase("am")) {
      } else {
        if(debug_flag) Util.prt("Time add on not AM or PM ="+s);
        if(ih < 8) ih+=12;          // We do not play before 8
      }
    } else {
      if(ih < 8) ih+=12;          // We do not play before 8
      
    }  
		
    Time t=new Time((long) ih*3600000+im*60000);
    return t;
  }

  // This sets the default time zone to GMT so that GregorianCalendar uses GMT 
  // as the local time zone!
  public static void  setModeGMT() {
    TimeZone tz =TimeZone.getTimeZone("GMT+0");
    TimeZone.setDefault(tz);
  }
  
  
  /** Create a SQL date from a year, month, day int.  
	 The SQL date comes from MS since 1970 but the "gregorianCalendar"
   Class likes to use MONTH based on 0=January.  This does the right
   Thing so I wont forget later!
   *@param year The year
   *@param month The month
   *@param day The day of month
   *@return Date in sql form
	 */
  public static java.sql.Date date(int year, int month, int day) {
    GregorianCalendar d = new GregorianCalendar(year, month-1, day);
    return new java.sql.Date(d.getTime().getTime());
  }
    /** Create a Java date from a year, month, day, hr, min,sec .  
	 The SQL date comes from MS since 1970 but the "gregorianCalendar"
   Class likes to use MONTH based on 0=January.  This does the right
   Thing so I wont forget later!
   *@param year The year
   *@param month The month
   *@param day The day of month
     *@param hr The hour of day
     *@param min The minute
     *@param sec The second
   *@return Date in sql form

 */
  public static java.util.Date date(int year, int month, int day, int hr, int min,int sec) {
    GregorianCalendar d = new GregorianCalendar(year, month-1, day, hr, min,sec);
    return new java.util.Date(d.getTime().getTime());
  }
  
	/** return current date (based on system time) as an SQL Date 
   * @return The current date as an SQL date*/
	public static java.sql.Date today() {
    GregorianCalendar d = new GregorianCalendar();
//		Util.prt("Year="+d.get(Calendar.YEAR)+" mon="+d.get(Calendar.MONTH));
//		Util.prt("d="+d.toString());
    return new java.sql.Date(d.getTime().getTime());
  }
  static GregorianCalendar gstat;
  /** get a gregorian calendar given a yymmdd encoded date and msecs
   *@param yymmdd The encoded date
   *@param msecs Millis since midnight
   *@return The number of millis since 1970 per GregorianCalendar
   */
  public synchronized static long toGregorian2(int yymmdd, int msecs) {
    if(gstat == null) {
      gstat = new GregorianCalendar();
      gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }
    int yr = yymmdd/10000;
    if(yr < 100) yr = yr + 2000;
    yymmdd = yymmdd-yr*10000;
    int mon = yymmdd/100;
    int day = yymmdd % 100;
    int hr = msecs /3600000;
    msecs = msecs - hr*3600000;
    int min = msecs / 60000;
    msecs = msecs - min * 60000;
    int secs = msecs/1000;
    msecs =msecs - secs*1000;
    if(yr < 2000 || yr > 2030 || mon<=0 || mon > 12 || hr<0 || hr >23 || min <0 || min >59
        || secs < 0 || secs > 59 || msecs < 0 || msecs > 999) {
      throw new RuntimeException("toGregorian data out of range yr="+yr+
          " mon="+mon+" day="+day+" "+hr+":"+min+":"+secs+"."+msecs);
    }
    gstat.set(yr, mon-1, day, hr, min,secs);
    gstat.add(Calendar.MILLISECOND, msecs);
    return gstat.getTimeInMillis();
  }
  public synchronized static GregorianCalendar toGregorian(int yymmdd, int msecs) 
  {
    int yr = yymmdd/10000;
    if(yr < 100) yr = yr + 2000;
    yymmdd = yymmdd-yr*10000;
    int mon = yymmdd/100;
    int day = yymmdd % 100;
    int hr = msecs /3600000;
    msecs = msecs - hr*3600000;
    int min = msecs / 60000;
    msecs = msecs - min * 60000;
    int secs = msecs/1000;
    msecs =msecs - secs*1000;
    if(yr < 2000 || yr > 2030 || mon<=0 || mon > 12 || hr<0 || hr >23 || min <0 || min >59
        || secs < 0 || secs > 59 || msecs < 0 || msecs > 999) {
      throw new RuntimeException("toGregorian data out of range yr="+yr+
          " mon="+mon+" day="+day+" "+hr+":"+min+":"+secs+"."+msecs);
    }
    GregorianCalendar now = new GregorianCalendar();
    now.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    now.set(yr, mon-1, day, hr, min,secs);
    now.add(Calendar.MILLISECOND, msecs);
    //Util.prt(yr+"/"+mon+"/"+day+" "+hr+":"+min+":"+secs+"."+msecs+" "+Util.asctime(now)+" "+Util.ascdate(now));
    return now;
  }
  /** given a gregorian calendar return a date encoded yymmdd
   *@param d The gregoriancalendar to convert
   *@return The yymmdd encoded date
   */
  public static int yymmddFromGregorian(GregorianCalendar d) {
    return d.get(Calendar.YEAR)*10000+(d.get(Calendar.MONTH)+1)*100+d.get(Calendar.DAY_OF_MONTH);
  }
  /** given a gregorian calendar return a millis since midngith
   *@param d The gregoriancalendar to convert
   *@return The millis since midnight
   */
  
  public static int msFromGregorian(GregorianCalendar d) {
      //Util.prt("timeinms="+d.getTimeInMillis());
      return (int) (d.getTimeInMillis() % 86400000L);
  }
  /** return a time string to the hundredths of second for current time
   *@return the time string hh:mm:ss.hh*/
  public static String asctime() {
    return asctime(new GregorianCalendar());
  }  
  /** return a time string to the hundredths of second from a GregorianCalendar
   *@param d A gregorian calendar to translate to time hh:mm:ss.hh
   *@return the time string hh:mm:ss.hh*/
  public static String asctime(GregorianCalendar d) {
    if(df == null) df= new DecimalFormat("00");
      return df.format(d.get(Calendar.HOUR_OF_DAY))+":"+df.format(d.get(Calendar.MINUTE))+":"+
        df.format(d.get(Calendar.SECOND))+
        "."+df.format((d.get(Calendar.MILLISECOND)+5)/10);
  }
  /** return a time string to the hundredths of second from a GregorianCalendar
   *@param ms A long with a ms from a GregorianCalendar etc
   *@return the time string hh:mm:ss.hh*/
  public static String asctime(long ms) {
    if(gstat == null) {
      gstat = new GregorianCalendar();
      gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }
    gstat.setTimeInMillis(ms);
    if(df == null) df= new DecimalFormat("00");
      return df.format(gstat.get(Calendar.HOUR_OF_DAY))+":"+df.format(gstat.get(Calendar.MINUTE))+":"+
        df.format(gstat.get(Calendar.SECOND))+
        "."+df.format((gstat.get(Calendar.MILLISECOND)+5)/10);
  }
  static DecimalFormat df;
  static DecimalFormat df3;
  /** return a time string to the millisecond from a GregorianCalendar
   *@param d A gregorian calendar to translate to time hh:mm:ss.mmm
   *@return the time string hh:mm:ss.mmm*/
  public static String asctime2(GregorianCalendar d) {
    if(df == null) df= new DecimalFormat("00");
    if(df3 == null)  df3=new DecimalFormat("000");
      return df.format(d.get(Calendar.HOUR_OF_DAY))+":"+df.format(d.get(Calendar.MINUTE))+":"+
        df.format(d.get(Calendar.SECOND))+
        "."+df3.format(d.get(Calendar.MILLISECOND));
  }
  /** return a time string to the millisecond from a GregorianCalendar
   *@param ms A milliseconds (1970 datum) to translate to time hh:mm:ss.mmm
   *@return the time string hh:mm:ss.mmm*/
  public static String asctime2(long ms) {
    if(gstat == null) {
      gstat = new GregorianCalendar();
      gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }
    gstat.setTimeInMillis(ms);
    if(df == null) df= new DecimalFormat("00");
    if(df3 == null)  df3=new DecimalFormat("000");
      return df.format(gstat.get(Calendar.HOUR_OF_DAY))+":"+df.format(gstat.get(Calendar.MINUTE))+":"+
        df.format(gstat.get(Calendar.SECOND))+
        "."+df3.format(gstat.get(Calendar.MILLISECOND));
  }
  /** give a ip address as 4 bytes convert it to a dotted string 
   *@param ip Four bytes with raw IP address
   *@param offset An offset in ip where the four raw bytes start
   *@return string of form nnn.nnn.nnn.nnn with leading zeros to fill out space
   */
  public static String stringFromIP(byte [] ip, int offset) {
    if(df == null) df= new DecimalFormat("00");
    if(df3 == null)  df3=new DecimalFormat("000");
    return df3.format(((int) ip[offset] & 0xff))+"."+
        df3.format(((int) ip[offset+1] & 0xff))+"."+
        df3.format(((int) ip[offset+2] & 0xff))+"."+
        df3.format(((int) ip[offset+3] & 0xff));
    
  }
  /*** return the current date as yyyy/mm/dd 
   *@return The current data */
  public static String ascdate() {
    return ascdate(new GregorianCalendar());
  }  
  /** return the current date as a yyyy_DDD string
   *@return YYYY_DDD of the current date */
  public static String toDOYString() {
    if(gstat == null) gstat=new GregorianCalendar();
    gstat.setTimeInMillis(System.currentTimeMillis());
    return toDOYString(gstat);
  }
  /** return a DOY formated string from a GregoianCalendar
   *@param gc The GregorianCalendar
   *@return string of form YYYY,DDD,HH:MM:SS */
  public static String toDOYString(GregorianCalendar gc) {
    return gc.get(Calendar.YEAR)+","+Util.leftPad(""+gc.get(Calendar.DAY_OF_YEAR),3).replaceAll(" ","0")+
        ","+Util.leftPad(""+gc.get(Calendar.HOUR_OF_DAY),2).replaceAll(" ","0")+":"+
        Util.leftPad(""+gc.get(Calendar.MINUTE),2).replaceAll(" ","0")+":"+
        Util.leftPad(""+gc.get(Calendar.SECOND),2).replaceAll(" ","0")+"."+
        Util.leftPad(""+gc.get(Calendar.MILLISECOND),3).replaceAll(" ","0");
  }
  /** return a DOY formated string from a TimeStamp
   *@param ts The time stamp
   *@return string of form YYYY,DDD,HH:MM:SS */
  public static String toDOYString(Timestamp ts) {
    if(gstat == null) {
      gstat = new GregorianCalendar();
      gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }
    gstat.setTimeInMillis(ts.getTime());
    return toDOYString(gstat);
  }
  /*** return the given GreogoianCalendar date as yyyy/mm/dd 
   *@param d A GregorianCalendar to translate
   *@return The current data */
  public static String ascdate(GregorianCalendar d) {
     if(df == null) df= new DecimalFormat("00");
      return d.get(Calendar.YEAR)+"/"+df.format(d.get(Calendar.MONTH)+1)+"/"+
      df.format(d.get(Calendar.DAY_OF_MONTH));
  }
  /*** return the given GreogoianCalendar date as yyyy/mm/dd 
   *@param ms A miliseconds value to translate (1970 or GregorianCalendar datum)
   *@return The current data */
  public static String ascdate(long ms) {
    if(gstat == null) {
      gstat = new GregorianCalendar();
      gstat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }
    gstat.setTimeInMillis(ms);
     if(df == null) df= new DecimalFormat("00");
      return gstat.get(Calendar.YEAR)+"/"+df.format(gstat.get(Calendar.MONTH)+1)+"/"+
      df.format(gstat.get(Calendar.DAY_OF_MONTH));
  }
  	/** return current date (based on system time) as an SQL Date 
     * @return the current time in SQL Time form*/
	public static Time time() {
    GregorianCalendar d = new GregorianCalendar();
//		Util.prt("Year="+d.get(Calendar.YEAR)+" mon="+d.get(Calendar.MONTH));
//		Util.prt("d="+d.toString());
    return new Time(d.getTime().getTime());
  }
  
	/** return current date (based on system time) as an SQL Date
   *@return The curent time/date as a Timestamp
   */
	public static Timestamp now() {
    GregorianCalendar d = new GregorianCalendar();
//		Util.prt("Year="+d.get(Calendar.YEAR)+" mon="+d.get(Calendar.MONTH));
//		Util.prt("d="+d.toString());
//    Util.prt("time in millis="+d.getTimeInMillis());
    return new Timestamp(d.getTimeInMillis());
  }
  /** get time in millis (this is the same as System.currentTimeInMillis()
   *@return THe current time in millis 
   */
  public static long getTimeInMillis() {
    return new GregorianCalendar().getTimeInMillis();
  }
  
  /** dateToString takes a JDBC date and makes it a mm/dd string 
   *@param d ate to translate to string
   *@return The ascii string in yyyy/mm/dd*/
  public static String dateToString(java.sql.Date d) {
    if(d == null) return "";
    String s= d.toString();    // returns yyyy-mm-dd
    if(s == null)
    if(s.equals("null")) return "";
//    Util.prt("datetostring="+s);
    StringTokenizer tk = new StringTokenizer(s,"-");

    int yr = Integer.parseInt(tk.nextToken());
    int mon= Integer.parseInt(tk.nextToken());
    int day  = Integer.parseInt(tk.nextToken());
    return ""+mon+"/"+day+"/"+yr;
    
  }
	
	/** return the current system time as an SQL Timestamp. 
   *@return the current time as a Timestamp*/
	public static Timestamp TimestampNow() {
    java.util.Date now = new java.util.Date();
    return  new Timestamp(now.getTime());
	}

  
  /** Convert a mm/dd string to a full SQL Date 
   * @param s string to decode to a sql date yyyy/mm/dd
   * @return The sql date from the string
   */
  public static java.sql.Date stringToDate(String s) {
    StringTokenizer tk = new StringTokenizer(s,"/");
    if(tk.countTokens() <2) {
      Util.prt("stringToDate no enough Tokens s="+s+" cnt="+
        tk.countTokens());
      return Util.date(1970,1,1);
    }
    String mon = tk.nextToken();
    String day = tk.nextToken();
    String yr;
    int m,d,y;
    if(tk.hasMoreTokens()) {
      yr = tk.nextToken();
    } else yr = ""+Util.year();
    try {
      m = Integer.parseInt(mon);
      d = Integer.parseInt(day);
      y = Integer.parseInt(yr);
    } catch( NumberFormatException e) {
      Util.prt("dateToString() Month or day not a int mon="+mon+ " day="+day);
      return Util.date(Util.year(),1,1);
    }
    if(m <= 0 || m >12) {
      Util.prt("stringToDate : bad month = " + mon + " s="+ s);
      return Util.date(1970,1,1);
    }
    if(d <=0 || d > 31) {
      Util.prt("stringToDate : bad day = " + day + " s="+s);
      return Util.date(1970,1,1);
    }
    if(y < 100) {
        if(y > 80) y+=1900;
        else y+=2000;
    }
 
    return Util.date(y,m,d);
  }
  
  /** convert a year and day of year to an array in yr,mon,day order
 * @param yr The year
 * @param doy  The day of the year
 * @return an array in yr, mon, day
   *@throws RuntimeException if its mis formatted
 */
public static  int [] ymd_from_doy(int yr, int doy) throws RuntimeException
{	int [] daytab = new int[] {0,31,28,31,30,31,30,31,31,30,31,30,31};
  int [] dayleap = new int[]{0,31,29,31,30,31,30,31,31,30,31,30,31};
  int j;
	int sum;
  if(yr >= 60 && yr < 100) yr = yr+1900;
  else if( yr <60 && yr >=0) yr = yr+2000;
	boolean leap= yr%4 == 0 && yr%100 != 0 || yr%400 == 0;	/* is it a leap year */
	sum=0;
  int [] ymd = new int[3];
  ymd[0]=yr;
  if(leap) {
    for(j=1; j<=12; j++) {
      if(sum < doy && sum+dayleap[j] >= doy) {
        ymd[1]=j;
        ymd[2]=doy-sum;
        return ymd;
      }
      sum += dayleap[j];
    }
  }
  else {
    for(j=1; j<=12; j++) {
      if(sum < doy && sum+daytab[j] >= doy) {
        ymd[1]=j;
        ymd[2]=doy-sum;
        return ymd;
      }
      sum += daytab[j];
    }
  }
  System.out.println("ymd_from_doy: impossible drop through!   yr="+yr+" doy="+doy);
  throw new RuntimeException("ymd_from_DOY : impossible yr="+yr+" doy="+doy);

}

  
   /** Convert a mm/dd string to a full Date Of the form mm/dd/yyyy hh:mm:ss or yyyy,doy hh:mm:ss
    *@param s The string to encode
    *@return The java.util.Date representing the string or a date in 1970 if the string is bad.
    */
  public static java.util.Date stringToDate2(String s) {
    StringTokenizer tk=null;
    String yr=null;
    String mon=null;
    String day=null;
    if(s.indexOf(",") > 0) {  // must be yyyy,doy format
      tk = new StringTokenizer(s,", -:.");
      if(tk.countTokens() == 2) {
        
      }
      else if(tk.countTokens() < 4) {
        Util.prt("StringToDate2 not enough tokens for doy form s="+s+" cnt="+tk.countTokens());
        return Util.date(1970,1,1);
      }
      yr = tk.nextToken();
      int doy = Integer.parseInt(tk.nextToken());
      int [] ymd = ymd_from_doy(Integer.parseInt(yr), doy);
      yr = ""+ymd[0];
      mon = ""+ymd[1];
      day = ""+ymd[2];
    }
    else {
      tk = new StringTokenizer(s,"/ -:.");
      if(tk.countTokens() <5 && tk.countTokens() != 3) {
        Util.prt("stringToDate no enough Tokens s="+s+" cnt="+
          tk.countTokens());
        return Util.date(1970,1,1);
      }
      yr = tk.nextToken();
      mon = tk.nextToken();
      day = tk.nextToken();
    }
    String hr="00"; 
    String min="00";
    String sec="00";
    String frac="0";
    int m,d,y,h,mn;
    int sc,ms;
    if(tk.hasMoreTokens()) {
      hr = tk.nextToken();
    }
    if(tk.hasMoreTokens()) {
      min = tk.nextToken();
    }
    if(tk.hasMoreTokens()) {
      sec = tk.nextToken();
    }
    if(tk.hasMoreTokens()) {
      frac = tk.nextToken();
      
    }
    try {
      m = Integer.parseInt(mon);
      d = Integer.parseInt(day);
      y = Integer.parseInt(yr);
      h = Integer.parseInt(hr);
      mn = Integer.parseInt(min);
      sc = Integer.parseInt(sec);
      ms = Integer.parseInt(frac);
      if(frac.length() == 1) ms = ms*100;
      if(frac.length() == 2) ms = ms*10;
      if(frac.length() == 4) ms = ms/10;
      if(frac.length() == 5) ms = ms/100;
      if(frac.length() == 6) ms = ms/1000;
    } catch( NumberFormatException e) {
      Util.prt("dateToString2() fail to decode ints s="+s);
      return Util.date(1970,1,1,0,0,0);
    }
    if(m <= 0 || m >12) {
      Util.prt("stringToDate : bad month = " + mon + " s="+ s);
      return Util.date(1970,1,1,0,0,0);
    }
    if(d <=0 || d > 31) {
      Util.prt("stringToDate : bad day = " + day + " s="+s);
      return Util.date(1970,1,1,0,0,0);
    }
    if(y < 100) {
        if(y > 80) y+=1900;
        else y+=2000;
    }
    if(h < 0 || h > 23) {
      Util.prt("stringToDate2 : bad hour = "+hr+" s="+s);
      return Util.date(1970,1,1,0,0,0);
    }
    if(mn < 0 || mn > 59) {
      Util.prt("stringToDate2 : bad min = "+mn+" s="+s);
      return Util.date(1970,1,1,0,0,0);
    }
    if(sc < 0 ||sc > 59) {
      Util.prt("stringToDate2 : bad sec = "+sc+" s="+s);
      return Util.date(1970,1,1,0,0,0);
    }
 
    java.util.Date dd = Util.date(y,m,d, h,mn,sc);
    if(ms != 0) dd.setTime(dd.getTime()+ms);
    return dd;
  }
   
  
	/** a quick hack to return the current year 
   *@return the current year
   */
  public static int year() {
    GregorianCalendar g = new GregorianCalendar();
    return g.get(Calendar.YEAR);
  }
	
      
  /** Left pad a string s to Width.  
   *@param s The string to pad
   *@param width The desired width
   *@return The padded string to width
  */
  public static String leftPad(String s, int width) {
    String tmp="";
    int npad = width - s.length();
    if( npad < 0) tmp = s.substring(0 ,width);
    else if( npad == 0) tmp = s;
    else {
      for (int i = 0; i < npad; i++) tmp += " ";
      tmp += s;
    }
    return tmp;
  }
	
	/** pad on right side of string to width.  Used to create "fixed field" 
	 *lines 
      *@param s The string to pad
   *@param width The desired width
   *@return The padded string to width
  */
  public static String rightPad(String s, int width) {
    String tmp = "";
    int npad = width - s.length();
    if(npad < 0) tmp = s.substring(0,width);
    else if(npad == 0) tmp = s;
    else {
      tmp = s;
      for (int i = 0; i < npad; i++) tmp += " ";
    }
    return tmp;
  }

		/** Pad both sides of a string to width Width so String is "centered" in 
		 *field 
        *@param s The string to pad
   *@param width The desired width
   *@return The padded string to width
  */
  public static String centerPad(String s, int width) {
    String tmp = "";
    int npad = width - s.length();
    if(npad < 0) tmp = s.substring(0,width);
    else if(npad == 0) tmp = s;
    else {
      for(int i = 0; i< npad/2; i++) tmp += " ";
      tmp += s;
      for (int i = tmp.length(); i < width; i++) tmp += " ";
    }
    return tmp;
  }
    
	/** Exit using System.exit() after printing the "in" string.  Use so its
	 *easier on Post-mortem to see where exit occured. 
   *@param in a string to print before exiting.*/
  public static void exit(String in)
  { Util.prt(in);
    System.exit(0);
  }
  
  static public String propfilename;
  static Properties prop,defprops;
  /** return the properties
   *@return The Properties
   */
  public static Properties getProperties() { return prop;}
  /** set a propter pere key value pair
   *@param tag The key in the property
   *@param val The value to set it to
   */
  public static void setProperty(String tag, String val) { 
    if(debug_flag) Util.prt("set prop "+tag+" to "+val); 
    prop.setProperty(tag,val);
  }
  
  /** Because this is run before the Util.prt out variable is set, it cannot use prt()
   * load the poperties from a file
   *@param filename The file to load from */
  public static void loadProperties(String filename) {
    if(filename.equals("")) return;
    if(userhome == null) {init(filename); return;}    // note: init will call this routine so we need to end recursion here
    if(debug_flag) System.out.println(" # default props="+defprops.size());
    if(prop == null) prop=new Properties(defprops);
    if(debug_flag) System.out.println(" prop after="+prop.size());
    if(OS.indexOf("Windows") >= 0) propfilename=userhome+"\\"+filename;
    else if (OS.indexOf("SunOS") >= 0) propfilename = userhome+"/"+filename;
    else if (OS.indexOf("Mac") >=0) propfilename=userhome+"/"+filename;
    else if (OS.indexOf("Linux") >=0) propfilename=userhome+"/"+filename;
    else {
      System.out.println("Unknown OS ="+OS+" userhome="+userhome);
      propfilename=userhome+"/"+filename;
    }
    if(debug_flag) 
      System.out.println("Load properties from "+propfilename+" procname="+getProcess());
    try {
      FileInputStream i = new FileInputStream(propfilename);
      prop.load(i);
      //prtProperties();
      i.close();
    } catch(FileNotFoundException e) {
      System.out.println("Properties file not found="+propfilename+" userhome="+userhome+" "+System.getProperty("user.home"));
      saveProperties();
    }
    catch (IOException e) {
      System.out.println("IOException reading properties file="+propfilename);
      exit("Cannot load properties");
    }
  }
  /** print out the current Property pairs
   */
  public static void prtProperties(){
    if(out == null) prop.list(System.out);
    else prop.list(out);
  }
  /** return the value of a given property
   *@param key The name/key of the property
   *@return the value associated with the key
   */
  public static String getProperty(String key) {
    return prop.getProperty(key);
  }
  /** save the properties to a file */
  public static void saveProperties() {
    if(debug_flag) 
      System.out.println(Util.asctime()+" Saving properties to "+propfilename+" nkeys="+prop.size());
    try {
      FileOutputStream o = new FileOutputStream(propfilename);
      prop.store(o,propfilename+" by "+getProcess()+" via Util.saveProperties() cp="+
          System.getProperties().getProperty("java.class.path"));
      o.close();
    }
    catch (FileNotFoundException e) {
      System.out.println("Could not write properties to "+propfilename);
      exit("Cannot write Properties");
    }
    catch (IOException e) {
      System.out.println("Write error on properties to "+propfilename);
      exit("Cannot write properties");
    }
  }
  /** conveniently get the value assoicated with the MySQLServer key
   *@return The value of the MySQLServer key
   */
  public static String getMySQLServer() {return prop.getProperty("MySQLServer");}
  /** add a default property
   *@param tag The key of the property
   *@param value The value of the default property
   */
  public static void addDefaultProperty(String tag, String value) {
    if(defprops == null) defprops = new Properties();
    defprops.setProperty(tag,value);
  }
  /** a simple assert routine - prints if string are not equal
   *@param tag Some text to print if assert fails
   *@param s1 A string which is tested against s2
   *@param s2 A string to test
   */
  public static  void assertEquals(String tag,String s1,String s2) {
    if(s1.equals(s2)) Util.prt("ASSERT: "+tag+" is o.k.");
    else {
      Util.prt(tag+"ASSERT: fails "+s1);
      Util.prt(tag+"Assert  !=    "+s2);
    }
  }
  /** Creates a new instance of Subprocess 
   @param cmd A command string to process
   *@return An array of strings of parsed tokens.
   *@throws IOException If reading stderr or stdout gives an error
   */
  public static String [] parseCommand(String cmd) throws IOException {

    // Start using bash , -c means use next command as start of line
    String [] cmdline = new String[20];
    int pos = cmd.indexOf(">");
    if(pos >= 0) cmd = cmd.substring(0, pos);
    
    // Break this up into the command elements in quotes
    StringTokenizer tk = new StringTokenizer(cmd,"\"\'");
    int i=0;
    int narg=0;
    while(tk.hasMoreTokens()) {
      if(i%2 ==0) {
        String [] args=tk.nextToken().split("\\s");
        for(int j=0; j<args.length; j++) cmdline[narg++]=args[j];
      }
      else cmdline[narg++]=tk.nextToken();     // this is a quoted string
      i++;
    }
    String [] finalcmd = new String[narg];
    for(i=0; i<narg; i++) {
      prt(i+"="+cmdline[i]);
      finalcmd[i]=cmdline[i];
    }
    return finalcmd;
  }
  /** Print a string in all printable characers, take non-printable to their hex vales
   *@param s The string to print after conversion
   *@return The String with non-printables converted
   */
  public static String toAllPrintable(String s) {
    byte [] b = s.getBytes();
    StringBuilder sb = new StringBuilder(s.length());
    for(int i=0; i<b.length; i++) 
      if(b[i] <32 || b[i] == 127) sb.append(Util.toHex(b[i]));
      else sb.append(s.charAt(i));
    return sb.toString();
  }
	/* Test the latest things added.  Many test functions were removed as the
	 *debugging was completed.
	 
  public  static  void  main3  (String  []  args)  {
    JDBConnection  jcjbl;
    Connection  C;
    User user=new User("dkt");
    try  {
      jcjbl  =  new  JDBConnection(UC.JDBCDriver(),  UC.JDBCDatabase());
      C  =  jcjbl.getConnection();
      UC.setConnection(C);
      
      Util.setApplet(true);
      user=new User(C,"dkt","karen");
    } catch (JCJBLBadPassword e) {
      System.err.println("Password must be wrong on User construction");
    } catch  (SQLException  e)  {
      Util.SQLErrorPrint(e," Main SQL unhandled=");
      System.err.println("SQLException  on  getting test $DBObject");
    }

    GregorianCalendar g = new GregorianCalendar();
    Util.prt("asctime="+asctime(g)+" ascdate="+ascdate(g));
    Time t = new Time((long) 12*3600000+11*60000+13000);
    String s = Util.timeToString(t);
    Util.prt(t.toString()+" timeToString returned=" + s);
    t = new Time((long) 11*3600000+11*60000+13000);
    s = Util.timeToString(t);
    Util.prt(t.toString()+" timeToString returned=" + s);
    t = new Time(15*3600000+11*60000+13000);
    s = Util.timeToString(t);
    Util.prt(t.toString()+" timeToString returned=" + s);
   
    s="10:31 am";
    t = Util.stringToTime(s);
    Util.prt(s+" from string returned " + t.toString());
    s="10:32 pm";
    t = Util.stringToTime(s);
    Util.prt(s+ " from string returned " + t.toString());
    s="10:31";
    t = Util.stringToTime(s);
    Util.prt(s+" from string returned " + t.toString());
    s="3:30";
    t = Util.stringToTime(s);
    Util.prt(s+" from string returned " + t.toString());
    s="a3:30";
    t = Util.stringToTime(s);
    Util.prt(s+" from string returned " + t.toString());
    s="3:3d";
    t = Util.stringToTime(s);
    Util.prt(s+" from string returned " + t.toString());
    s="3:30 ap";
    t = Util.stringToTime(s);
    Util.prt(s+" from string returned " + t.toString());
    
    Date d = Util.date(2000,3,1);
    Util.prt("2000,3,1 gave "+ Util.dateToString(d));
    d=Util.date(2000,12,30);
    Util.prt("2000,12,30 gave "+ Util.dateToString(d));

    s = "1/1";
    d=Util.stringToDate(s);
    Util.prt(s + " returned " + d.toString());
     s = "12/31";
    d=Util.stringToDate(s);
    Util.prt(s + " returned " + d.toString());
    s = "1/1/2000";
    d=Util.stringToDate(s);
    Util.prt(s + " returned " + d.toString());
    s = "09/1/00";
    d=Util.stringToDate(s);
    Util.prt(s + " returned " + d.toString());
    s = "01/12/99";
    d=Util.stringToDate(s);
    Util.prt(s + " returned " + d.toString());
    s = "13/1";
    d=Util.stringToDate(s);
    Util.prt(s + " returned " + d.toString());
    
    s = "12/32";
    d=Util.stringToDate(s);
    Util.prt(s + " returned " + d.toString());
    Util.prt(""+stringToDate2("2006/1/1 12:00"));
    Util.prt(""+stringToDate2("6/1/1-12:01"));
    Util.prt(""+stringToDate2("2006,104 12:00"));
    Util.prt(""+stringToDate2("6,104-12:01:02"));
    
  }*/
  /** convert to hex string
   *@param b The item to convert to hex 
   *@return The hex string */
  public static String toHex(byte b) {return toHex(((long) b) & 0xFFL);}
  /** convert to hex string
   *@param b The item to convert to hex 
   *@return The hex string */
  public static String toHex(short b) {return  toHex(((long) b) & 0xFFFFL); }
  /** convert to hex string
   *@param b The item to convert to hex 
   *@return The hex string */
  public static String toHex(int b) {return toHex(((long) b) & 0xFFFFFFFFL); }
  /** convert to hex string
   *@param i The item to convert to hex 
   *@return The hex string */
  public static String toHex(long i) {
    StringBuilder s = new StringBuilder(16);
    int j = 60;
    int k;
    long val;
    char c;
    boolean flag = false;
    s.append("0x");
   
    for(k=0; k<16; k++) {
      val = (i >> j) & 0xf;
      //prt(i+" i >> j="+j+" 0xF="+val);
      if(val < 10) c = (char) (val + '0');
      else c = (char) (val -10 + 'a');
      if(c != '0') flag = true;
      if(flag) s.append( c );
      j = j - 4;
    }
    if( ! flag) s.append("0");
    return s.toString();
  }
    /** static method that insures a seedname makes some sense.
   * 1)  Name is 12 characters long nnssssscccll.
   * 2) All characters are characters,  digits, spaces, question marks or dashes
   * 3) Network code contain blanks
   * 4) Station code must be at least 3 characters long
   * 5) Channel codes must be characters in first two places
   *@param name A seed string to check
     *@return True if seename passes tests.
   */
  public static boolean isValidSeedName(String name) {
    if(name.length() != 12 ) return false;
    
    char ch;
    //char [] ch = name.toCharArray();
    for(int i=0; i<12; i++) {
      ch = name.charAt(i);
      if( !(Character.isLetterOrDigit(ch) || ch == ' ' || ch == '?' || ch == '_' ||
              ch == '-')) 
        return false;
    }
    if(name.charAt(0) == ' ' /*|| name.charAt(1) == ' '*/) return false;
    if(name.charAt(2) == ' ' || name.charAt(3) == ' ' || name.charAt(4) == ' ') return false;
    if( !(Character.isLetter(name.charAt(7)) && Character.isLetter(name.charAt(8)) &&
        Character.isLetterOrDigit(name.charAt(9)))) return false;
       
    return true;  
  }
  /* Test routine
    /* @param args the command line arguments
  */
  public static void main(String[] args) {
    init("edge.prop");
    java.util.Date dd = Util.stringToDate2("2006/10/10 12:34:56.789");
    Util.prt(dd.toString()+" ms="+(dd.getTime()%1000));
    String [] role = Util.getRoles(null);
    byte [] b = new byte[12];
    String s;
    b[0] =0;
    b[1] = 'a';
    b[2] = 2;
    b[3] = 'b';
    b[4] = 127;
    b[5] = -2;
    b[6] = 'c';
    b[7] = 'd';
    b[8] = 'A';
    b[9] = 'B';
    b[10] = 126;
    b[11] = 1;
    s=new String(b);
    Util.prt(Util.toAllPrintable(s)+" should be "+"0x0a0x2b0x7f0xfecdAB~0x1");
    for(int i=0; i<128; i=i+12) {
      for(int j=i; j<i+12; j++) b[j-i]= (byte) j;
      s = new String(b);
      Util.prt("i="+i+" "+Util.toAllPrintable(s));
    }
    for(int i=-127; i<0; i=i+12) {
      for(int j=i; j<i+12; j++) b[j-i]= (byte) j;
      s = new String(b);
      Util.prt("i="+i+" "+Util.toAllPrintable(s));
    }
    
    Util.loadProperties("anss.prop");
    lpt("This is a test message for printing!\nIt is two lines of text\n");
    lpt("This is a third line");
    lptSpool();
  
  }
  /** This returns the full short computer node for adding to the file name, the first time it
   * actually runs the uname -n and parses, it sets the static variable node and that
   * will be returned on all successive calls.
   *@return a tag with the edge node number the node name up to the first "." like "edge5"
   */
  public static String getNode() {
    if(node != null) return node;
        if(OS.indexOf("Windows") >= 0) {
      node = Util.getProperty("WindowsNode");
      if(node == null) node="windows0";
      return node;
    }

    try {
      Subprocess sp = new Subprocess("uname -n"); 
      sp.waitFor();
      String s = sp.getOutput();      // remember this might have stuff after the end
      //Util.prt("getNode uname -n returns ="+s);
      s.trim();
      //if(dbg) Util.prta("getNode() uname -n returned="+s+" len="+s.length());
      int dot = s.indexOf(".");             // see if this is like edge3.cr.usgs.gov
      if(dot > 0) s=s.substring(0, dot);    // Trim off to the first dot if any
      s.replaceAll("\n","");
      node = s.trim();
      return s.trim();

    }
    catch (IOException e) {
      Util.prt("Cannot run uname -n e="+e);
      try {
        node = InetAddress.getLocalHost().getHostName();
        node = node.trim();
        Util.prt("Use InetAddress.getLocalHost().getHostname() + "+node);
        return node;
      }
      catch(Exception e2) {Util.prt("Could not get IndetAddress.getLocalHost() e="+e);}
      e.printStackTrace();
      
      //SendEvent.edgeSMEEvent("BadUnameCall", "Could not call uname from "+node+"/"+process, "Util");
      //System.exit(0);
    } 
    catch (InterruptedException e) {
      Util.prt("uname -n interrupted!");
      System.exit(0);
    }
    return "---";
  }  
  public static String cleanIP(String ip) {
    for(int i=0; i<2; i++) {
      if(ip.substring(0,1).equals("0")) ip = ip.substring(1);
      ip=ip.replaceAll("\\.0", ".");
    }
    ip = ip.replaceAll("\\.\\.",".0.");
    return ip;
  }
  /*public static void main2(String[] args) {
    int i = 0x12345678;
    prt(i+" is hex "+toHex(i));
    i = 0xFFFFFFFF;
    prt(i+" is hex "+toHex(i));
    i = 0;
    prt(i+" is hex "+toHex(i));
    i = 4096;
    prt(i+" is hex "+toHex(i));
     long l = 0xabcdef0123456789l;
    prt(l+" is hex "+toHex(l));
    System.exit(0);
 }*/
  /** read fully the number of bytes, or throw exception
   *@param in The InputStream to read from
   *@param buf The byte buffer to receive the data
   *@param off The offset into the buffer to start the read
   *@param len Then desired # of bytes
   * @return The length of the read in bytes, zero if EOF is reached
   * @throws IOException if one is thrown by the InputStream
   */
  public static int readFully(InputStream in, byte [] buf, int off, int len) throws IOException {
    int nchar=0;
    int l=off;
    while(len > 0) {            //
      //while(in.available() <= 0) try{Thread.sleep(10);} catch(InterruptedException e) {}
      nchar= in.read(buf, l, len);// get nchar
      if(nchar <= 0) {
        //Util.prta(len+" RF read nchar="+nchar+" len="+len+" in.avail="+in.available());
        return 0;
      }     // EOF - close up
      l += nchar;               // update the offset
      len -= nchar;             // reduce the number left to read
    }
    return l;
  }

  /** read fully the number of bytes, or throw exception.  Suitable for sockets since the read method
   * uses a lot of CPU if you just call it.  This checks to make sure there is data before attemping the read.
   *@param in The InputStream to read from
   *@param buf The byte buffer to receive the data
   *@param off The offset into the buffer to start the read
   *@param len Then desired # of bytes
   * @return The length of the read in bytes, zero if EOF is reached, - bytes read if EOF came before all bytes were read
   * @throws IOException if one is thrown by the InputStream
   */
  static long countErr=0;
  public static int socketReadFully(InputStream in, byte [] buf, int off, int len) throws IOException {
    int nchar=0;
    int l=off;
    while(len > 0) {            //
      nchar= in.read(buf, l, len);// get nchar
      if(nchar <= 0) {
        Util.prta(len+" SRF read nchar="+nchar+" len="+len+" off="+off+" in.avail="+in.available());
        if(countErr++ % 1000 == 999) new RuntimeException("SRF EOF called 1000 times "+countErr).printStackTrace();
        return (l == off? 0 : off -l);  // negative bytes read
      }     // EOF - close up
      l += nchar;               // update the offset
      len -= nchar;             // reduce the number left to read
    }
    return l-off;
  }  /** read up to the number of bytes, or throw exception.  Suitable for sockets since the read method
   * uses a lot of CPU if you just call it.  This checks to make sure there is data before attemping the read.
   *@param in The InputStream to read from
   *@param buf The byte buffer to receive the data
   *@param off The offset into the buffer to start the read
   *@param len Then desired # of bytes
   * @return The length of the read in bytes, zero if EOF is reached
   * @throws IOException if one is thrown by the InputStream
   */
  public static int socketRead(InputStream in, byte [] buf, int off, int len) throws IOException {
    int nchar=0;
    int loop=0;
    nchar= in.read(buf, off, len);// get nchar
    if(nchar <= 0) {
      Util.prta(len+" SR read nchar="+nchar+" len="+len+" in.avail="+in.available());
      return 0;
    }
    return nchar;
  }
   
   /** read fully a single byte, or throw exception.  Suitable for sockets since the read method
   * uses a lot of CPU if you just call it.  This checks to make sure there is data before attemping the read,
    * and sleeps if no data is available
   *@param in The InputStream to read from
    * @return the single byte read as a int
    * @throws IOException if one is found
  */

  public static int socketRead(InputStream in) throws IOException {
    return in.read();
  }
}


