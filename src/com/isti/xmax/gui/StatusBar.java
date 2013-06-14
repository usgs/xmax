package com.isti.xmax.gui;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import java.awt.Dimension;
import javax.swing.BoxLayout;

import org.apache.log4j.Logger;

import com.isti.traceview.gui.IColorModeState;
import com.isti.traceview.gui.IMeanState;
import com.isti.traceview.gui.IOffsetState;
import com.isti.traceview.gui.IScaleModeState;
import com.isti.traceview.gui.GraphPanel.GraphPanelObservable;
import com.isti.traceview.processing.IFilter;

import java.awt.ComponentOrientation;
import java.awt.Font;
import java.util.Observable;
import java.util.Observer;

/**
 * <p>
 * Status bar in the bottom of main frame.
 * </p>
 * <p>
 * Realize observer pattern, i.e watch for registered object changing and reflect changes.
 * </p>
 * 
 * @author Max Kokoulin
 */
public class StatusBar extends JPanel implements Observer {
	private static Logger lg = Logger.getLogger(StatusBar.class);

	private Font font = null;
	private JLabel channelCountLabel = null;
	private JLabel messageLabel = null;
	private JLabel pickLabel = null;
	private JLabel filterLabel = null;
	private JLabel ovrLabel = null;
	private JLabel selLabel = null;
	private JLabel scaleModeLabel = null;

	/**
	 * Default constructor
	 */
	public StatusBar() {
		super();
		Font defaultFont = this.getFont();
		font = new Font(defaultFont.getName(), defaultFont.getStyle(), 10);
		initialize();
	}

	/**
	 * This method initializes this status bar
	 */
	private void initialize() {
		channelCountLabel = new JLabel();
		channelCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
		channelCountLabel.setText("0-0 of 0");
		channelCountLabel.setPreferredSize(new Dimension(60, 18));
		channelCountLabel.setMinimumSize(new Dimension(60, 18));
		channelCountLabel.setMaximumSize(new Dimension(60, 18));
		channelCountLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		channelCountLabel.setToolTipText("Shown channels count");

		messageLabel = new JLabel();
		messageLabel.setHorizontalAlignment(SwingConstants.LEFT);
		messageLabel.setText("");
		messageLabel.setPreferredSize(new Dimension(100, 18));
		messageLabel.setMinimumSize(new Dimension(200, 18));
		messageLabel.setMaximumSize(new Dimension(100000, 18));
		messageLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		pickLabel = new JLabel();
		pickLabel.setHorizontalAlignment(SwingConstants.CENTER);
		pickLabel.setText("");
		pickLabel.setPreferredSize(new Dimension(50, 18));
		pickLabel.setMinimumSize(new Dimension(50, 18));
		pickLabel.setMaximumSize(new Dimension(50, 18));
		pickLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		pickLabel.setToolTipText("Pick mode state");

		filterLabel = new JLabel();
		filterLabel.setHorizontalAlignment(SwingConstants.CENTER);
		filterLabel.setText("NONE");
		filterLabel.setPreferredSize(new Dimension(50, 18));
		filterLabel.setMinimumSize(new Dimension(50, 18));
		filterLabel.setMaximumSize(new Dimension(50, 18));
		filterLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		filterLabel.setToolTipText("Current filter");

		ovrLabel = new JLabel();
		ovrLabel.setHorizontalAlignment(SwingConstants.CENTER);
		ovrLabel.setText("");
		ovrLabel.setMaximumSize(new Dimension(40, 18));
		ovrLabel.setMinimumSize(new Dimension(40, 18));
		ovrLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		ovrLabel.setPreferredSize(new Dimension(40, 18));
		ovrLabel.setToolTipText("Overlay mode state");

		selLabel = new JLabel();
		selLabel.setHorizontalAlignment(SwingConstants.CENTER);
		selLabel.setText("");
		selLabel.setMaximumSize(new Dimension(40, 18));
		selLabel.setMinimumSize(new Dimension(40, 18));
		selLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		selLabel.setPreferredSize(new Dimension(40, 18));
		selLabel.setToolTipText("Select mode state");

		scaleModeLabel = new JLabel();
		scaleModeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		scaleModeLabel.setText("");
		scaleModeLabel.setMaximumSize(new Dimension(50, 18));
		scaleModeLabel.setMinimumSize(new Dimension(50, 18));
		scaleModeLabel.setPreferredSize(new Dimension(50, 18));
		scaleModeLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		scaleModeLabel.setToolTipText("Current scale mode");

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		this.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		this.setPreferredSize(new Dimension(200, 20));
		this.add(channelCountLabel, null);
		this.add(messageLabel, null);
		this.add(scaleModeLabel, null);
		this.add(filterLabel, null);
		this.add(pickLabel, null);
		this.add(ovrLabel, null);
		this.add(selLabel, null);
	}

	public void update(Observable o, Object arg) {
		if (o instanceof GraphPanelObservable) {
			lg.debug("updating status bar due to request from " + o.getClass().getName());
			if (arg instanceof IScaleModeState) {
				scaleModeLabel.setText(((IScaleModeState) arg).getStateName());
			} else if (arg instanceof IOffsetState) {

			} else if (arg instanceof IMeanState) {

			} else if (arg instanceof IColorModeState) {

			} else if ((arg instanceof IFilter) || (arg == null)) {
				if (arg == null) {
					filterLabel.setText("NONE");
				} else {
					filterLabel.setText(((IFilter) arg).getName());
				}
			} else if (arg instanceof String) {
				String message = (String) arg;
				if (message.equals("PICK ON")) {
					pickLabel.setText("PICK");
				} else if (message.equals("PICK OFF")) {
					pickLabel.setText("");
				} else if (message.equals("OVR ON")) {
					ovrLabel.setText("OVR");
				} else if (message.equals("OVR OFF")) {
					ovrLabel.setText("");
				} else if (message.equals("SEL ON")) {
					selLabel.setText("SEL");
				} else if (message.equals("SEL OFF")) {
					selLabel.setText("");
				} else if (message.startsWith("ROT")) {

				}
			}
		}
	}

	/**
	 * Set information message
	 */
	public void setMessage(String message) {
		messageLabel.setText(message);
	}

	/**
	 * Sets channel counter values
	 * 
	 * @param start
	 *            number of first shown trace
	 * @param end
	 *            number of last shown trace
	 * @param all
	 *            total traces
	 */
	public void setChannelCountMessage(int start, int end, int all) {
		String text = new Integer(start).toString() + "-" + new Integer(end).toString() + " of " + new Integer(all).toString();
		Dimension dim = new Dimension(channelCountLabel.getFontMetrics(channelCountLabel.getFont()).stringWidth(text)+5, 18);
		channelCountLabel.setPreferredSize(dim);
		channelCountLabel.setMinimumSize(dim);
		channelCountLabel.setMaximumSize(dim);
		channelCountLabel.setText(text);
	}
}
