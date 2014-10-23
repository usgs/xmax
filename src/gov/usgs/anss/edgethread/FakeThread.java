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

package gov.usgs.anss.edgethread;

/** This sets up a faked up edgethead so that logging can be used from test routines
 *  that do not need to have a real EdgeThread.  Typically create one of these with
 * "-empty","FAKE", and then call setUseConsole(true) to have logging go to std out.
 * With setUseConsole(false) it would go to log/edgemom.log or the file set with EdgeThread.setMainLogName?
 * 
 *
 * @author davidketchum
 */
public class FakeThread extends EdgeThread {
  public String getMonitorString() {return "No MONITOR string";}
  public String getStatusString() {return "No STATUS string";}
  public String getConsoleOutput() {return "No console output";}
  public void terminate() {prt("Fake terminate called!");}

  public FakeThread(String argline, String tag) {
    super(argline, tag);
  }
}