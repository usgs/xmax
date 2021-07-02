package com.isti.traceview.transformations.ppm;

import asl.utils.Filter;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.common.TraceViewChartPanel;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.plot.PolarPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;

/**
 * Dialog to view PPM results.
 * 
 * @author Max Kokoulin
 */
class ViewPPM extends JDialog implements PropertyChangeListener {

	private static final long serialVersionUID = 1L;
	private JOptionPane optionPane;
	private static PPMPolarItemRenderer renderer = new PPMPolarItemRenderer();
	private static TraceViewChartPanel cp = null;
    private static JPanel ret = null;
    private double altAngle;

	ViewPPM(Frame owner, XYDataset dataset, TimeInterval ti, String annotation, Filter filter,
			double estimatedBackAzimuth) {

		super(owner, "Particle Motion", true);

		Object[] options = { "Close", "Print", "Enter Angle", "+1", "+5", "+30", "-1", "-5", "-30" };
		// Create the JOptionPane. Have to instantiate textTitle w/ default angle here
		// which will be set to the value calculated by the back-azimuth estimation
		optionPane = new JOptionPane(
				createChartPanel(dataset, ti, annotation, filter,  estimatedBackAzimuth),
				JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION,  null, options, options[0]);

		// make sure that altAngle changes along with starting value -- 180 out from gotten azimuth
		altAngle = (estimatedBackAzimuth + 180) % 360;
		TextTitle subTitle =
				new TextTitle("Back Azimuth Est.: " +
						Math.min(estimatedBackAzimuth, altAngle) + "\u00b0/" +
						Math.max(estimatedBackAzimuth, altAngle) + "\u00b0", ret.getFont());
		cp.getChart().addSubtitle(1, subTitle);

		// Make this dialog display it.
		setContentPane(optionPane);
		optionPane.addPropertyChangeListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property.
				 */
				optionPane.setValue("Close");
			}
		});
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();
		if (isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
			Object value = optionPane.getValue();
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			// If you were going to check something
			// before closing the window, you'd do
			// it here.
			
			if (value.equals("Close")) {
				setVisible(false);
				dispose();
			} else if (value.equals("Print")) {
				cp.createChartPrintJob();
			} else if (value.equals("Enter Angle")) {
				double angle = getAngle();
				if(angle >= 180)
					altAngle = angle - 180;
				else
					altAngle = angle + 180;
				if (angle != Double.POSITIVE_INFINITY) {
					renderer.setRulerAngle(angle);
					cp.setRefreshBuffer(true);
					cp.repaint();
				}
			} else if (value.equals("+1")) {
				renderer.setRulerAngle(renderer.getRulerAngle() + 1);
				altAngle = altAngle + 1; 
				cp.setRefreshBuffer(true);
				cp.repaint();
			} else if (value.equals("+5")) {
				renderer.setRulerAngle(renderer.getRulerAngle() + 5);
				cp.setRefreshBuffer(true);
				altAngle = altAngle + 5;
				cp.repaint();
			} else if (value.equals("+30")) {
				renderer.setRulerAngle(renderer.getRulerAngle() + 30);
				cp.setRefreshBuffer(true);
				altAngle = altAngle + 30;
				cp.repaint();
			} else if (value.equals("-1")) {
				renderer.setRulerAngle(renderer.getRulerAngle() - 1);
				cp.setRefreshBuffer(true);
				altAngle = altAngle - 1;
				cp.repaint();
			} else if (value.equals("-5")) {
				renderer.setRulerAngle(renderer.getRulerAngle() - 5);
				cp.setRefreshBuffer(true);
				altAngle = altAngle - 5;
				cp.repaint();
			} else if (value.equals("-30")) {
				renderer.setRulerAngle(renderer.getRulerAngle() - 30);
				cp.setRefreshBuffer(true);
				altAngle = altAngle - 30;
				cp.repaint();
			}
			
			if(renderer.getRulerAngle() < 0)
				renderer.setRulerAngle(renderer.getRulerAngle() + 360);
			else if (renderer.getRulerAngle() >= 360)
				renderer.setRulerAngle(renderer.getRulerAngle() - 360);
			
			if(altAngle < 0)
				altAngle = altAngle + 360;
			else if (altAngle >= 360)
				altAngle = altAngle - 360;

			double firstAngle = Math.min(altAngle, renderer.getRulerAngle());
			double secondAngle = Math.max(altAngle, renderer.getRulerAngle());

			TextTitle subTitle = new TextTitle("Angle: " + firstAngle + "\u00b0/" +
					secondAngle + "\u00b0", ret.getFont());
			cp.getChart().removeSubtitle(cp.getChart().getSubtitle(0));
			cp.getChart().addSubtitle(0, subTitle);
			cp.chartChanged(new ChartChangeEvent(cp.getChart()));
		}
	}

	private double getAngle() {
		AngleInputDialog ai = new AngleInputDialog(this, renderer.getRulerAngle());
		double ret = ai.getAngle();
		return ret;
	}
	
	private static JPanel createChartPanel(XYDataset dataset, TimeInterval ti,
			String annotation, Filter filter, double bAzimuth) {
		ret = new JPanel();
		BoxLayout retLayout = new BoxLayout(ret, javax.swing.BoxLayout.Y_AXIS);
		ret.setLayout(retLayout);
		JFreeChart chart = ChartFactory.createPolarChart(null, // title
				dataset, // dataset
				false, // legend
				true, // tooltips
				false// include URLs
		);
		String filterName = "None";
		if (filter != null) {
			filterName = filter.filterType.getName();
		}
		TextTitle title = new TextTitle("Start time: "
				+ TimeInterval.formatDate(ti.getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
				+ ", Duration: " + ti.convert() + ". Filter: " + filterName + ".", ret.getFont());
		chart.setTitle(title);


		PolarPlot plot = (PolarPlot) chart.getPlot();
		plot.setRenderer(renderer);
		double rulerAngle = bAzimuth % 180;
		renderer.setRulerAngle(rulerAngle);
		TextTitle subTitle = new TextTitle("Angle: " + renderer.getRulerAngle() +
				"\u00b0/" + (rulerAngle + 180) + "\u00b0", ret.getFont());
		chart.addSubtitle(0, subTitle);
		plot.addCornerTextItem(annotation);
		cp = new TraceViewChartPanel(chart, true);
		ret.add(cp);
		return ret;
	}
}
