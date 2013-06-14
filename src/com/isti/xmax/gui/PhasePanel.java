package com.isti.xmax.gui;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JScrollPane;

import com.isti.xmax.XMAX;
import com.isti.xmax.common.Earthquake;
import com.isti.traceview.common.IEvent;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JSplitPane;
import javax.swing.BoxLayout;

import org.apache.log4j.Logger;

/**
 * Side-panel for earthquakes and phases selection. Contain list if available earthquakes (i.e those
 * from list from XMAXDataModule which may have arrivals in the current time range) and list of phase
 * names. First user selects earthquakes, then phases list updates for current earthquakes
 * selection, and he selects phases from updated list. Selected earthquakes and phases shows as a
 * marks on graph panel.
 * 
 * @author Max Kokoulin
 */
public class PhasePanel extends JPanel implements ListSelectionListener {
	private static Logger lg = Logger.getLogger(PhasePanel.class); // @jve:decl-index=0:
	private static SimpleDateFormat df = new SimpleDateFormat("yyyy,DDD,HH:mm:ss");
	private static Object[] initialSelectedPhases = { "P" };

	static {
		df.setTimeZone(XMAX.timeZone);
	}

	private JScrollPane earthquakesSP = null;
	private JScrollPane phasesSP = null;
	private PhaseList earthquakesL = null;
	private PhaseList phasesL = null;
	private JSplitPane splitter = null;
	private XMAXGraphPanel graphPanel = null;
	private boolean init = false;

	/**
	 * Earthquakes ever selected to fill selection
	 */
	private Set<IEvent> everSelectedEarthquakes = null; // @jve:decl-index=0:
	/**
	 * Phases ever selected to fill selection
	 */
	private Set<String> everSelectedPhases = null; // @jve:decl-index=0:

	/**
	 * This method initializes phase panel
	 */
	public PhasePanel(XMAXGraphPanel gp) {
		super();
		this.graphPanel = gp;
		initialize();
		init = true;
		refreshAvailableEarthQuakes();
		splitter.setDividerLocation(new Double(gp.getHeight() / 3).intValue());
		init = false;
		earthquakesL.setSelectedValues(graphPanel.getAvailableEarthquakes(), false);
		phasesL.setSelectedValues(initialSelectedPhases, false);
	}

	/**
	 * This method initializes this phase panel
	 */
	private void initialize() {
		this.setPreferredSize(new Dimension(120, 100));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(getSplitter(), null);
		everSelectedEarthquakes = new HashSet<IEvent>();
		everSelectedPhases = new HashSet<String>();
	}

	/**
	 * This method initializes earthquakesSP
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getEarthquakesSP() {
		if (earthquakesSP == null) {
			earthquakesSP = new JScrollPane();
			earthquakesSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			earthquakesSP.setViewportView(getEarthquakesL());
			earthquakesSP.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		}
		return earthquakesSP;
	}

	/**
	 * This method initializes phasesSP
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getPhasesSP() {
		if (phasesSP == null) {
			phasesSP = new JScrollPane();
			phasesSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			phasesSP.setViewportView(getPhasesL());
			phasesSP.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		}
		return phasesSP;
	}

	/**
	 * This method initializes earthquakesL
	 * 
	 * @return javax.swing.JList
	 */
	private JList getEarthquakesL() {
		if (earthquakesL == null) {
			earthquakesL = new PhaseList(XMAX.getDataModule().getEarthquakes().toArray());
			earthquakesL.setCellRenderer(new EarthquakeCellRenderer());
			earthquakesL.addListSelectionListener(this);
		}
		return earthquakesL;
	}

	/**
	 * This method initializes phasesL
	 * 
	 * @return javax.swing.JList
	 */
	private JList getPhasesL() {
		if (phasesL == null) {
			phasesL = new PhaseList();
			phasesL.addListSelectionListener(this);
		}
		return phasesL;
	}

	/**
	 * This method initializes splitter
	 * 
	 * @return javax.swing.JSplitPane
	 */
	private JSplitPane getSplitter() {
		if (splitter == null) {
			splitter = new JSplitPane();
			splitter.setOrientation(JSplitPane.VERTICAL_SPLIT);
			splitter.setDividerSize(3);
			splitter.setBottomComponent(getPhasesSP());
			splitter.setTopComponent(getEarthquakesSP());
		}
		return splitter;
	}

