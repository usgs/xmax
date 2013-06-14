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
import java.util.Random;
import gov.usgs.anss.util.*;
/**
 *
 * @author davidketchum
 */
public class Canada {
/* Copyright 1994 Science Applications International Corporation
 *
 * This software may not be used, copied, modified, or distributed without
 * the express written permission of Science Applications International
 * Corporation (SAIC).  SAIC makes no warranty of any kind with regard
 * to this software, including, but not limited to, the implied warranties
 * of fitness for a particular purpose.
 *

private static final int  CANCOMP_ERR	-1	/* unrecoverable error (malloc fails) */
private static final int  CANCOMP_SUCCESS	= 0;	/* success */
private static final int  CANCOMP_NOT_20  = 1;	/* number of samples not divisible by 20 */
private static final int  CANCOMP_CORRUPT	= 2;	/* corrupted call */
private static final int  CANCOMP_EXCEED	= 3;	/* number of bytes available in compressed
				   data exceeded during decompression */
static boolean dbg=true;

static int corrupt = 0;
//private static StringBuilder sb = new StringBuilder(100);
//public static void clearSB() {if(sb.length() > 0) sb.delete(0,sb.length());}
//public static StringBuilder getSB() {return sb;}
	/**
	 * Uncompresses time series data according to the Canadian
	 * algorithm.
	 * @param b is the array of compressed data bytes
	 * @param y is the array of 4-byte uncompressed integer samples
	 * @param n is the number of bytes in b
	 * @param m is the number of samples (must be divisible by 4)
	 * @param v0 is the last value, which may be freely disregarded.
	 * @return status code CANCOMP_SUCCESS=0, other conditions now throw an exception
   * @throws CanadaException If found CANCOMP_NOT_20, or CANCOMP_CORRUPT, or CANCOMP_EXCEED
	 * note that there are m samples, but m+1 elements in array y.
	 * the last element is needed for the differences.
	 *
	 * sets *n to number of bytes used and elements of y
	 */
public static int canada_uncompress(byte [] b, int [] y, int n, int m, int v0) throws CanadaException
{


	int i, j, k;
	int x;
	int first; 
  int save;
	if (m % 4 != 0) throw new CanadaException("Number of samples is not a multiple of 4="+m);
	corrupt = 0;
  //sb.append("ucmp ");
  ByteBuffer bb = ByteBuffer.wrap(b);
	/*
	 * get first sample
	 */
	j = m / 10;     // key space at the beginning 2 bytes per 20 samples.
  bb.position(j);      // skip over the keys
	//first = (b[j] << 24) | (b[j + 1] << 16) | (b[j + 2] << 8) | b[j + 3];
  first = bb.getInt();
	j += 4;

	/*
	 * unpack 20 samples at a time
	 */
  int py=0;       // we have converted py from a pointer to to unsigned long, to index into y
	for (i = 0; i < (m+9) / 10; i += 2, py += 20) {   // so for each key (2 bytes) is i, goes by 20 (so 80 bytes0)
    bb.position(i);
    x = bb.getShort();      // Get the key

		//if (b[i] >= 0x80) {
    if( (x & 0x8000) != 0) {
			/*
			 * 4,8,12,16,20,24,28 or 32 bits/sample
			 * x is index location
			 * 0x1c is 3-bit mask
			 */
			//x = ((b[i] & 0x7f) << 8) | b[i + 1];
      x = x & 0x7fff;
			if(py   <  m) j = unpack(((x >> 10) & 0x1c) + 4, y, py     , bb, j);  // The if are needed if m not multiple of 20!
			if(py+4  < m) j = unpack(((x >>  7) & 0x1c) + 4, y, py +  4, bb, j);
			if(py+8  < m) j = unpack(((x >>  4) & 0x1c) + 4, y, py +  8, bb, j);
			if(py+12 < m) j = unpack(((x >>  1) & 0x1c) + 4, y, py + 12, bb, j);
			if(py+16 < m) j = unpack(((x <<  2) & 0x1c) + 4, y, py + 16, bb, j);
		}
		else {
			/*
			 * 4,6,8,10,12,14,16 or 18 bits/sample
			 * x is index location
			 * 0xe is 3-bit mask
			 */
			//x = (b[i] << 8) | b[i + 1];
			if(py   <  m) j = unpack(((x >> 11) & 0xe) + 4, y, py     , bb, j);
			if(py+4  < m) j = unpack(((x >>  8) & 0xe) + 4, y, py +  4, bb, j);
			if(py+8  < m) j = unpack(((x >>  5) & 0xe) + 4, y, py +  8, bb, j);
			if(py+12 < m) j = unpack(((x >>  2) & 0xe) + 4, y, py + 12, bb, j);
			if(py+16 < m) j = unpack(((x <<  1) & 0xe) + 4, y, py + 16, bb, j);
		}
		if (j > n) throw new CanadaException("Ran out of decompress buffer before all samples were decoded "+j+">"+n+" ns="+py+" of "+m);
	}
  //sb.append("\nUcmp "+first+","); for(i=0; i<m; i++) sb.append(y[i]+",");sb.append("->");
	/*
	 * undo second difference
	 */
	for (k = 1; k < m; k++) y[k] += y[k - 1];

	/*
	 * undo first difference
	 * (done so that first value gets put in first position,
	 * and last value pops off to be thrown away if necessary).
	 */
	for (k = 0; k < m; k++) {
		save = y[k];
		y[k] = first;
    //sb.append(y[k]+",");
		first += save;
	}
	v0 = first;

	if (corrupt != 0) throw new CanadaException("Buffer being decompressed is corrupt");
	return CANCOMP_SUCCESS;
}
/* Unpack 4 samples or m bits int y array at offset using the ByteBuffer b, position staring a j
 * @param m is max bits/sample
 * @param y is array of output, 4 ints are decompressed and put into y at offset
 * @param b is array of compressed data, already position to next data,
 * @param j The position in b on leaving it should be positioned to the data after that just compressed
 * @return The next value of j (value of the next unprocessed byte in b)
 *  dck *cannot pass j back through pointer so return int through function call
 */

public static int unpack(int m, int [] y, int offset, ByteBuffer b, int j)

