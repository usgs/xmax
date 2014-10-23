package com.isti.traceview.data;

import static edu.sc.seis.seisFile.sac.SacConstants.FALSE;
import static edu.sc.seis.seisFile.sac.SacConstants.ITIME;
import static edu.sc.seis.seisFile.sac.SacConstants.TRUE;
import static edu.sc.seis.seisFile.sac.SacConstants.IAMPH;
import static edu.sc.seis.seisFile.sac.SacConstants.IRLIM;
import static edu.sc.seis.seisFile.sac.SacConstants.IntelByteOrder;
import static edu.sc.seis.seisFile.sac.SacConstants.LITTLE_ENDIAN;
import static edu.sc.seis.seisFile.sac.SacConstants.data_offset;
import static edu.sc.seis.seisFile.sac.SacConstants.IXY;
import static edu.sc.seis.seisFile.sac.SacConstants.IUNKN;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import edu.sc.seis.seisFile.sac.SacHeader;
import edu.sc.seis.seisFile.sac.SacTimeSeries;


public class SacTimeSeriesASCII extends SacTimeSeries {
	public static final DecimalFormat floatFormat = new DecimalFormat("######0.0000000"); // G15.7
	public static final DecimalFormat intFormat = new DecimalFormat("##########"); // I10

	public SacTimeSeriesASCII() {
		super();
	}
	
	public SacTimeSeriesASCII(SacHeader header, float[] data) {
		super(header, data);
	}
	
