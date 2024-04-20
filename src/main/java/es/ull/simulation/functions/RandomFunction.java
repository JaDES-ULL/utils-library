package es.ull.simulation.functions;

import simkit.random.RandomVariate;

/**
 * @author Iv�n Castilla Rodr�guez
 *
 */
public class RandomFunction extends AbstractTimeFunction {
  private RandomVariate rnd;

  /**
   *
   */
  public RandomFunction(RandomVariate rnd) {
    super();
    this.rnd = rnd;
  }

  /**
   *
   */
  public RandomFunction() {
    super();
  }

  /**
   * @return Returns the rnd.
   */
  public RandomVariate getRandom() {
    return rnd;
  }

  /**
   * @param rnd The rnd to set.
   */
  public void setRandom(RandomVariate rnd) {
    this.rnd = rnd;
  }

  @Override
  public double getValue(TimeFunctionParams params) {
    return rnd.generate();
  }

  @Override
  public void setParameters(Object... params) {
    if (params.length < 1)
      throw new IllegalArgumentException("Need (RandomVariate), received " +
          params.length + " parameters");
    else {
      setRandom((RandomVariate)params[0]);
    }
  }

}