  {
    /*
     * unpack 4 samples into y from "m" bits/sample in b
     *
     * output - 4 samples of y
     * input - packed data, number of bytes is incremented on *j
     *
     * m must accurately reflect the max bits required.  Note that
     * since y (in reality) may be signed, must check the extra bit
     * (the 0 or 1) then fill the values out to the MSB.
     *
     * Note - union is used to reduce operations.
     * other simplifications from the original:
     * 1) use all unsigned arithmetic - no need to check sign bit
     * 2) use all logical bit operations (& << >> |)
     * 3) recognize that right and left shifts off the end of a field
     *    mean the bits drop on the floor (no need to precisely mask
     *    bits being shifted to the edges of fields).
     * 4) do not mask at all if bits left-shifted sufficiently.
     */
     int y0=0, y1=0, y2=0, y3=0, ul=0,vl=0;
     long ll;
     byte [] ub = new byte[4];
     byte [] vb = new byte[4];
     //sb.append(m+",");
     b.position(j);       // set the buffer position based on j
    switch (m) {	/* switch on bits/sample */

    case  4:
      byte pb = b.get();

      y0 = pb >> 4;
      y1 = pb & 0xf;
      pb = b.get();
      y2 = pb >> 4;
      y3 = pb++ & 0xf;
      if ((y0 & 0x8) != 0) y0 |= 0xfffffff0;
      if ((y1 & 0x8) != 0) y1 |= 0xfffffff0;
      if ((y2 & 0x8) != 0) y2 |= 0xfffffff0;
      if ((y3 & 0x8) != 0) y3 |= 0xfffffff0;
      j +=2;
      break;

    case  6:
      ul = b.getInt();
      b.position(b.position() -1);      // we only needed 3 bytes

      y0 = (ul >> 26);
      y1 = (ul >> 20) & 0x3f;
      y2 = (ul >> 14) & 0x3f;
      y3 = (ul >>  8) & 0x3f;
      if ((y0 & 0x20) != 0) y0 |= 0xffffffc0;
      if ((y1 & 0x20) != 0) y1 |= 0xffffffc0;
      if ((y2 & 0x20) != 0) y2 |= 0xffffffc0;
      if ((y3 & 0x20) != 0) y3 |= 0xffffffc0;
      j += 3;
      break;

    case  8:
      y0 = b.get();
      y1 = b.get();
      y2 = b.get();
      y3 = b.get();
      if ( (y0 & 0x80) != 0) y0 |= 0xffffff00;
      if ( (y1 & 0x80) != 0) y1 |= 0xffffff00;
      if ( (y2 & 0x80) != 0) y2 |= 0xffffff00;
      if ( (y3 & 0x80) != 0) y3 |= 0xffffff00;
      j += 4;
      break;

    case 10:
      ll = b.getLong();
      b.position(b.position() -3);    // only need 5 bytes

      y0 = (int) (ll >> 54);
      y1 = (int) (ll >> 44) & 0x3ff;
      y2 = (int) (ll >>  34) & 0x3ff;
      y3 = (int) (ll >>  24) & 0x3ff ;
      if ( (y0 & 0x200) != 0) y0 |= 0xfffffc00;
      if ( (y1 & 0x200) != 0) y1 |= 0xfffffc00;
      if ( (y2 & 0x200) != 0) y2 |= 0xfffffc00;
      if ( (y3 & 0x200) != 0) y3 |= 0xfffffc00;
      j+= 5;
      break;

    case 12:
      ll = b.getLong();
      b.position(b.position() -2);    // only need 6 bytes

      y0 = (int) (ll >> 52);
      y1 = (int) (ll >> 40) & 0xfff;
      y2 = (int) (ll >> 28) & 0xfff;
      y3 = (int) (ll >> 16) & 0xfff;
      if ( (y0 & 0x800) != 0) y0 |= 0xfffff000;
      if ( (y1 & 0x800) != 0) y1 |= 0xfffff000;
      if ( (y2 & 0x800) != 0) y2 |= 0xfffff000;
      if ( (y3 & 0x800) != 0) y3 |= 0xfffff000;
      j += 6;
      break;

    case 14:
      ll = b.getLong();
      b.position(b.position() -1);    // only need 7 bytes

      y0 = (int) (ll >> 50);
      y1 = (int) (ll >> 36) & 0x3fff;
      y2 = (int) (ll >> 22) & 0x3fff ;
      y3 = (int) (ll >> 8) & 0x3fff;
      if ( (y0 & 0x2000) != 0) y0 |= 0xffffc000;
      if ( (y1 & 0x2000) != 0) y1 |= 0xffffc000;
      if ( (y2 & 0x2000) != 0) y2 |= 0xffffc000;
      if ( (y3 & 0x2000) != 0) y3 |= 0xffffc000;
      j += 7;
      break;

    case 16:
      y0 = b.getShort();
      y1 = b.getShort();
      y2 = b.getShort();
      y3 = b.getShort();
      if ( (y0 & 0x8000) != 0) y0 |= 0xffff0000;
      if ( (y1 & 0x8000) != 0) y1 |= 0xffff0000;
      if ( (y2 & 0x8000) != 0) y2 |= 0xffff0000;
      if ( (y3 & 0x8000) != 0) y3 |= 0xffff0000;
      j += 8;
      break;

    case 18:
      ll = b.getLong();
      ul = b.get();     // one byte

      y0 = (int) (ll >> 46);
      y1 = (int) (ll >> 28) & 0x3ffff;
      y2 = (int) (ll >> 10) & 0x3ffff;
      y3 = (int) ((ll & 0x3ff) <<  8)  | (ul & 0xff);
      if ( (y0 & 0x20000) != 0) y0 |= 0xfffc0000;
      if ( (y1 & 0x20000) != 0) y1 |= 0xfffc0000;
      if ( (y2 & 0x20000) != 0) y2 |= 0xfffc0000;
      if ( (y3 & 0x20000) != 0) y3 |= 0xfffc0000;
      j += 9;
      break;

    case 20:    // total of 10 bytes
      ll = b.getLong();
      ul = b.getShort();
      y0 = (int) (ll >> 44) & 0xfffff;
      y1 = (int) (ll >>  24) & 0xfffff ;
      y2 = (int) (ll >>  4) & 0xfffff;
      y3 = (int) ((ll & 0xf) << 16) | ( ul & 0xffff);
      if ( (y0 & 0x80000) != 0) y0 |= 0xfff00000;
      if ( (y1 & 0x80000) != 0) y1 |= 0xfff00000;
      if ( (y2 & 0x80000) != 0) y2 |= 0xfff00000;
      if ( (y3 & 0x80000) != 0) y3 |= 0xfff00000;
      j += 10;
      break;

    case 24:
      ll = b.getLong();
      ul = b.getInt();

      y0 = (int) (ll >> 40) & 0xffffff;
      y1 = (int) (ll >> 16) & 0xffffff ;
      y2 = (int) ((ll & 0xffff) << 8) | ((ul >> 24) & 0xff);
      y3 = (int) (ul  & 0xffffff);
      if ( (y0 & 0x800000) != 0) y0 |= 0xff000000;
      if ( (y1 & 0x800000) != 0) y1 |= 0xff000000;
      if ( (y2 & 0x800000) != 0) y2 |= 0xff000000;
      if ( (y3 & 0x800000) != 0) y3 |= 0xff000000;
      j += 12;
      break;

    case 28:
      ul = b.getInt();
      vl = b.getInt();
      y0 = (ul >>  4);
      y1 = ((ul & 0xf) << 24) | ((vl >>  8) & 0xffffff);
      ul = b.getInt();
      y2 = ( (vl & 0xff) << 20) | ((ul >> 12) &0xfffff);
      vl = b.getInt();
      b.position(j+14);
      y3 = ((ul &0xfff)<< 16)  | ((vl >> 16) & 0xffff);
      if ( (y0 & 0x8000000) !=0)  y0 |= 0xf0000000;
      if ( (y1 & 0x8000000) !=0) y1 |= 0xf0000000;
      if ( (y2 & 0x8000000) !=0) y2 |= 0xf0000000;
      if ( (y3 & 0x8000000) !=0) y3 |= 0xf0000000;
      j += 14;
      break;

    case 32:
      y0 = b.getInt();
      y1 = b.getInt();
      y2 = b.getInt();
      y3 = b.getInt();
      j += 16;
      break;

    default:
      /* No default - assume calling program is corrupt */
      corrupt = 1;
      break;
    }
    if(b.position() != j) 
      if(dbg) System.out.println("J and position misaligned");
    y[offset] = y0;
    y[offset+1] = y1;
    y[offset+2] = y2;
    y[offset+3] = y3;
    return j;
  }
/** compress data into a byte buffer from data
 * 
 * @param b  The ByteBuffer being used to compress the data
 * @param data The data samples as int[].
 * @param m The number of samples (must be divisible by 20)
 * @param v0 is used as the last value
 * @return The revised number of bytes used in b
 */
  public static int canada_compress(ByteBuffer b, int [] data,  int m, int v0) {
    int y0, y1, y2, y3, y4;

    if(m % 20 != 0) return CANCOMP_NOT_20;
    int [] pm = new int[m / 4];      // the number of bits in each 4 sample group
    int [] y = new int[m+1]; // first differences
    int first = data[0];
    for(int k=0; k< m-1; k++) y[k] = data[k+1] - data[k];
    y[m-1] = v0 - y[m-1];

    //second difference. y[0] is not modified and remains a first difference
    for(int k=m-1; k>0; k--) y[k] -= y[k-1];
    //sb.append("Comp ");sb.append(first+","); for(int i=0; i<m; i++) sb.append(y[i]+",");sb.append("->");
    //for(int i=0; i<m; i++) sb.append(data[i]+",");
    //sb.append("\nComp ");
    int maxbits=0;
    // number for bits for each block of 4 differences into pm
    for(int i=0; i<m; i=i+4) {
      y0 = y[i]; if( y0 < 0) y0 = (0xffffffff ^ y0);  // convert to positive, but one less (abs(y0) -1)
      y1 = y[i+1]; if( y1 < 0) y1 = (0xffffffff ^ y1);  
      y2 = y[i+2]; if( y2 < 0) y2 = (0xffffffff ^ y2); 
      y3 = y[i+3]; if( y3 < 0) y3 = (0xffffffff ^ y3); 
      y4 = y0 | y1 | y2 | y3;                       // this masks all of the bits that will be needed as positive values
      if(      (y4 & 0x78000000) != 0) pm[i/4] = 32;     // need top 4 bits so more than 28 bits needed bits needed
      else if ((y4 & 0x07800000) != 0) pm[i/4] = 28; 
      else if ((y4 & 0x00780000) != 0) pm[i/4] = 24; 
      else if ((y4 & 0x00060000) != 0) pm[i/4] = 20; 
      else if ((y4 & 0x00018000) != 0) pm[i/4] = 18; 
      else if ((y4 & 0x00006000) != 0) pm[i/4] = 16; 
      else if ((y4 & 0x00001800) != 0) pm[i/4] = 14; 
      else if ((y4 & 0x00000600) != 0) pm[i/4] = 12; 
      else if ((y4 & 0x00000180) != 0) pm[i/4] = 10; 
      else if ((y4 & 0x00000060) != 0) pm[i/4] = 8; 
      else if ((y4 & 0x00000018) != 0) pm[i/4] = 6; 
      else  pm[i/4] = 4; 
      //sb.append(pm[i/4]+",");
    }
    //sb.append("\n");
    
    int j = m / 10;       // offset where data portion starts, first m/10 bytes are keys (1 short for each 20 samples)
    b.position(j);
    b.putInt(first);      // put the DC level (this is S(1) in the documentation
    j += 4;
    for(int i=0; i<m/10; i=i+2) {     // for each group of 20 samples
      // if any of the values is > 18, use big encoding amounts
      b.position(j);
      int pmi = i/2*5;
      maxbits=0;
      for(int k=pmi; k<pmi+5; k++) if(maxbits < pm[k]) maxbits = pm[k];
      if(maxbits > 18) {    // 4, 8, 12, 16, 20, 24, 28, 32 bits/sample
        for(int k=pmi; k<pmi+5; k++) {
          pm[k] += pm[k] & 2;           // adjust the number of bits to the right range
          j = pack(pm[k], y, k*4, b, j); // pack 4 samples of width pm[k] into b from y at offset i*4
        }
        // build up the key, note key value is bit (value -4)/4
        short im = (short) (((pm[pmi] -4 << 10) | (pm[pmi+1]-4 << 7) | (pm[pmi+2]-4 << 4) | (pm[pmi+3]-4 << 1) | (pm[pmi+4]-4 >> 2)) & 0xffff);
        b.position(i);
        im |= 0x8000;     // set high bit of short 
        b.putShort(im);
      }
      else {    // 4, 6, 8,10,12,14,16,18 bits/sample
         // pack 5 sets of 4 samples
        for(int k=0; k<5; k++) {
          j = pack(pm[pmi+k], y, k*4+i*10, b, j);
          if(b.position() != j) if(dbg) System.out.println("J and B are out of position");
          pm[pmi+k] -= 4;     // convert to key value*2 (the *2 is removed in the shifting)
        }
        short im = (short) (((pm[pmi] << 11) | (pm[pmi+1] << 8) | (pm[pmi+2] << 5) | (pm[pmi+3] << 2) | (pm[pmi+4] >> 1)) & 0xffff);
        b.position(i);
        b.putShort(im);
      }
    }
    b.position(j);
    return j;
  }
  /** pack 4 samples of width m bits into b, starting at position j.
   * @param m  width of data to pack
   * @param y array with the data samples
   * @param offset offset in y to start (next for samples)
   * @param b The byte buffer where data will be put
   * @param j The position n b to put the data
   * @return The position of the next place to put data (the length of b used)
   */
  public static int pack(int m, int [] y, int offset, ByteBuffer b, int j) {
    int y0 = y[offset];
    int y1 = y[offset+1];
    int y2 = y[offset+2];
    int y3 = y[offset+3];
    int ul = 0;
    short sl=0;
    switch (m ) {     // switch on bits per sample
      case 4:
        b.put( (byte) ((y0 & 0xf) << 4 | (y1 & 0xf)));
        b.put( (byte) ((y2 & 0xf) << 4 | (y3 & 0xf)));
        j += 2;
        break;
      case 6:
        ul = ((y0 & 0x3f) << 26) | (y1 & 0x3f) << 20 | (y2 & 0x3f) << 14 | (y3 & 0x3f) << 8;
        b.putInt(ul);
        j += 3;
        b.position(j);
        break;
      case 8:
        b.put((byte) (y0 & 0xff));
        b.put((byte) (y1 & 0xff));
        b.put((byte) (y2 & 0xff));
        b.put((byte) (y3 & 0xff));
        j += 4;
        break;
      case 10:
        ul =  (y0 & 0x3ff) << 22 | (y1 & 0x3ff) << 12 | (y2 & 0x3ff) << 2 | (y3 & 0x3ff) >> 8;
        b.putInt(ul);
        b.put((byte) (y3 & 0xff));
        j += 5;
        break;
      case 12:
        ul = (y0 & 0xfff) << 20 | (y1 & 0xfff) << 8 | (y2 & 0xfff) >> 4;
        b.putInt(ul);
        sl = (short) ((y2 & 0xf) << 12 | (y3 & 0xfff));
        b.putShort(sl);
        j += 6;
        break;
      case 14: 
        ul = (y0 & 0x3fff) << 18 | (y1 & 0x3fff) << 4 | (y2 & 0x3fff) >> 10;
        b.putInt(ul);
        ul = (y2 & 0x3ff) << 22 | (y3 & 0x3fff)<< 8;
        b.putInt(ul);
        j += 7;
        b.position(j);
        break;
      case 16:
        ul = y0 << 16 | (y1 & 0xffff);
        b.putInt(ul);
        ul = y2 << 16 | (y3 & 0xffff);
        b.putInt(ul);
        j += 8;
        break;
      case 18: 
        ul = y0 << 14 | (y1 & 0x3ffff) >> 4;
        b.putInt(ul);
        ul = (y1 & 0xf) << 28 | (y2 & 0x3ffff) << 10 | (y3 & 0x3ffff) >> 8;
        b.putInt(ul);
        b.put((byte) (y3 & 0xff));
        j += 9;
        break;
      case 20:
        ul = y0 << 12 | (y1 & 0xfffff) >> 8;
        b.putInt(ul);
        ul = (y1 & 0xff) << 24 | (y2 & 0xfffff) << 4 | (y3 & 0xfffff) >> 16;
        b.putInt(ul);
        b.putShort((short) (y3 & 0xffff));
        j += 10;
        break;
      case 24:
        ul = y0 <<  8 | (y1 & 0xffffff) >> 16;
        b.putInt(ul);
        ul = y1 << 16 | (y2 & 0xffffff) >>  8;
        b.putInt(ul);
        ul = y2 << 24 | (y3 & 0xffffff);
        b.putInt(ul);
        j += 12;
        break;
      case 28:
        ul = y0 <<  4 | (y1 & 0xfffffff) >> 24;
        b.putInt(ul);
        ul = y1 <<  8 | (y2 & 0xfffffff) >> 20;
        b.putInt(ul);
        ul = y2 << 12 | (y3 & 0xfffffff) >> 16;
        b.putInt(ul);
        b.putShort((short) (y3 & 0xffff));
        j += 14;
        break;
      case 32:
        b.putInt(y0);
        b.putInt(y1);
        b.putInt(y2);
        b.putInt(y3);
        j += 16;
        break;
      default:
        System.out.println("Got illegal mask size in pack mask="+m);
        break;
    }
    return j;
  }
  public static boolean compare(int [] in, int [] out, int len) {
    boolean ret=false;
    for(int i=0; i< len; i++) if(in[i] != out[i]) {System.out.println(i+" diff "+in[i]+"!="+out[i]); ret=true;}
    return ret;
  }
  public static  void main(String [] args) {
    int [] offs = {1<<3, 1<<5, 1<<7, 1<<9, 1<<11, 1<<13, 1<<15, 1<<17, 1<<19,1<<23, 1<<27, 1<<31};
    
    int [] data = new int[100];
    byte [] buf = new byte[400];
    int v0=0;
    offs[11] = -(offs[11]+1);
    ByteBuffer bb = ByteBuffer.wrap(buf);
    int [] out = new int[10000];
    for(int off  = 0; off<offs.length; off++) {
      int d = offs[off];
      System.out.println("Start "+d);
      for(int i=0; i<20; i++) data[i] = i*d+10;
      int len = Canada.canada_compress(bb, data, 40, 0);
      try {
        Canada.canada_uncompress(buf, out, len, 40, v0);
        //if(compare(data, out, 40)) 
          //System.out.println(Canada.getSB());
      }
      catch(CanadaException e) {
        System.out.println("CanadaException e="+e);
      }
      //Canada.clearSB();
    }
    Random ran = new Random();
    int ind=0;
    for(int j =0; j<100000000; j++) {
      if(j % 250000 == 0) System.out.println(Util.asctime()+" "+j);
      for(int i=0; i<40; i=i+4) {
        ind = (int) (ran.nextDouble()*12.);
        for(int k=i; k<i+4; k++) data[k] = (int) (ran.nextDouble()*offs[ind]);
      }
      int len = Canada.canada_compress(bb, data, 40, 0);
      try {
        Canada.canada_uncompress(buf, out, len, 40, v0);
        //if(compare(data, out, 40)) 
          //System.out.println(Canada.getSB());
      }
      catch(CanadaException e) {
        System.out.println("CanadaException e="+e);
      }      
      //Canada.clearSB();
    }
    
  }
}