	public void writeHeader(DataOutputStream dos) throws IOException {
		// 1
		writeFmt(dos, getHeader().getDelta());
		writeFmt(dos, getHeader().getDepmin());
		writeFmt(dos, getHeader().getDepmax());
		writeFmt(dos, getHeader().getScale());
		writeFmt(dos, getHeader().getOdelta());
		dos.writeBytes("\n");
		// 2
		writeFmt(dos, getHeader().getB());
		writeFmt(dos, getHeader().getE());
		writeFmt(dos, getHeader().getO());
		writeFmt(dos, getHeader().getA());
		writeFmt(dos, getHeader().getFmt());
		dos.writeBytes("\n");
		// 3
		writeFmt(dos, getHeader().getT0());
		writeFmt(dos, getHeader().getT1());
		writeFmt(dos, getHeader().getT2());
		writeFmt(dos, getHeader().getT3());
		writeFmt(dos, getHeader().getT4());
		dos.writeBytes("\n");
		// 4
		writeFmt(dos, getHeader().getT5());
		writeFmt(dos, getHeader().getT6());
		writeFmt(dos, getHeader().getT7());
		writeFmt(dos, getHeader().getT8());
		writeFmt(dos, getHeader().getT9());
		dos.writeBytes("\n");
		// 5
		writeFmt(dos, getHeader().getF());
		writeFmt(dos, getHeader().getResp0());
		writeFmt(dos, getHeader().getResp1());
		writeFmt(dos, getHeader().getResp2());
		writeFmt(dos, getHeader().getResp3());
		dos.writeBytes("\n");
		// 6
		writeFmt(dos, getHeader().getResp4());
		writeFmt(dos, getHeader().getResp5());
		writeFmt(dos, getHeader().getResp6());
		writeFmt(dos, getHeader().getResp7());
		writeFmt(dos, getHeader().getResp8());
		dos.writeBytes("\n");
		// 7
		writeFmt(dos, getHeader().getResp9());
		writeFmt(dos, getHeader().getStla());
		writeFmt(dos, getHeader().getStlo());
		writeFmt(dos, getHeader().getStel());
		writeFmt(dos, getHeader().getStdp());
		dos.writeBytes("\n");
		// 8
		writeFmt(dos, getHeader().getEvla());
		writeFmt(dos, getHeader().getEvlo());
		writeFmt(dos, getHeader().getEvel());
		writeFmt(dos, getHeader().getEvdp());
		writeFmt(dos, getHeader().getMag());
		dos.writeBytes("\n");
		// 9
		writeFmt(dos, getHeader().getUser0());
		writeFmt(dos, getHeader().getUser1());
		writeFmt(dos, getHeader().getUser2());
		writeFmt(dos, getHeader().getUser3());
		writeFmt(dos, getHeader().getUser4());
		dos.writeBytes("\n");
		// 10
		writeFmt(dos, getHeader().getUser5());
		writeFmt(dos, getHeader().getUser6());
		writeFmt(dos, getHeader().getUser7());
		writeFmt(dos, getHeader().getUser8());
		writeFmt(dos, getHeader().getUser9());
		dos.writeBytes("\n");
		// 11
		writeFmt(dos, getHeader().getDist());
		writeFmt(dos, getHeader().getAz());
		writeFmt(dos, getHeader().getBaz());
		writeFmt(dos, getHeader().getGcarc());
		writeFmt(dos, getHeader().getSb());
		dos.writeBytes("\n");
		// 12
		writeFmt(dos, getHeader().getSdelta());
		writeFmt(dos, getHeader().getDepmen());
		writeFmt(dos, getHeader().getCmpaz());
		writeFmt(dos, getHeader().getCmpinc());
		writeFmt(dos, getHeader().getXminimum());
		dos.writeBytes("\n");
		// 13
		writeFmt(dos, getHeader().getXmaximum());
		writeFmt(dos, getHeader().getYminimum());
		writeFmt(dos, getHeader().getYmaximum());
		writeFmt(dos, getHeader().getUnused6());
		writeFmt(dos, getHeader().getUnused7());
		dos.writeBytes("\n");
		// 14
		writeFmt(dos, getHeader().getUnused8());
		writeFmt(dos, getHeader().getUnused9());
		writeFmt(dos, getHeader().getUnused10());
		writeFmt(dos, getHeader().getUnused11());
		writeFmt(dos, getHeader().getUnused12());
		dos.writeBytes("\n");
		// 15
		writeFmt(dos, getHeader().getNzyear());
		writeFmt(dos, getHeader().getNzjday());
		writeFmt(dos, getHeader().getNzhour());
		writeFmt(dos, getHeader().getNzmin());
		writeFmt(dos, getHeader().getNzsec());
		dos.writeBytes("\n");
		// 16
		writeFmt(dos, getHeader().getNzmsec());
		writeFmt(dos, getHeader().getNvhdr());
		writeFmt(dos, getHeader().getNorid());
		writeFmt(dos, getHeader().getNevid());
		writeFmt(dos, getHeader().getNpts());
		dos.writeBytes("\n");
		// 17
		writeFmt(dos, getHeader().getNsnpts());
		writeFmt(dos, getHeader().getNwfid());
		writeFmt(dos, getHeader().getNxsize());
		writeFmt(dos, getHeader().getNysize());
		writeFmt(dos, getHeader().getUnused15());
		dos.writeBytes("\n");
		// 18
		writeFmt(dos, getHeader().getIftype());
		writeFmt(dos, getHeader().getIdep());
		writeFmt(dos, getHeader().getIztype());
		writeFmt(dos, getHeader().getUnused16());
		writeFmt(dos, getHeader().getIinst());
		dos.writeBytes("\n");
		// 19
		writeFmt(dos, getHeader().getIstreg());
		writeFmt(dos, getHeader().getIevreg());
		writeFmt(dos, getHeader().getIevtyp());
		writeFmt(dos, getHeader().getIqual());
		writeFmt(dos, getHeader().getIsynth());
		dos.writeBytes("\n");
		// 20
		writeFmt(dos, getHeader().getImagtyp());
		writeFmt(dos, getHeader().getImagsrc());
		writeFmt(dos, getHeader().getUnused19());
		writeFmt(dos, getHeader().getUnused20());
		writeFmt(dos, getHeader().getUnused21());
		dos.writeBytes("\n");
		// 21
		writeFmt(dos, getHeader().getUnused22());
		writeFmt(dos, getHeader().getUnused23());
		writeFmt(dos, getHeader().getUnused24());
		writeFmt(dos, getHeader().getUnused25());
		writeFmt(dos, getHeader().getUnused26());
		dos.writeBytes("\n");
		// 22
		writeFmt(dos, getHeader().getLeven());
		writeFmt(dos, getHeader().getLpspol());
		writeFmt(dos, getHeader().getLovrok());
		writeFmt(dos, getHeader().getLcalda());
		writeFmt(dos, getHeader().getUnused27());
		dos.writeBytes("\n");
		// 23
		writeFmt(dos, getHeader().getKstnm(), 8);
		writeFmt(dos, getHeader().getKevnm(), 16);
		dos.writeBytes("\n");
		// 24
		writeFmt(dos, getHeader().getKhole(), 8);
		writeFmt(dos, getHeader().getKo(), 8);
		writeFmt(dos, getHeader().getKa(), 8);
		dos.writeBytes("\n");
		// 25
		writeFmt(dos, getHeader().getKt0(), 8);
		writeFmt(dos, getHeader().getKt1(), 8);
		writeFmt(dos, getHeader().getKt2(), 8);
		dos.writeBytes("\n");
		// 26
		writeFmt(dos, getHeader().getKt3(), 8);
		writeFmt(dos, getHeader().getKt4(), 8);
		writeFmt(dos, getHeader().getKt5(), 8);
		dos.writeBytes("\n");
		// 27
		writeFmt(dos, getHeader().getKt6(), 8);
		writeFmt(dos, getHeader().getKt7(), 8);
		writeFmt(dos, getHeader().getKt8(), 8);
		dos.writeBytes("\n");
		// 28
		writeFmt(dos, getHeader().getKt9(), 8);
		writeFmt(dos, getHeader().getKf(), 8);
		writeFmt(dos, getHeader().getKuser0(), 8);
		dos.writeBytes("\n");
		// 29
		writeFmt(dos, getHeader().getKuser1(), 8);
		writeFmt(dos, getHeader().getKuser2(), 8);
		writeFmt(dos, getHeader().getKcmpnm(), 8);
		dos.writeBytes("\n");
		// 30
		writeFmt(dos, getHeader().getKnetwk(), 8);
		writeFmt(dos, getHeader().getKdatrd(), 8);
		writeFmt(dos, getHeader().getKinst(), 8);
		dos.writeBytes("\n");
	}

