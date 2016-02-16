package com.isti.traceview.transformations.ppm;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.Rotation;
import com.isti.traceview.transformations.ITransformation;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;

/**
 * Particle motion transformation. Prepares data for presentation in
 * {@link ViewPPM}
 * 
 * @author Max Kokoulin
 */
public class TransPPM implements ITransformation {

	public static final String NAME = "Particle motion";

	private int maxDataLength = 8192;

	@Override
	public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Rotation rotation,
			Object configuration, JFrame parentFrame) {
		if ((input == null) || (input.size() != 2)) {
			JOptionPane.showMessageDialog(parentFrame, "You should select two channels to view PPM", "Error",
					JOptionPane.ERROR_MESSAGE);
		} else {
			try {
				/*
				 * Whenever we use channels with N and E as a third symbol
				 * (like, BHN/NHE) N channel ALWAYS denotes the north and E
				 * channel always denotes the East (regardless of the selection
				 * order). Same for channels BH1 and BH2 : 1 is North; 2 is
				 * East. For all other channel pairs, they go in the selection
				 * order: first NS, second EW.
				 */
				List<PlotDataProvider> inputRepositioned = new ArrayList<PlotDataProvider>();
				char type1 = input.get(0).getType();
				char type2 = input.get(1).getType();
				if (((type2 == 'N' || type2 == '1') && type1 != 'N' && type1 != '1')
						|| ((type1 == 'E' || type1 == '2') && type2 != 'E' && type2 != '2')) {
					inputRepositioned.add(input.get(1));
					inputRepositioned.add(input.get(0));
				} else {
					inputRepositioned.add(input.get(0));
					inputRepositioned.add(input.get(1));
				}
				@SuppressWarnings("unused")
				ViewPPM vr = new ViewPPM(parentFrame, createDataset(inputRepositioned, filter, rotation, ti), ti,
						"N:" + inputRepositioned.get(0).getName() + "  E:" + inputRepositioned.get(1).getName(),
						filter);
			} catch (XMAXException e) {
				JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}
		((XMAXframe) parentFrame).getGraphPanel().forceRepaint();
	}

	@Override
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
	 * @return jFreeChart dataset of trace data in polar coordinates
	 * @throws XMAXException
	 *             if sample rates differ, gaps in the data, or no data for a
	 *             channel
	 */
	private XYDataset createDataset(List<PlotDataProvider> input, IFilter filter, Rotation rotation, TimeInterval ti)
			throws XMAXException {
		XYSeriesCollection dataset = new XYSeriesCollection();
		PlotDataProvider channel1 = input.get(0); // N/S
		PlotDataProvider channel2 = input.get(1); // E/W
		if (channel1.getSampleRate() != channel2.getSampleRate())
			throw new XMAXException("Channels have different sample rate");
		XYSeries series = new XYSeries(channel1.getName() + " " + channel2.getName(), false);
		double sampleRate;
		List<Segment> segments1;
		if(rotation != null)
			segments1 = channel1.getRawData(rotation, ti);				
		else
			segments1 = channel1.getRawData(ti);	

		int[] intData1 = new int[0];
		if (segments1.size() > 0) {
			long segment_end_time = 0;
			sampleRate = segments1.get(0).getSampleRate();
			for (Segment segment : segments1) {
				if (segment.getSampleRate() != sampleRate) {
					throw new XMAXException(
							"You have data with different sample rate for channel " + channel1.getName());
				}
				if (segment_end_time != 0
						&& Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), sampleRate)) {
					throw new XMAXException("You have gap in the data for channel " + channel1.getName());
				}
				segment_end_time = segment.getEndTime().getTime();
				intData1 = IstiUtilsMath.padArray(intData1, segment.getData(ti).data);
			}

		} else {
			throw new XMAXException("You have no data for channel " + channel1.getName());
		}
		List<Segment> segments2;
		if(rotation != null)
			segments2 = channel2.getRawData(rotation, ti);				
		else
			segments2 = channel2.getRawData(ti);	
		int[] intData2 = new int[0];
		if (segments2.size() > 0) {
			long segment_end_time = 0;
			for (Segment segment : segments2) {
				if (segment.getSampleRate() != sampleRate) {
					throw new XMAXException("Channels " + channel1.getName() + " and " + channel2.getName()
							+ " have different sample rates: " + sampleRate + " and " + segment.getSampleRate());
				}
				if (segment_end_time != 0
						&& Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(), sampleRate)) {
					throw new XMAXException("You have gap in the data for channel " + channel2.getName());
				}
				segment_end_time = segment.getEndTime().getTime();
				intData2 = IstiUtilsMath.padArray(intData2, segment.getData(ti).data);
			}

		} else {
			throw new XMAXException("You have no data for channel " + channel1.getName());
		}
		if (filter != null) {
			intData1 = new FilterFacade(filter, channel1).filter(intData1);
			intData2 = new FilterFacade(filter, channel2).filter(intData2);
		}
		int dataSize = Math.min(intData1.length, intData2.length);
		if (dataSize > maxDataLength) {
			throw new XMAXException("Too many datapoints are selected.");
		}
		ArrayValues values1 = new ArrayValues(intData1, dataSize);
		ArrayValues values2 = new ArrayValues(intData2, dataSize);
		for (int i = 0; i < dataSize; i++) {
			double x = intData1[i] - values1.getAverage();
			double y = intData2[i] - values2.getAverage();
			double radius = Math.sqrt(x * x + y * y);
			double theta = 180 * Math.atan2(y, x) / Math.PI;
			series.add(theta, radius);
		}
		dataset.addSeries(series);
		return dataset;
	}

	private class ArrayValues {
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		int average = 0;

		public ArrayValues(int[] array, int size) {
			for (int i = 0; i < size; i++) {
				if (array[i] > max)
					max = array[i];
				if (array[i] < min)
					min = array[i];
				average = average + array[i];
			}
			average = average / size;
		}

		@SuppressWarnings("unused")
		public int getMax() {
			return max;
		}

		@SuppressWarnings("unused")
		public int getMin() {
			return min;
		}

		public double getAverage() {
			return average;
		}
	}

	@Override
	public String getName() {
		return TransPPM.NAME;
	}
}