	public Set<IEvent> getEverSelectedEarthquakes() {
		return everSelectedEarthquakes;
	}

	public Set<String> getEverSelectedPhases() {
		return everSelectedPhases;
	}

	public void setEverSelectedPhases(Set<IEvent> earthquakes, Set<String> phases) {
		lg.debug("PhasePanel: setting selected values");
		everSelectedEarthquakes = earthquakes;
		everSelectedPhases = phases;
	}

	public void addEverSelectedPhases(Set<IEvent> earthquakes, Set<String> phases) {
		// lg.debug("PhasePanel: adding selected values");
		everSelectedEarthquakes = earthquakes;
		everSelectedPhases.addAll(phases);
	}

	/**
	 * we must call with method after time scale changes in graph panel
	 */
	public void refreshAvailableEarthQuakes() {
		// lg.debug("refreshing earthquakes");
		init = true;
		earthquakesL.setListData(graphPanel.getAvailableEarthquakes());
		earthquakesL.setSelectedValues(getEverSelectedEarthquakes().toArray(), false);
		refreshAvailablePhases();
		init = false;
	}

	/**
	 * this method called after changing selected earthquakes list
	 */
	private void refreshAvailablePhases() {
		// lg.debug("refreshing phases");
		init = true;
		phasesL.setListData(graphPanel.getAvailablePhases(earthquakesL.getSelectedValues()));
		phasesL.setSelectedValues(getEverSelectedPhases().toArray(), false);
		init = false;

	}

	/**
	 * This method is required by ListSelectionListener.
	 */
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false && !init) {
			if (e.getSource().equals(earthquakesL)) {
				refreshAvailablePhases();
			}
			Set<IEvent> le = new HashSet<IEvent>();
			for (Object o: earthquakesL.getSelectedValues()) {
				le.add((IEvent) o);
			}
			Set<String> lp = new HashSet<String>();
			for (Object o: phasesL.getSelectedValues()) {
				lp.add((String) o);
			}
			if (e.getSource().equals(earthquakesL)) {
				addEverSelectedPhases(le, lp);
			} else {
				setEverSelectedPhases(le, lp);
			}
			graphPanel.setSelectedPhases(le, lp);
		}
	}

	private class PhaseList extends JList {
		public PhaseList() {
			super();
			init();
		}

		public PhaseList(Object[] listData) {
			super(listData);
			init();
		}

		private void init() {
			setBackground(XMAXframe.getInstance().getBackground());
		}

		/**
		 * Selects the specified array of objects from the list.
		 * 
		 * @param anObject[]
		 *            the array if objects to select
		 * @param shouldScroll
		 *            {@code true} if the list should scroll to display the selected object, if one
		 *            exists; otherwise {@code false}
		 */
		public void setSelectedValues(Object[] objects, boolean shouldScroll) {
			if (objects == null) {
				setSelectedIndex(-1);
			} else {
				int i, c;
				clearSelection();
				ListModel dm = getModel();
				for (Object object: objects) {
					for (i = 0, c = dm.getSize(); i < c; i++) {
						if (object.equals(dm.getElementAt(i))) {
							this.addSelectionInterval(i, i);
							if (shouldScroll) {
								ensureIndexIsVisible(i);
							}
						}
					}
				}
			}
			repaint();
		}
	}

	private class EarthquakeCellRenderer extends JLabel implements ListCellRenderer {
		public Component getListCellRendererComponent(JList list, // the list
				Object value, // value to display
				int index, // cell index
				boolean isSelected, // is the cell selected
				boolean cellHasFocus) // does the cell have focus
		{
			Earthquake earthquake = (Earthquake) value;
			setText(earthquake.getSourceCode());
			setToolTipText("<html>" + df.format(earthquake.getStartTime()) + "<br><i>Long  </i>" + earthquake.getLongitude() + "<br><i>Lat   </i>"
					+ earthquake.getLatitude() + "<br><i>Depth </i>" + earthquake.getDepth() + "<br><i>Mag1  </i>" + earthquake.getMagnitude_mb()
					+ "<br><i>Mag2  </i>" + earthquake.getMagnitude_MS() + "<br>" + earthquake.getLocation() + "</html>");
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}
}
