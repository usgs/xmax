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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usgs.anss.cd11;

import java.nio.ByteBuffer;
import java.util.GregorianCalendar;
/** This class does I/O and encodeing and decodeing of CD1.1 frames.  It was implented 
 * using version 0.3 of the document dated 18 Dec 2002.  References in here should be
 * to that document.  It can be use to load data for specific frame formats for output, but
 * is mostly used for decoding incoming frames.
 *
 * @author davidketchum
 */
class CD11Frame {
  private static final int TYPE_DATA=5;
  private static final int TYPE_CD1_ENCAPSULATION=13;
  private static final String [] typeString ={"zero","ConnRqst","ConnResp","OptRqst","OptResp",
      "Data","Ack","Alert","CmdRqst","CmdResp","Ten","Eleven","Twelve","CD1Encap"};
  // the header portion of the frame
  private int type;
  private int trailerOffset;
  private byte [] creator;
  private byte [] destination;
  private long seq;
  private int series;             // The sequence series for the frame
  // The trailer portion of the frame
  private int authID;             // Authentication identifier
  private int authSize;           // size of authentication body
  private byte [] authBody;       // the authorization body, if null, no authentication
  private long commVerification;  // This is essentially the CRC
  private long commComputed;      // This is the computed CRC from when the fram was read.
  // the frame body
  private byte [] body;
  private int lenBody;
  private ByteBuffer bbody;
  private long outSeq;             // Output Sequence number
  private byte [] frameSet;        // creator:destination padded to 20 character
  //(used be getOutputBytes to know to do data frame processing)
  // Data frame related fields
  private int dataNChan;
  private int dataFrameMS;    // Data frame length in milliseconds
  private String nominalTime; // The nominal time for all of the data
  private StringBuilder channelString = new StringBuilder(100);
  /** Return number of data channels in DataFrame
   * 
   * @return # of channels
   */
  public int getDataNchan() {return dataNChan;}
  public int getDataFrameMS() {return dataFrameMS;}
  public String getDataNominalTime() {return nominalTime;}
  public StringBuilder getChannelString() {return channelString;}
  private boolean crcOK() {return commVerification == commComputed;}
  public int getType() {return type;}
  public int getBodyLength() {return lenBody;}
  public byte [] getBody() {return body;}
  public ByteBuffer getBodyByteBuffer() {return bbody;}
  public byte [] getAuthBody() {return authBody;}
  public int getAuthSize() {return authSize;}
  public int getAuthID() {return authID;}
  public byte [] getCreator() {return creator;}
  public byte [] getDestination() {return destination;}
  public int getSeries() {return series;}
  public long getSeq() {return seq;}
  public long getOutSeq() {return outSeq;}
  public String getCreatorString() {return stringFrom(creator, 8);}
  public String getDestinationString() {return stringFrom(destination, 8);}
  public String getFrameSetString() {return stringFrom(frameSet,20);}
  
  /** CD1.1 uses null padded strings, but Java wants space padded for convenience.  This routine
   * will return a line from a byte array with zeros/null turned to spaces
   * @param buf Buffer with data to convert to a string
   * @param len Desired sring length
   * @return A string with spaces replacing the nulls
   */
  private static  String stringFrom(byte [] buf, int len) {
    for(int i=0; i<len; i++) if(buf[i] == 0) len = i;
     return new String(buf, 0, len).trim();
  }
  public void setType(int ty) {type=ty;}
  @Override
  public String toString() {return "CD11Frm:"+(type >=0 && type <=13?typeString[type]:"UnkwnType")+
      (type == TYPE_DATA || type == TYPE_CD1_ENCAPSULATION ? " "+nominalTime:"")+" toff="+trailerOffset+
      " "+stringFrom(creator,8)+":"+stringFrom(destination,8)+" sq="+series+"/"+seq+
      " bLen="+lenBody+" aid="+authID+" sz="+authSize+" CRC="+(crcOK()?"t":"f**");
  }

  
  /** given a 20 byte CD1.1 time string, convert it to a gregorian calendar
   * @param s The 20 byte CD1.1 time string yyyyddd_hh:mm:ss.mmm
   * @param g A gregorianCalendar to set to the time from s
   */
  static void fromCDTimeString(String s, GregorianCalendar g) {
    try {
      int year = Integer.parseInt(s.substring(0,4));
      int doy = Integer.parseInt(s.substring(4,7));
      int hr = Integer.parseInt(s.substring(8,10));
      int min = Integer.parseInt(s.substring(11,13));
      int sec = Integer.parseInt(s.substring(14,16));
      int ms = Integer.parseInt(s.substring(17,20));
      int [] ymd = SeedUtil.ymd_from_doy(year, doy);
      g.set(year, ymd[1]-1, ymd[2], hr, min, sec);
      g.setTimeInMillis(g.getTimeInMillis()/1000L*1000L+ms);
    }
    catch(NumberFormatException e) {
      g.setTimeInMillis(86400000L);
    }
    
  }
}
