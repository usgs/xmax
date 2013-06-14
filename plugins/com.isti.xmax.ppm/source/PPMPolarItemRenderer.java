

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PolarPlot;
import org.jfree.chart.renderer.DefaultPolarItemRenderer;
import org.jfree.data.xy.XYDataset;

public class PPMPolarItemRenderer extends DefaultPolarItemRenderer {
	double rulerTheta = 0.0;
	
	public void setRulerAngle(double theta){
		this.rulerTheta = theta;
	}
	
	public double getRulerAngle(){
		return rulerTheta;
	}
	
	/**
	 * Plots the data for a given series. Changes default closed polygon realisation to chain of sections First series is angle mark(2 values), other
	 * is data Add begin mark to data series
	 * 
	 * @param g2 the drawing surface.
	 * @param dataArea the data area.
	 * @param info collects plot rendering info.
	 * @param plot the plot.
	 * @param dataset the dataset.
	 * @param seriesIndex the series index.
	 */
	public void drawSeries(Graphics2D g2, Rectangle2D dataArea, PlotRenderingInfo info, PolarPlot plot, XYDataset dataset, int seriesIndex) {
		GeneralPath gp = new GeneralPath();
		int numPoints = dataset.getItemCount(seriesIndex);
		Point begin = null;
		double maxRadius = 0;
		for (int i = 0; i < numPoints; i++) {
			double theta = dataset.getXValue(seriesIndex, i);
			double radius = dataset.getYValue(seriesIndex, i);
			if(radius > maxRadius){
				maxRadius = radius;
			}
			Point p = plot.translateValueThetaRadiusToJava2D(theta, radius, dataArea);
			if (i == 0) {
				gp.moveTo(new Float(p.getX()), new Float(p.getY()));
				begin = p;
			} else {
				gp.lineTo(new Float(p.getX()), new Float(p.getY()));
			}
		}
		g2.setPaint(lookupSeriesPaint(seriesIndex));
		g2.setStroke(lookupSeriesStroke(seriesIndex));
		g2.draw(gp);
		//Drawing starting mark
		g2.setColor(Color.BLACK);
		g2.drawOval(begin.x-2, begin.y-2, 4, 4);
		//Drawing ruler
		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(2));
		Point ruler1 = plot.translateValueThetaRadiusToJava2D(rulerTheta, maxRadius, dataArea);
		Point ruler2 = plot.translateValueThetaRadiusToJava2D(rulerTheta+180, maxRadius, dataArea);
		g2.drawLine(ruler1.x, ruler1.y, ruler2.x, ruler2.y);	
	}
}
