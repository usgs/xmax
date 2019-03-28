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

/**
 * 
 * @author davidketchum
 */
class Canada {
	/*
	 * Copyright 1994 Science Applications International Corporation
	 *
	 * This software may not be used, copied, modified, or distributed without
	 * the express written permission of Science Applications International
	 * Corporation (SAIC). SAIC makes no warranty of any kind with regard to
	 * this software, including, but not limited to, the implied warranties of
	 * fitness for a particular purpose.
	 */

	private static int corrupt = 0;

	/**
	 * Uncompresses time series data according to the Canadian algorithm.
	 * 
	 * @param b
	 *            is the array of compressed data bytes
	 * @param y
	 *            is the array of 4-byte uncompressed integer samples
	 * @param n
	 *            is the number of bytes in b
	 * @param m
	 *            is the number of samples (must be divisible by 4)
	 * @throws CanadaException
	 *             If found CANCOMP_NOT_20, or CANCOMP_CORRUPT, or
	 *             CANCOMP_EXCEED note that there are m samples, but m+1
	 *             elements in array y. the last element is needed for the
	 *             differences.
	 * 
	 *             sets *n to number of bytes used and elements of y
	 */
	static void canada_uncompress(byte[] b, int[] y, int n, int m)
			throws CanadaException {
		int i, j, k;
		int x;
		int first;
		int save;
		if (m % 4 != 0)
			throw new CanadaException(
					"Number of samples is not a multiple of 4=" + m);
		corrupt = 0;
		// sb.append("ucmp ");
		ByteBuffer bb = ByteBuffer.wrap(b);
		/*
		 * get first sample
		 */
		j = m / 10; // key space at the beginning 2 bytes per 20 samples.
		bb.position(j); // skip over the keys
		first = bb.getInt();
		j += 4;

		/*
		 * unpack 20 samples at a time
		 */
		int py = 0; // we have converted py from a pointer to to unsigned long,
					// to index into y
		for (i = 0; i < (m + 9) / 10; i += 2, py += 20) { // so for each key (2
															// bytes) is i, goes
															// by 20 (so 80
															// bytes0)
			bb.position(i);
			x = bb.getShort(); // Get the key

			// if (b[i] >= 0x80) {
			if ((x & 0x8000) != 0) {
				/*
				 * 4,8,12,16,20,24,28 or 32 bits/sample x is index location 0x1c
				 * is 3-bit mask
				 */
				x = x & 0x7fff;
				if (py < m)
					j = unpack(((x >> 10) & 0x1c) + 4, y, py, bb, j); // The if
																		// are
																		// needed
																		// if m
																		// not
																		// multiple
																		// of
																		// 20!
				if (py + 4 < m)
					j = unpack(((x >> 7) & 0x1c) + 4, y, py + 4, bb, j);
				if (py + 8 < m)
					j = unpack(((x >> 4) & 0x1c) + 4, y, py + 8, bb, j);
				if (py + 12 < m)
					j = unpack(((x >> 1) & 0x1c) + 4, y, py + 12, bb, j);
				if (py + 16 < m)
					j = unpack(((x << 2) & 0x1c) + 4, y, py + 16, bb, j);
			} else {
				/*
				 * 4,6,8,10,12,14,16 or 18 bits/sample x is index location 0xe
				 * is 3-bit mask
				 */
				if (py < m)
					j = unpack(((x >> 11) & 0xe) + 4, y, py, bb, j);
				if (py + 4 < m)
					j = unpack(((x >> 8) & 0xe) + 4, y, py + 4, bb, j);
				if (py + 8 < m)
					j = unpack(((x >> 5) & 0xe) + 4, y, py + 8, bb, j);
				if (py + 12 < m)
					j = unpack(((x >> 2) & 0xe) + 4, y, py + 12, bb, j);
				if (py + 16 < m)
					j = unpack(((x << 1) & 0xe) + 4, y, py + 16, bb, j);
			}
			if (j > n)
				throw new CanadaException(
						"Ran out of decompress buffer before all samples were decoded "
								+ j + ">" + n + " ns=" + py + " of " + m);
		}
		/*
		 * undo second difference
		 */
		for (k = 1; k < m; k++)
			y[k] += y[k - 1];

		/*
		 * undo first difference (done so that first value gets put in first
		 * position, and last value pops off to be thrown away if necessary).
		 */
		for (k = 0; k < m; k++) {
			save = y[k];
			y[k] = first;
			first += save;
		}

		if (corrupt != 0)
			throw new CanadaException("Buffer being decompressed is corrupt");
	}

