package com.isti.xmax.gui;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import java.awt.Dimension;

/**
 * Special button used in command button panel. Has three action which launched by clicking left,
 * middle or right mouse button over command button area.
 * 
 * @author Max Kokoulin
 */
public class CommandButton extends JPanel implements MouseListener {
	private Action action1 = null; // @jve:decl-index=0:
	private Action action2 = null;
	private Action action3 = null;
	private JLabel label1 = null;
	private JLabel label2 = null;
	private JLabel label3 = null;
	private JLabel nameLabel = null;

	/**
	 * default constructor
	 * @param name name of button
	 */
	public CommandButton(String name) {
		super();
		initialize();
		if (name != null && name.length() > 0) {
			nameLabel.setText(name + ":");
		}
		addMouseListener(this);
	}

	/**
	 * This method initializes command button
	 */
	private void initialize() {
		nameLabel = new JLabel();
		nameLabel.addMouseListener(this);
		nameLabel.setText("");
		label3 = new JLabel();
		label3.addMouseListener(this);
		label3.setText("");
		label2 = new JLabel();
		label2.addMouseListener(this);
		label2.setText("");
		label1 = new JLabel();
		label1.addMouseListener(this);
		label1.setText("");
		this.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		this.setPreferredSize(new Dimension(40, 18));
		this.setMinimumSize(new Dimension(40, 18));
		this.setSize(new Dimension(40, 18));
		this.add(nameLabel, null);
		this.add(label1, null);
		this.add(label2, null);
		this.add(label3, null);
	}

	/**
	 * Sets action for left mouse button
	 */
	public void setAction1(Action action) {
		action1 = action;
		if (action == null) {
			label1.setText("");
			label1.setToolTipText("");
		} else {
			label1.setText("A) " + (String) action.getValue(Action.SHORT_DESCRIPTION));
			label1.setToolTipText((String) action.getValue(Action.NAME));
		}
	}

	/**
	 * Sets action for middle mouse button
	 */
	public void setAction2(Action action) {
		action2 = action;
		if (action == null) {
			label2.setText("");
			label2.setToolTipText("");
		} else {
			label2.setText("B) " + (String) action.getValue(Action.SHORT_DESCRIPTION));
			label2.setToolTipText((String) action.getValue(Action.NAME));
		}
	}

	/**
	 * Sets action for right mouse button
	 */
	public void setAction3(Action action) {
		action3 = action;
		if (action == null) {
			label3.setText("");
			label3.setToolTipText("");
		} else {
			label3.setText("C) " + (String) action.getValue(Action.SHORT_DESCRIPTION));
			label3.setToolTipText((String) action.getValue(Action.NAME));
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1 && !e.isShiftDown() && !e.isControlDown()) {
			if (action1 != null) {
				action1.actionPerformed(new ActionEvent(this, 0, (String) action1.getValue(Action.NAME)));
			}
			//emulate middle mouse button by shift-click
		} else if (e.getButton() == MouseEvent.BUTTON2 || e.isShiftDown()) {
			if (action2 != null) {
				action2.actionPerformed(new ActionEvent(this, 0, (String) action2.getValue(Action.NAME)));
			}
			//emulate right mouse button by control-click
		} else if (e.getButton() == MouseEvent.BUTTON3 || e.isControlDown()) {
			if (action3 != null) {
				action3.actionPerformed(new ActionEvent(this, 0, (String) action3.getValue(Action.NAME)));
			}
		}
	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {

	}

	public void mouseReleased(MouseEvent e) {

	}
}
