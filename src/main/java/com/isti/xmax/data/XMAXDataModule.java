package com.isti.xmax.data;

import com.isti.traceview.TraceView;
import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.data.DataModule;
import com.isti.traceview.source.SourceFile;
import com.isti.traceview.data.TemporaryStorage;
import com.isti.xmax.XMAXException;
import com.isti.xmax.common.Earthquake;
import com.isti.xmax.common.Pick;
import com.isti.xmax.common.QCIssue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import org.apache.log4j.Logger;

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
	private static final Logger logger = Logger.getLogger(XMAXDataModule.class);

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
		// super.loadData();
		loadNewDataFromSources(getDataFiles());
		// Adding events
		earthquakes = Earthquake.getEarthquakes(getAllDataTimeInterval());

		// Loading picks from xml database
		try {
			Pick.loadPicks();
		} catch (XMAXException e) {
			logger.error("Can't load picks: ", e);
		}

		loadStations();
		setChanged();
		notifyObservers();
	}

	/**
	 * Gets data to load in during startup. If useTempData flag set in the configuration,
	 * first looks in temporary storage, after looks in configured data
	 * directory and parse file data sources which absent in temp storage area
	 */
	File[] getDataFiles() throws TraceViewException {
		logger.debug("== Enter\n");
		List<File> dataFiles = new ArrayList<>();

		// -t: Read serialized PlotDataProviders from TEMP_DATA
		if (TraceView.getConfiguration().getUseTempData()) {
			logger.debug("-t: Read from temp storage\n");
			if (storage == null) {
				storage = new TemporaryStorage(TraceView.getConfiguration().getDataTempPath());
			}
			for (String tempFileName : storage.getAllTempFiles()) {
				logger.debug("PDP.load: tempFileName = " + tempFileName);
				File file = new File(tempFileName);
				dataFiles.add(file);
				System.out.format("\tRead serialized file:%s\n", tempFileName);
			}
			// Move to after the -d read in case there are other channels ... ?

			logger.debug("-t: Read from temp storage DONE\n\n");
		}

		// -d: Read data from data path. At this point we are merely *parsing* the
		//     data (e.g., mseed files) to construct PlotDataProviders, and the actual
		//     traces (=Segments) won't be read in until just before they are displayed on the screen.
		if (TraceView.getConfiguration().getUseDataPath()) {
			logger.debug("-d: Read from data path --> addDataSources()\n");

			// IMPLEMENT ExecutorService to split getDataFiles() and addDataSources() into multi-threads
			dataFiles.addAll(SourceFile.getDataFiles(TraceView.getConfiguration().getDataPath()));
			logger.debug("-d: Read from data path DONE\n\n");
		} else if (!TraceView.getConfiguration().getUseTempData()) {
			logger.debug("-d + -t are both false: Read from data path --> addDataSources()\n");

			// IMPLEMENT ExecutorService to split getDataFiles() and addDataSources() into multi-threads
			dataFiles.addAll(SourceFile.getDataFiles(TraceView.getConfiguration().getDataPath()));
			logger.debug("-d + -t: Read from data path DONE\n\n");
		}

		/*
		// Fill up stations from station file
		loadStations();
		setChanged();
		notifyObservers();
		*/
		//printAllChannels();
		logger.debug("== Exit getDataFiles()\n\n");
		return dataFiles.toArray(new File[]{});
	}

	public static XMAXDataModule getInstance() {
		if (instance == null) {
			instance = new XMAXDataModule();
		}
		return instance;
	}
}