	/*
	 * Unpack 4 samples or m bits int y array at offset using the ByteBuffer b,
	 * position staring a j
	 * 
	 * @param m is max bits/sample
	 * 
	 * @param y is array of output, 4 ints are decompressed and put into y at
	 * offset
	 * 
	 * @param b is array of compressed data, already position to next data,
	 * 
	 * @param j The position in b on leaving it should be positioned to the data
	 * after that just compressed
	 * 
	 * @return The next value of j (value of the next unprocessed byte in b) dck
	 * *cannot pass j back through pointer so return int through function call
	 */

	private static int unpack(int m, int[] y, int offset, ByteBuffer b, int j)

	{
		/*
		 * unpack 4 samples into y from "m" bits/sample in b
		 * 
		 * output - 4 samples of y input - packed data, number of bytes is
		 * incremented on *j
		 * 
		 * m must accurately reflect the max bits required. Note that since y
		 * (in reality) may be signed, must check the extra bit (the 0 or 1)
		 * then fill the values out to the MSB.
		 * 
		 * Note - union is used to reduce operations. other simplifications from
		 * the original: 1) use all unsigned arithmetic - no need to check sign
		 * bit 2) use all logical bit operations (& << >> |) 3) recognize that
		 * right and left shifts off the end of a field mean the bits drop on
		 * the floor (no need to precisely mask bits being shifted to the edges
		 * of fields). 4) do not mask at all if bits left-shifted sufficiently.
		 */
		int y0 = 0, y1 = 0, y2 = 0, y3 = 0, ul = 0, vl = 0;
		long ll;
		b.position(j); // set the buffer position based on j
		switch (m) { /* switch on bits/sample */

		case 4:
			byte pb = b.get();

			y0 = pb >> 4;
			y1 = pb & 0xf;
			pb = b.get();
			y2 = pb >> 4;
			y3 = pb++ & 0xf;
			if ((y0 & 0x8) != 0)
				y0 |= 0xfffffff0;
			if ((y1 & 0x8) != 0)
				y1 |= 0xfffffff0;
			if ((y2 & 0x8) != 0)
				y2 |= 0xfffffff0;
			if ((y3 & 0x8) != 0)
				y3 |= 0xfffffff0;
			j += 2;
			break;

		case 6:
			ul = b.getInt();
			b.position(b.position() - 1); // we only needed 3 bytes

			y0 = (ul >> 26);
			y1 = (ul >> 20) & 0x3f;
			y2 = (ul >> 14) & 0x3f;
			y3 = (ul >> 8) & 0x3f;
			if ((y0 & 0x20) != 0)
				y0 |= 0xffffffc0;
			if ((y1 & 0x20) != 0)
				y1 |= 0xffffffc0;
			if ((y2 & 0x20) != 0)
				y2 |= 0xffffffc0;
			if ((y3 & 0x20) != 0)
				y3 |= 0xffffffc0;
			j += 3;
			break;

		case 8:
			y0 = b.get();
			y1 = b.get();
			y2 = b.get();
			y3 = b.get();
			if ((y0 & 0x80) != 0)
				y0 |= 0xffffff00;
			if ((y1 & 0x80) != 0)
				y1 |= 0xffffff00;
			if ((y2 & 0x80) != 0)
				y2 |= 0xffffff00;
			if ((y3 & 0x80) != 0)
				y3 |= 0xffffff00;
			j += 4;
			break;

		case 10:
			ll = b.getLong();
			b.position(b.position() - 3); // only need 5 bytes

			y0 = (int) (ll >> 54);
			y1 = (int) (ll >> 44) & 0x3ff;
			y2 = (int) (ll >> 34) & 0x3ff;
			y3 = (int) (ll >> 24) & 0x3ff;
			if ((y0 & 0x200) != 0)
				y0 |= 0xfffffc00;
			if ((y1 & 0x200) != 0)
				y1 |= 0xfffffc00;
			if ((y2 & 0x200) != 0)
				y2 |= 0xfffffc00;
			if ((y3 & 0x200) != 0)
				y3 |= 0xfffffc00;
			j += 5;
			break;

		case 12:
			ll = b.getLong();
			b.position(b.position() - 2); // only need 6 bytes

			y0 = (int) (ll >> 52);
			y1 = (int) (ll >> 40) & 0xfff;
			y2 = (int) (ll >> 28) & 0xfff;
			y3 = (int) (ll >> 16) & 0xfff;
			if ((y0 & 0x800) != 0)
				y0 |= 0xfffff000;
			if ((y1 & 0x800) != 0)
				y1 |= 0xfffff000;
			if ((y2 & 0x800) != 0)
				y2 |= 0xfffff000;
			if ((y3 & 0x800) != 0)
				y3 |= 0xfffff000;
			j += 6;
			break;

		case 14:
			ll = b.getLong();
			b.position(b.position() - 1); // only need 7 bytes

			y0 = (int) (ll >> 50);
			y1 = (int) (ll >> 36) & 0x3fff;
			y2 = (int) (ll >> 22) & 0x3fff;
			y3 = (int) (ll >> 8) & 0x3fff;
			if ((y0 & 0x2000) != 0)
				y0 |= 0xffffc000;
			if ((y1 & 0x2000) != 0)
				y1 |= 0xffffc000;
			if ((y2 & 0x2000) != 0)
				y2 |= 0xffffc000;
			if ((y3 & 0x2000) != 0)
				y3 |= 0xffffc000;
			j += 7;
			break;

		case 16:
			y0 = b.getShort();
			y1 = b.getShort();
			y2 = b.getShort();
			y3 = b.getShort();
			if ((y0 & 0x8000) != 0)
				y0 |= 0xffff0000;
			if ((y1 & 0x8000) != 0)
				y1 |= 0xffff0000;
			if ((y2 & 0x8000) != 0)
				y2 |= 0xffff0000;
			if ((y3 & 0x8000) != 0)
				y3 |= 0xffff0000;
			j += 8;
			break;

		case 18:
			ll = b.getLong();
			ul = b.get(); // one byte

			y0 = (int) (ll >> 46);
			y1 = (int) (ll >> 28) & 0x3ffff;
			y2 = (int) (ll >> 10) & 0x3ffff;
			y3 = (int) ((ll & 0x3ff) << 8) | (ul & 0xff);
			if ((y0 & 0x20000) != 0)
				y0 |= 0xfffc0000;
			if ((y1 & 0x20000) != 0)
				y1 |= 0xfffc0000;
			if ((y2 & 0x20000) != 0)
				y2 |= 0xfffc0000;
			if ((y3 & 0x20000) != 0)
				y3 |= 0xfffc0000;
			j += 9;
			break;

		case 20: // total of 10 bytes
			ll = b.getLong();
			ul = b.getShort();
			y0 = (int) (ll >> 44) & 0xfffff;
			y1 = (int) (ll >> 24) & 0xfffff;
			y2 = (int) (ll >> 4) & 0xfffff;
			y3 = (int) ((ll & 0xf) << 16) | (ul & 0xffff);
			if ((y0 & 0x80000) != 0)
				y0 |= 0xfff00000;
			if ((y1 & 0x80000) != 0)
				y1 |= 0xfff00000;
			if ((y2 & 0x80000) != 0)
				y2 |= 0xfff00000;
			if ((y3 & 0x80000) != 0)
				y3 |= 0xfff00000;
			j += 10;
			break;

		case 24:
			ll = b.getLong();
			ul = b.getInt();

			y0 = (int) (ll >> 40) & 0xffffff;
			y1 = (int) (ll >> 16) & 0xffffff;
			y2 = (int) ((ll & 0xffff) << 8) | ((ul >> 24) & 0xff);
			// (int)
			y3 = (ul & 0xffffff);
			if ((y0 & 0x800000) != 0)
				y0 |= 0xff000000;
			if ((y1 & 0x800000) != 0)
				y1 |= 0xff000000;
			if ((y2 & 0x800000) != 0)
				y2 |= 0xff000000;
			if ((y3 & 0x800000) != 0)
				y3 |= 0xff000000;
			j += 12;
			break;

		case 28:
			ul = b.getInt();
			vl = b.getInt();
			y0 = (ul >> 4);
			y1 = ((ul & 0xf) << 24) | ((vl >> 8) & 0xffffff);
			ul = b.getInt();
			y2 = ((vl & 0xff) << 20) | ((ul >> 12) & 0xfffff);
			vl = b.getInt();
			b.position(j + 14);
			y3 = ((ul & 0xfff) << 16) | ((vl >> 16) & 0xffff);
			if ((y0 & 0x8000000) != 0)
				y0 |= 0xf0000000;
			if ((y1 & 0x8000000) != 0)
				y1 |= 0xf0000000;
			if ((y2 & 0x8000000) != 0)
				y2 |= 0xf0000000;
			if ((y3 & 0x8000000) != 0)
				y3 |= 0xf0000000;
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
		/* number of bytes available in
		 * compressed data exceeded
		 * during decompression
		 */
		if (b.position() != j)
			System.out.println("J and position misaligned");
		y[offset] = y0;
		y[offset + 1] = y1;
		y[offset + 2] = y2;
		y[offset + 3] = y3;
		return j;
	}

}
