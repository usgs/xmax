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

package gov.usgs.anss.util;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.text.DecimalFormat;

/** This clas is all static methods for maniupulating julian days, day-of-year,
 * back and forth.
 *
 * @author davidketchum
 */
public class SeedUtil {
static int [] daytab = new int[] {0,31,28,31,30,31,30,31,31,30,31,30,31};
static int [] dayleap = new int[]{0,31,29,31,30,31,30,31,31,30,31,30,31};
/**
	From a year, month, and day of the month 
	calculate the Julian day of year.  Parts stolen from K&R's C book.
 * @param ymd The year, month day as an array
 * @return The day of the year
*/
public static int doy_from_ymd(int [] ymd)
{	int yr = sanitizeYear(ymd[0]);
  boolean leap= yr%4 == 0 && yr%100 != 0 || yr%400 == 0;	/* is it a leap year */
/*	printf("Mon=%s month=%d leap=%d\n",mn,month,leap);*/
  int day=ymd[2];
	if(leap) for (int i=1; i< ymd[1]; i++) day+=dayleap[i];	/* add up all the full months */
  else for (int i=1; i< ymd[1]; i++) day+=daytab[i];	/* add up all the full months */
	return day;
} /**
	From a year, month, and day of the month 
	calculate the Julian day of year.  Parts stolen from K&R's C book.
 * @param yr The year
 * @param mon a three didget Man (first 3 letters of english month
 * @param day day of the monthe
 * @return The day of the year
*/
public static int doy_from_ymd(int yr, int mon,int day)
{	yr = sanitizeYear(yr);
  boolean leap= yr%4 == 0 && yr%100 != 0 || yr%400 == 0;	/* is it a leap year */
/*	printf("Mon=%s month=%d leap=%d\n",mn,month,leap);*/
	if(leap) for (int i=1; i< mon; i++) day+=dayleap[i];	/* add up all the full months */
  else for (int i=1; i< mon; i++) day+=daytab[i];	/* add up all the full months */
	return day;
} 
/**
	From a Unix 3 character month in ascii, a day of month, and year,
	calculate the Julian day of year.  Parts stolen from K&R's C book.
 * @param yr The year
 * @param mon a three didget Man (first 3 letters of english month
 * @param day day of the monthe
 * @return The day of the year
*/
public static int doy_from_ymd(int yr, String mon,int day)
{

	int month=-1;
	if(mon.equalsIgnoreCase("Jan")) month=1;				/* calculate month of year */
	if(mon.equalsIgnoreCase("Feb")) month=2;
	if(mon.equalsIgnoreCase("Mar")) month=3;
	if(mon.equalsIgnoreCase("Apr")) month=4;
	if(mon.equalsIgnoreCase("May")) month=5;
	if(mon.equalsIgnoreCase("Jun")) month=6;
	if(mon.equalsIgnoreCase("Jul")) month=7;
	if(mon.equalsIgnoreCase("Aug")) month=8;
	if(mon.equalsIgnoreCase("Sep")) month=9;
	if(mon.equalsIgnoreCase("Oct")) month=10;
	if(mon.equalsIgnoreCase("Nov")) month=11;
	if(mon.equalsIgnoreCase("Dec")) month=12;
  return doy_from_ymd(yr, month, day);
}

/** convert a year and day of year to an array in yr,mon,day order
 * @param yr The year
 * @param doy  The day of the year
 * @return an array in yr, mon, day
 *@throws RuntimeException ill formed especially doy being too big.
 */
public static  int [] ymd_from_doy(int yr, int doy) throws RuntimeException
{	int j;
	int sum;
  yr=sanitizeYear(yr);
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

/** sanitize year using the rule of 60, two digit ears >=60 = 1900+yr
 * years less than 60 are 2000+yr.  If its 4 digits already, just return it.
 * @param yr The year to sanitize
 * @return the year sanitized by rule of 60.
 */
public static int sanitizeYear(int yr) {
  if(yr >= 100) return yr;
  if(yr >= 60 && yr < 100) return yr+1900;
  else if( yr <60 && yr >=0) return yr+2000;
  System.out.println("Illegal year to sanitize ="+yr);
  return -1;
}
/**
* Returns the Julian day number that begins at noon of
* this day, Positive year signifies A.D., negative year B.C.
* Remember that the year after 1 B.C. was 1 A.D.  This is good
 * for translating day differences because it will calculate the
 * calendar days correctly.
*
* ref :
*  Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
*/

   // Gregorian Calendar adopted Oct. 15, 1582 (2299161)
   private static int JGREG= 15 + 31*(10+12*1582);
   private static double HALFSECOND = 0.5;
   /** Given a year and day of year, return the true julian day
    *@param year The year, this will be run through sanitizeYear()
    *@param doy The day of the year
    *@return The julian day
    */
   public static int toJulian(int year, int doy) {
     int [] ymd = ymd_from_doy
         (year, doy);
     return toJulian(ymd[0], ymd[1], ymd[2]);
   }
   
   /** create the julian day from a GregorianCalendar
    *@param now the GregorianCalendar to manipulate
    *@return the julian day
    */
   public static int toJulian(GregorianCalendar now) {
     return toJulian(now.get(Calendar.YEAR), now.get(Calendar.MONTH)+1, 
         now.get(Calendar.DAY_OF_MONTH));
   }

   /** Convert a Date to a Julian date.
    * @param date the date to convert
    * @return the Julian date
    */
   public static int toJulian(Date date) {
     GregorianCalendar cal;

     cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+0000"));
     cal.setTime(date);

     return toJulian(cal);
   }

   /** Convert a year, month, and day of month to a julian date
    *@param year The year - it will be satitizedYear()
    *@param month The month
    *@param day The day of the month.
    *@return the julian day
    */
   public static int toJulian(int year, int month, int day) {

    int julianYear = sanitizeYear(year);
    if (year < 0) julianYear++;
    int julianMonth = month;
    if (month > 2) {
        julianMonth++;
    }
    else {
        julianYear--;
        julianMonth += 13;
    }

    double julian = (java.lang.Math.floor(365.25 * julianYear)
      + java.lang.Math.floor(30.6001*julianMonth) + day + 1720995.0);

    if (day + 31 * (month + 12 * year) >= JGREG) {
       // change over to Gregorian calendar
       int ja = (int)(0.01 * julianYear);
       julian += 2 - ja + (0.25 * ja);
      }
      return (int) (java.lang.Math.floor(julian)+.000001);
   }

/**
* Converts a Julian day to a calendar date in a ymd array
* ref :
* Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
 *@param injulian The julian date (a big number!)
 *@return An array of 3 ints representing the year, month and day in that order
*/
   public static int[] fromJulian(int injulian) {

    int jalpha,ja,jb,jc,jd,je,year,month,day;
    double julian = (double) injulian + HALFSECOND / 86400.0;

    ja = (int) injulian;
    if (ja>= JGREG) {    
       jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
       ja = ja + 1 + jalpha - jalpha / 4;
    }

    jb = ja + 1524;
    jc = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
    jd = 365 * jc + jc / 4;
    je = (int) ((jb - jd) / 30.6001);
    day = jb - jd - (int) (30.6001 * je);
    month = je - 1;
    if (month > 12) month = month - 12;
    year = jc - 4715;
    if (month > 2) year--;
    if (year <= 0) year--;

    return new int[] {year, month, day};
 }
 /** for a julian day, create the file stub yyyy_doy
  *@param julian day
  *@return a string with yyyy_ddd
  */
 public static String fileStub(int julian) {
   int [] ymd = fromJulian(julian);
   int doy=doy_from_ymd(ymd[0], ymd[1], ymd[2]);
   return fileStub(ymd[0],doy);
 }
 /**
  * for a year a doy, create the file stub yyyy_doy
  * @param year It will be put through rule of 60(sanitizeYear)
  * @param jday The day of  year
  * @return a string with yyyy_ddd
  */
 public static String fileStub(int year, int jday) {
   DecimalFormat df4 = new DecimalFormat("0000");
   return df4.format(sanitizeYear(year))+"_"+df4.format(jday).substring(1,4);
 }
 /** Unit testing main routine - not used operationally
  *@param args - command line args
  */
 public static void main(String args[]) {
     // FIRST TEST reference point
     System.out.println("Julian date for May 23, 1968 : "
       + toJulian( 1968, 5, 23  ));
     // output : 2440000
     int results[] = fromJulian(toJulian(1968, 5, 23 ));
     System.out.println
       ("... back to calendar : " + results[0] + " "
        + results[1] + " " + results[2]);
     
     System.out.println("Julian date for Oct 15, 1582 : "
       + toJulian( 1582, 10, 15  ));
     // output : 2440000
     results = fromJulian(toJulian(1582, 10, 15 ));
     System.out.println
       ("... back to calendar : " + results[0] + " "
        + results[1] + " " + results[2]);

    // SECOND TEST today
    Calendar today = Calendar.getInstance();
    int todayJulian = toJulian
      (today.get(Calendar.YEAR), today.get(Calendar.MONTH)+1, 
         today.get(Calendar.DATE));
    System.out.println("Julian date for today : " + todayJulian);
    results = fromJulian(todayJulian);
    System.out.println
      ("... back to calendar : " + results[0] + " " + results[1] 
         + " " + results[2]);
    int jday = doy_from_ymd(today.get(Calendar.YEAR), today.get(Calendar.MONTH)+1, 
         today.get(Calendar.DATE));
    System.out.println("Today is julian day "+jday);
    System.out.println("Today is julian day "+toJulian(today.get(Calendar.YEAR), jday));
    // THIRD TEST
    int date1 = toJulian(2005,1,1);
    int date2 = toJulian(2005,1,31);
    System.out.println("Between 2005-01-01 and 2005-01-31 : "
      + (date2 - date1) + " days");
    /*
     expected output :

     Julian date for May 23, 1968 : 2440000.0
     ... back to calendar 1968 5 23
     Julian date for today : 2453487.0
     ... back to calendar 2005 4 26
     Between 2005-01-01 and 2005-01-31 : 30.0 days
    */
    jday = doy_from_ymd(2005, 12, 1);
    System.out.println("2005-12-1 is julday="+jday);
    jday = doy_from_ymd(2004, 12, 1);
    System.out.println("2004-12-1 is julday="+jday);
    int [] ymd;
    ymd = ymd_from_doy(2005, 335);
    System.out.println("2005-335 is "+ymd[0]+"/"+ymd[1]+"/"+ymd[2]);
    ymd = ymd_from_doy(2004, 335);
    System.out.println("2004-335 is "+ymd[0]+"/"+ymd[1]+"/"+ymd[2]);
    System.out.println("2004-35 is file="+fileStub(2004,35));
 }  
}
