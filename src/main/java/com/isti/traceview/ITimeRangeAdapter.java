package com.isti.traceview;

import com.isti.traceview.common.TimeInterval;

/**
 * Interface to represent actions after time range setting
 * @author Max Kokoulin
 *
 */

public interface ITimeRangeAdapter {
	void setTimeRange(TimeInterval timeRange);

}
