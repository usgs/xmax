package com.isti.traceview.filters;

import asl.utils.Filter;
import com.isti.xmax.gui.XMAXframe;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import org.apache.log4j.Logger;

/**
 * Filter with visual dialog in constructor do manually design it. Use HP, BP or
 * LP filter to process data, depends from entered values
 * 
 * @author Max Kokoulin
 */
public class FilterDYO extends JDialog implements  PropertyChangeListener {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(FilterDYO.class);

	public static final String DESCRIPTION = "Creates DYO filter for selected channels and apply it";
	public static final String NAME = "DYO";

	//State is maintained in these fields
	// There may be a memory leak here, if the JOptionPane is kept alive because of a reverse link.
	// This has not shown up though.
	private final static JTextField lowFrequencyTF;
	private final static JTextField highFrequencyTF;
	private final static JComboBox<Object> orderCB;

	private Filter filter = null;
	private JOptionPane optionPane;

	private boolean needProcessing = false;

	static {
		Double cutLowFrequency = 0.1;
		Double cutHighFrequency = 0.5;
		Integer order = 4;

		lowFrequencyTF = new JTextField();
		lowFrequencyTF.setText(cutLowFrequency.toString());
		lowFrequencyTF.setPreferredSize(new Dimension(50, 20));

		highFrequencyTF = new JTextField();
		highFrequencyTF.setText(cutHighFrequency.toString());
		highFrequencyTF.setPreferredSize(new Dimension(50, 20));

		orderCB = new JComboBox<>();
		for (int i = 1; i <= 5; ++i) {
			orderCB.addItem((i * 2));
		}
		orderCB.setSelectedItem(order);
	}

	public FilterDYO() {
		super(XMAXframe.getInstance(), "Design your own filter", true);
		Object[] options = { "OK", "Close" };


		// Create the JOptionPane.
		optionPane = new JOptionPane(createDesignPanel(),
				JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, options[0]);
		// Make this dialog display it.
		setContentPane(optionPane);
		optionPane.addPropertyChangeListener(this);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property.
				 */
				optionPane.setValue("Close");
			}
		});
		pack();
		setLocationRelativeTo(super.getOwner());
		setVisible(true);
	}

	private JPanel createDesignPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		panel.setLayout(new GridBagLayout());

		JLabel orderL = new JLabel();
		orderL.setText("Order:");
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.EAST;
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.insets = new Insets(2, 3, 2, 3);
		panel.add(orderL, gridBagConstraints);

		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.gridy = 1;
		gridBagConstraints1.gridx = 2;
		panel.add(getOrderCB(), gridBagConstraints1);

		JLabel lowFrequencyL = new JLabel();
		lowFrequencyL.setText("Low cut frequency, Hz:");
		lowFrequencyL.setLabelFor(lowFrequencyTF);
		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
		gridBagConstraints2.gridx = 1;
		gridBagConstraints2.gridy = 2;
		gridBagConstraints2.anchor = GridBagConstraints.EAST;
		gridBagConstraints2.insets = new Insets(2, 3, 2, 3);
		panel.add(lowFrequencyL, gridBagConstraints2);

		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.fill = GridBagConstraints.VERTICAL;
		gridBagConstraints3.gridx = 2;
		gridBagConstraints3.gridy = 2;
		gridBagConstraints3.insets = new Insets(2, 3, 2, 3);
		panel.add(getLowFrequencyTF(), gridBagConstraints3);

		JLabel highFrequencyL = new JLabel();
		highFrequencyL.setText("High cut frequency, Hz:");
		highFrequencyL.setLabelFor(highFrequencyTF);
		GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
		gridBagConstraints4.gridx = 1;
		gridBagConstraints4.gridy = 3;
		gridBagConstraints4.anchor = GridBagConstraints.EAST;
		gridBagConstraints4.insets = new Insets(2, 3, 2, 3);
		panel.add(highFrequencyL, gridBagConstraints4);

		GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
		gridBagConstraints5.fill = GridBagConstraints.VERTICAL;
		gridBagConstraints5.gridx = 2;
		gridBagConstraints5.gridy = 3;
		gridBagConstraints5.insets = new Insets(2, 3, 2, 3);
		panel.add(getHighFrequencyTF(), gridBagConstraints5);
		return panel;
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
				needProcessing = false;
			} else if (value.equals("OK")) {
				// Initialize to invalid value in case of error during parsing
				double cutLowFrequency = Double.NaN;
				double cutHighFrequency = Double.NaN;
				try {
					cutLowFrequency = Double.parseDouble(lowFrequencyTF.getText());
				} catch (NumberFormatException e1) {
					logger.warn("NumberFormatException (cutLowFrequency=NaN):", e1);
				}
				try {
					cutHighFrequency = Double.parseDouble(highFrequencyTF.getText());
				} catch (NumberFormatException e1) {
					logger.warn("NumberFormatException (cutHighFrequency=NaN):", e1);
				}
				Integer order = (Integer) orderCB.getSelectedItem();
				if (!Double.isNaN(cutLowFrequency) && !Double.isNaN(cutHighFrequency) &&
						cutLowFrequency > 0 && cutHighFrequency > 0) {
					logger.info("Band pass filtering triggered");
					if (cutLowFrequency < cutHighFrequency) {
						filter = new Filter().withBandPass(cutLowFrequency, cutHighFrequency).withOrder(order).withZeroPhase(true);
						//new FilterBP(order, cutLowFrequency, cutHighFrequency);
						setVisible(false);
						needProcessing = true;
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(),
								"Low frequency should be less then high one", "Error", JOptionPane.ERROR_MESSAGE);
					}
				} else if (!Double.isNaN(cutLowFrequency) && cutLowFrequency > 0) {
					logger.info("Low pass filtering triggered");
					filter = new Filter().withLowPass(cutLowFrequency).withOrder(order).withZeroPhase(true);
							//new FilterLP(order, cutLowFrequency);
					setVisible(false);
					needProcessing = true;
				} else if (!Double.isNaN(cutHighFrequency) && cutHighFrequency > 0) {
					logger.info("High pass filtering triggered");
					filter = new Filter().withHighPass(cutHighFrequency).withOrder(order).withZeroPhase(true);
					//new FilterHP(order, cutHighFrequency);
					setVisible(false);
					needProcessing = true;
				} else {
					logger.warn("Could not get suitable filter parameters");
					filter = null;
					JOptionPane.showMessageDialog(XMAXframe.getInstance(),
							"Please enter either low or high frequencies", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			// Nullifying this for memory cleanup.
			// Might not be needed, but it is a circular reference and the optionPane is recreated each time.
			this.optionPane = null;
		}
	}

	private JTextField getLowFrequencyTF(){
		return lowFrequencyTF;
	}

	private JTextField getHighFrequencyTF(){
		return highFrequencyTF;
	}

	private JComboBox<Object> getOrderCB() {
		return orderCB;
	}

	public Function<double[], double[]> getFilterFunction() {
		return this.filter.buildFilterBulkFunction();
	}

	public boolean equals(Object o) {
		if (filter == null)
			return false;
		else
			return filter.equals(o);
	}

	@Override
	public String getName() {
		return FilterDYO.NAME;
	}
}
