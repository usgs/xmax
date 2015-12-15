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
 * SeedUtil.java
 *
 * Created on May 27, 2005, 3:47 PM
 * Here lie all of the static functions needed as Utilies for the Edge and
 * seed files.  The Julian day routines can make the calculation a year end 
 * meaningless.
 */

package gov.usgs.anss.cd11;

/**
 * This is a stripped down version of gov.usgs.anss.util.SeedUtil. It contains
 * only code needed for Xmax's copy of CD11
 * 
 * @author David Ketchum
 */
public class SeedUtil {
	private static int[] daytab = new int[] { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
	private static int[] dayleap = new int[] { 0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	/**
	 * convert a year and day of year to an array in yr,mon,day order
	 * 
	 * @param yr
	 *            The year
	 * @param doy
	 *            The day of the year
	 * @return an array in yr, mon, day
	 * @throws RuntimeException
	 *             ill formed especially doy being too big.
	 */
	public static int[] ymd_from_doy(int yr, int doy) throws RuntimeException {
		int j;
		int sum;
		yr = sanitizeYear(yr);
		boolean leap = yr % 4 == 0 && yr % 100 != 0
				|| yr % 400 == 0; /*
									 * is it a leap year
									 */
		sum = 0;
		int[] ymd = new int[3];
		ymd[0] = yr;
		if (leap) {
			for (j = 1; j <= 12; j++) {
				if (sum < doy && sum + dayleap[j] >= doy) {
					ymd[1] = j;
					ymd[2] = doy - sum;
					return ymd;
				}
				sum += dayleap[j];
			}
		} else {
			for (j = 1; j <= 12; j++) {
				if (sum < doy && sum + daytab[j] >= doy) {
					ymd[1] = j;
					ymd[2] = doy - sum;
					return ymd;
				}
				sum += daytab[j];
			}
		}
		System.out.println("ymd_from_doy: impossible drop through!   yr=" + yr + " doy=" + doy);
		throw new RuntimeException("ymd_from_DOY : impossible yr=" + yr + " doy=" + doy);

	}

	/**
	 * Sanitize year using the rule of 60, two digit years {@literal >=60} =
	 * 1900+yr years less than 60 are 2000+yr. If its 4 digits already, just
	 * return it.
	 * 
	 * @param yr
	 *            The year to sanitize
	 * @return the year sanitized by rule of 60.
	 */
	private static int sanitizeYear(int yr) {
		if (yr >= 100)
			return yr;
		if (yr >= 60 && yr < 100)
			return yr + 1900;
		else if (yr < 60 && yr >= 0)
			return yr + 2000;
		System.out.println("Illegal year to sanitize =" + yr);
		return -1;
	}

}
