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
/** This classs was derived from a C module CRC64.c given to us by James BlinkHorn of 
 *  the Canadian data center.  Below are comments from the original source:
 *
 * Defined to be the 64 bit Cyclic Redundancy Check with
  * the polynomial x64 + x4 + x3 + x1 + x0.
  * The high order bit is implicitly 1, so this is specified
  * by the value 11011 in binary or 0x1b.
  * To speed computation, we pre-compute a "T" vector.
  * T[i] is the remainder of dividing i*x64 by the polynomial.n
  * For more information on CRC see D.V. Sarwate, "Computation
  * of cyclic redundancy via table look-up," Comm. ACM 31(8),
  * Aug. 1988, p. 1008-1013.

 *
 * @author davidketchum
 */
public class CRC64 {
  static long [] tvec;        // This contains the pre-computed coefficients for each of the possible 256 values
  /** compute the 256 CRC elements the first time */
  private static void CRCinit() {
    tvec = new long[256];
    long  crcPoly = 0x1BL;
    for(int i=0; i<256; i++) {
      tvec[i]=0;
      for(int j=7; j>=0; j--) {
        if( (i & (1 << j)) != 0) {
          tvec[i] ^= (crcPoly << j);
        }
      }
      //Util.prt(i+"="+Util.toHex(tvec[i]));
    }
  }
  /** compute a CRC on the array b of length len
   * @param b The byte array to compute a CRC for
   * @param len The length of the array in bytes.
   * @return
   */
  public static long compute(byte [] b, int len) {
    if(tvec == null)  CRCinit();
    long crc = 0L;
    for(int i=0; i<len; i++) {
      crc = tvec[(int) ((crc>>56) & 0xffL)] ^ (crc << 8 | (((long) (b[i])) & 0xffL));
    }
    return crc;
  }

}
