package com.isti.traceview.gui.controls;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BoxLayout;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputListener;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceView;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.gui.GraphPanel;
import com.isti.traceview.gui.GraphPanel.GraphPanelObservable;

/**
 * Scroll bar to control traces in Graph Panel. 
 * 
 * @author Max Kokoulin
 */
public class Slider extends JPanel implements Observer {
	private static Logger lg = Logger.getLogger(Slider.class);
	private static final Dimension size = new Dimension(200, 30);
	private static Insets inset = new Insets(0, 0, 0, 0);
	private Font labelFont = null;

	private GraphPanel graphPanel = null;
	private SliderPanel slider = null;
	private TimeInterval range = null;

	private int mousePressX = Integer.MIN_VALUE;
	private int sliderMoved = 0;

	public Slider(GraphPanel graphPanel) {
		super();
		this.graphPanel = graphPanel;
		this.range = null;
		BoxLayout thisLayout = new BoxLayout(this, javax.swing.BoxLayout.X_AXIS);
		setLayout(thisLayout);
		graphPanel.addObserver(this);
		TraceView.getDataModule().addObserver(this);
		slider = new SliderPanel();
		add(slider);
		setSize(size);
		setPreferredSize(size);
		setMinimumSize(new Dimension(200, 30));
		setMaximumSize(new Dimension(10000, size.width));
		labelFont = new Font(getFont().getName(), getFont().getStyle(), getFont().getSize() - 1);
		this.validate();
	}

	/**
	 * Gets time range of scroll bar, this time range represented whole length of bar
	 */
	public TimeInterval getRange() {
		return range;
	}

	/**
	 * Sets time range of scroll bar, this time range represented whole length of bar
	 */
	public void setRange(TimeInterval range) {
		this.range = range;
	}

	/**
	 * Gets position of slider, in pixels
	 */
	private int getSliderPosition() {
		return new Long(slider.getWidth() * (graphPanel.getTimeRange().getStart() - range.getStart()) / range.getDuration()).intValue();
	}
	/**
	 * 	Gets width of slider, in pixels
	 */
	private int getSliderWidth() {
		return new Long(slider.getWidth() * (graphPanel.getTimeRange().getEnd() - range.getStart()) / range.getDuration()).intValue()
				- getSliderPosition();
	}

	/**
	 * Gets date corresponding position on scroll bar
	 * @param x
	 * @return
	 */
	private Date getDate(int x) {
		return new Date(range.getStart() + range.getDuration() * x / slider.getWidth());
	}

	public void update(Observable o, Object arg) {
		lg.debug(this + ": update request from " + o);
		if (o instanceof DataModule) {
			if (arg instanceof TimeInterval) {
				setRange((TimeInterval) arg);
				repaint();
			}
		} else if (o instanceof GraphPanelObservable) {
			if (arg instanceof TimeInterval) {
				repaint();
			}
		}
	}

	private class SliderPanel extends JPanel implements MouseInputListener {

		public SliderPanel() {
			super();
			this.setBorder(new LineBorder(Color.BLACK, 2, true));
			this.setBackground(Color.WHITE);
			this.setMaximumSize(new Dimension(10000, 18));
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (graphPanel.getTimeRange() == null || range == null) {
				g.drawLine(0, 0, getWidth(), getHeight());
				g.drawLine(0, getHeight(), getWidth(), 0);
			} else {
				lg.debug("SliderPanel repaint: position " + getSliderPosition() + ", width " + getSliderWidth() + ", shift " + sliderMoved);
				int startx = getSliderPosition() + sliderMoved;
				int width = getSliderWidth();
				g.fillRoundRect(startx, 0, width, getHeight(), 3, 3);
				if (mousePressX != Integer.MIN_VALUE) {
					g.setFont(labelFont);
					FontMetrics fontMetrics = g.getFontMetrics();
					g.setXORMode(Color.WHITE);
					g.drawString(TimeInterval.formatDate(getDate(startx), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE), 5,
							fontMetrics.getHeight() - 2);
					String end = TimeInterval.formatDate(getDate(startx + width), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE);
					g.drawString(end, getWidth() - fontMetrics.stringWidth(end) - 5, fontMetrics.getHeight() - 2);
				}
			}
		}

		/**
		 * Methods from MouseInputListener interface to handle mouse events.
		 */

		public void mouseMoved(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			lg.debug("SliderPanel: mouseDragged");
			if (mousePressX == Integer.MIN_VALUE) {
				sliderMoved = 0;
			} else {
				sliderMoved = e.getX() - mousePressX;
			}
			repaint();
		}

		public void mouseClicked(MouseEvent e) {
			int x = e.getX();
			if (x < getSliderPosition()) {
				TimeInterval current = graphPanel.getTimeRange();
				graphPanel.setTimeRange(new TimeInterval(current.getStart() - current.getDuration(), current.getEnd() - current.getDuration()));
			} else if (x > (getSliderPosition() + getSliderWidth())) {
				TimeInterval current = graphPanel.getTimeRange();
				graphPanel.setTimeRange(new TimeInterval(current.getStart() + current.getDuration(), current.getEnd() + current.getDuration()));
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			lg.debug("SliderPanel: mousePressed");
			int x = e.getX();
			if (x > getSliderPosition() && x < (getSliderPosition() + getSliderWidth())) {
				mousePressX = x;
				sliderMoved = 0;
				repaint();
			}
		}

		public void mouseReleased(MouseEvent e) {
			lg.debug("SliderPanel: mouseReleased");
			mousePressX = Integer.MIN_VALUE;
			long start = getDate(getSliderPosition() + sliderMoved).getTime();
			graphPanel.setTimeRange(new TimeInterval(start, start + graphPanel.getTimeRange().getDuration()));
			sliderMoved = 0;
		}
	}
}
