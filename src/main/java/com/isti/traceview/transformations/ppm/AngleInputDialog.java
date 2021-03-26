package com.isti.traceview.transformations.ppm;

import java.awt.Dialog;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

class AngleInputDialog extends JDialog implements PropertyChangeListener {

	private static final long serialVersionUID = 1L;
	private JOptionPane optionPane;
	private JTextField textField;
	private double angle = Double.POSITIVE_INFINITY;
	private double curValue = Double.POSITIVE_INFINITY;

	public double getAngle() {
		return angle;
	}

	AngleInputDialog(Dialog f, double value) {
		super(f, "Angle Input Dialog", true);
		curValue = value; 
		JPanel panel = new JPanel();
		textField = new JTextField();
		textField.setPreferredSize(new Dimension(70, 22));
		textField.setText(Double.toString(value));
		panel.add(new JLabel("Enter angle:"));
		panel.add(textField);
		optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		setContentPane(optionPane);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		optionPane.addPropertyChangeListener(this);
		pack();
		setLocationRelativeTo(f);
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
			if (value.equals(JOptionPane.OK_OPTION)) {
				try {
					angle = Double.parseDouble(textField.getText());
					setVisible(false);
					dispose();
				} catch (NumberFormatException e1) {
					JOptionPane.showMessageDialog(this, "Enter double value", "Parsing error",
							JOptionPane.ERROR_MESSAGE);
					setVisible(true);
				}
			} else {
				angle = curValue; // if cancel, then keep angle the same
				setVisible(false);
				dispose();
			}
		}
	}
}
