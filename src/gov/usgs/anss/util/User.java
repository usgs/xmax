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


/** This class encapsulate the notion of a user of the system per the inventory system.
*
* @author david ketchum
* @version 1.00
*/

public  class User {
  static int userID=0;
  static String username="";        // Use initials or ID
  static String master="";          // Master priveleges
  static String name="";            // User name in person space
  static String password="";        // Pasword in the database
  static String phoneHome="";
  static String phoneWork="";
  static String phoneCell="";
  static String phoneFax="";
  static String email="";
  static String role="";
  static String allowedRoles="";
  String temp;
  
  public User(String u){ 
    Util.init(); temp=u;
    username=u; master="Y"; userID = 99;
    return;
  }
  public User() {
    Util.init(); 
    username="dkt"; master="Y"; userID=99;
    return;
  }

//
// Getter Methods
//
	public static String UserString() {
		return "user="+username+" master="+master+
		"name="+name;
	}
  
  public static String getEncodedPassword() {return password;}
  public static String getUser() { return username;}
  public static int getUserID() { return userID;}
  public static String getMaster() { return master.toUpperCase();}
  public static String getAllowedRoles() {return allowedRoles;}
  public static String getRoles(){return role;}
  public static boolean setRoles(String req) {
    if(allowedRoles.indexOf('M') < 0) {     // The master does not get checked
      for(int i=0; i<req.length(); i++ ) {
        if(allowedRoles.indexOf(req.charAt(i)) < 0) return false;
      }
    }
    role=req;
    return true;
  }
  public static boolean isRole(char rolein) {
    if(role.indexOf(rolein) >= 0) return true;
    if(role.indexOf('M') >= 0) return true;
    return false;
  }
  /** return true if the user has any of the roles in the string list
   *@param roles A list of single character roles
   *@return True if any of the roles are found in the users role string
   */
  public static boolean isRole(String roles) {
    for(int i=0; i<roles.length(); i++) 
      if(isRole(roles.charAt(i))) return true;
    return false;
  }
  /* Like User(Connection, String, String) without the password checking.
     All it does is set the password, role, etc. */
  public User(Connection C, String user) throws SQLException
  {
    Util.init();

    try {
      Statement stmt = C.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM inv.person"
              + " WHERE person = " + Util.sqlEscape(user));
      rs.next();
      userID= Util.getInt(rs,"ID");
      username = Util.getString(rs,"Person");
      master = Util.getString(rs,"Master");
      name = Util.getString(rs,"Name");
      phoneHome = Util.getString(rs,"PhoneHome");
      phoneWork = Util.getString(rs,"PhoneWork");
      phoneCell = Util.getString(rs,"PhoneCell");
      phoneFax = Util.getString(rs,"PhoneFax");
      email = Util.getString(rs,"Email");
      role = Util.getString(rs,"Role");
      allowedRoles=role;
			rs.close();
      stmt.close();
    }
    catch (SQLException E)
    { allowedRoles="M";
      role=allowedRoles;
      Util.prta("  ****** User "+user+" is not in anss.person assume master since roles are not set up");
      //Util.SQLErrorPrint(E,"SQL User failed");
      //E.printStackTrace();
      throw E;
    }
  }  /* Like User(Connection, String, String) without the password checking.
     All it does is set the password, role, etc. */
  public User(String database, Connection C, String user) 
  {
    Util.init();

    try {
      Statement stmt = C.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM "+database+".person"
              + " WHERE person = " + Util.sqlEscape(user));
      rs.next();
      userID= Util.getInt(rs,"ID");
      username = Util.getString(rs,"Person");
      master = Util.getString(rs,"Master");
      name = Util.getString(rs,"Name");
      phoneHome = Util.getString(rs,"PhoneHome");
      phoneWork = Util.getString(rs,"PhoneWork");
      phoneCell = Util.getString(rs,"PhoneCell");
      phoneFax = Util.getString(rs,"PhoneFax");
      email = Util.getString(rs,"Email");
      role = Util.getString(rs,"Role");
      allowedRoles=role;
			rs.close();
      stmt.close();
    }
    catch (SQLException E)
    { allowedRoles="M";
      role=allowedRoles;
      Util.prta("  ****** User "+user+" is not in "+database+".person assume master since roles are not set up");
      //Util.SQLErrorPrint(E,"SQL User failed");
      //E.printStackTrace();
    }
  }
  
  public User (Connection C, String user, String passwdEntered)
    throws SQLException, JCJBLBadPassword {
    //Util.prt("USER : " + user + " pwd:" + passwdEntered + " c="+C);
    Util.init();
    try {
      Statement stmt2 = C.createStatement();  // Use statement for query
      ResultSet rs2 = stmt2.executeQuery("SELECT password("
              + Util.sqlEscape(passwdEntered) + ")");
      rs2.next();
      String passReturned = rs2.getString(1).substring(0,16);
      Util.prt("Encode password gave " + passReturned);
      Statement stmt = C.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM inv.person"
              + " WHERE Person = " + Util.sqlEscape(user));
      rs.next();
      password = Util.getString(rs,"password");
//      Util.prt("Password for user = " + user +" is " + password);
      if( password.compareTo(passReturned) != 0 ) {
 				rs.close();
       Util.prt("not equal|" + password +"|"+ passReturned+"|" +
          password.length() + passReturned.length() + 
        " ct:" + password.compareTo(passReturned));
        throw new JCJBLBadPassword();
      }
      userID= Util.getInt(rs,"ID");
      username = Util.getString(rs,"Person");
      master = Util.getString(rs,"Master");
      name = Util.getString(rs,"Name");
      phoneHome = Util.getString(rs,"PhoneHome");
      phoneWork = Util.getString(rs,"PhoneWork");
      phoneCell = Util.getString(rs,"PhoneCell");
      phoneFax = Util.getString(rs,"PhoneFax");
      email = Util.getString(rs,"Email");
      role = Util.getString(rs,"Role");
			rs.close();
    }
    catch (SQLException E)
    { role="*";
      allowedRoles=role;
      Util.prta("  ****** User "+user+" is not in anss.person assume master since roles are not set up");
      //Util.SQLErrorPrint(E,"SQL User failed");
      E.printStackTrace();
      throw E;
    }

  }
}
