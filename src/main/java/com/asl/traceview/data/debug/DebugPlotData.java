package com.asl.traceview.data.debug;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import com.isti.traceview.data.Segment;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * This class is used for debugging purposes. It provides a variety of ways to quickly plot data 
 * @author nfalco
 *
 */

public class DebugPlotData extends JDialog {

	private static final long serialVersionUID = 1L;
	private List<Segment> data = new ArrayList<>();
	private TimeInterval timeInterval;
	private XYPlot plot = null;
	private TraceViewChartPanel chartPanel = null;
	
	public DebugPlotData(List<Segment> data, TimeInterval ti) {
		this.timeInterval = ti;
		this.data = data;
		JPanel plot = createChartPanel(createDataSet(data));
		setContentPane(plot);
		JFrame frame = new JFrame("Debug Plot");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(plot);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}
	
	private JPanel createChartPanel(XYDataset dataset) {
		JPanel ret = new JPanel();
		BoxLayout retLayout = new BoxLayout(ret, javax.swing.BoxLayout.Y_AXIS);
		ret.setLayout(retLayout);
		JFreeChart chart = ChartFactory.createXYLineChart(null, // title
				"Period, s", // x-axis label
				"Amplitude", // y-axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
		);
		chart.setBackgroundPaint(Color.white);
		TextTitle title = new TextTitle("Start time: "
				+ TimeInterval.formatDate(timeInterval.getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
				+ ", Duration: " + timeInterval.convert(), ret.getFont());
		chart.setTitle(title);
		plot = chart.getXYPlot();
		NumberAxis domainAxis = new NumberAxis("Time");
		plot.setDomainAxis(domainAxis);
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setAutoRange(true);
		rangeAxis.setAutoRangeIncludesZero(false);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		chartPanel = new TraceViewChartPanel(chart, true);
		ret.add(chartPanel);
		return ret;
	}
	
	private double[] getAmpArray() {
		//calculate the total number of points for all segments
		int totalNumPoints = 0;
		for(Segment segment : data) {
			totalNumPoints += segment.getData(timeInterval).data.length;
		}
		//fill an array with all points
		double[] out = new double[totalNumPoints];
		for(Segment segment : data) {
			int[] segData = segment.getData(timeInterval).data;
			for(int i = 0; i < segData.length; i++) {
				out[i] = segData[i];
			}
		}
		return out; 
	}
	
	
	private XYDataset createDataSet(List<Segment> ds) {
		XYSeriesCollection ret = new XYSeriesCollection();
		XYSeries series = new XYSeries("Debug");
		double[] out = getAmpArray();
		for (int i = 0; i < out.length; i++) {
			double x = i;
			double y = out[i];
			series.add(x, y);
		}
		ret.addSeries(series);
		return ret;
	}

}
