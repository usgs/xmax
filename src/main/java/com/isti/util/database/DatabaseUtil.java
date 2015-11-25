//DatabaseUtil:  Contains various static database utility functions
//
//   3/25/2003 -- [KF]  Initial version.
//   9/22/2004 -- [KF]  Added 'getWhereClause' method with 'wildCardFlag'
//                      parameter.
//
//

package com.isti.util.database;

import java.util.*;
import com.isti.util.Math10;

public class DatabaseUtil
{
  public final static String EMPTY_TEXT = "";

  //protected constructor so that no object instances may be created
  // (static access only)
  protected DatabaseUtil()
  {
  }

  //SQL Text String
  public final static String SELECT_DISTINCT_TEXT = "SELECT DISTINCT ";
  public final static String SELECT_ALL_TEXT = "SELECT * ";
  public final static String FROM_TEXT = " FROM ";
  public final static String WHERE_TEXT_BEGIN = " WHERE (";
  public final static String WHERE_TEXT_END = ")";
  public final static String ORDER_BY_TEXT = " ORDER BY ";

  /**
   * Inverts the value.
   * @param d value
   * @return inverse of the value.
   */
  public static double invertValue(double d)
  {
    return d > 0.0?1.0/d:0.0;
  }

  /**
   * convert value to frequency if needed
   * @param val value
   * @param periodFlag true if period
   * @return frequency value
   */
  public static double convertToFrequency(double val, boolean periodFlag)
  {
    // if period no conversion needed
    if (!periodFlag)
    {
      // convert frequency to period
      val = invertValue(val);
    }
    return val;
  }

  /**
   * convert value to period if needed
   * @param val value
   * @param periodFlag true if period
   * @return period value
   */
  public static double convertToPeriod(double val, boolean periodFlag)
  {
    // if period conversion needed
    if (periodFlag)
    {
      // convert frequency to period
      val = invertValue(val);
    }
    return val;
  }

  /**
   * convert the noise value from Acceleration to Velocity
   * value (low or high) and period
   * @param val acceleration value
   * @param p peroid
   * @return velocity value
   */
  public static double convertToVel(double val, double p)
  {
    return val + 20.0 * Math10.log10(p/(2.0 * Math.PI));
  }

  /**
   * convert the noise value from Acceleration to Velocity
   * value (low or high) and period if needed
   * @param val acceleration value
   * @param p peroid
   * @param velocityFlag true for velocity
   * @return velocity value
   */
  public static double convertToVel(double val, double p,
                                    boolean velocityFlag)
  {
    if (velocityFlag)
    {
      /// convert the noise value from Acceleration to Velocity
      val = convertToVel(val, p);
    }
    return val;
  }

  /**
   * @return the time in seconds
   * @param cal calendar
   */
  public static double getTimeInSeconds(Calendar cal)
  {
    return cal.getTime().getTime()/1000.0;
  }


  // DATATYPES

  // 15-byte ASCII single precision
  public final static String ASCII_SINGLE_PRECISION = "a0";
  // 24-byte ASCII double precision
  public final static String ASCII_DOUBLE_PRECISION = "b0";
  // 12-byte ASCII integer
  public final static String ASCII_INTEGER = "c0";
  // 15-byte ASCII single precision
  public final static String ASCII_SINGLE_PRECISION_ALT = "a#";
  // 24-byte ASCII double precision
  public final static String ASCII_DOUBLE_PRECISION_ALT = "b#";
  // 12-byte ASCII integer
  public final static String ASCII_INTEGER_ALT = "c#";
  // 4-byte SUN IEEE single precision real
  public final static String SUN_IEEE_SINGLE_PRECISION_REAL = "t4";
  // 8-byte SUN IEEE double precision real
  public final static String SUN_IEEE_DOUBLE_PRECISION_REAL = "t8";
  // 4-byte SUN IEEE integer
  public final static String SUN_IEEE_INTEGER = "s4";
  // 2-byte SUN IEEE short integer
  public final static String SUN_IEEE_SHORT_INTEGER = "s2";
  // 4-byte VAX IEEE single precision real
  public final static String VAX_IEEE_SINGLE_PRECISION_REAL = "f4";
  // 8-byte VAX IEEE double precision real
  public final static String VAX_IEEE_DOUBLE_PRECISION_REAL = "f8";
  // 4-byte VAX IEEE integer
  public final static String VAX_IEEE_INTEGER = "i4";
  // 2-byte VAX IEEE short integer
  public final static String VAX_IEEE_SHORT_INTEGER = "i2";
  // 2-byte NORESS gain-ranged
  public final static String NORESS_GAIN_RANGED = "g2";

