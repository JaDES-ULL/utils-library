package ull.tfg.functions;

/**
 * Defines a time function which consists of an array of other time functions. The time unit
 * is used to choose the function corresponding to the current timestamp. Therefore,
 * getValue will return a value of part[i], where i = (ts / timeUnit) % functionArray.length
 * @author Iv�n Castilla Rodr�guez
 */
public class UniformlyDistributedSplitFunction extends AbstractTimeFunction {
  private AbstractTimeFunction [] part;
  private double timeUnit;

  public UniformlyDistributedSplitFunction() {

  }


  /**
   * @param part
   * @param timeUnit
   */
  public UniformlyDistributedSplitFunction(AbstractTimeFunction[] part, double timeUnit) {
    super();
    this.part = part;
    this.timeUnit = timeUnit;
  }


  /* (non-Javadoc)
   * @see es.ull.iis.function.TimeFunction#getValue(double)
   */
  @Override
  public double getValue(TimeFunctionParams params) {
    int unit = (int) (params.getTime() / timeUnit);
    int index = unit % part.length;
    return part[index].getValue(params);
  }

  /* (non-Javadoc)
   * @see es.ull.iis.function.TimeFunction#setParameters(java.lang.Object[])
   */
  @Override
  public void setParameters(Object... params) {
    if (params.length != 2)
      throw new IllegalArgumentException("Need 2, received " +
          params.length + " parameters");
    if (!(params[0] instanceof AbstractTimeFunction[]) || !(params[1] instanceof Number))
      throw new IllegalArgumentException("Parameters must be TimeFunction[] and double");
    setPart((AbstractTimeFunction[])params[0]);
    setTimeUnit((Double)params[1]);
  }


  /**
   * @return the part
   */
  public AbstractTimeFunction[] getPart() {
    return part;
  }


  /**
   * @param part the part to set
   */
  public void setPart(AbstractTimeFunction[] part) {
    this.part = part;
  }


  /**
   * @return the timeUnit
   */
  public double getTimeUnit() {
    return timeUnit;
  }


  /**
   * @param timeUnit the timeUnit to set
   */
  public void setTimeUnit(double timeUnit) {
    this.timeUnit = timeUnit;
  }

}