	public void writeData(DataOutputStream dos) throws IOException {
		for (int i = 0; i < getHeader().getNpts(); i++) {
			writeFmt(dos, getY()[i]);
			if (((i + 1) % 5 == 0) && (i != 0)) {
				dos.writeBytes("\n");
			}
		}
		dos.writeBytes("\n");
		if (getHeader().getLeven() == FALSE || getHeader().getIftype() == IRLIM || getHeader().getIftype() == IAMPH) {
			for (int i = 0; i < getHeader().getNpts(); i++) {
				writeFmt(dos, getX()[i]);
				if (((i + 1) % 5 == 0) && (i != 0)) {
					dos.writeBytes("\n");
				}
			}
		}
		dos.writeBytes("\n");
	}

	public static void writeFmt(DataOutputStream dos, float f) throws IOException {
		dos.writeBytes(String.format("%15.7G", f));
	}

	public static void writeFmt(DataOutputStream dos, int i) throws IOException {
		dos.writeBytes(String.format("%10d", i));
	}

	public static void writeFmt(DataOutputStream dos, String s, int length) throws IOException {
		String format = "%" + length + "s";
		dos.writeBytes(String.format(format, s));
	}

	public static SacTimeSeriesASCII getSAC(Channel channel, Date begin, float[]ydata) {
		SacTimeSeriesASCII sac = initSAC(channel, begin, ydata);
		sac.getHeader().setIftype(ITIME);
		sac.getHeader().setLeven(TRUE);
	return sac;
	}
	
	
	/***
	 * Creates a SacTimeSeries object from a LocalSeismogram. Headers in the SAC object are filled
	 * in as much as possible, with the notable exception of event information and station location
	 * and channel orientation.
	 * 
	 * @param seis
	 *            the <code>LocalSeismogramImpl</code> with the data
	 * @return a <code>SacTimeSeries</code> with data and headers filled
	 */

	public static SacTimeSeriesASCII getSAC(Channel channel, Date begin, float[] xdata, float[]ydata){
		SacTimeSeriesASCII sac = initSAC(channel, begin, ydata);
		sac.setX(xdata);
		sac.getHeader().setB(xdata[0]);
		sac.getHeader().setE(xdata[xdata.length-1]);
		sac.getHeader().setIftype(IXY);
		sac.getHeader().setLeven(FALSE);
		return sac;
	}
	
	private static SacTimeSeriesASCII initSAC(Channel channel, Date begin, float[] ydata){
		SacHeader sac = new SacHeader();
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		float sum = 0.0f;
		for(float y: ydata){
			if (y> max) max=y;
			if (y<min) min = y;
			sum +=y;
		}
		sac.setNpts(ydata.length);
		float f = new Double(channel.getSampleRate()).floatValue()/1000.0f;
		sac.setB(0);
		sac.setE(sac.getNpts() * f);
		sac.setDelta(f);
		sac.setIdep(IUNKN);
		sac.setDepmin(min);
		sac.setDepmax(max);
		sac.setDepmen(sum/ydata.length);

		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		cal.setTime(begin);
		sac.setNzyear(cal.get(Calendar.YEAR));
		sac.setNzjday(cal.get(Calendar.DAY_OF_YEAR));
		sac.setNzhour(cal.get(Calendar.HOUR_OF_DAY));
		sac.setNzmin(cal.get(Calendar.MINUTE));
		sac.setNzsec(cal.get(Calendar.SECOND));
		sac.setNzmsec(cal.get(Calendar.MILLISECOND));

		sac.setKnetwk(channel.getNetworkName());
		sac.setKstnm(channel.getStation().getName());
		sac.setKcmpnm(channel.getChannelName());
		sac.setKhole(channel.getLocationName());
		sac.setKinst(channel.getLocationName());
		return new SacTimeSeriesASCII(sac, ydata);
	}
}
