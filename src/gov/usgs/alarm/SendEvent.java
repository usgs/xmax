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
 * SendEvent.java
 *
 * Created on July 20, 2007, 12:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gov.usgs.alarm;
import gov.usgs.anss.util.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.IOException;
/** This class allows and event to get sent to the alaram system.  It generally is called staticly.
 * 
 *
 * @author davidketchum
 */
public class SendEvent {

  static DatagramSocket out;
  static DatagramPacket dp;
  static byte[] outbuf;
  static ByteBuffer bf;
  static String eventHandlerIP=(Util.getProperty("AlarmIP") == null || (Util.getProperty("AlarmIP")+"").equals("") ?
    "136.177.24.74,136.177.24.65":Util.getProperty("AlarmIP"));
  static String [] ips;
  static int eventHandlerPort=7964;
  /** override the IP address to which Events are sent 
   *@param ip The IP address to use for events , if null do not change
   *@param port The port to use (if <= 0, do not change)
   */
  public static void setEventHandler(String ip, int port) {
    Util.prta("Bef setEventHandler ip="+eventHandlerIP+"/"+eventHandlerPort);
    if(ip != null) eventHandlerIP=ip; 
    if(port > 0) eventHandlerPort=port;
    Util.prta("Aft setEventHandler ip="+eventHandlerIP+"/"+eventHandlerPort);
    ips = eventHandlerIP.split(",");
  }
  /** Creates a new instance of SendEvent */
  public SendEvent() {
  }
  private static String getNodeRole() {
    String [] roles = Util.getRoles(Util.getNode());
    if(roles != null)return roles[0]+":"+Util.getNode();
    return "Null";
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param obj An object whose class name will be used as the process
   */
  public static void edgeEvent(String cd, String payload, Object obj) {
    sendEvent("Edge",cd,payload,getNodeRole(),obj.getClass().getSimpleName());
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param str An String to be used as the process
   */
  public static void edgeEvent(String cd, String payload, String str) {
    sendEvent("Edge",cd,payload,getNodeRole(),str);
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param obj An object whose class name will be used as the process
   */
  public static void debugEvent(String cd, String payload, Object obj) {
    debugEvent(cd,payload,obj.getClass().getSimpleName());
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param process An String to be used as the process
   */
  public static void debugEvent(String cd, String payload, String process) {
    if(out == null) setup();
    String src="Dbg";
    String node = getNodeRole();
    Util.prta("SendEvent: "+src.trim()+"-"+cd.trim()+" "+node.trim()+"/"+process.trim()+" "+payload.trim());
    for(int i=4; i<140; i++) outbuf[i]=0;
    bf.position(4);
    bf.put(process.getBytes(),0, Math.min(20, process.length()));
    bf.position(24);
    bf.put(src.getBytes(),0, Math.min(12, src.length()));
    bf.position(36);
    bf.put(cd.getBytes(),0, Math.min(12, cd.length()));
    bf.position(48);
    bf.put(payload.getBytes(),0, Math.min(80, payload.length()));
    bf.position(128);
    bf.put(node.getBytes(),0, Math.min(12, node.length()));
    try {
      dp.setAddress(InetAddress.getByName(ips[0].trim()));
      out.send(dp);
    }
    catch(IOException e) {
      Util.prt("UDP error sending a message");
    }
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param obj An object whose class name will be used as the process
   */
  public static void edgeSMEEvent(String cd, String payload, Object obj) {
    sendEvent("EdgeSME",cd,payload,getNodeRole(),obj.getClass().getSimpleName());
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param str An String to be used as the process
   */
  public static void edgeSMEEvent(String cd, String payload, String str) {
    sendEvent("EdgeSME",cd,payload,getNodeRole(),str);
  } /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param obj An object whose class name will be used as the process
   */
  public static void pageSMEEvent(String cd, String payload, Object obj) {
    sendEvent("PageSME",cd,payload,getNodeRole(),obj.getClass().getSimpleName());
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param str An String to be used as the process
   */
  public static void pageSMEEvent(String cd, String payload, String str) {
    sendEvent("PageSME",cd,payload,getNodeRole(),str);
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param obj An object whose class name will be used as the process
   */
  public static void debugSMEEvent(String cd, String payload, Object obj) {
    debugEvent(cd,payload,obj.getClass().getSimpleName());
  }
 /** send a UDP packet with an event the current eventHandler IP and port with Edge source and obj class process
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param process An String to be used as the process
   */
  public static void debugSMEEvent(String cd, String payload, String process) {
    if(out == null) setup();
    String src="DbgSME";
    String node = getNodeRole();
    Util.prta("SendEvent: "+src.trim()+"-"+cd.trim()+" "+node.trim()+"/"+process.trim()+" "+payload.trim());
    for(int i=4; i<140; i++) outbuf[i]=0;
    bf.position(4);
    bf.put(process.getBytes(),0, Math.min(20, process.length()));
    bf.position(24);
    bf.put(src.getBytes(),0, Math.min(12, src.length()));
    bf.position(36);
    bf.put(cd.getBytes(),0, Math.min(12, cd.length()));
    bf.position(48);
    bf.put(payload.getBytes(),0, Math.min(80, payload.length()));
    bf.position(128);
    bf.put(node.getBytes(),0, Math.min(12, node.length()));
    try {
      dp.setAddress(InetAddress.getByName(ips[0].trim()));
      out.send(dp);
    }
    catch(IOException e) {
      Util.prt("UDP error sending a message");
    }
  }
  private  synchronized static void setup() {
    try {
      Util.prta("SendEvent: Setup to  "+eventHandlerIP+"/"+eventHandlerPort);
      ips = eventHandlerIP.split(",");
      out = new DatagramSocket();
      outbuf = new byte[140];
      bf = ByteBuffer.wrap(outbuf);
      dp = new DatagramPacket(outbuf, 0, 140, InetAddress.getByName(ips[0].trim()), eventHandlerPort);
      outbuf[0]=(byte) 33; outbuf[1]=(byte) 3; outbuf[2]=(byte) 0; outbuf[3]=(byte) -55;
    }
    catch(UnknownHostException e) {
      Util.prt("SendEvent: Unknown host ="+e);
      return;
    }
    catch(IOException e) {
      Util.prt("SendEvent: cannot open datagram socket on this computer! e="+e);
      return;
    }    
  }
 /** send a UDP packet with an event the current eventHandler IP and port
   *@param src The 12 character source
   *@param cd The 12 character error code
   *@param payload Up to 80 characters of message payload
   *@param node Up to 12 character computer node name
   *@param process Up to 12 character process name
   */
  public synchronized static void sendEvent(String src, String cd, String payload, String node, String process) {
    if(out == null) setup();
     Util.prta("SendEvent: "+src.trim()+"-"+cd.trim()+" "+node.trim()+"/"+process.trim()+" "+payload.trim());
    // Put the data in the packet
    for(int i=4; i<140; i++) outbuf[i]=0;
    bf.position(4);
    bf.put(process.getBytes(),0, Math.min(20, process.length()));
    bf.position(24);
    bf.put(src.getBytes(),0, Math.min(12, src.length()));
    bf.position(36);
    bf.put(cd.getBytes(),0, Math.min(12, cd.length()));
    bf.position(48);
    bf.put(payload.getBytes(),0, Math.min(80, payload.length()));
    bf.position(128);
    bf.put(node.getBytes(),0, Math.min(12, node.length()));
    try {
      for(int i=0; i<ips.length; i++) {
        dp.setAddress(InetAddress.getByName(ips[i].trim()));
        out.send(dp);
      }
    }
    catch(IOException e) {
      Util.prt("UDP error sending a message");
    } 
  }    
  public static void doOutOfMemory(String msg, Object ths) {
    SendEvent.edgeEvent("OutOfMemory",msg+" Out of Memory in "+
            ths.getClass().getName()+" on "+Util.getNode()+"/"+Util.getAccount()+" "+Util.getProcess(),ths);
    SimpleSMTPThread.email(Util.getProperty("emailTo"),"Out of memory "+Util.getNode()+"/"+Util.getAccount()+" "+Util.getProcess(),
                Util.asctime()+" "+Util.ascdate()+" Out of Memory in "+ths.getClass().getName()+" on "+Util.getNode()+"/"+Util.getAccount()+" "+Util.getProcess()+"\n"+
                msg);
  }
}

