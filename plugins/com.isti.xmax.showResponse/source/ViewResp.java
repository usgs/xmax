import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import com.isti.traceview.common.TraceViewChartPanel;

public class ViewResp extends JDialog implements PropertyChangeListener {

	private static final long serialVersionUID = 1L;
	private JOptionPane optionPane;

	public ViewResp(Frame owner, XYDataset dataset) {
		super(owner, "Response view", true);
		Object[] options = {"Close", "Print"};
		// Create the JOptionPane.
		optionPane = new JOptionPane(createChartPanel(dataset), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION, null, options,
				options[0]);
		// Make this dialog display it.
		setContentPane(optionPane);
		optionPane.addPropertyChangeListener(this);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change the JOptionPane's value property.
				 */
				optionPane.setValue("Close");
			}
		});
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

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
				TraceViewChartPanel cp = (TraceViewChartPanel)optionPane.getMessage();
				cp.createChartPrintJob();			
			}
		}
	}

	private static TraceViewChartPanel createChartPanel(XYDataset dataset) {
		JFreeChart chart = ChartFactory.createXYLineChart(null, // title
				"Log10(Frequency, Hz)", // x-axis label
				"Log10(Response)", // y-axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
				);
		chart.setBackgroundPaint(Color.white);
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		return new TraceViewChartPanel(chart, true);
	}

//	public static void main(String[] args) {
//		JFrame frame = new JFrame();
//		frame.setVisible(true);
//		ViewResp vr = new ViewResp(frame);
//		vr.setVisible(true);
//
//	}
}
