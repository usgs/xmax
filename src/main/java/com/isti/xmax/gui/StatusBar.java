package com.isti.xmax.gui;

import asl.utils.Filter;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.IColorModeState;
import com.isti.traceview.gui.IMeanState;
import com.isti.traceview.gui.IOffsetState;
import com.isti.traceview.gui.IScaleModeState;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import org.apache.log4j.Logger;

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
public class StatusBar extends JPanel implements PropertyChangeListener {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(StatusBar.class);

	@SuppressWarnings("unused")	
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
		// set text to handle case where it would display "1-0 of 0" more gracefully
		String text = end > 0 ?
				start + "-" + end + " of " + all :
				"NO DATA";
		Dimension dim = new Dimension(channelCountLabel.getFontMetrics(channelCountLabel.getFont()).stringWidth(text)+5, 18);
		channelCountLabel.setPreferredSize(dim);
		channelCountLabel.setMinimumSize(dim);
		channelCountLabel.setMaximumSize(dim);
		channelCountLabel.setText(text);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Object arg = evt.getNewValue();
		String message = evt.getPropertyName();
		if (evt.getSource() instanceof GraphPanel) {
			logger.debug("updating status bar due to request from " +
					evt.getSource().getClass().getName());
			if (arg instanceof IScaleModeState) {
				scaleModeLabel.setText(((IScaleModeState) arg).getStateName());
			} else if (arg instanceof IOffsetState) {

			} else if (arg instanceof IMeanState) {

			} else if (arg instanceof IColorModeState) {

			} else if ((arg instanceof Filter.FilterType) || (arg == null)) {
				if (arg == null) {
					filterLabel.setText("NONE");
				} else {
					filterLabel.setText(((Filter.FilterType) arg).getName());
				}
			} else  {
				if (message.equals("pick state")) {
					Boolean pickState = (Boolean) arg;
					if (pickState) {
						pickLabel.setText("PICK");
					} else {
						pickLabel.setText("");
					}
				} else if (message.equals("overlay state")) {
					Boolean overlayState = (Boolean) arg;
					if (overlayState) {
						ovrLabel.setText("OVR");
					} else {
						ovrLabel.setText("");
					}
				} else if (message.equals("select state")) {
					Boolean selectState = (Boolean) arg;
					if (selectState) {
						selLabel.setText("SEL");
					} else {
						selLabel.setText("");
					}
				} else if (message.startsWith("rotate")) {

				}
			}
		}
	}
}
