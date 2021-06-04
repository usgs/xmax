package com.isti.traceview.data;

import com.isti.traceview.TraceViewException;
import com.isti.traceview.common.IEvent;
import com.isti.traceview.common.Station;
import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.gui.IColorModeState;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.processing.RemoveGain;
import com.isti.traceview.processing.RemoveGainException;
import com.isti.traceview.processing.Rotation;
import com.isti.traceview.processing.Rotation.RotationGapException;
import com.isti.xmax.XMAXException;
import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;
import org.apache.log4j.Logger;

/**
 * <p>
 * Class for trace representation, produces and holds pixelized trace view and manage pixalization
 * policy.
 * </p>
 * <p>
 * The concept of a pixelized view is a realization of a simple idea that the computer screen can
 * fit up to a couple of thousand horizontal data points (pixels) at most, and we cannot show all
 * trace points on the computer's screen. So we have to pixelize raw data, i.e each pixelized
 * visible screen point contain information about some time range of raw data. In this way we can
 * significantly speed-up plotting capability of the program.
 * </p>
 * <p>
 * Observed by associated ChannelView, i.e can report of changes to observing instances.
 * </p>
 *
 * @author Max Kokoulin
 */

public class PlotDataProvider extends RawDataProvider {

  public static final long serialVersionUID = 1;
  private static final Logger logger = Logger.getLogger(PlotDataProvider.class);

  /**
   * Set of events attached to this data provider
   */
  protected transient SortedSet<IEvent> events;

  /**
   * May be used by ColorModeByTrace to color trace in manual mode.
   */
  private final Color manualColor = Color.BLACK;

  /**
   * Current plots applied rotation
   */
  private Rotation rotation = null;

  public PlotDataProvider(String channelName, Station station, String networkName,
      String locationName) {
    super(channelName, station, networkName, locationName);
    events = Collections.synchronizedSortedSet(new TreeSet<>());
  }

