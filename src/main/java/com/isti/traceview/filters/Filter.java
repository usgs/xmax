package com.isti.traceview.filters;

import static asl.utils.FilterUtils.bandFilter;
import static asl.utils.FilterUtils.highPassFilter;
import static asl.utils.FilterUtils.lowPassFilter;

import java.util.function.Function;

/**
 * @author James Holland - USGS
 */
public enum Filter {
  HIGHPASS{
    @Override
    public Function<double[], double[]> getFilter(double sampleRate) {
      return this.getFilter(sampleRate, false, 4, 1, null);
    }

    @Override
    public Function<double[], double[]> getFilter(double sampleRate, boolean zeroPhase, int order,
        double firstCorner, Double secondCorner) {
      //Drop the secondCorner, not needed here
      return (data) -> highPassFilter(data, sampleRate, firstCorner, order, zeroPhase);
    }
  }, LOWPASS {
    @Override
    public Function<double[], double[]> getFilter(double sampleRate) {
      return this.getFilter(sampleRate, false, 4, 0.05, null);
    }

    @Override
    public Function<double[], double[]> getFilter(double sampleRate, boolean zeroPhase, int order,
        double firstCorner, Double secondCorner) {
      //Drop the secondCorner, not needed here
      return (data) -> lowPassFilter(data, sampleRate, firstCorner, order, zeroPhase);
    }
  }, BANDPASS {
    @Override
    public Function<double[], double[]> getFilter(double sampleRate) {
      return this.getFilter(sampleRate, false, 4, 0.1, 0.5 );
    }

    @Override
    public Function<double[], double[]> getFilter(double sampleRate, boolean zeroPhase, int order,
        double firstCorner, Double secondCorner) {
      return (data) -> bandFilter(data, sampleRate, firstCorner, secondCorner, order, zeroPhase);
    }
  };

  /**
   * Generate default filter function for a class
   * @param sampleRate sampleRate in Hz for the data being processed
   * @return Function that matches the format double[] function(double[])
   */
  public abstract Function<double[], double[]> getFilter(double sampleRate);

  /**
   *
   * @param sampleRate sampleRate in Hz for the data being processed
   * @param zeroPhase Perform both a forward and reverse filter
   * @param order filter order
   * @param firstCorner first corner or Low corner
   * @param secondCorner Optional second corner or High Corner
   * @return Function that matches the format double[] function(double[])
   */
  public abstract Function<double[], double[]> getFilter(double sampleRate, boolean zeroPhase, int order, double firstCorner, Double secondCorner);

}
