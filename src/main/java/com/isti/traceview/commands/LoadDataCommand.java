package com.isti.traceview.commands;

import java.util.List;
import org.apache.log4j.Logger;

import com.isti.traceview.AbstractCommand;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;

/**
 * This command loads raw data into data providers from sources discovered on the parse stage
 * 
 * **NOTE: Currently not used, replaced with loop in GraphPanel.setChannelShowSet()
 * 
 * @author Max Kokoulin
 */

public class LoadDataCommand extends AbstractCommand {
	private static final Logger logger = Logger.getLogger(LoadDataCommand.class);

	List<PlotDataProvider> channels;
	TimeInterval ti = null;
	
	/**
	 * 
	 * @param channels list of data providers
	 * @param ti time interval to load
	 */
	public LoadDataCommand(List<PlotDataProvider> channels, TimeInterval ti) {
		this.channels = channels;
		this.ti = ti;
		this.setPriority(5);
	}

	public void run() {
		try {
			super.run();
			if (channels.size() == 1) {
				PlotDataProvider channel = channels.get(0);
				logger.debug("== Load data command: " + channel.toString() + ti);
				channel.load(ti);
				
			} else {
				for (PlotDataProvider channel: channels) {
					logger.debug("== Load data command: " + channel.toString() + ti);
					channel.load(ti);
				}
			}
		} catch (Exception e) {
			logger.error("Exception: ", e);
		}
	}
}
