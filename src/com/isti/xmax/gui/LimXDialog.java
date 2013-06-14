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

import com.isti.traceview.common.TimeInterval;
import com.isti.xmax.XMAX;

import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dialog to query time range
 * 
 * @author Max Kokoulin
 */
public class LimXDialog extends JDialog implements PropertyChangeListener {

	private static SimpleDateFormat df = new SimpleDateFormat("yyyy,DDD,HH:mm:ss");
	private JOptionPane optionPane = null;
	private JTextField startTimeTE = null;
	private JLabel startTimeLabel = null;
	private JTextField durationTE = null;
	private JLabel durationLabel = null;
	private TimeInterval ret = null;

	static {
		df.setTimeZone(XMAX.timeZone);
	}

	/**
	 * This method initializes dialog
	 * 
	 * @param frame
	 *            parent frame
	 * @param ti
	 *            initial time interval to fill dialog
	 */
	public LimXDialog(JFrame frame, TimeInterval ti) {
		super(frame, "Enter time limits:", true);
		Object[] options = { "OK", "Cancel" };
		// Create the JOptionPane.
		optionPane = new JOptionPane(createPanel(ti), JOptionPane.PLAIN_MESSAGE, JOptionPane.CLOSED_OPTION, null, options, options[0]);
		startTimeTE.setText(df.format(ti.getStartTime()));
		durationTE.setText(new Integer(new Double(ti.getDuration() / 1000).intValue()).toString());
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
	private JPanel createPanel(TimeInterval ti) {
		JPanel panel = new JPanel();
		panel.setMaximumSize(new Dimension(400, 80));
		panel.setSize(new Dimension(400, 80));
		panel.setPreferredSize(new Dimension(400, 80));
		panel.setMinimumSize(new Dimension(400, 80));
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
		durationLabel = new JLabel();
		durationLabel.setText("Duration (seconds):");
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
		startTimeLabel = new JLabel();
		startTimeLabel.setText("Start time (yyyy,DDD,hh:mm:ss):");
		panel.setLayout(new GridBagLayout());
		panel.add(startTimeLabel, gridBagConstraints);
		panel.add(durationLabel, gridBagConstraints11);
		panel.add(getDurationTE(), gridBagConstraints1);
		panel.add(getStartTimeTE(), gridBagConstraints2);
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
				ret = null;
				setVisible(false);
				dispose();
			} else if (value.equals("OK")) {
				try {
					Date begin = df.parse(startTimeTE.getText());
					int secInterval = Integer.parseInt(durationTE.getText());
					ret = new TimeInterval(begin, new Date(begin.getTime() + secInterval * 1000));
					if (secInterval > 0) {
						setVisible(false);
						dispose();
					} else {
						JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Duration should be positive", "Error", JOptionPane.ERROR_MESSAGE);
					}
				} catch (NumberFormatException e1) {
					ret = null;
					JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Enter valid integer value for duration", "Error",
							JOptionPane.ERROR_MESSAGE);
				} catch (ParseException e1) {
					ret = null;
					JOptionPane.showMessageDialog(XMAXframe.getInstance(), "Enter valid date", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	/**
	 * This method initializes startTimeTE
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getStartTimeTE() {
		if (startTimeTE == null) {
			startTimeTE = new JTextField();
			startTimeTE.setPreferredSize(new Dimension(120, 22));
		}
		return startTimeTE;
	}

	/**
	 * This method initializes durationTE
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getDurationTE() {
		if (durationTE == null) {
			durationTE = new JTextField();
			durationTE.setPreferredSize(new Dimension(100, 22));
		}
		return durationTE;
	}

	public static TimeInterval showDialog(JFrame frame, TimeInterval ti) {
		LimXDialog dialog = new LimXDialog(frame, ti);
		TimeInterval ret = dialog.ret;
		return ret;
	}
} // @jve:decl-index=0:visual-constraint="103,20"
