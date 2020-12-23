package com.asl.traceview.transformations.coherence;

import static com.isti.traceview.processing.IstiUtilsMath.generateFreqArray;

import com.isti.traceview.common.TimeInterval;
import com.isti.traceview.data.PlotDataProvider;
import com.isti.traceview.data.Response;
import com.isti.traceview.filters.IFilter;
import com.isti.traceview.processing.FilterFacade;
import com.isti.traceview.processing.IstiUtilsMath;
import com.isti.traceview.transformations.ITransformation;
import com.isti.traceview.transformations.TransformationUtils;
import com.isti.xmax.XMAXException;
import com.isti.xmax.gui.XMAXframe;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.math3.complex.Complex;
import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class TransCoherence implements ITransformation{

  public static final String NAME = "Coherence";

  private int effectiveLength = 0;

  private static final Logger logger = Logger.getLogger(TransCoherence.class);

  @Override
  public void transform(List<PlotDataProvider> input, TimeInterval ti, IFilter filter, Object configuration,
      JFrame parentFrame) {

    if (input.size() != 2) {
      JOptionPane.showMessageDialog(parentFrame, "Please select 2 channels", "Coherence computation warning",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    if (input.get(0).getDataLength(ti) < 32) {
      JOptionPane.showMessageDialog(parentFrame, "One or more of the traces that you selected does not contain enough datapoints (<32). Please select a longer dataset.", "Coherence computation warning",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    // sample rate is interval in ms -- larger sample rate is the lower-frequency data
    // and if they don't match up we should downsample to the lower frequency rate
    double sampleRate = Math.max(input.get(0).getSampleRate(), input.get(1).getSampleRate());

    if (input.get(0).getSampleRate() != input.get(1).getSampleRate()){
      JOptionPane.showMessageDialog(parentFrame, "Channel sample rates do not match. ("+input.get(0).getLocationName()+"/"
              +input.get(0).getChannelName()+"= "+input.get(0).getSampleRate()+", " +input.get(1).getLocationName()+"/"
              +input.get(1).getChannelName()+"= "+input.get(1).getSampleRate()+")\n"+
              "Downsampling will be done on the higher-frequency data.",
          "Coherence computation warning",
          JOptionPane.WARNING_MESSAGE);
    }



    try {
      XYSeriesCollection plotSeries = createData(input, filter, ti, sampleRate, parentFrame);
      TimeInterval effectiveInterval = new TimeInterval(ti.getStart(),
          ti.getStart() + new Double(input.get(0).getSampleRate() * effectiveLength).longValue());
      @SuppressWarnings("unused")
      ViewCoherence vc = new ViewCoherence(parentFrame, plotSeries, effectiveInterval);
    } catch (XMAXException e) {
      if (!e.getMessage().equals("Operation cancelled")) {
        JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
      }
    } catch (RuntimeException e) {
      JOptionPane.showMessageDialog(parentFrame, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    ((XMAXframe) parentFrame).getGraphPanel().forceRepaint();

  }

  /**
   * @param input
   *            List of traces to process
   * @param filter
   *            Filter applied to traces before correlation
   * @param ti
   *            Time interval to define processed range
   * @param parentFrame
   *            parent frame
   * @return list of spectra for selected traces and time ranges
   * @throws XMAXException
   *             if sample rates differ, gaps in the data, or no data for a
   *             channel
   */

  private XYSeriesCollection createData(List<PlotDataProvider> input, IFilter filter, TimeInterval ti, double downsampleInterval, JFrame parentFrame)
      throws XMAXException {
    XYSeriesCollection dataset = new XYSeriesCollection();
    List<Complex[]> xSegmentData = new ArrayList<>();
    List<Complex[]> ySegmentData = new ArrayList<>();
    final int[] numsegs = {0}; // allows us to keep segment counter finalized within loop

    // change for the sake of change? not quite, allows us to easily parallelize this loop
    // by using stream.of(...).parallel().forEach() or forEachOrdered()
    // however, as long as the nested loops are there this is unlikely to parallelize well
    Stream.of(0,1).parallel().forEachOrdered(index -> {

      PlotDataProvider channel = input.get(index);

      int[] intData;
      try {
        intData = channel.getContinuousGaplessDataOverRange(ti);
      } catch (XMAXException e) {
        logger.error("Caught exception while iterating through transformation: ", e);
        throw new RuntimeException(e.getMessage());
      }

      // now that we have data, time to perform downsampling
      double[] dblData = new double[intData.length];
      for (int i = 0; i < dblData.length; ++i) {
        dblData[i] = (double) intData[i];
      }
      dblData =
          TransformationUtils.decimate(dblData, (long) channel.getSampleRate(), (long) downsampleInterval);
      intData = new int[dblData.length];
      for (int i = 0; i < dblData.length; ++i) {
        intData[i] = (int) dblData[i];
      }

      if (intData.length > maxDataLength) {
        int ds = getPower2Length(maxDataLength);
        int[] tempIntData = new int[ds];
        if (maxDataLength >= 0) {
          System.arraycopy(intData, 0, tempIntData, 0, maxDataLength);
        }
        intData = tempIntData;
        ((XMAXframe) parentFrame).getStatusBar().setMessage(
            "Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
      }

      /*
       * Here we compute the power spectral density of the selected data
       * using the Welch method with 13 windows 75% overlap. The actual
       * PSD is calculated in the getPSD function within Spectra.java.
       */
      int dsDataSegment = new Double(Math.round(intData.length / 4.0)).intValue();

      int smallDataSegmentLimit = new Double(
          Math.ceil(Math.pow(2, (Math.ceil(IstiUtilsMath.log2(dsDataSegment)) - 1)))).intValue();
      // this is one power of 2 less than the dsDataSegment length

      int[] data = new int[smallDataSegmentLimit]; // data values in the the time domain
      Complex[] noise_spectra = new Complex[smallDataSegmentLimit]; // current segment fft
      Complex[] finalNoiseSpectraData = new Complex[(smallDataSegmentLimit / 2) + 1]; // cumulative sum

      // initialize the finalNoiseSpectraData array to all zeros since we
      // will be taking a cumulative sum of the data.
      for (int i = 0; i < finalNoiseSpectraData.length; i++) {
        finalNoiseSpectraData[i] = new Complex(0, 0);
      }

      // loop indexes
      int dsDataSegmentLimit = dsDataSegment; // keeps track of where a
      // segment ends in the data
      // array
      int cnt = 0; // keeps track where in the intData array the index is
      int segIndex = 0; // keeps track of where the index is within an
      // individual segment

      int ds;
      if (intData.length > maxDataLength) {
        ds = getPower2Length(maxDataLength);
        int[] tempIntData = new int[ds];
        if (maxDataLength >= 0)
          System.arraycopy(intData, 0, tempIntData, 0, maxDataLength);
        intData = tempIntData;
        ((XMAXframe) parentFrame).getStatusBar().setMessage(
            "Points count (" + intData.length + ") exceeds max value for trace " + channel.getName());
      } else {
        ds = intData.length;
      }
      if (ds > effectiveLength) {
        effectiveLength = ds;
      }

      // Perform windowing and compute the FFT of each segment. The
      // finalNoiseSpectraData array contains the sum of the FFTs for all
      // segments.
      numsegs[0] = 1;
      while (cnt < intData.length) {

        if (cnt < dsDataSegmentLimit) {
          if (segIndex < smallDataSegmentLimit)
            data[segIndex] = intData[cnt];
          cnt++;
          segIndex++;
        } else {
          if (filter != null) {
            data = new FilterFacade(filter, channel).filter(data);
          }

          // Make a copy of data to make it an array of doubles
          double[] dataCopy = new double[data.length];
          for (int i = 0; i < data.length; i++)
            dataCopy[i] = data[i];

          // Calculate FFT of the current segment
          noise_spectra = IstiUtilsMath.processFft(dataCopy);

          if(index == 0){
            xSegmentData.add(noise_spectra);
          } else {
            ySegmentData.add(noise_spectra);
          }

          // move cursors
          segIndex = 0;
          if (cnt + smallDataSegmentLimit > intData.length) // correction for last segment
          {
            cnt = intData.length - smallDataSegmentLimit;
            dsDataSegmentLimit = intData.length;
          } else {
            cnt = cnt - ((smallDataSegmentLimit * 3) / 4); // move window backwards 75%
            dsDataSegmentLimit = dsDataSegmentLimit + (smallDataSegmentLimit / 4);
            // above line increases dsDataSegmentLimit by 25%
            numsegs[0]++;
          }
        }
      }
    });

    //Caculate the averaged Pxx*
    Complex[] pXConj = new Complex[xSegmentData.get(0).length];
    for(int i = 0; i < pXConj.length; i++)
      pXConj[i] = new Complex(0,0);
    for(Complex[] segdata : xSegmentData) {
      for(int i = 0; i < segdata.length; i++) {
        pXConj[i] = pXConj[i].add(segdata[i].multiply(segdata[i].conjugate()));
      }
    }
    for(int i = 0; i < pXConj.length; i++) {
      pXConj[i] = pXConj[i].divide(numsegs[0]);
    }

    //Calculate the average Pyy*
    Complex[] pYConj = new Complex[ySegmentData.get(0).length];
    for(int i = 0; i < pYConj.length; i++)
      pYConj[i] = new Complex(0,0);
    for(Complex[] segdata : ySegmentData) {
      for(int i = 0; i < segdata.length; i++) {
        pYConj[i] = pYConj[i].add(segdata[i].multiply(segdata[i].conjugate()));
      }
    }
    for(int i = 0; i < pYConj.length; i++) {
      pYConj[i] = pYConj[i].divide(numsegs[0]);
    }

    //Calculate the average Pxy*
    Complex[] pXYConj = new Complex[ySegmentData.get(0).length];
    for(int i = 0; i < pXYConj.length; i++)
      pXYConj[i] = new Complex(0,0);
    for(int r = 0; r < numsegs[0]; r++) {
      Complex[] curXSeg = xSegmentData.get(r);
      Complex[] curYSeg = ySegmentData.get(r);
      for(int c = 0; c < pXYConj.length; c++) {
        pXYConj[c] =  pXYConj[c].add(curXSeg[c].multiply(curYSeg[c].conjugate()));
      }
    }
    for(int i = 0; i < pXYConj.length; i++) {
      pXYConj[i] = pXYConj[i].divide(numsegs[0]);
    }

    //Calculate the average Pyx*
    Complex[] pYXConj = new Complex[ySegmentData.get(0).length];
    for(int i = 0; i < pYXConj.length; i++)
      pYXConj[i] = new Complex(0,0);
    for(int r = 0; r < numsegs[0]; r++) {
      Complex[] curXSeg = xSegmentData.get(r);
      Complex[] curYSeg = ySegmentData.get(r);
      for(int c = 0; c < pYXConj.length; c++) {
        pYXConj[c] = pYXConj[c].add(curYSeg[c].multiply(curXSeg[c].conjugate()));
      }
    }
    for(int i = 0; i < pXYConj.length; i++) {
      pYXConj[i] = pYXConj[i].divide(numsegs[0]);
    }

    final double[] finalCoherence;
    double[] coherenceTrace = new double[pXConj.length];
    for(int i = 0; i < pXConj.length; i++){
      Complex pxx = pXConj[i];
      Complex pyy = pYConj[i];
      Complex pxy = pXYConj[i];
      Complex pyx = pYXConj[i];
      //Calcluate |Pxy|^2
      double numerator = pxy.multiply(pyx).getReal();
      //Calculate |Pxx| * |Pyy|
      double denominator = pxx.multiply(pyy).getReal();
      coherenceTrace[i] = numerator / denominator; //normalized coherence value
    }
    finalCoherence = coherenceTrace;

    // Note that channel.getSampleRate() really returns the sampling
    // interval. (e.g. For a sample frequency of 40Hz you have
    // 1000.0/channel.getSampleRate() = 1000.0/25 = 40Hz)
    final Response.FreqParameters fp = Response.getFreqParameters(finalCoherence.length*2,
        1000.0 / downsampleInterval);
    final double[] frequenciesArray = generateFreqArray(fp.startFreq, fp.endFreq, fp.numFreq);
    double[] sqrtCoherence = new double[frequenciesArray.length];

    XYSeries series = new XYSeries("raw series");
    for(int i = 0; i < finalCoherence.length; i++){
      sqrtCoherence[i] = Math.sqrt(finalCoherence[i]);
      series.add(1.0 / frequenciesArray[i], sqrtCoherence[i]);
    }

    XYSeries smoothedSeries = new XYSeries("smoothed series");
    // smooth entire array (start at index 0)
    double[] smoothedData = IstiUtilsMath.getSmoothedPSD(frequenciesArray, sqrtCoherence, 0);
    for (int i = 0; i < finalCoherence.length; ++i) {
      smoothedSeries.add(1.0 / frequenciesArray[i], smoothedData[i]);
    }
    // add smoothed series first, so that it has priority in plot
    dataset.addSeries(smoothedSeries);
    dataset.addSeries(series);
    return dataset;
  }

  private static int getPower2Length(int length) {
    return new Double(Math.pow(2, Math.ceil(IstiUtilsMath.log2(length)))).intValue();
  }

  /**
   * Sets maximum amount of processed data
   */
  public void setMaxDataLength(int dataLength) {

  }

  /**
   * Return name of transformation
   */
  public String getName() {
    return TransCoherence.NAME;
  }

}
