package com.isti.xmax.data;

import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.data.DataModule;
import com.isti.xmax.XMAXException;
import com.isti.xmax.common.Earthquake;
import com.isti.xmax.common.Pick;
import com.isti.xmax.common.QCIssue;

/**
 * <p>
 * Customized {@link DataModule}.
 * </p>
 * <p>
 * Realize singleton pattern, i.e we can have only one data module in the program.
 * </p>
 * 
 * @author Max Kokoulin
 */
public class XMAXDataModule extends DataModule {
	private static Logger lg = Logger.getLogger(XMAXDataModule.class);

	/**
	 * List of known earthquakes
	 */
	private List<IEvent> earthquakes = null;

	private static XMAXDataModule instance = null;

	private XMAXDataModule() {
		super();
		setChannelFactory(new XMAXChannelFactory());
	}

	/**
	 * @return set of known quality control issues
	 */
	public SortedSet<QCIssue> getAllQCIssues() {
		return null;
	}

	/**
	 * @return list of known earthquakes, ordered by date
	 */
	public List<IEvent> getEarthquakes() {
		return earthquakes;
	}

	/**
	 * Customized {@link DataModule#loadData()} - also initializes earthquakes and picks
	 */
	public void loadData() throws TraceViewException {
		super.loadData();
		// Adding events
		earthquakes = Earthquake.getEarthquakes(getAllDataTimeInterval());

		// Loading picks from xml database
		try {
			Pick.loadPicks();
		} catch (XMAXException e) {
			lg.error("Can't load picks: " + e);
		}
	}
	
	public static XMAXDataModule getInstance() {
		if (instance == null) {
			instance = new XMAXDataModule();
		}
		return instance;
	}
}
