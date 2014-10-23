/**
 * SEGD Table Of Content File
 */

package edu.sc.seis.seisFile.segd;

import java.util.ArrayList;
import java.util.List;

public class TOC {
	
	//------------------TOC file 448 bytes header-----------------
    char[] tocID 								= new char[13];
    char[] segd_revision 						= new char[5];
    char[] serial_number						= new char[11];
    char[] clientID								= new char[31];
    char[] vesselID								= new char[31];
    char[] country								= new char[31];
    char[] region								= new char[31];
    char[] block								= new char[31];
    char[] survey_area_name						= new char[31];
    char[] contract_num							= new char[31];
    char[] licenseID							= new char[31];
    char[] survey_type							= new char[31];   
    char[] acquisition_contractor				= new char[31];
    char[] acquisition_dates					= new char[24];
    char[] jobID								= new char[31];
    char[] media_sequence						= new char[5];
    char[] dataTypeID							= new char[31];
    char[] undefined							= new char[3];
    char[] tocEntrries_num						= new char[9];
    
    List<TOCentry> TOCentries = new ArrayList<TOCentry>();

}