  //Map methods

  /**
   * Gets a Object value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return Object value
   *
   * @exception  NullPointerException  if the value was not found.
   */
  public static Object getObject(Map map, String columnName)
  {
    Object val = map.get(columnName);
    if (val == null)  //if value not found
    {
      //determine if there is a column prefix
      int prefix = columnName.indexOf(".");
      if (prefix >= 0)  //if prefix was found
      {
        //try again without the column prefix
        columnName = columnName.substring(prefix + 1);
        val = map.get(columnName);
      }
    }
    if (val == null)  //if value not found
    {
      throw new NullPointerException();
    }
    return val;
  }

  /**
   * Gets a String value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return String value
   *
   * @exception  NullPointerException  if the value was not found.
   */
  public static String getString(Map map, String columnName)
  {
    Object val = getObject(map, columnName);
    return val.toString();
  }

  /**
   * Gets a byte value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return byte value
   *
   * @exception  NullPointerException  if the value was not found.
   * @exception  NumberFormatException  if the string does not contain a
   *               parsable number.
   */
  public static byte getByte(Map map, String columnName)
  {
    final byte retVal;

    Object val = getObject(map, columnName);
    if (val instanceof Number)
    {
      retVal = ((Number)val).byteValue();
    }
    else
    {
      retVal = new Byte(val.toString()).byteValue();
    }
    return retVal;
  }

  /**
   * Gets a double value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return double value
   *
   * @exception  NullPointerException  if the value was not found.
   * @exception  NumberFormatException  if the string does not contain a
   *               parsable number.
   */
  public static double getDouble(Map map, String columnName)
    throws NumberFormatException
  {
    final double retVal;

    Object val = getObject(map, columnName);
    if (val instanceof Number)
    {
      retVal = ((Number)val).doubleValue();
    }
    else
    {
      retVal = new Double(val.toString()).doubleValue();
    }
    return retVal;
  }

  /**
   * Gets a float value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return float value
   *
   * @exception  NullPointerException  if the value was not found.
   * @exception  NumberFormatException  if the string does not contain a
   *               parsable number.
   */
  public static float getFloat(Map map, String columnName)
  {
    final float retVal;

    Object val = getObject(map, columnName);
    if (val instanceof Number)
    {
      retVal = ((Number)val).floatValue();
    }
    else
    {
      retVal = new Float(val.toString()).floatValue();
    }
    return retVal;
  }

  /**
   * Gets a int value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return int value
   *
   * @exception  NullPointerException  if the value was not found.
   * @exception  NumberFormatException  if the string does not contain a
   *               parsable number.
   */
  public static int getInt(Map map, String columnName)
  {
    final int retVal;

    Object val = getObject(map, columnName);
    if (val instanceof Number)
    {
      retVal = ((Number)val).intValue();
    }
    else
    {
      retVal = new Integer(val.toString()).intValue();
    }
    return retVal;
  }

  /**
   * Gets a long value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return long value
   *
   * @exception  NullPointerException  if the value was not found.
   * @exception  NumberFormatException  if the string does not contain a
   *               parsable number.
   */
  public static long getLong(Map map, String columnName)
  {
    final long retVal;

    Object val = getObject(map, columnName);
    if (val instanceof Number)
    {
      retVal = ((Number)val).longValue();
    }
    else
    {
      retVal = new Long(val.toString()).longValue();
    }
    return retVal;
  }

