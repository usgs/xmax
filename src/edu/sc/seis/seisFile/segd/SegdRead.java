package edu.sc.seis.seisFile.segd;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SegdRead {
	protected DataInput inStream;
	
	//--------------------------------------------------------
	//-----------Storage unit unitL data 128 bytes------------
    char[] storage_unit_seq_number 				= new char[4];
    char[] segd_revision 						= new char[5];
    char[] storage_unit_structure				= new char[6];  //fixed or variable
    char[] binding_edition						= new char[4];
    char[] max_block_size						= new char[10];
    char[] API_producer_org_code				= new char[10];
    char[] creation_date						= new char[11];
    char[] serial_number						= new char[21];
    char[] reserved_1							= new char[6];
    char[] ext_label_name						= new char[12];
    char[] recording_entity_name				= new char[24];
    char[] user_defined							= new char[14];
    char[] max_shot_records_per_field_record	= new char[10];

    List<SegdRecord> records = null;
	TOC toc = null;

	protected SegdRead() {
		records = new ArrayList<SegdRecord>();
	}

	public SegdRead(DataInput inStream) throws IOException {
		this.inStream = inStream;
	}

	public void close() throws IOException {
		inStream = null;
	}
}
