package com.isti.traceview.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.MouseInputListener;

import com.isti.traceview.CommandExecutor;		// multithread executor for LoadDataCommand
import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.commands.LoadDataCommand;	// testing memory usage of splitting channels
import com.isti.traceview.common.IEvent;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.EventWrapper;
import com.isti.traceview.data.PlotData;
import com.isti.traceview.data.PlotDataPoint;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Segment;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;

import javax.swing.JCheckBox;

import org.apache.log4j.Logger;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.RangeType;
import org.jfree.ui.RectangleEdge;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Graphics panel to plot several traces in the same time and values coordinate axis on a single
 * panel. Has auxiliary panel on the left side and big graph panel to plot data on the right side.
 * 
 * @author Max Kokoulin
 */

public class ChannelView extends JPanel implements Comparable<Object>, Observer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(ChannelView.class); // @jve:decl-index=0:

	public static boolean tooltipVisible = false;
	public static final int defaultInfoPanelWidth = 80;
	protected static int currentSelectionNumber = 0;

	/**
	 * @uml.property name="plotDataProviders" multiplicity="(0 -1)" dimension="1"
	 */
	private List<PlotDataProvider> plotDataProviders = null; // @jve:decl-index=0:
	List<PlotData> graphs = null;
	int height = 0;
	int maxValueAllChannels = Integer.MIN_VALUE;
	int minValueAllChannels = Integer.MAX_VALUE;
	double meanValue = Double.POSITIVE_INFINITY;

	private InfoPanel infoPanel = null;
	private GraphAreaPanel graphAreaPanel = null;
	private GraphPanel graphPanel = null;

	private int mousePressX;
	private int mousePressY;
	private NumberAxis axis = null; // @jve:decl-index=0:

	private int lastClickedY = Integer.MIN_VALUE; // to
	private int lastClickedX = Integer.MIN_VALUE;

	private List<MarkPosition> markPositions = null;
	private int selectionNumber = 0;
	//private boolean isDrawSelectionCheckBox = true;

	/**
	 * Mouse adapter for GraphAreaPanel - internal panel containing graphs
	 */
	private IMouseAdapter mouseAdapter = null;

	public ChannelView(List<PlotDataProvider> channels, int infoPanelWidth, boolean isDrawSelectionCheckBox, Color graphAreaBgColor, Color infoAreaBgColor) {
		super();
		String names = "";
		for (PlotDataProvider channel: channels) {
			names = names + channel.toString() + ";";
		}
		logger.debug("ChannelView created for list: " + names);
		initialize(infoPanelWidth, isDrawSelectionCheckBox, graphAreaBgColor, infoAreaBgColor);
		setPlotDataProviders(channels);
	}

	public ChannelView(PlotDataProvider channel, int infoPanelWidth, boolean isDrawSelectionCheckBox, Color graphAreaBgColor, Color infoAreaBgColor) {
		super();
		logger.debug("ChannelView created for " + channel.toString());
		initialize(infoPanelWidth, isDrawSelectionCheckBox, graphAreaBgColor, infoAreaBgColor);
		List<PlotDataProvider> lst = new ArrayList<PlotDataProvider>();
		lst.add(channel);
		setPlotDataProviders(lst);
	}
	
	public ChannelView(List<PlotDataProvider> channels) {
		this(channels, defaultInfoPanelWidth, true, null, null);
	}
	
	public ChannelView(PlotDataProvider channel) {
		this(channel, defaultInfoPanelWidth, true, null, null);
	}

	public ChannelView() {
		this(new ArrayList<PlotDataProvider>(), defaultInfoPanelWidth, true, null, null);
	}

	private void initialize(int infoPanelWidth, boolean isDrawSelectionCheckBox, Color graphAreaBgColor, Color infoAreaBgColor) {
		ToolTipManager.sharedInstance().unregisterComponent(this);
		axis = new NumberAxis();
		axis.setRangeType(RangeType.FULL);
		this.setLayout(new BorderLayout());
		// setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		this.add(getInfoPanel(infoPanelWidth, isDrawSelectionCheckBox, infoAreaBgColor), BorderLayout.WEST);
		this.add(getGraphAreaPanel(graphAreaBgColor), BorderLayout.CENTER);
		markPositions = new ArrayList<MarkPosition>();
	}

	/**
	 * Sets mouse adapter defines mouse action inside this ChannelView
	 * 
	 * @param ma
	 */
	public void setMouseAdapter(IMouseAdapter ma) {
		mouseAdapter = ma;
	}

	/**
	 * Gets current mouse adapter defines mouse action inside this ChannelView
	 */
	public IMouseAdapter getMouseAdapter() {
		return mouseAdapter;
	}

	/**
	 * @return value coordinate of last clicked point on this ChannelView (in pixels inside graph
	 *         area panel)
	 */
	public int getLastClickedY() {
		return lastClickedY;
	}

	/**
	 * @return time coordinate of last clicked point on this ChannelView (in pixels inside graph
	 *         area panel)
	 */
	public int getLastClickedX() {
		return lastClickedX;
	}

	/**
	 * @return width of area graph itself occupy, without left auxiliary panel
	 */
	public int getGraphAreaWidth() {
		return graphAreaPanel.width;
	}

	/**
	 * @param x
	 *            time coordinate of point (in pixels inside graph area panel)
	 * @return Set of events in this point
	 */
	public Set<IEvent> getEvents(int x) {
		Set<EventWrapper> eventWrappers = graphAreaPanel.getEvents(x);
		if (eventWrappers == null)
			return null;
		Set<IEvent> ret = new HashSet<IEvent>();
		for (EventWrapper eventWrapper: eventWrappers) {
			ret.add(eventWrapper.getEvent());
		}
		return ret;
	}

	/**
	 * @return Necessary count of data point in graph to draw inside this panel
	 */
	public int getPointCount() {
		return graphAreaPanel.getWidth();
	}

	public void update(Observable observable, Object arg) {
		logger.debug(this + ": update request from " + observable);
		if (arg instanceof TimeInterval) {
			TimeInterval ti = (TimeInterval) arg;
			logger.debug(this + " updating for range " + ti + " due to request from " + observable.getClass().getName());
			graphAreaPanel.repaint();
		}
	}

	/**
	 * Sets graph panel contains this ChannelView
	 * 
	 * @param gp
	 */
	public void setGraphPanel(GraphPanel gp) {
		this.graphPanel = gp;
	}

	/**
	 * Gets graph panel contains this ChannelView
	 */
	public GraphPanel getGraphPanel() {
		return graphPanel;
	}

	/**
	 * @return time coordinate of last pressed point on this ChannelView (in pixels inside graph
	 *         area panel)
	 */
	public int getMousePressX() {
		return mousePressX;
	}

	/**
	 * @return value coordinate of last pressed point on this ChannelView (in pixels inside graph
	 *         area panel)
	 */
	public int getMousePressY() {
		return mousePressY;
	}

	/**
	 * Adds marker to screen. Marker image loaded from markPosition.gif file placed in the jar.
	 * Parameters are coordinates of image's center.
	 * 
	 * @param time
	 *            time position (in internal Java time representation)
	 * @param value
	 *            value position
	 */
	public void addMarkPosition(long time, double value) {
		markPositions.add(new MarkPosition(time, value));
	}

	/**
	 * Clears all markers
	 */
	public void clearMarkPositions() {
		markPositions.clear();
	}

	/**
	 * Getter of the property <tt>plotDataProviders</tt>
	 * 
	 * @return the list of PlotDataProviders drawn in this ChannelView.
	 * @uml.property name="plotDataProviders"
	 */
	public List<PlotDataProvider> getPlotDataProviders() {
		return plotDataProviders;
	}

	/**
	 * @return pixelized graph data drawn in this ChannelView.
	 */
	public List<PlotData> getPlotData() {
		return graphs;
	}

	/**
	 * Setter of the property <tt>plotDataProviders</tt>
	 * 
	 * @param channels
	 *            List of plotDataProviders to draw inside this ChannelView.
	 * @uml.property name="plotDataProviders"
	 */
	public void setPlotDataProviders(List<PlotDataProvider> channels) {
		if (plotDataProviders != null) {
			for (PlotDataProvider channel: plotDataProviders) {
				channel.deleteObserver(this);
			}
		}
		plotDataProviders = channels;
		for (PlotDataProvider channel: plotDataProviders) {
			channel.addObserver(this);
			logger.debug("Observer for " + channel.toString() + " added");
			if (channel.getMaxValue() > maxValueAllChannels) {
				maxValueAllChannels = channel.getMaxValue();
			}
			if (channel.getMinValue() < minValueAllChannels) {
				minValueAllChannels = channel.getMinValue();
			}
		}
		//CommandExecutor.getInstance().execute(new LoadDataCommand(channels, null));
		lastClickedY = Integer.MIN_VALUE;
	}

	/**
	 * Return time interval of loaded data
	 */
	public TimeInterval getLoadedTimeRange() {
		TimeInterval ret = null;
		for (PlotDataProvider channel: plotDataProviders) {
			if (ret == null) {
				ret = channel.getTimeRange();
			} else {
				ret = TimeInterval.getAggregate(ret, channel.getTimeRange());
			}
		}
		return ret;
	}

	/**
	 * Prepares pixelized data for PlotDataProviders to draw. Should be called before paint.
	 */
	public synchronized void updateData() {

		int width = graphAreaPanel.getWidth();// - graphAreaPanel.getInsets().left -
		// graphAreaPanel.getInsets().right;
		logger.debug("Updating data " + this + "Width = " + width);
		graphs = new ArrayList<PlotData>();
		
		/**
		System.out.println();
		System.out.println("== ChannelView updateData() Take a peak:");
				for (PlotDataProvider channel: plotDataProviders) {
		            System.out.format("   [PDP: %s] [nsegs=%d] [isLoadingStarted=%s] [isLoaded=%s]\n",
		                channel.toString(), channel.getSegmentCount(), channel.isLoadingStarted(), channel.isLoaded() );
		            List<Segment> segs = channel.getRawData();
		            //System.out.format("     [rdp#][chanSerial#]:Segment\n");
		            for (Segment seg : segs) {
		                System.out.format("         [%d][%d]:%s [Source:%s]\n", seg.getSourceSerialNumber(), seg.getChannelSerialNumber(),
		                    seg.toString(), seg.getDataSource().getName() );
		            }
		        }
		System.out.println("== ChannelView updateData() Done");
		System.out.println();
		**/

		for (PlotDataProvider channel: plotDataProviders) {
			// lg.debug("processing channel: " + channel);
			PlotData data = null;
			try {
				data = channel.getPlotData(graphPanel.getTimeRange(), width, graphPanel.getRotation(), graphPanel.getFilter(), graphPanel.getColorMode());
			} catch (TraceViewException e) {
				graphPanel.setRotation(null);
				JOptionPane.showMessageDialog(TraceView.getFrame(), e, "Rotation warning", JOptionPane.WARNING_MESSAGE);
				try {
					data = channel.getPlotData(graphPanel.getTimeRange(), width, null, graphPanel.getFilter(), graphPanel.getColorMode());
				} catch (TraceViewException e1) {
					// do nothing
					logger.error("TraceViewException:", e1);	
				}
			}

			graphs.add(data);
			meanValue = data.getMeanValue();
		}
		Collections.sort(graphs);
	}

	/**
	 * Cistomized method to paint events.
	 */
	public void paintCustomEvent(Graphics g, EventWrapper eventWrapper, int x, int ymax, int ymin) {
	}

	/**
	 * This method initializes infoPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private InfoPanel getInfoPanel(int infoPanelWidth, boolean isDrawSelectionCheckBox, Color infoAreaBgColor) {
		if (infoPanel == null) {
			infoPanel = new InfoPanel(this, infoPanelWidth, isDrawSelectionCheckBox, infoAreaBgColor);
		}
		return infoPanel;
	}

	/**
	 * This method initializes graphAreaPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private GraphAreaPanel getGraphAreaPanel(Color graphAreaBgColor) {
		if (graphAreaPanel == null) {
			graphAreaPanel = new GraphAreaPanel(this, graphAreaBgColor);
		}
		return graphAreaPanel;
	}

	/**
	 * Gets string representation of ChannelView in the debug purposes
	 */
	public String toString() {
		String ret = "";
		for (PlotDataProvider channel: plotDataProviders) {
			ret = ret + channel.getStation().getName() + "/" + channel.getChannelName() + " ";
		}
		return "ChannelView: " + ret;
	}

	/**
	 * @return flag if this ChannelView selected(by checkbox on info panel)
	 */
	public boolean isSelected() {
		return infoPanel.isSelected();
	}

	/**
	 * Adds MouseListener to internal graph panel
	 */
	public void addMouseListener(MouseListener l) {
		graphAreaPanel.addMouseListener(l);
	}

	/**
	 * Adds MouseMotionListener to internal graph panel
	 */
	public void addMouseMotionListener(MouseMotionListener l) {
		graphAreaPanel.addMouseMotionListener(l);
	}

	/**
	 * Sets cursor for internal graph panel
	 */
	public void setCursor(Cursor cursor) {
		graphAreaPanel.setCursor(cursor);
	}

	/**
	 * When user selects several ChannelViews on the {@link GraphPanel}, each ChannelView stores
	 * his sequential number in this selection. If GraphPanel will be set to Selection mode,
	 * ChannelView will be shown in the order of their selectionNumbers
	 * 
	 * @return sequential number during selection
	 */
	public int getSelectionNumber() {
		return selectionNumber;
	}

	/**
	 * This method should be called after selection mode changing in GraphPanel
	 */
	public void selectionPerformed() {

	}

	/**
	 * Comparator by selection number
	 */
	public int compareTo(Object o) {
		if (o instanceof ChannelView) {
			ChannelView c = (ChannelView) o;
			return new Integer(getSelectionNumber()).compareTo(new Integer(c.getSelectionNumber()));
		} else {
			return 1;
		}
	}

	/**
	 * Left panel for auxiliary information: selection checkbox, axis painting etc
	 */
	class InfoPanel extends JPanel implements ItemListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private NonPrintableCheckBox selected = null;
		private ChannelView channelView = null;

		public InfoPanel(ChannelView cv, int infoPanelWidth, boolean isDrawSelectionCheckBox, Color infoAreaBgColor) {
			super();
			if(infoAreaBgColor!=null){
				setBackground(infoAreaBgColor);
			}
			this.channelView = cv;
			setPreferredSize(new Dimension(infoPanelWidth, 0));
			setBorder(BorderFactory.createEtchedBorder());
			setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
			if(isDrawSelectionCheckBox){
				add(getSelected());
			}
		}

		public boolean isSelected() {
			return selected.isSelected();
		}

		/**
		 * This method initializes selected
		 * 
		 * @return Customized checkbox for ChannelView selection which drawn on screen and don't
		 *         drawn during rendering for print
		 */
		private NonPrintableCheckBox getSelected() {
			if (selected == null) {
				selected = new NonPrintableCheckBox();
				selected.addItemListener(this);
			}
			return selected;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			axis.draw((Graphics2D) g, getWidth() - 5, new Rectangle(0, 0, getWidth(), getHeight()), new Rectangle(0, 0, getWidth(), getHeight()),
					RectangleEdge.LEFT, null);
		}

		private class NonPrintableCheckBox extends JCheckBox {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void paint(Graphics g) {
				// if(!isPaintingForPrint()){ //works only in jre 1.6
				super.paint(g);
				// }
			}
		}

		/** Listens to the check box. */
		public void itemStateChanged(ItemEvent e) {
			if (!(graphPanel.getSelectState() || graphPanel.getOverlayState())) {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					selectionNumber = 0;
					graphPanel.getSelectedChannelShowSet().remove(channelView);
				} else if (e.getStateChange() == ItemEvent.SELECTED) {
					graphPanel.getSelectedChannelShowSet().add(channelView);
					currentSelectionNumber++;
					selectionNumber = currentSelectionNumber;
				}
			}
			selectionPerformed();
		}
	}

	/**
	 * Big right panel for graphs drawing
	 */
	class GraphAreaPanel extends JPanel implements MouseInputListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Panel height in pixel
		 */
		int height;

		/**
		 * Panel width in pixel
		 */
		int width;

		/**
		 * Apmlitude of contained graph - precomputed value to increase speed of paintComponent()
		 * calculations
		 */
		int amp;

		ChannelView cv = null;

		private int button = MouseEvent.NOBUTTON;
		private int fontHeight = 0;

		public GraphAreaPanel(ChannelView cv, Color graphAreaBgColor) {
			super();
			this.cv = cv;
			if(graphAreaBgColor != null){
				setBackground(graphAreaBgColor);
			}
			setLayout(new GridBagLayout());
			setBorder(BorderFactory.createEtchedBorder());
			addMouseListener(this);
			addMouseMotionListener(this);
			setToolTipText("YYY");
		}

		public void paint(Graphics g) {
			logger.debug("Repainting " + this);
			super.paint(g);
			// maxValue = Integer.MIN_VALUE;
			// minValue = Integer.MAX_VALUE;
			height = this.getHeight();
			width = this.getWidth();
			IScaleModeState scaleMode = graphPanel.getScaleMode();
			IMeanState meanState = graphPanel.getMeanState();
			IOffsetState offsetState = graphPanel.getOffsetState();
			scaleMode.init(graphs, (graphPanel.getOverlayState() == true) || (graphPanel.getSelectState() == true) ? graphPanel
					.getCurrentChannelShowSet() : graphPanel.getChannelShowSet(), graphPanel.getTimeRange(), meanState, height);
			//lg.debug("scaleMode Initialized:" + scaleMode.getStateName() + scaleMode.getMaxValue() + scaleMode.getMinValue());
			if (scaleMode.getMinValue() != Double.POSITIVE_INFINITY && scaleMode.getMaxValue() != Double.NEGATIVE_INFINITY) {
				axis.setRange(scaleMode.getMinValue(), scaleMode.getMaxValue());
			}
			//Offset step is 1/20 of graph height
			offsetState.setShift((scaleMode.getMaxValue() - scaleMode.getMinValue()) / 20);
			logger.debug("Set ChannelView " + this + " boundaries: " + scaleMode.getMaxValue() + "-" + scaleMode.getMinValue());
			// Graph's number, used to separate graphs then overlay mode is activated
			int graphNum = 0;
			Color segmentColor = null;
			for (PlotData data: graphs) {
				int i = 0;
				logger.debug("Drawing PlotData " + i + ", " + data.getLabel() + ": max " + data.getMaxValue() + ", min " + data.getMinValue() + ", mean " + data.getMeanValue());
				// strokes for previous pixel
				List<Stroke> yprev = new ArrayList<Stroke>();
				for (PlotDataPoint[] points: data.getPixels()) {
					int j = 0;
					for (PlotDataPoint point: points) {
						//add previous stroke to list if list has unsuffisient length
						if(yprev.size()==j || yprev.get(j)==null){
							yprev.add(j, new Stroke());
						}
						Stroke current = new Stroke();
						if (point.getSegmentNumber() >= 0) {
							segmentColor = graphPanel.getColorMode().getSegmentColor(graphNum + point.getSegmentNumber(), graphNum + point.getRawDataProviderNumber(), graphNum + point.getContinueAreaNumber(), data.getTraceColor());
							if (point.getSegmentNumber() == 0 && data.getLabelColor() == null) {
								data.setLabelColor(segmentColor);
							}
							g.setColor(segmentColor);
							// reinit previous stroke if color differs
							if ((yprev.get(j).color!=null) && (yprev.get(j).color != segmentColor)){
								yprev.set(j, new Stroke());
							}
							current.color = segmentColor;
							current.top = scaleMode.getY(meanState.getValue(offsetState.getValue(point.getTop(), point.getSegmentNumber()), data.getMeanValue()));
							current.bottom = scaleMode.getY(meanState.getValue(offsetState.getValue(point.getBottom(), point.getSegmentNumber()), data.getMeanValue()));
							//lg.debug("Drawing pixel " + j + ": " + point.getTop() + "-" + point.getBottom() + ", " + current);
							g.drawLine(i, current.top, i, current.bottom);
							if (i > 0) {
								// fill vertical gaps
								if (current.bottom < yprev.get(j).top) {
									//lg.debug("Fill gap at top: " +  yprev.get(j).top + "-" + current.bottom);
									g.drawLine(i - 1, yprev.get(j).top, i, current.bottom);
								}
								if (current.top > yprev.get(j).bottom) {
									//lg.debug("Fill gap at bottom: " + yprev.get(j).bottom + "-" + current.top);
									g.drawLine(i - 1, yprev.get(j).bottom, i, current.top);
								}
							}
							yprev.set(j, current);
						} else {
							//we have gap, set previous values to it's default
							yprev.set(j, new Stroke());
						}
						// drawing events
						//long currentTime = getTime(i);
						for (EventWrapper eventWrapper: point.getEvents()) {
							logger.debug("drawing event front");
							g.setColor(eventWrapper.getEvent().getColor());
							if (eventWrapper.getEvent().getType().equals("ARRIVAL") && graphPanel.getPhaseState()) {
								// drawing phases
								if (graphPanel.getSelectedEarthquakes().contains(eventWrapper.getEvent().getParameterValue("EARTHQUAKE"))
										&& graphPanel.getSelectedPhases().contains(eventWrapper.getEvent().getParameterValue("PHASE"))) {
									g.drawLine(i, getHeight(), i, 0);
									g.drawString((String) eventWrapper.getEvent().getParameterValue("PHASE"), i + 2, getHeight() - 5);
								}
							} else if (eventWrapper.getEvent().getType().equals("PICK") && graphPanel.getPickState()) {
								// drawing picks
								g.drawLine(i, getHeight(), i, 0);
								Polygon p = new Polygon();
								p.addPoint(i, 0);
								p.addPoint(i + 4, 4);
								p.addPoint(i, 8);
								g.fillPolygon(p);
							} else {
								paintCustomEvent(g, eventWrapper, i, current.top, current.bottom);
								g.setColor(segmentColor);
							}
						}
						j++;
					}
					while(j<yprev.size()){
						yprev.set(j, new Stroke());
						j++;
					}
					i++;
				}
				graphNum++;
			}

			if (plotDataProviders != null) {
				logger.debug("drawing channel labels");
				g.setFont(GraphPanel.getAxisFont());
				fontHeight = g.getFontMetrics().getHeight();
				int i = 1;
				for (PlotData data: graphs) {
					g.setColor(data.getLabelColor());
					g.drawString(data.getLabel(), getWidth() - 120, i++ * fontHeight);
				}
				// drawing Y axis labels
				g.setColor(Color.BLACK);
				if (scaleMode.getMaxValue() != Double.POSITIVE_INFINITY && scaleMode.getMaxValue() != Double.NEGATIVE_INFINITY
						&& !Double.isInfinite(scaleMode.getMinValue())) {
					g.drawString(new Double(scaleMode.getMaxValue()).toString(), 10, fontHeight);
				}
				if (scaleMode.getMinValue() != Double.POSITIVE_INFINITY && scaleMode.getMinValue() != Double.NEGATIVE_INFINITY
						&& !Double.isInfinite(scaleMode.getMinValue())) {
					g.drawString(new Double(scaleMode.getMinValue()).toString(), 10, getHeight() - 10);
				}
				// drawing marks
				for (MarkPosition mp: markPositions) {
					Image image = graphPanel.getMarkPositionImage();
					g.drawImage(image, graphPanel.getXposition(mp.getTime()) - image.getHeight(this) / 2, graphPanel.getScaleMode().getY(
							mp.getValue())
							- image.getHeight(this) / 2, this);
				}
			}
			logger.debug("Repainting end " + this);
		}
		
		
		/**
		 * Computes trace time value
		 * 
		 * @param x
		 *            screen panel coordinate
		 * @return time value in internal Java format
		 */
		public long getTime(int x) {
			// lg.debug("ChannelView getTime: " + x);
			TimeInterval ti = graphPanel.getTimeRange();
			return new Double(ti.getStart() + x * new Double(ti.getDuration())/ getWidth()).longValue();
		}

		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			if ((button != MouseEvent.NOBUTTON) && (e.isControlDown() || e.isShiftDown())) {
				mouseDragged(e);
			} else {
				if (mouseAdapter != null) {
					mouseAdapter.mouseMoved(x, e.getY(), cv);
				}
				graphPanel.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, graphPanel));
			}
		}

		public void mouseDragged(MouseEvent e) {
			// lg.debug("ChannelView.mouseDragged");

			if (mouseAdapter != null) {
				mouseAdapter.mouseDragged(e.getX(), e.getY(), cv);
			}
			graphPanel.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, graphPanel));
		}

		public void mouseClicked(MouseEvent e) {
			int clickedX = e.getX();
			int clickedY = e.getY();
			//long clickedTime = graphPanel.getTime(clickedX);
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (mouseAdapter != null) {
					mouseAdapter.mouseClickedButton1(clickedX, clickedY, cv);
				}
				lastClickedY = clickedY;
				lastClickedX = clickedX;
			} else if (e.getButton() == MouseEvent.BUTTON3) {
				if (mouseAdapter != null) {
					mouseAdapter.mouseClickedButton3(clickedX, clickedY, cv);
				}
			}
			graphPanel.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, graphPanel));
		}

		public void mouseEntered(MouseEvent e) {
			graphPanel.forceRepaint();
		}

		public void mouseExited(MouseEvent e) {

		}

		public void mousePressed(MouseEvent e) {
			// lg.debug("ChannelView.mousePressed");
			mousePressX = e.getX();
			mousePressY = e.getY();
			graphPanel.getScaleMode().init(
					graphs,
					(graphPanel.getOverlayState() == true) || (graphPanel.getSelectState() == true) ? graphPanel.getCurrentChannelShowSet()
							: graphPanel.getChannelShowSet(), graphPanel.getTimeRange(), graphPanel.getMeanState(), getHeight());
			// one-button mouse Mac OSX behaviour emulation
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.isShiftDown()) {
					button = MouseEvent.BUTTON2;
				} else if (e.isControlDown()) {
					button = MouseEvent.BUTTON3;
				} else {
					button = MouseEvent.BUTTON1;
				}
			} else {
				button = e.getButton();
			}
			graphPanel.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, graphPanel));
		}

		public void mouseReleased(MouseEvent e) {
			// lg.debug("ChannelView.mouseReleased");
			if (button != MouseEvent.NOBUTTON && ((mousePressX != e.getX()) || (mousePressY != e.getY()))) {
				if (button == MouseEvent.BUTTON3 || (button == MouseEvent.BUTTON1 && e.isControlDown() == true)) {
					if (mouseAdapter != null) {
						mouseAdapter.mouseReleasedButton3(e.getX(), e.getY(), cv);
					}
				} else if (e.getButton() == MouseEvent.BUTTON1) {
					if (mouseAdapter != null) {
						mouseAdapter.mouseReleasedButton1(e.getX(), e.getY(), cv);
					}
				}
			}
			button = MouseEvent.NOBUTTON;
			graphPanel.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, graphPanel));
		}

		// Hack to correct repaint in all-screen mode
		private boolean toolTipTextWasChanged = false;

		public String getToolTipText(MouseEvent event) {
			int x = event.getX();
			int y = event.getY();
			int channelNumber = 0;
			// lg.debug("getToolTipText: X=" + x + "; Y=" + y);
			if (fontHeight != 0) {
				channelNumber = y / fontHeight;
			}
			if ((channelNumber < plotDataProviders.size()) && (x > getWidth() - 120) && (x < getWidth() - 20)) {
				PlotDataProvider channel = plotDataProviders.get(channelNumber);
				toolTipTextWasChanged = true;
				// lg.debug("getToolTipText: label");
				return getChannelLabelText(channel);
			} else if(graphPanel.getShowBlockHeader()){
				toolTipTextWasChanged = true;
				PlotDataProvider channel = plotDataProviders.get(0);
				return getBlockHeaderText(channel, x);
			} else {
				// lg.debug("getToolTipText: empty");
				if (toolTipTextWasChanged) {
					toolTipTextWasChanged = false;
					graphPanel.forceRepaint();
					graphPanel.repaint();
				}
				return null;
			}
		}

		protected String getChannelLabelText(PlotDataProvider channel) {
			String respname = "No response";
			try {
				if (channel.getResponse() != null) {
					respname = channel.getResponse().getFileName();
				}
			} catch (TraceViewException e) {
				// do nothing
			}
			return "<html>" + channel.getName() + "<br><i>Start time: </i> "
					+ TimeInterval.formatDate(channel.getTimeRange().getStartTime(), TimeInterval.DateFormatType.DATE_FORMAT_MIDDLE)
					+ "<br><i>Duration: </i> " + channel.getTimeRange().convert() + "<br><i>Sample rate: </i>" + channel.getSampleRate() + " ms <br>"
					+ respname + "</html>";
		}
		
		protected String getBlockHeaderText(PlotDataProvider channel, int x) {
			long time = getTime(x);
			List<Segment> segments = channel.getRawData(new TimeInterval(time, time));
			if(segments.size()>0){
				return  segments.get(0).getBlockHeaderText(time);
			} else {
				return  "<html>There is no data in this place</html>";
			}
		}
		
		
		public JToolTip createToolTip(){
			return new CVToolTip();
		}
		
		class CVToolTip extends JToolTip{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public CVToolTip(){
				logger.debug("CVToolTip: create");
				tooltipVisible = true;
			}
			
			@SuppressWarnings("deprecation")
			public void show(){
				logger.debug("CVToolTip: show");
				super.show();
			}
			
			@SuppressWarnings("deprecation")
			public void hide(){
				logger.debug("CVToolTip: hide");
				super.hide();
			}
			
			public void repaint(){
				super.repaint();
				graphPanel.repaint();
			}
			
			protected void finalize()throws Throwable {
				logger.debug("CVToolTip: false");
				tooltipVisible = false;
				super.finalize();
			}
		}

		private Set<EventWrapper> getEvents(int x) {
			// lg.debug("getEvents: x= " + x);
			if (graphs != null) {
				for (PlotData data: graphs) {
					if (data.getPointCount() > x) {
						Set<EventWrapper> ret = new HashSet<EventWrapper>();
						for (PlotDataPoint dp: data.getPixels().get(x)) {
							if (dp.getEvents().size() != 0) {
								ret.addAll(dp.getEvents());
							}
						}
						if (ret.size() > 0) {
							return ret;
						} else {
							return null;
						}
					} else {
						return null;
					}
				}
			}
			return null;
		}
	}

}// @jve:decl-index=0:visual-constraint="10,10"

/*
class CustomToolTipManager extends ToolTipManager {
	public CustomToolTipManager(){}
	public void mouseMoved(MouseEvent event){
		super.mouseMoved(event);
		if(event.getSource() instanceof ChannelView){
			ChannelView cv = (ChannelView)event.getSource(); 
			cv.getGraphPanel().forceRepaint = true;
		}
	}
}
*/

class Stroke {
	int top = Integer.MIN_VALUE;
	int bottom = Integer.MAX_VALUE;
	Color color = null;
	
	public String toString(){
		return "Stroke: " + top + "-" + bottom + ", color " + color;
	}
}

/**
 * Class to represent marker on graph area.
 */
class MarkPosition {
	private long time = Long.MIN_VALUE;
	private double value = Double.NEGATIVE_INFINITY;

	public MarkPosition(long time, double value) {
		this.time = time;
		this.value = value;
	}

	public long getTime() {
		return time;
	}

	public double getValue() {
		return value;
	}
}