  /**
   * Gets a short value from the database map.
   * @param map database map
   * @param columnName database column name
   * @return short value
   *
   * @exception  NullPointerException  if the value was not found.
   * @exception  NumberFormatException  if the string does not contain a
   *               parsable number.
   */
  public static short getShort(Map map, String columnName)
  {
    final short retVal;

    Object val = getObject(map, columnName);
    if (val instanceof Number)
    {
      retVal = ((Number)val).shortValue();
    }
    else
    {
      retVal = new Short(val.toString()).shortValue();
    }
    return retVal;
  }


  //Property names

  //host name property
  public final static String PROPERTY_HOST_NAME = "HOST_NAME";
  //port property
  public final static String PROPERTY_PORT = "PORT";
  //sid property
  public final static String PROPERTY_SID = "SID";
  //user name property
  public final static String PROPERTY_USER_NAME = "USER_NAME";
  //password property
  public final static String PROPERTY_PASS_WORD = "PASS_WORD";

  // Properties methods

  /**
   * Set host name
   * @param info properties information
   * @param hostName host name
   */
  public final static void setHostName(Properties info, String hostName)
  {
    info.setProperty(PROPERTY_HOST_NAME, hostName);
  }

  /**
   * Set port
   * @param info properties information
   * @param port port value
   */
  public final static void setPort(Properties info, String port)
  {
    info.setProperty(PROPERTY_PORT, port);
  }

  /**
   * Set sid
   * @param info properties information
   * @param sid sid value
   */
  public final static void setSid(Properties info, String sid)
  {
    info.setProperty(PROPERTY_SID, sid);
  }

  /**
   * Set user name
   * @param info properties information
   * @param userName user name
   */
  public final static void setUserName(Properties info, String userName)
  {
    info.setProperty(PROPERTY_USER_NAME, userName);
  }

  /**
   * Set pass word
   * @param info properties information
   * @param passWord pass word
   */
  public final static void setPassWord(Properties info, String passWord)
  {
    info.setProperty(PROPERTY_PASS_WORD, passWord);
  }

  /**
   * @return host name
   * @param info properties information
   * @exception InstantiationException if the information was not found.
   */
  public final static String getHostName(Properties info)
      throws InstantiationException
  {
    String hostName = info.getProperty(PROPERTY_HOST_NAME);
    if (hostName == null || hostName.length() <= 0)
    {
      throw new InstantiationException(
          "Invalid host name: " + hostName);
    }
    return hostName;
  }

  /**
   * @return port
   * @param info properties information
   * @exception InstantiationException if the information was not found.
   */
  public final static int getPort(Properties info)
      throws InstantiationException
  {
    String port = info.getProperty(PROPERTY_PORT);
    Integer portNumber;
    try
    {
      portNumber = new Integer(port);
    }
    catch (Exception ex)
    {
      portNumber = null;
    }
    if (port == null || port.length() <= 0 || portNumber == null)
    {
      throw new InstantiationException(
          "Invalid port: " + port);
    }
    return portNumber.intValue();
  }

  /**
   * @return sid
   * @param info properties information
   * @exception InstantiationException if the information was not found.
   */
  public final static String getSid(Properties info)
      throws InstantiationException
  {
    String sid = info.getProperty(PROPERTY_SID);
    if (sid == null || sid.length() <= 0)
    {
      throw new InstantiationException(
          "Invalid SID: " + sid);
    }
    return sid;
  }

  /**
   * @return user name
   * @param info properties information
   * @exception InstantiationException if the information was not found.
   */
  public final static String getUserName(Properties info)
      throws InstantiationException
  {
    String userName = info.getProperty(PROPERTY_USER_NAME);
    if (userName == null || userName.length() <= 0)
    {
      throw new InstantiationException(
          "Invalid user name: " + userName);
    }
    return userName;
  }

  /**
   * @return pass word or user name if no pass word exists
   * @param info properties information
   * @exception InstantiationException if the information was not found.
   */
  public final static String getPassWord(Properties info)
      throws InstantiationException
  {
    String defaultPassWord;
    try
    {
      defaultPassWord = getUserName(info);
    }
    catch (InstantiationException instEx)
    {
      defaultPassWord = null;
    }
    return getPassWord(info, defaultPassWord);
  }

