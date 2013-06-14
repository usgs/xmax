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


package gov.usgs.anss.cd11;
import java.nio.ByteBuffer;
import gov.usgs.anss.edgethread.EdgeThread;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import com.isti.traceview.data.ims.DAT2;

import gov.usgs.anss.util.Util;
import gov.usgs.alarm.SendEvent;
/** This class represents a CD1.1 channel subframe and has methods for reading that information
 *  from a positioned byte buffer.  The subframe is detailed in the manual as table 2.10.  This class
 * was implemented based on the Version 0.3 version of the manual dated 18 Dec 2002
 *
 * @author davidketchum
 */
public class ChannelSubframe {
	
  private static Logger lg = Logger.getLogger(ChannelSubframe.class);
  private int len;
  private int authOffset;
  private byte auth;
  private byte transform;
  private byte sensorType;
  private byte optionFlag;
  private String station;       // a SSSSSCCCLL name!
  private byte [] statbuf = new byte[10];
  private String uncompressedFormat;    // two characters
  private float calibFactor;
  private float calibPeriod;
  private String timeStamp;
  private byte [] timebuf = new byte[20];  //scratch space to get time
  private GregorianCalendar time =new GregorianCalendar();
  private int msLength;
  private int nsamp;
  private int statusSize;
  private byte [] status;
  private int dataSize;
  private byte []  data;
  private ByteBuffer bdata;
  private int subframeCount;
  private int authKeyID;
  private int authSize;
  private byte [] authBytes;
  public int getFrameLength() {return len;}
  public int getTransform() {return transform;}
  public byte getSensorType() {return sensorType;}
  /** get the station
   * 
   * @return a SSSSSCCCLL station name
   */
  public String getStation() {return station;}
  public String getUncompressedFormat() {return uncompressedFormat;}
  public String getCDTimeString() {return timeStamp;}
  public GregorianCalendar getGregorianTime() {return time;}
  public int getMSLength() {return msLength;}
  public int getNsamp() {return nsamp;}
  public int getStatusSize() {return statusSize;}
  public byte [] getStatusBytes() {return status;}
  public int getDataSize() {return dataSize;}
  public byte [] getDataBytes() {return data;}
  public int getSubframeCount() {return subframeCount;}
  public double getRate() {return nsamp/(msLength/1000.);}

