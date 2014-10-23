package edu.sc.seis.seisFile.segd;

import java.io.DataInput;
import java.io.IOException;

public class AdditionalGeneralHeader {
	
	public enum PhaseControl {
		NOT_RECORDED, BASEPLATE_ACCELEROMETER, REACTION_MASS, WEIGHTED_SUM, DIRECT_FORCE_MEASUREMENT
	};
	
	public enum VibratorType {
		NOT_RECORDED, P_WAVE, SHEAR_WAVE, MARINE
	};
	
	int fileNumber = -1;
	int sourceLineNumberInteger = -1;
	int sourceLineNumberFraction = -1;
	int sourcePointNumberInteger = -1;
	int sourcePointNumberFraction = -1;
	short sourcePointIndex = -1;
	PhaseControl phaseControl = null;
	VibratorType vibratorType = null;
	short phaseAngle = 0;
	int generalHeaderBlock_number = -1;
	int sourceSet_number = -1;
	
	public AdditionalGeneralHeader(){
	}
	
	public void read(DataInput inStream) throws IOException{
		try {
			fileNumber = SegdRecord.getDataValue(SegdRecord.getSections(SegdRecord.readBytes(inStream, 3, null), 1), 2);
			sourceLineNumberInteger = SegdRecord.readSignedTriple(inStream);
			sourceLineNumberFraction = SegdRecord.readShorts(inStream, 1)[0];
			sourcePointNumberInteger = SegdRecord.readSignedTriple(inStream);
			sourcePointNumberFraction = SegdRecord.readShorts(inStream, 1)[0];
			sourcePointIndex = SegdRecord.readBytes(inStream, 1, null)[0];
			short byte15 = SegdRecord.readBytes(inStream, 1, null)[0];
			switch (byte15) {
			case 0:
				phaseControl = PhaseControl.NOT_RECORDED;
				break;
			case 1:
				phaseControl = PhaseControl.BASEPLATE_ACCELEROMETER;
				break;
			case 2:
				phaseControl = PhaseControl.REACTION_MASS;
				break;
			case 3:
				phaseControl = PhaseControl.WEIGHTED_SUM;
				break;
			case 4:
				phaseControl = PhaseControl.DIRECT_FORCE_MEASUREMENT;
				break;
			default:
				phaseControl = null;
			}
			short byte16 = SegdRecord.readBytes(inStream, 1, null)[0];
			switch (byte16) {
			case 0:
				vibratorType = VibratorType.NOT_RECORDED;
				break;
			case 1:
				vibratorType = VibratorType.P_WAVE;
				break;
			case 2:
				vibratorType = VibratorType.SHEAR_WAVE;
				break;
			case 3:
				vibratorType = VibratorType.MARINE;
				break;
			default:
				vibratorType = null;
			}
			phaseAngle = inStream.readShort();
			generalHeaderBlock_number = SegdRecord.readBytes(inStream, 1, null)[0];
			sourceSet_number = SegdRecord.readBytes(inStream, 1, null)[0];
			inStream.skipBytes(12); //not used
		} catch (CheckFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String toString(){
		return "\nAdditional General Header # " + generalHeaderBlock_number
		+"\n\tFile number: " + fileNumber
		+"\n\tSource line number: " + sourceLineNumberInteger + "." + sourceLineNumberFraction
		+"\n\tSource point number: " + sourcePointNumberInteger + "." + sourcePointNumberFraction
		+"\n\tSource point index: " + sourcePointIndex
		+"\n\tPhase control: " + phaseControl
		+"\n\tVibrator type: " + vibratorType
		+"\n\tPhase angle: " + phaseAngle
		+"\n\tSource set number: " + sourceSet_number;
	}
}