  /**
   * @return pass word
   * @param info properties information
   * @param defaultPassWord default pass word
   * @exception InstantiationException if the information was not found.
   */
  public final static String getPassWord(Properties info,
      String defaultPassWord)
      throws InstantiationException
  {
    String passWord = info.getProperty(PROPERTY_PASS_WORD);
    if (passWord == null || passWord.length() <= 0)
    {
      if (defaultPassWord == null || defaultPassWord.length() <= 0)
      {
        throw new InstantiationException(
            "Invalid pass word: " + passWord);
      }
      else
      {
        passWord = defaultPassWord;
      }
    }
    return passWord;
  }

  /**
   * @return the column prefix
   * @param tablePrefix table prefix
   */
  public static String getColumnPrefix(String tablePrefix)
  {
    String text = EMPTY_TEXT;
    if (tablePrefix.length() > 0)
    {
      text = tablePrefix + ".";
    }
    return text;
  }

  /**
   * Adds the where clause to the text.
   * @param text text for the entire where clause
   * @param newText new where clause text
   * @return text for the entire where clause
   */
  private static String addWhereClause(String text, String newText)
  {
    if (newText != null)
    {
      if (text != null && text.length() > 0)
      {
        text += " and ";
      }
      text += newText;
    }
    return text;
  }

  /**
   * Gets the where clause for the specified value.
   * @param text text for the entire where clause
   * @param columnName column name
   * @param columnText column text
   * @return the where clause or null if empty text
   */
  public static String getWhereClause(
      String text, String columnName, String columnText)
  {
    return getWhereClause(text, columnName, columnText, true);
  }

  /**
   * Gets the where clause for the specified value.
   * @param text text for the entire where clause
   * @param columnName column name
   * @param columnText column text
   * @param wildCardFlag true to check for wild cards ('%' or '_'),
   * false otherwise.
   * @return the where clause or null if empty text
   */
  public static String getWhereClause(
      String text, String columnName, String columnText, boolean wildCardFlag)
  {
    String newText = null;
    columnText = columnText.trim();  //remove white space

    if (columnText.length() > 0)
    {
      newText =  "(" + columnName + " ";
      if (wildCardFlag &&
          (columnText.indexOf("%") >= 0 || columnText.indexOf("_") >= 0))
      {
        newText += "like";
      }
      else
      {
        newText += "=";
      }
      newText += " '" + columnText + "')";
    }
    return addWhereClause(text, newText);
  }

  /**
   * Gets the where clause for the specified value.
   * @param text text for the entire where clause
   * @param columnName column name
   * @param columnValues array of column values
   * @return the where clause or null if empty text
   */
  public static String getWhereClause(
      String text, String columnName, int[] columnValues)
  {
    String newText = null;

    for (int i = 0; i < columnValues.length; i++)
    {
      int columnValue = columnValues[i];

      if (newText == null)
      {
        newText = columnName + " in (";
      }
      else
      {
        newText += ",";
      }
      newText += columnValue;
    }

    if (newText != null)
    {
      newText += ")";
    }
    return addWhereClause(text, newText);
  }

  /**
   * Gets the where clause for the specified value.
   * @param text text for the entire where clause
   * @param columnName column name
   * @param columnBeginValue column begin value
   * @param columnEndValue column end value
   * @return the where clause or null if empty text
   */
  public static String getWhereClause(
      String text, String columnName,
      int columnBeginValue, int columnEndValue)
  {
    String newText = null;

    if (columnBeginValue <= columnEndValue)
    {
      newText = columnName + " between " +
                columnBeginValue + " and " + columnEndValue;
    }
    else
    {
      newText = columnName + " not between " +
                columnEndValue + " and " + columnBeginValue;
    }
    return addWhereClause(text, newText);
  }

