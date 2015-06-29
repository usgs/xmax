import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.commons.configuration.Configuration;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IFilter;
import com.isti.traceview.processing.ITransformation;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Spectra;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;

/**
 * Power spectra density transformation. Prepares data for presentation in {@link ViewPSD}
 * 
 * @author Max Kokoulin
 */
public class TransPSD implements ITransformation {
	private static final Logger logger = Logger.getLogger(TransPSD.class);
	private static final boolean verboseDebug = false;
	public int maxDataLength = 65536;
	private int effectiveLength = 0;

	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration, JFrame parentFrame) {
		if (input.size() == 0) {
			JOptionPane.showMessageDialog(parentFrame, "Please select channels", "PSD computation warning", JOptionPane.WARNING_MESSAGE);
		} else {
			try {
				List<Spectra> spList = createData(input, filter, ti, parentFrame);
				TimeInterval effectiveInterval = new TimeInterval(ti.getStart(), ti.getStart() + new Double(input.get(0).getSampleRate()*effectiveLength).longValue());
				@SuppressWarnings("unused")	
				ViewPSD vp = new ViewPSD(parentFrame, spList, effectiveInterval, (Configuration)configuration, input);
			} catch (XMAXException e) {
				if (!e.getMessage().equals("Operation cancelled")) {
					JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
				}
			} catch (TraceViewException e) {
				JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}
		((XMAXframe)parentFrame).getGraphPanel().forceRepaint();
	}

	public void setMaxDataLength(int dataLength) {
		this.maxDataLength = dataLength;
	}

	/**
	 * @param input
	 *            List of traces to process
	 * @param filter
	 *            Filter applied to traces before correlation
	 * @param ti
	 *            Time interval to define processed range
	 * @param parentFrame
	 *            parent frame
	 * @return list of spectra for selected traces and time ranges
	 * @throws XMAXException if sample rates differ, gaps in the data, or no data for a channel
	 */

	private List<Spectra> createData(List<PlotDataProvider> input, IFilter filter, TimeInterval ti, JFrame parentFrame) throws TraceViewException,
			XMAXException {
		List<Spectra> dataset = new ArrayList<Spectra>();
		ListIterator<PlotDataProvider> li = input.listIterator();
		String respNotFound = "";
		//int userAnswer = -1;
		while (li.hasNext()) {
			PlotDataProvider channel = li.next();
			List<Segment> segments = channel.getRawData(ti);
			double samplerate;
			long segment_end_time = 0;
			int[] intData = new int[0];
			if (segments.size() > 0) {
				samplerate = segments.get(0).getSampleRate();
				for (Segment segment: segments) {
					if (segment.getSampleRate() != samplerate) {
						throw new XMAXException("You have data with different sample rate for channel " + channel.getName());
					}
					if (segment_end_time != 0 && Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), samplerate)) {
						throw new XMAXException("You have gap in the data for channel " + channel.getName());
					}
					segment_end_time = segment.getEndTime().getTime();
					intData = IstiUtilsMath.padArray(intData, segment.getData(ti).data);
				}

			} else {
				throw new XMAXException("You have no data for channel " + channel.getName());
			}
			int ds;
			if (intData.length > maxDataLength) {
				ds = getPower2Length(maxDataLength);				
				((XMAXframe) parentFrame).getStatusBar().setMessage(
						"Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
			} else {
				ds = getPower2Length(intData.length);
			}
			if(ds>effectiveLength){
				effectiveLength = ds;
			}
			/*
			 * // this code shows pop-up if point count is exceeded if (ds > maxDataLength &&
			 * userAnswer == -1) { Object[] options = { "Proceed with ALL points", "Proceed with
			 * first " + maxDataLength + " points", "Cancel" }; userAnswer =
			 * JOptionPane.showOptionDialog(parentFrame, "Too many points. Computation could be
			 * slow.", "Too many points", JOptionPane.YES_NO_CANCEL_OPTION,
			 * JOptionPane.QUESTION_MESSAGE, null, options, options[1]); } if (userAnswer != -1) {
			 * if (userAnswer == JOptionPane.NO_OPTION) { if (ds > maxDataLength) { ds = new
			 * Double(Math.pow(2, new
			 * Double(IstiUtilsMath.log2(maxDataLength)).intValue())).intValue(); } } else if
			 * (userAnswer == JOptionPane.CANCEL_OPTION) { throw new XMAXException("Operation
			 * cancelled"); } }
			 */
			logger.debug("data size = " + ds);
			int[] data = new int[ds];
			for (int i = 0; i < ds; i++) {
				data[i] = intData[i];
			}
			if (filter != null) {
				data = new FilterFacade(filter, channel).filter(data);
			}

			Spectra spectra = IstiUtilsMath.getNoiseSpectra(data, channel.getResponse(), ti.getStartTime(), channel,
					verboseDebug);
			if (spectra.getResp() != null) {
				dataset.add(spectra);
			} else {
				if (respNotFound.length() > 0) {
					respNotFound = respNotFound + ", ";
				}
				respNotFound = respNotFound + channel.getName();
				li.remove();
			}
		}
		if (input.size() == 0) {
			throw new XMAXException("Can not find responses");
		} else {
			if (respNotFound.length() > 0) {
				JOptionPane.showMessageDialog(parentFrame, "Can not find responses for channels: " + respNotFound, "Warning",
						JOptionPane.WARNING_MESSAGE);
			}
		}
		return dataset;
	}
	
	protected static int getPower2Length(int length){
		return new Double(Math.pow(2, new Double(IstiUtilsMath.log2(length)).intValue())).intValue();		
	}
}
