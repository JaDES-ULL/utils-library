package es.ull.functions;

import static org.junit.jupiter.api.Assertions.*;

import es.ull.simulation.functions.PeriodicProportionFunction;
import es.ull.simulation.functions.TimeFunctionParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeriodicProportionFunctionTest {

  PeriodicProportionFunction periodicProportionFunction;
  TimeFunctionParams params;

  @BeforeEach
  void setUp() {
    // Configuración inicial para cada prueba
    int[] nElem = {1, 2, 3};
    double[] prop = {0.3, 0.5, 0.2};
    double timeUnit = 10.0;
    periodicProportionFunction = new PeriodicProportionFunction(nElem, prop, timeUnit);
    params = () -> 2.0;
  }

  @Test
  void getValue() {
    assertEquals(0.3, periodicProportionFunction.getValue(params));
  }

  @Test
  void setParameters() {
    // Caso de prueba para el método setParameters
    int[] nElem = {2, 3, 4};
    double[] prop = {0.4, 0.3, 0.3};
    double timeUnit = 5.0;
    periodicProportionFunction.setParameters(nElem, prop, timeUnit);
    assertArrayEquals(nElem, periodicProportionFunction.getNElem());
    assertArrayEquals(prop, periodicProportionFunction.getProp());
    assertEquals(timeUnit, periodicProportionFunction.getTimeUnit());
  }

  @Test
  void getNElem() {
    // Caso de prueba para el método getNElem
    int[] expectedNElem = {1, 2, 3};
    assertArrayEquals(expectedNElem, periodicProportionFunction.getNElem());
  }

  @Test
  void setNElem() {
    // Caso de prueba para el método setNElem
    int[] nElem = {2, 3, 4};
    periodicProportionFunction.setNElem(nElem);
    assertArrayEquals(nElem, periodicProportionFunction.getNElem());
  }

  @Test
  void getProp() {
    // Caso de prueba para el método getProp
    double[] expectedProp = {0.3, 0.5, 0.2};
    assertArrayEquals(expectedProp, periodicProportionFunction.getProp());
  }

  @Test
  void setProp() {
    // Caso de prueba para el método setProp
    double[] prop = {0.4, 0.3, 0.3};
    periodicProportionFunction.setProp(prop);
    assertArrayEquals(prop, periodicProportionFunction.getProp());
  }

  @Test
  void getTimeUnit() {
    // Caso de prueba para el método getTimeUnit
    assertEquals(10.0, periodicProportionFunction.getTimeUnit());
  }

  @Test
  void setTimeUnit() {
    // Caso de prueba para el método setTimeUnit
    double timeUnit = 5.0;
    periodicProportionFunction.setTimeUnit(timeUnit);
    assertEquals(timeUnit, periodicProportionFunction.getTimeUnit());
  }
}