  /**
   * Gets the where clause for the specified value.
   * @param text text for the entire where clause
   * @param fullColumnNames array of full column names
   * @return the where clause or null if empty text
   */
  public static String getWhereClause(
      String text, String[] fullColumnNames)
  {
    String newText = EMPTY_TEXT;

    for (int i = 0; i < fullColumnNames.length; i++)
    {
      if (i > 0)
        newText += " = ";
      newText += fullColumnNames[i];
    }
    return addWhereClause(text, newText);
  }

  /**
   * Gets the where clause for the specified value.
   * @param text text for the entire where clause
   * @param columnName column name
   * @param columnBeginText column begin text
   * @param columnEndText column end text
   * @return the where clause or null if empty text
   */
  public static String getWhereClause(
      String text, String columnName,
      String columnBeginText, String columnEndText)
  {
    String newText = null;
    columnBeginText = columnBeginText.trim();  //remove white space
    columnEndText = columnEndText.trim();  //remove white space

    if (columnBeginText.length() > 0 && columnEndText.length() > 0)
    {
      newText = "(" + columnName + " between " +
                columnBeginText + " and " + columnEndText + ")";
    }
    return addWhereClause(text, newText);
  }

  /**
   * @return the select clause
   * @param text select text
   * @param columnNames column names
   */
  public static String getSelectClause(
      String text, String[] columnNames)
  {
    text = getSelectClause(text, columnNames, columnNames.length);
    return text;
  }

  /**
   * @return the select clause
   * @param text select text
   * @param columnNames column names
   * @param selectCount selection count
   */
  public static String getSelectClause(
      String text, String[] columnNames, int selectCount)
  {
    if (selectCount > 0)
    {
      for (int i = 0; i <  selectCount; i++)
      {
        if (text.length() > 0)
          text += ",";
        text += columnNames[i];
      }
    }
    return text;
  }

  /**
   * @return the from clause
   * @param text from text
   * @param tableName table name
   * @param tablePrefix table prefix
   */
  public static String getFromClause(
      String text, String tableName, String tablePrefix)
  {
    if (text.length() > 0)
      text += ",";
    text += tableName;
    if (tablePrefix.length() > 0)
    {
      text += " " + tablePrefix;
    }
    return text;
  }

  /**
   * @return the order by clause
   * @param text select text
   * @param columnNames column names
   * @param orderCount number of columns to use for ordering
   */
  public static String getOrderByClause(
      String text, String[] columnNames, int orderCount)
  {
    if (columnNames.length > 0 && orderCount > 0)
    {
      for (int i = 0; i <  columnNames.length && i < orderCount; i++)
      {
        if (text.length() > 0)
          text += ",";
        text += columnNames[i];
      }
    }
    return text;
  }

  /**
   * Combines the clauses to create the sql statement for a query.
   * @param selectText select text
   * @param fromText from text
   * @param whereText where text
   * @param orderByText order by text
   * @return the sql statement
   *
   * @see executeQuery
   */
  public static String combineClauses(
      String selectText, String fromText, String whereText, String orderByText)
  {
    String sql;

    //select
    if (selectText.length() > 0)
    {
      sql = DatabaseUtil.SELECT_DISTINCT_TEXT + selectText;
    }
    else
    {
      sql = DatabaseUtil.SELECT_ALL_TEXT;
    }

    //from
    if (fromText.length() > 0)
    {
      sql += DatabaseUtil.FROM_TEXT + fromText;
    }

    //where
    if (whereText.length() > 0)
    {
      sql += DatabaseUtil.WHERE_TEXT_BEGIN +
             whereText +
             DatabaseUtil.WHERE_TEXT_END;
    }

    //order by
    if (orderByText.length() > 0)
    {
      sql += DatabaseUtil.ORDER_BY_TEXT + orderByText;
    }

    return sql;
  }

  /**
   * @return the column text for the specified column or null if none.
   * @param columnNames array of column names
   * @param columnName column name
   */
  public static String getColumnText(
      String[] columnNames, String columnName)
  {
    for (int i = 0; i < columnNames.length; i++)
    {
      if (columnNames[i].equals(columnName))
        return columnName;
    }
    return null;
  }
}