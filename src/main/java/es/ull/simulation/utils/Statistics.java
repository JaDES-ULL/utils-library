package es.ull.simulation.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * A simple package to get some basic statistics.
 * @author Iván Castilla Rodríguez
 *
 */
public class Statistics {
  /** The factor to calculate 95% CI from SD */
  final static private double CI95FACTOR = 1.96;

  /**
   * Returns the average of a set of values
   * @param values Set of values.
   * @return The average of a set of values
   */
  public static double average(double[] values) {
    if (values.length == 0)
      return Double.NaN;
    double acc = 0.0;
    for (double val : values)
      acc += val;
    return acc / (double)values.length;
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @param av The precalculated average
   * @return The standard deviation of a set of values
   */
  public static double stdDev(double []values, double av) {
    if (values.length == 0)
      return Double.NaN;
    else if (values.length == 1)
      return 0.0;
    double acc = 0.0;
    for (double val : values)
      acc += (val - av) * (val - av);
    return Math.sqrt(acc / (double)(values.length - 1));
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @return The standard deviation of a set of values
   */
  public static double stdDev(double []values) {
    return stdDev(values, average(values));
  }

  /**
   * Returns the average of a set of values
   * @param values Set of values.
   * @return The average of a set of values
   */
  public static double average(int[] values) {
    if (values.length == 0)
      return Double.NaN;
    double acc = 0.0;
    for (int val : values)
      acc += val;
    return acc / (double)values.length;
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @param av The precalculated average
   * @return The standard deviation of a set of values
   */
  public static double stdDev(int []values, double av) {
    if (values.length == 0)
      return Double.NaN;
    else if (values.length == 1)
      return 0.0;
    double acc = 0.0;
    for (int val : values)
      acc += (val - av) * (val - av);
    return Math.sqrt(acc / (double)(values.length - 1));
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @return The standard deviation of a set of values
   */
  public static double stdDev(int []values) {
    return stdDev(values, average(values));
  }

  /**
   * Returns the average of a set of values
   * @param values Set of values.
   * @return The average of a set of values
   */
  public static double average(Double[] values) {
    if (values.length == 0)
      return Double.NaN;
    double acc = 0.0;
    for (double val : values)
      acc += val;
    return acc / (double)values.length;
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @param av The precalculated average
   * @return The standard deviation of a set of values
   */
  public static double stdDev(Double []values, double av) {
    if (values.length == 0)
      return Double.NaN;
    else if (values.length == 1)
      return 0.0;
    double acc = 0.0;
    for (double val : values)
      acc += (val - av) * (val - av);
    return Math.sqrt(acc / (double)(values.length - 1));
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @return The standard deviation of a set of values
   */
  public static double stdDev(Double []values) {
    return stdDev(values, average(values));
  }

  /**
   * Returns the average of a set of values
   * @param values Set of values.
   * @return The average of a set of values
   */
  public static double average(ArrayList<? extends Number> values) {
    if (values.size() == 0)
      return Double.NaN;
    double acc = 0.0;
    for (Number val : values)
      acc += val.doubleValue();
    return acc / (double)values.size();
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @param av The precalculated average
   * @return The standard deviation of a set of values
   */
  public static double stdDev(ArrayList<? extends Number> values, double av) {
    if (values.size() == 0)
      return Double.NaN;
    else if (values.size() == 1)
      return 0.0;
    double acc = 0.0;
    for (Number val : values)
      acc += (val.doubleValue() - av) * (val.doubleValue() - av);
    return Math.sqrt(acc / (double)(values.size() - 1));
  }

  /**
   * Returns the standard deviation of a set of values
   * @param values Set of values.
   * @return The standard deviation of a set of values
   */
  public static double stdDev(ArrayList<? extends Number> values) {
    return stdDev(values, average(values));
  }

  /**
   * Returns the relative error computed as abs(th -exp) / th.
   * @param th Theoretical value
   * @param exp Experimental value
   * @return The relative error.
   */
  public static double relError(double th, double exp) {
    return (Math.abs(th - exp) / th);
  }

  /**
   * Returns the relative error computed as abs(th -exp) / th * 100.
   * @param th Theoretical value
   * @param exp Experimental value
   * @return The relative error in percentage.
   */
  public static double relError100(double th, double exp) {
    return relError(th, exp) * 100;
  }

  public static double[] normal95CI(double mean, double sd, int n) {
    final double ci = CI95FACTOR * sd / Math.sqrt(n);
    return new double[] {mean - ci, mean + ci};
  }

  public static double percentile(double[] sortedValues, double percent) {
    if (percent <= 0.0 || percent > 1.0)
      return Double.NaN;
    if (sortedValues.length == 1)
      return sortedValues[0];
    int pos = (int)(sortedValues.length * percent);
    double dif = sortedValues.length * percent - pos;
    if (pos < 1)
      return sortedValues[0];
    if (pos >= sortedValues.length - 1)
      return sortedValues[sortedValues.length - 1];
    return sortedValues[pos] + dif * (sortedValues[pos + 1] - sortedValues[pos]);
  }

	/**
	 * Returns the 2.5% and 97.5% percentiles of the array. The array does not need to be previously ordered
	 * @param values An array of values
	 * @return the 2.5% and 97.5% percentiles of the array
	 */
	public static double[] getPercentile95CI(double[] values) {
		int n = values.length;
		final double[] ordered = Arrays.copyOf(values, n);
		Arrays.sort(ordered);
		final int index = (int)Math.ceil(n * 0.025);
		return new double[] {ordered[index - 1], ordered[n - index]}; 
	}

	/**
	 * Returns the 2.5% and 97.5% percentiles of the array. The array does not need to be previously ordered
	 * @param values An array of values
	 * @return the 2.5% and 97.5% percentiles of the array
	 */
	public static int[] getPercentile95CI(int[] values) {
		int n = values.length;
		final int[] ordered = Arrays.copyOf(values, n);
		Arrays.sort(ordered);
		final int index = (int)Math.ceil(n * 0.025);
		return new int[] {ordered[index - 1], ordered[n - index]}; 
	}

  /**
   * Generates a time to event based on annual risk. The time to event is absolute,
   * i.e., can be used directly to schedule a new event.
   * @param p Annual risk of the event
   * @param logRnd The natural log of a random number (0, 1)
   * @param rr Relative risk for the patient
   * @return a time to event based on annual risk
   */
  public static double getAnnualBasedTimeToEvent(double p, double logRnd, double rr) {
    // In case the probability of transition was 0
    if (p == 0.0)
      return Double.MAX_VALUE;
    final double newMinus = -1 / (1-Math.pow(1 - p, rr));
    return newMinus * logRnd;
  }

  /**
   * Generates a time to event based on annual rate. The time to event is absolute,
   * i.e., can be used directly to schedule a new event.
   * @param rate Annual rate of the event
   * @param logRnd The natural log of a random number (0, 1)
   * @param irr Incidence rate ratio for the patient
   * @return a time to event based on annual rate
   */
  public static double getAnnualBasedTimeToEventFromRate(double rate, double logRnd, double irr) {
    // In case the rate was 0
    if (rate == 0.0)
      return Double.MAX_VALUE;
    final double newMinus = -1 / (rate * irr);
    return newMinus * logRnd;
  }

  /**
   * Computes the standard deviation from 95% confidence intervals. Assumes that
   * the confidence intervals are based in a normal distribution.
   * @param ci Original 95% confidence intervals
   * @return the standard deviation corresponding to the specified confidence intervals
   */
  public static double sdFrom95CI(double[] ci) {
    return (ci[1] - ci[0])/(1.96*2);
  }

  /**
   * Computes the alfa and beta parameters for a beta distribution from an average and
   * a standard deviation.
   * @param avg Original average of data
   * @param sd Original standard deviation of data
   * @return the alfa and beta parameters for a beta distribution
   */
  public static double[] betaParametersFromNormal(double avg, double sd) {
    final double alfa = (((1 - avg) / (sd*sd)) - (1 / avg)) *avg*avg;
    return new double[] {alfa, alfa * (1 / avg - 1)};
  }

  /**
   * Computes the alfa and beta parameters for a gamma distribution from an average and
   * a standard deviation.
   * @param avg Original average of data
   * @param sd Original standard deviation of data
   * @return the alfa and beta parameters for a beta distribution
   */
  public static double[] gammaParametersFromNormal(double avg, double sd) {
    return new double[] {(avg / sd) * (avg / sd), sd * sd / avg};
  }

  /**
   * Computes the alfa and beta parameters for a beta distribution from an average,
   * a mode, and a maximum and minimum values.
   * Important note: let's the output be [ALFA, BETA]. To use with RandomVariate:
   * final RandomVariate rnd = RandomVariateFactory.getInstance("BetaVariate", ALFA, BETA);
   * return RandomVariateFactory.getInstance("ScaledVariate", rnd, max - min, min);
   * @param avg Original average of data
   * @param mode Most probable value within the interval (must be different from average)
   * @param min Minimum value of the generated values
   * @param max Maximum value of the generated values
   * @return the alfa and beta parameters for a beta distribution
   */
  public static double[] betaParametersFromEmpiricData(
      double avg, double mode, double min, double max) {
    final double alfa = ((avg-min)*(2*mode-min-max))/((mode-avg)*(max-min));
    return new double[] {alfa, ((max-avg)*alfa)/(avg-min)};
  }

  /**
   * Approximates the mode of a beta distribution from mean and standard deviation
   * @param mean Observed mean
   * @param sd Observed standard deviation
   * @return the mode of a beta distribution from mean and standard deviation
   */
  public static double betaModeFromMeanSD(double mean, double sd) {
    final double[] initBetaParams = betaParametersFromNormal(mean, sd);
    final double k = ((initBetaParams[0] + initBetaParams[1])*(initBetaParams[0] +
        initBetaParams[1]))/initBetaParams[1];
    final double variance = sd * sd;
    return variance * k * (initBetaParams[0] - 1) / (initBetaParams[0] - 3 * variance * k);
  }

	
	/**
	 * Computes a weighted average by multiplying each member of the <code>values</code> array by the corresponding member of the <code>weights</code> array, 
	 * and then dividing by the sum of weights. Both arrays must be the same length.
	 * @param weights Array of weights for each value
	 * @param values Array of values 
	 * @return The weighted average of the values applying the corresponding weights
	 */
	public static double weightedAverage(double[] weights, double[] values) {
		if (values.length == 0)
			return Double.NaN;
		if (values.length != weights.length)
			return Double.NaN;
		double acc = 0.0;
		double accWeights = 0.0;
		for (int i = 0; i < values.length; i++) {
			if (weights[i] > 0) {
				acc += values[i] * weights[i];
				accWeights += weights[i];
			}
		}
		return acc / accWeights;		
	}
	
	/**
	 * Returns the weighted percentile for the specified values. Adapted from https://stackoverflow.com/questions/21844024/weighted-percentile-using-numpy/29677616#29677616  
	 * @param weights Array of weights for each value
	 * @param values Array of values 
	 * @param percent Percentile to be found
	 * @param sorted Specifies if the array was previously ordered
	 * @return the weighted percentile for the specified values
	 */
	public static double weightedPercentile(double[] weights, double []values, double percent, boolean sorted) {
		if (percent <= 0.0 || percent > 1.0)
			return Double.NaN;
		if (values.length == 0)
			return Double.NaN;
		if (values.length != weights.length)
			return Double.NaN;
		if (values.length == 1)
			return values[0];
		final ArrayList<WeightedValue> sortedValues = new ArrayList<>();
		double totalWeight = 0.0;
		for (int i = 0; i < values.length; i++) {
			if (weights[i] > 0) {
				sortedValues.add(new WeightedValue(weights[i], values[i]));
				totalWeight += weights[i];
			}
		}
		if (sortedValues.size() == 0)
			return Double.NaN;
		if (!sorted)
			Collections.sort(sortedValues);
		double cummWeight = 0.0;
		double previousAux;
		double aux = 0.0;
		for (int i = 0; i < sortedValues.size(); i++) {
			cummWeight += sortedValues.get(i).getWeight();
			previousAux = aux;
			aux = (cummWeight - 0.5 * sortedValues.get(i).getWeight()) / totalWeight;
			if (aux > percent) {
				return (i == 0) ? 
					sortedValues.get(0).getValue() : 
					sortedValues.get(i).getValue() - ((sortedValues.get(i).getValue() - sortedValues.get(i - 1).getValue()) * (aux - percent)) / (aux - previousAux);  
			}
		}
		return sortedValues.get(sortedValues.size() - 1).getValue();
	}
	
	/**
	 * Auxiliary class to order together weights and values 
	 * @author Iván Castilla Rodríguez
	 *
	 */
	private static class WeightedValue implements Comparable<WeightedValue> {
		private final double weight;
		private final double value;
		
		public WeightedValue(double weight, double value) {
			this.weight = weight;
			this.value = value;
		}

		public double getWeight() {
			return weight;
		}

		public double getValue() {
			return value;
		}

		@Override
		public int compareTo(WeightedValue o) {
			return (int)Math.signum(value - o.value);
		}
	}
}