  @Override
  public String toString() {return station+"  "+timeStamp+" "+Util.asctime2(time)+" #samp="+nsamp+" msLen="+msLength+
      " tfrm="+transform+" ucfrm="+uncompressedFormat+" sens="+sensorType+" auth="+auth+
      " #sta="+statusSize+" #data="+dataSize;}
  public ChannelSubframe(ByteBuffer b) {
    load(b);
  }
  /** Load this ChannelSubframe with data from byte buffer b starting at the current position of b
   * @param b A ByteBuffer position to the start of a ChannelSubframe
   */
  public void load(ByteBuffer b) {
    // save position of beginning - These fields are in table 10 pg 23 of manual
    int pos = b.position();
    len = b.getInt();
    authOffset = b.getInt();    // 
    auth=b.get();
    transform=b.get();
    sensorType = b.get();
    optionFlag = b.get();
    b.get(statbuf);     // get 10 bytes of station name
    for(int i=0; i<10; i++) if(statbuf[i] == 0) statbuf[i] = 32;
    station = new String(statbuf);
    if(station.substring(5,7).equals("sz")) station = station.substring(0,5)+"SHZ"+station.substring(8,10);
    if(station.substring(5,7).equals("sn")) station = station.substring(0,5)+"SHN"+station.substring(8,10);
    if(station.substring(5,7).equals("se")) station = station.substring(0,5)+"SHE"+station.substring(8,10);
    if(station.substring(5,7).equals("bz")) station = station.substring(0,5)+"BHZ"+station.substring(8,10);
    if(station.substring(5,7).equals("bn")) station = station.substring(0,5)+"BHN"+station.substring(8,10);
    if(station.substring(5,7).equals("be")) station = station.substring(0,5)+"BHE"+station.substring(8,10);
    /*
    if( !(station.substring(5,8).equals("BHZ") || station.substring(5,8).equals("BHN") ||station.substring(5,8).equals("BHE") ||
          station.substring(5,8).equals("BH1") || station.substring(5,8).equals("BH2") ||
          station.substring(5,8).equals("SHZ") || station.substring(5,8).equals("SHN") ||station.substring(5,8).equals("SHE") ||
          station.substring(5,8).equals("MHZ") || station.substring(5,8).equals("MHN") ||station.substring(5,8).equals("MHE") ||
          station.substring(5,8).equals("HHZ") || station.substring(5,8).equals("HHN") ||station.substring(5,8).equals("HHE") ||
          station.substring(5,8).equals("EHZ") || station.substring(5,8).equals("EHN") ||station.substring(5,8).equals("EHE") ||
          station.substring(5,8).equals("HNZ") || station.substring(5,8).equals("HNN") ||station.substring(5,8).equals("HNE") ||
          station.substring(5,8).equals("BNZ") || station.substring(5,8).equals("BNN") ||station.substring(5,8).equals("BNE") ||
          station.substring(5,8).equals("EHZ") ||
          station.substring(5,8).equals("EDH"))
          ) {
    	
    	lg.error("  ****** CD1.1 channel subframe : Got bad component name="+station+"|"+station.substring(5,8)+"|");
    }
    */
    b.get(timebuf, 0, 2);
    for(int i=0; i<2; i++) if(timebuf[i] == 0) timebuf[i] = 32;
    uncompressedFormat = new String(timebuf, 0, 2);
    calibFactor = b.getFloat();
    calibPeriod = b.getFloat();
    b.get(timebuf);
    timeStamp = new String(timebuf);
    CD11Frame.fromCDTimeString(timeStamp, time);
    msLength = b.getInt();
    nsamp = b.getInt();
    
    // Only the type 1 packet from table 4.22 makes any sense here
    statusSize=b.getInt();
    if(statusSize > 0) {
      try {
        if(status == null) status=new byte[statusSize];
        else if(status.length < statusSize) status = new byte[statusSize];
        b.get(status,0, statusSize);
        if(statusSize % 4 != 0) 
          b.position(b.position()+4-(statusSize%4));
      }
      catch(RuntimeException e) {
        lg.error("Runtime getting status sssize="+statusSize+" pos="+b.position()+" e="+e);
      }
      
    }
    // The datasize has to be at least 8 bytes bigger than the actual data. The uncompressor often gets a long
    // when it does not need all of it and you get buffer underflow if there are not enough bytes in the backing buffer
    dataSize = b.getInt();
    if(data == null) {data = new byte[dataSize+8];bdata = ByteBuffer.wrap(data);}
    else if(data.length < dataSize+8) {data = new byte[dataSize+8];bdata = ByteBuffer.wrap(data);}
    if(dataSize <= 0) 
    	lg.error("**** ChannelSubFrame: got a load datasize <= 0! datsize="+dataSize+" pos="+b.position()+" "+toString());
    else {
      try {
        
      b.get(data, 0, dataSize);
      if(dataSize % 4 != 0) 
        b.position(b.position()+ 4-(dataSize % 4));   // i*4 align
      }
      catch(RuntimeException e) {
    	  lg.error("*** ChannelSubFrame: got buffer runtime datsize="+dataSize+" pos="+b.position()+" e="+e);
      }
    }
    subframeCount= b.getInt();
    authKeyID=b.getInt();
    authSize = b.getInt();
    if(authSize > 0) {
      try {
        authBytes = new byte[authSize];
        b.get(authBytes);
        if(authSize % 4 != 0) 
          b.position(b.position() + (4-(authSize %4))); // i*4 align
      }
       catch(RuntimeException e) {
    	   lg.error("*** ChannelSubFrame: AUTH authsize="+authSize+" pos="+b.position()+" e="+e);
      }
     
    }
    if(b.position() - pos != len+4)         // test that we are positions where the length says 
    	lg.error("Seem to have the wrong subframe length!");
  }
  /** get the data samples from this subframe, this routine does all of the decoding 
   * of various allowed formats for the data.
   * @param samples A user buffer to conain the samples.  It must be big enough!
   * @return The number of samples decoded
   * @throws CanadaException If detected during decompression of Canadian Compressed frame
   */
  public int getSamples(int [] samples) throws CanadaException {
    bdata.position(0);
    switch(transform) {
      case 0:     // no transform, type is done by uncompressed format
        if(uncompressedFormat.equals("s4")) {
          for(int i=0; i<nsamp; i++) {samples[i] = bdata.getInt();}
          return nsamp;  
        }
        else if(uncompressedFormat.equals("s3")) {
          for(int i=0; i<nsamp; i++) {
            samples[i] = ((((int) bdata.get()) & 0xff)<<16) | ((((int) bdata.get()) & 0xff) <<8) | (((int) bdata.get()) & 0xff);
          }
          return nsamp;
        }
        else if(uncompressedFormat.equals("s2")) {
          for(int i=0; i<nsamp; i++) {samples[i] = bdata.getShort();}
          return nsamp;
        }
        else if(uncompressedFormat.equals("i4")) {
        	lg.error("Cannot do format "+uncompressedFormat);
          break;
        }
        else if(uncompressedFormat.equals("i2")) {
        	lg.error("Cannot do format "+uncompressedFormat);
          break;
        }
        else if(uncompressedFormat.equals("CD")) {
        	lg.error("Cannot do format "+uncompressedFormat);
          break;
        }
        else {
        	lg.error("Cannot do format "+uncompressedFormat);
          break;
          
        }
      case 1:  // Canadian compression applied before signature
        if(uncompressedFormat.equals("CD")) {   // This is the CD1.0 encapsulated data
          ByteBuffer bb = ByteBuffer.wrap(data);
          bb.position(0);
          int len2 = bb.getInt();
          if(auth != 0) {
            bb.position(bb.position()+40);  // Skip the auth bytes
          }
          double time2 =bb.getDouble();
          GregorianCalendar g2 = new GregorianCalendar();
          g2.setTimeInMillis((long) (time2*1000.));
          int ns = bb.getInt();
          int stat2 = bb.getInt();
          //par.prt("CD : "+station+" len="+len2+" datasize="+dataSize+" time="+time2+" as g ="+Util.ascdate(g2)+" "+Util.asctime2(g2)+" status="+Util.toHex(stat2)+" ns="+ns);
                  
          byte [] cddata = new byte[len2 - bb.position() +4 +8];
          bb.get(cddata, 0, len2 - bb.position() +4);
          Canada.canada_uncompress(cddata, samples, cddata.length-8, nsamp, 0);
          
        }
        else 
          Canada.canada_uncompress(data, samples, dataSize, nsamp, 0);
        return nsamp;
      case 2: // Canadian compression applied after signature
          Canada.canada_uncompress(data, samples, dataSize, nsamp, 0);
        
        break;
      case 3: // Steim compression applied before signature
        
        break;
      case 4: // Steim compression applied after signature
        break;
      default:
    	  lg.error("transformation type "+transform+" is not implemented!");
    }
    return 0;   // if we got here, the decoding failed or is not implemented.
  }
}
