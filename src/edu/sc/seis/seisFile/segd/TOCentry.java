/**
 * 64-byte Table Of Content entry, contains info about one SEGD record
 */

package edu.sc.seis.seisFile.segd;

public class TOCentry {
	char[] mediaFile_num		= new char[8];
	char[] record_within_file	= new char[6];
	char[] time_of_shot			= new char[9];
	char[] segd_file_number		= new char[9];
	char[] recordID				= new char[4];
	char[] recordSet_num		= new char[9];
	char[] lineID				= new char[24];
	char[] user_defined			= new char[33];
}
