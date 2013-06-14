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

import java.sql.*;
import java.awt.Color;
import java.awt.Component;

/**
 * This class holds constants and other (mostly) static data. It also holds one
 * reference to a Connection object for global access.
 *
 * The static methods can be hidden in a subclass if some settings need to be
 * changed. Beware: because of the way static inheritance works, you need to
 * define the value-returning methods in the subclass, not just the constants
 * they return. So, to change the property filename in a subclass, define
 * getPropertyFilename, not PROPERTY_FILENAME.
 */
public class UC
{
  /* These users have a special hard-coded database server. Using any of these
     users will cause isTestDB to return true. */
  private static final String[] TEST_USERS = { "dkt", "local", "test" };

  private static final String[] FORM_HOSTS = { "NEISA.CR.USGS.GOV", "NSN1.CR.USGS.GOV" };

  private static String JDBC_DRIVER_NAME = "com.mysql.jdbc.Driver";
  private static final String JDBC_CATALOG = null;
  private static final String JDBC_TEST_HOST = "136.177.30.50";

  private static final String FEDERAL_CENTER_IP_ADDR = "136.177.23.242";

  private static final String PROPERTY_FILENAME = "anss.prop";

  public static final String DEFAULT_USER = "inv";
  public static final String DEFAULT_PASSWORD = "nop240";

  public static final int XSIZE = 750;
  public static final int YSIZE = 650;

  public static String DEFAULT_RTS_VMS_NODE="nsn7";
  //public static String DEFAULT_CONSOLE_VMS_NODE="nsn7";
  public static String DEFAULT_CONSOLE_VMS_NODE="gacq1";
  public static String DEFAULT_DATA_VMS_NODE="nsn7";
  public static String DEFAULT_MSHEAR_NODE="gacq1";
  public static String DEFAULT_COMMAND_NODE="gacq1";
  public static String DEFAULT_STATUS_UDP_NODE="gacq1";
  public static String DEFAULT_QDPING_UDP_NODE="gacq1";
  public static String DEFAULT_TUNNEL_NODE="gacq1";
  
  public static String DEFAULT_GPS_VMS_NODE="nsn7";
  public static String RTS_SERVER_NODE="gacq1"; // Where the RTSServer program is running

  private static Connection C;

  public static void setFedCtr()
  {
    if (Util.getProperty("MySQLServer").equals(FEDERAL_CENTER_IP_ADDR)) {
      DEFAULT_RTS_VMS_NODE="nsn1";
      DEFAULT_CONSOLE_VMS_NODE="nsn1";
      DEFAULT_DATA_VMS_NODE="nsn1";
      DEFAULT_MSHEAR_NODE="vdldfc";
      DEFAULT_COMMAND_NODE="vdldfc";
      DEFAULT_STATUS_UDP_NODE="vdldfc";

      DEFAULT_GPS_VMS_NODE="nsn1";
      RTS_SERVER_NODE="136.177.23.85"; // Where the RTSServer program is running
      Util.prt("Using FedCtr Computers!!!!!");
    }
    if(Util.getProperty("MySQLServer").equals("136.177.36.20")) {
      DEFAULT_STATUS_UDP_NODE="gacqlab";
      RTS_SERVER_NODE="gacqlab";
      DEFAULT_STATUS_UDP_NODE="gacqlab";
      DEFAULT_COMMAND_NODE="gacqlab";
    }
  }

  /**
   * Return an array of form host names.
   *
   * @return An array of host names.
   */
  public static String[] getFormHosts()
  {
    return FORM_HOSTS;
  }

  /**
   * Return true if the user of the database (as defined by User.getUser()) is
   * one of a few test users; false otherwise.
   *
   * @return A boolean indicating whether the user is a test user.
   */
  public static boolean isTestDB()
  {
    int i;

    for (i = 0; i < TEST_USERS.length; i++) {
      if (User.getUser().equalsIgnoreCase(TEST_USERS[i]))
        return true;
    }

    return false;
  }

  /**
   * Return the name of the driver to use for JDBC connections.
   *
   * @return The JDBC driver class name.
   */
  public static String JDBCDriver()
  {
    return JDBC_DRIVER_NAME;
  }

  /**
   * Return the host name of the MySQL server. This is taken from the user's
   * properties unless a test database is being used.
   *
   * @return The host name of the MySQL server.
   */
  public static String JDBCHost()
  {
    if (isTestDB())
      return JDBC_TEST_HOST;
    else
      return Util.getProperty("MySQLServer");
  }

  /**
   * Return the name of the database to use (JDBC calls this a "catalog").
   *
   * @return The name of the database.
   */
  public static String JDBCCatalog()
  {
    return JDBC_CATALOG;
  }

  /**
   * Return a URL containing the location of the database to connect to. The
   * host name and catalog are calculated using {@link #JDBCCatalog()} and
   * {@link #JDBCHost()}.
   *
   * @return A JDBC URL.
   */
  public static String JDBCDatabase()
  {
    Util.prt("database user: " + User.getUser());

    if (isTestDB())
      Util.prt("\n   *** Note: TEST database in use ****\n");
    else
      Util.prt("\n   *** Live data base in use ****\n");

    return "jdbc:mysql://" + JDBCHost() + "/" + JDBCCatalog()
            + "?user=" + DEFAULT_USER + "&password=" + DEFAULT_PASSWORD;
  }

  /**
   * Save a Connection for later retrieval.
   *
   * @param Cin The Connection to save.
   */
  public static void setConnection(Connection Cin)
  { Util.prt("Set connection to "+Cin);
    C = Cin;
  }

  /**
   * Return the Connection previously saved with {@link
   * #setConnection(Connection)}.
   *
   * @return A reference to the saved Connection object.
   */
  public static Connection getConnection()
  {
    return C;
  }

  /**
   * Return the name of the file that holds user properties.
   *
   * @return The property filename
   */
  public static String getPropertyFilename()
  {
    return PROPERTY_FILENAME;
  }

  public static final Color look = new Color(153, 255, 204);
  public static final Color yellow = new Color(255, 255, 204);
  public static final Color red = new Color(255, 204, 204);
  public static final Color purple = new Color(200, 200, 255);
  public static final Color white = Color.white;

  /**
   * Give a Component a certain look. This just sets the background color.
   *
   * @param c The Component to modify
   */
  public static void Look(Component c)
  { 
    c.setBackground(look);
  }
}