  /**
   * Sets rotation. Null means rotation doesn't affected. Selected traces will be redrawn with
   * rotation with using of "selection" mode.
   *
   * @param rotation rotation to set to set
   */
  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }

  /**
   * Gets the rotation.
   *
   * @return current rotation, null if rotation is not present
   */
  public Rotation getRotation() {
    return this.rotation;
  }

  /**
   * Returns whether the current channel is rotated or not
   *
   * @return true if channel is rotated otherwise false
   */
  public boolean isRotated() {
    return this.rotation != null && this.rotation.getRotationType() != null;
  }

  /**
   * Generate plot data
   *
   * @param ti Requested time interval
   * @param pointCount - requested count of points
   * @param filter - filter to apply
   * @return generated plot data to draw
   * @throws TraceViewException if thrown in {@link RemoveGain#removegain(PlotDataProvider,
   * TimeInterval, int, IFilter, IColorModeState)}
   */
  public PlotData getPlotData(TimeInterval ti, int pointCount, IFilter filter,
      RemoveGain rg, IColorModeState colorMode)
      throws TraceViewException, RemoveGainException {
    if (rg != null && rg.removestate && this.rotation == null) {
      return rg.removegain(this, ti, pointCount, filter, colorMode);
    } else {
      return getPlotData(ti, pointCount, filter);
    }
  }

  /**
   * Generate original plot data
   *
   * @param ti Requested time interval
   * @param pointCount - requested count of points
   * @param filter - filter to apply
   * @return generated plot data to draw from original dataset
   * @throws TraceViewException if thrown in {@link RemoveGain#removegain(PlotDataProvider,
   * TimeInterval, int, IFilter, IColorModeState)}
   */
  public PlotData getOriginalPlotData(TimeInterval ti, int pointCount, IFilter filter,
      RemoveGain rg, IColorModeState colorMode)
      throws TraceViewException, RemoveGainException {
    if (rg != null) {
      return rg.removegain(this, ti, pointCount, filter, colorMode);
    }
    return getPlotData(ti, pointCount, filter);
  }

  /**
   * Generate plot data
   *
   * @param ti Requested time interval
   * @param pointCount requested count of points
   * @param filter filter to apply
   * @return generated plot data to draw
   */
  private PlotData getPlotData(TimeInterval ti, int pointCount,
      IFilter filter) {
    logger.debug(this + "; " + ti + "(" + ti.getStart() + "-" + ti.getEnd() + ")" + "; pointCount "
        + pointCount);

    List<PlotDataPoint[]> points = null;

    // Time range need to be pixelized - intersection of requested pixalization range and
    // channel's time range
    PlotData ret = new PlotData(this.getName(), this.getColor());
    TimeInterval initialTimeRange = getTimeRange();
    TimeInterval effectiveTimeRange = TimeInterval.getIntersect(ti, initialTimeRange);

    if (effectiveTimeRange != null) {
      try {
        points = pixelize(effectiveTimeRange,
            (int) (2 * pointCount * effectiveTimeRange.getDuration() / (double) ti.getDuration()),
            filter);
      } catch (PlotDataException e) {
        logger.error("PlotDataException:", e);
      }

      // Second level of pixelization related to screen size (i.e. width)
      double timeRatio = (ti.getDuration()) / (double) pointCount;
      for (int i = 0; i < pointCount; i++) {
        // we divide requested time range into pointCount time slices and calculate data to
        // display for every slice
        double startSlice = ti.getStart() + i * timeRatio; // start slice time
        double endSlice = ti.getStart() + ((i + 1.0) * timeRatio); // end slice time
        if (!((startSlice >= effectiveTimeRange.getEnd() && endSlice >= effectiveTimeRange.getEnd())
            || (startSlice <= effectiveTimeRange.getStart() && endSlice <= effectiveTimeRange
            .getStart()))) {
          // if effective time range intersects this time slice
          int startIndex = (int) (Math.round(
              (startSlice - effectiveTimeRange.getStart()) * points.size()
                  / (double) effectiveTimeRange.getDuration()));
          if (startIndex < 0) {
            startIndex = 0;
          }
          int endIndex = (int) (Math.round(
              (endSlice - effectiveTimeRange.getStart()) * points.size()
                  / (double) effectiveTimeRange.getDuration()));
          if (endIndex > points.size()) {
            endIndex = points.size();
          }
          if ((startIndex == endIndex) && (endIndex != points.size())) {
            // to avoid gaps on very large zoom
            endIndex = endIndex + 1;
          }
          List<PlotDataPoint[]> data = points.subList(startIndex, endIndex);
          List<SliceData> sliceDataList = new ArrayList<>();
          int k = 0;
          for (PlotDataPoint[] sublist : data) {
            while (sliceDataList.size() <= k) {
              sliceDataList.add(new SliceData());
            }
            for (PlotDataPoint value : sublist) { // may have 1 or 2 points
              sliceDataList.get(k).top = Math.max(sliceDataList.get(k).top, value.getTop());
              sliceDataList.get(k).bottom = Math
                  .min(sliceDataList.get(k).bottom, value.getBottom());
              if (value.getTop() != Double.NEGATIVE_INFINITY) {
                sliceDataList.get(k).sum += value.getMean();
                sliceDataList.get(k).segmentNumber = value.getSegmentNumber();
                sliceDataList.get(k).continueAreaNumber = value.getContinueAreaNumber();
                sliceDataList.get(k).rdpNumber = value.getRawDataProviderNumber();
                sliceDataList.get(k).dataPointCount++;
              }
            }
            k++;
          }
          if (events == null) {
            events = Collections.synchronizedSortedSet(new TreeSet<>()); // class was deserialized
          }
          SortedSet<EventWrapper> evts = new TreeSet<>();
          for (IEvent event : events) {
            long eventTime = event.getStartTime().getTime();
            if (eventTime > startSlice && eventTime <= endSlice) {
              evts.add(new EventWrapper(event, true));
            } else if (!(eventTime >= endSlice && eventTime + event.getDuration() >= endSlice
                || (eventTime <= startSlice && eventTime + event.getDuration() <= startSlice))) {
              evts.add(new EventWrapper(event, false));
            }
          }

          PlotDataPoint[] pdpArray = new PlotDataPoint[sliceDataList.size()];
          int m = 0;
          for (SliceData sliceData : sliceDataList) {  // if gaps exist m > 1
            pdpArray[m] = sliceData.getPoint(evts);
            //lg.debug("Added point " + m + ": " + pdpArray[m].toString());
            m++;
          }
          ret.addPixel(pdpArray);
          if (evts.size() > 0) {
            logger.debug("Event time: "
                + TimeInterval.formatDate(evts.first().getEvent().getStartTime(),
                TimeInterval.DateFormatType.DATE_FORMAT_NORMAL)
                + "(" + evts.first().getEvent().getStartTime().getTime() + ")"
                + "; point number " + ret.getPointCount());
          }
        } else {
          PlotDataPoint[] pdpArray = new PlotDataPoint[]{
              new PlotDataPoint(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN,
                  -1, -1, -1, null)};
          ret.addPixel(pdpArray);
        }
      }
    }
    logger.debug("== END: " + this);
    return ret;
  }


  /**
   * Pixelize raw data
   *
   * @param ti Time interval to pixelize
   * @param pointCount Requested count of points
   * @param filter filter to apply to raw data before pixelization
   * @return List of PlotDataPoint
   */
  private List<PlotDataPoint[]> pixelize(TimeInterval ti, int pointCount, IFilter filter)
      throws PlotDataException {
    //logger.debug("pixelizing " + this +"; "+ ti + "; "+ "pointCount " + pointCount);
    List<PlotDataPoint[]> pointSet = new ArrayList<>(pointCount);

    List<Segment> unfinalSegments; // finalize this only after we determine if rotation is possible
    try {
      unfinalSegments = (rotation == null) ? getRawData(ti) : rotation.rotate(this, ti);
    } catch (TraceViewException e) {
      throw new PlotDataException("Error when trying to rotate channel " + getName(), e);
    } catch (RotationGapException e) {
      logger.warn("Cannot rotate " + getName() + " due to presence of gap");
      rotation = null;
      // reset rotation here; any complement channels will also have rotation nulled
      unfinalSegments = getRawData(ti);
    }
    List<Segment> segments = unfinalSegments;
    int numSegments = segments.size();
    SegmentData[] plottingData;
    {
      final SegmentData[] rawDataFinal = new SegmentData[numSegments];
      // Combine segments if no gap and colormode is not by source, to correct filtering
      IntStream.range(0, numSegments).parallel().forEach(i -> {
        //ALL requested for pixelization time range in this segment
        Segment segment = segments.get(i);
        TimeInterval currentSegmentDataTI = TimeInterval.getIntersect(ti,
            new TimeInterval(segment.getStartTime(), segment.getEndTime()));
        SegmentData segmentData = segment.getData(currentSegmentDataTI);
        rawDataFinal[i] = segmentData;
      });
      plottingData = rawDataFinal;
    }

    // filtering; cannot be parallelized, and the
    if (filter != null) {
      synchronized (filter) {
        FilterFacade ff = new FilterFacade(filter, this);
        SegmentData[] filteredRawData = new SegmentData[plottingData.length];
        for (int i = 0; i < plottingData.length; i++) {
          SegmentData segmentData = plottingData[i];
          filteredRawData[i] = new SegmentData(segmentData.startTime, segmentData.sampleRate,
              segmentData.sourceSerialNumber, segmentData.channelSerialNumber,
              segmentData.continueAreaNumber, segmentData.previous, segmentData.next,
              ff.filter(segmentData.data));
        }
        plottingData = filteredRawData;
      }
    }

    double interval = (ti.getDuration()) / (double) pointCount;
    double time = ti.getStart();

    for (int i = 0; i < pointCount; i++) {
      // Get segmentData objects in the interval (time, time+interval)
      SegmentData[] intervalData = getSegmentData(plottingData, time, time + interval);
      if (intervalData != null) {
        int k = 0;
        int intervalDataLength = intervalData.length;  // number of continuous segmentData objects
        PlotDataPoint[] intervalPoints = new PlotDataPoint[intervalDataLength];
        for (SegmentData segData : intervalData) {
          TimeInterval currentSegmentDataTI = new TimeInterval(segData.startTime,
              segData.endTime());
          double top = Double.NEGATIVE_INFINITY;
          double bottom = Double.POSITIVE_INFINITY;
          double sum = 0.0;
          int[] data;
          if (i == (pointCount - 1)) {
            data = segData.getData(time, ti.getEnd()).data;  // last chunk
          } else if (segData.getData(time, time + interval) != null) {
            data = segData.getData(time, time + interval).data;  // interval sized chunks
          } else {
            data = new int[]{};
          }
          int rawDataPointCount = data.length;
          if (rawDataPointCount > 0) {
            for (int value : data) {
              top = Math.max(top, value);
              bottom = Math.min(bottom, value);
              sum = sum + value;
            }
            intervalPoints[k] = new PlotDataPoint(top, bottom, sum / rawDataPointCount,
                segData.channelSerialNumber, segData.sourceSerialNumber, segData.continueAreaNumber,
                null);
          } else {
            if (currentSegmentDataTI.isContain((long) time)) {
              double value = segData.interpolateValue(time);
              intervalPoints[k] = new PlotDataPoint(value, value, value,
                  segData.channelSerialNumber,
                  segData.sourceSerialNumber, segData.continueAreaNumber, null);
            } else {
              intervalPoints[k] = new PlotDataPoint(Double.NEGATIVE_INFINITY,
                  Double.POSITIVE_INFINITY,
                  Double.NaN, segData.channelSerialNumber, segData.sourceSerialNumber,
                  segData.continueAreaNumber, null);
            }
          }
          k++;
        }
        pointSet.add(intervalPoints);
      } else {
        //lg.debug("Pixelizing : segment null");
        PlotDataPoint[] intervalPoints = new PlotDataPoint[1];
        intervalPoints[0] = new PlotDataPoint(
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN,
            -1, -1, -1, null);
        pointSet.add(intervalPoints);
      }
      time = time + interval;
    }
    logger.debug("pixelizing end " + this);
    return pointSet;
  }

  /**
   * @param sps Segments list
   * @param start start time
   * @param end end time
   * @return subset of segment list which lies in the requested time interval. For one channel, the
   * normal situation is none or one segment, but it can be bigger count in the case of segment
   * overlapping or gaps. If no segments found, return null.
   */
  private static SegmentData[] getSegmentData(SegmentData[] sps, double start, double end) {
    List<SegmentData> ret = new ArrayList<>();
    for (SegmentData segData : sps) {
      long retStart = segData.startTime;
      long retEnd = segData.endTime();
      retEnd = Math.max(retEnd, retStart + (long) segData.sampleRate);
      if (!((start >= retEnd && end >= retEnd) || (start <= retStart && end <= retStart))) {
        ret.add(segData);
      }
    }
    if (ret.size() == 0) {
      return null;
    } else {
      return ret.toArray(new SegmentData[]{});
    }
  }


  /**
   * @param sps Segments list
   * @param start start time
   * @param end end time
   * @return subset of segment list which lies in the requested time interval. For one channel, the
   * normal situation is none or one segment, but it can be bigger count in the case of segment
   * overlapping. If no segments found, return null.
   */
  @SuppressWarnings("unused")
  private static Segment[] getSegment(List<Segment> sps, double start, double end) {
    List<Segment> ret = new ArrayList<>();
    for (Segment seg : sps) {
      long retStart = seg.getStartTime().getTime();
      long retEnd = seg.getEndTime().getTime();
      if (!((start >= retEnd && end >= retEnd) || (start <= retStart && end <= retStart))) {
        ret.add(seg);
      }
    }
    if (ret.size() == 0) {
      return null;
    } else {
      return ret.toArray(new Segment[1]);
    }
  }

  /**
   * set of events which lies near given time
   *
   * @param time the Date to find events near (not Time, bug?)
   * @param precision time range to find, in milliseconds
   * @return set of events
   */
  public SortedSet<IEvent> getEvents(Date time, long precision) {
    SortedSet<IEvent> ret = Collections.synchronizedSortedSet(new TreeSet<>());
    for (IEvent event : events) {
      if (event.getStartTime().getTime() > time.getTime()
          && event.getStartTime().getTime() < time.getTime() + 2 * precision) {
        ret.add(event);
      }
    }
    return ret;
  }

  /**
   * Adds event to plot data provider
   *
   * @param event Event to add
   * @return <tt>true</tt> if events set did not already contain the given one
   */
  public boolean addEvent(IEvent event) {
    if (events == null) {
      events = Collections.synchronizedSortedSet(new TreeSet<>());
    }
    return events.add(event);
  }

  /**
   * Remove event
   *
   * @param event to remove
   * @return <tt>true</tt> if events set contained the specified event
   */
  public boolean removeEvent(IEvent event) {
    if (events == null) {
      events = Collections.synchronizedSortedSet(new TreeSet<>());
    }
    return events.remove(event);
  }

  /**
   * Adds event set to plot data provider
   *
   * @param evt the events to add
   */
  public void addEvents(Set<IEvent> evt) {
    logger.debug("Adding " + evt.size() + " events to plot data provider" + this);
    if (events == null) {
      events = Collections.synchronizedSortedSet(new TreeSet<>());
    }
    events.addAll(evt);
  }


  /**
   * Returns a string representation of the PlotDataProvider for debug purposes.
   *
   * @return a string representation of the PlotDataProvider.
   */
  public String toString() {
    return "PlotDataProvider: " + getName();
  }

  /**
   * Get plot data provider name, currently we use name of underlies raw data provider
   *
   * @return a string representation of the PlotDataProvider.
   */
  public String getName() {
    return super.getName();
  }

  public int[] getContinuousGaplessDataOverRange(TimeInterval ti)
      throws XMAXException {
    List<Segment> segments = getDataWithRotation(getRotation(), ti);
    int[] intData = new int[0];
    if (segments.size() > 0) {
      long segment_end_time = 0;
      double firstSampleRate = segments.get(0).getSampleIntervalMillis();
      for (Segment segment : segments) {
        if (segment.getSampleIntervalMillis() != firstSampleRate) {
          throw new XMAXException(
              "You have data with different sample rate for channel " + getName());
        }
        if (segment_end_time != 0 &&
            Segment.isDataBreak(segment_end_time, segment.getStartTime().getTime(),
                firstSampleRate)) {
          throw new XMAXException("You have gap in the data for channel " + getName());
        }
        segment_end_time = segment.getEndTime().getTime();
        intData = IstiUtilsMath.padArray(intData, segment.getData(ti).data);
      }

    } else {
      throw new XMAXException("You have no data for channel " + getName());
    }

    return intData;
  }

  /**
   * Dumps trace to file in temporary storage in internal format
   */
  public void dump(String serialFileName) {
    ObjectOutputStream out = null;
    try {
      logger.debug("== ENTER: serfialFileName=" + serialFileName);
      out = new ObjectOutputStream(new FileOutputStream(serialFileName + ".SER"));
      setDataStream(serialFileName + ".DATA");
      synchronized (this) {
        logger.info("Serializing " + this + " to file " + serialFileName);
        out.writeObject(this);
        notifyAll();
      }
    } catch (Exception ex) {
      logger.error("Can't save channel: ", ex);
    } finally {
      try {
        setDataStream(null);
        out.close();
      } catch (IOException e) {
        // Do nothing
        logger.error("IOException:", e);
      }
    }
    logger.debug("== EXIT");
  }

  /**
   * get color to color traces in manual mode
   */
  public Color getColor() {
    return manualColor;
  }

  /**
   * Temporary class to accumulate slice statistics
   */
  private static class SliceData {

    double top = Double.NEGATIVE_INFINITY; // max value for slice
    double bottom = Double.POSITIVE_INFINITY; // min value for slice
    double sum = 0.0;
    int dataPointCount = 0;
    int segmentNumber = -1;
    int continueAreaNumber = -1;
    int rdpNumber = -1;

    PlotDataPoint getPoint(SortedSet<EventWrapper> evts) {
      double mean =
          dataPointCount == 0.0 ? Double.POSITIVE_INFINITY : sum / (double) dataPointCount;
      return new PlotDataPoint(top, bottom, mean, segmentNumber, rdpNumber, continueAreaNumber,
          evts);
    }
  }
}
