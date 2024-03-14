package com.ull.functions;

import java.util.ArrayList;

public class ReplicableTimeFunction extends AbstractTimeFunction {
  private AbstractTimeFunction innerTimeFunction;
  final private ArrayList<Double> genValues;
  private int counter;


  public ReplicableTimeFunction(AbstractTimeFunction innerTimeFunction) {
    this();
    this.innerTimeFunction = innerTimeFunction;
  }

  public ReplicableTimeFunction() {
    genValues = new ArrayList<>();
    counter = 0;
  }

  public void reStart() {
    counter = 0;
  }

  /* (non-Javadoc)
   * @see es.ull.iis.function.TimeFunction#getValue(es.ull.iis.function.TimeFunctionParams)
   */
  @Override
  public double getValue(TimeFunctionParams params) {
    if (counter == genValues.size())
      genValues.add(innerTimeFunction.getValue(params));
    return genValues.get(counter++);
  }

  /* (non-Javadoc)
   * @see es.ull.iis.function.TimeFunction#setParameters(java.lang.Object[])
   */
  @Override
  public void setParameters(Object... params) {
    if (params.length != 1) {
      throw new IllegalArgumentException("Should be one parameters for PositiveTimeFunction: " +
          params.length + " passed.");
    }
    if  (!(params[0] instanceof AbstractTimeFunction)) {
      throw new IllegalArgumentException("Parameters must be a TimeFunction");
    }
    else {
      setInnerTimeFunction((AbstractTimeFunction) params[0]);
    }
  }

  /**
   * @return the innerTimeFunction
   */
  public AbstractTimeFunction getInnerTimeFunction() {
    return innerTimeFunction;
  }

  /**
   * @param innerTimeFunction the innerTimeFunction to set
   */
  public void setInnerTimeFunction(AbstractTimeFunction innerTimeFunction) {
    this.innerTimeFunction = innerTimeFunction;
  }
}
