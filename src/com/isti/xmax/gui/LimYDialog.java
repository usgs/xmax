package com.isti.xmax.gui;

import javax.swing.JDialog;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Dialog for entering value axis range
 * 
 * @author Max Kokoulin
 */
public class LimYDialog extends JDialog implements PropertyChangeListener {

	private JOptionPane optionPane = null;
	private JTextField topValueTE = null;
	private JLabel topValueLabel = null;
	private JTextField lowValueTE = null;
	private JLabel lowValueLabel = null;
	int max = Integer.MIN_VALUE;
	int min = Integer.MAX_VALUE;

	/**
	 * This method initializes dialog
	 * 
	 * @param frame
	 *            parent frame
	 * @param min
	 *            initial minimum value to fill dialog
	 * @param max
	 *            initial maximum value to fill dialog
	 */
	public LimYDialog(JFrame frame, int min, int max) {
		super(frame, "Enter limits:", true);
		Object[] options = { "OK", "Cancel" };
		// Create the JOptionPane.
		optionPane = new JOptionPane(createPanel(min, max), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION, null, options, options[0]);
		// Init text fields
		if (max != Integer.MIN_VALUE) {
			topValueTE.setText(new Integer(max).toString());
		}
		if (min != Integer.MAX_VALUE) {
			lowValueTE.setText(new Integer(min).toString());
		}
		// Make this dialog display it.
		setContentPane(optionPane);
		optionPane.addPropertyChangeListener(this);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change the JOptionPane's
				 * value property.
				 */
				optionPane.setValue("Cancel");
			}
		});
		pack();
		setLocationRelativeTo(super.getOwner());
		setVisible(true);
	}

	/**
	 * This method initializes this dialog
	 */
	private JPanel createPanel(int min, int max) {
		JPanel panel = new JPanel();
		panel.setMaximumSize(new Dimension(350, 80));
		panel.setSize(new Dimension(350, 80));
		panel.setPreferredSize(new Dimension(350, 80));
		panel.setMinimumSize(new Dimension(350, 80));
		panel.setBorder(BorderFactory.createEtchedBorder());

		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
		gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints2.gridy = 0;
		gridBagConstraints2.weightx = 1.0;
		gridBagConstraints2.insets = new Insets(5, 10, 5, 10);
		gridBagConstraints2.anchor = GridBagConstraints.WEST;
		gridBagConstraints2.gridx = 1;
		GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
		gridBagConstraints11.gridx = 0;
		gridBagConstraints11.anchor = GridBagConstraints.WEST;
		gridBagConstraints11.gridwidth = 1;
		gridBagConstraints11.insets = new Insets(5, 10, 5, 10);
		gridBagConstraints11.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints11.gridy = 2;
		lowValueLabel = new JLabel();
		lowValueLabel.setText("Low value:");
		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.fill = GridBagConstraints.BOTH;
		gridBagConstraints1.gridy = 2;
		gridBagConstraints1.weightx = 1.0;
		gridBagConstraints1.anchor = GridBagConstraints.WEST;
		gridBagConstraints1.insets = new Insets(5, 10, 5, 10);
		gridBagConstraints1.gridx = 1;
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.insets = new Insets(5, 10, 5, 10);
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.gridy = 0;
		topValueLabel = new JLabel();
		topValueLabel.setText("Top value:");
		panel.setLayout(new GridBagLayout());
		panel.add(topValueLabel, gridBagConstraints);
		panel.add(lowValueLabel, gridBagConstraints11);
		panel.add(getLowValueTE(), gridBagConstraints1);
		panel.add(getTopValueTE(), gridBagConstraints2);
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
			if (value.equals("Cancel")) {
				max = Integer.MIN_VALUE;
				min = Integer.MAX_VALUE;
				setVisible(false);
				dispose();
			} else if (value.equals("OK")) {
				try {
					max = Integer.parseInt(topValueTE.getText());
					min = Integer.parseInt(lowValueTE.getText());
					if (max > min) {
						setVisible(false);
						dispose();
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Max value should be greater than min", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				} catch (NumberFormatException e1) {
					max = Integer.MIN_VALUE;
					min = Integer.MAX_VALUE;
					JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Enter valid integer values", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	/**
	 * This method initializes topValueTE
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getTopValueTE() {
		if (topValueTE == null) {
			topValueTE = new JTextField();
			topValueTE.setPreferredSize(new Dimension(120, 22));
		}
		return topValueTE;
	}

	/**
	 * This method initializes lowValueTE
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getLowValueTE() {
		if (lowValueTE == null) {
			lowValueTE = new JTextField();
			lowValueTE.setPreferredSize(new Dimension(100, 22));
		}
		return lowValueTE;
	}
} // @jve:decl-index=0:visual-constraint="103,20"
