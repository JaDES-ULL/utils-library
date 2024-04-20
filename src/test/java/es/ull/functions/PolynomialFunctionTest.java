package es.ull.functions;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import es.ull.simulation.functions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simkit.random.RandomVariateFactory;

class PolynomialFunctionTest {

  PolynomialFunction polynomialFunction;
  TimeFunctionParams params;

  @BeforeEach
  void setUp() {
    // Configuración inicial para cada prueba
    double[] coefficients = {1.0, 2.0, 3.0};
    polynomialFunction = new PolynomialFunction(coefficients);
    params = () -> 2.0;
  }

  @Test
  void getValue() {
    assertEquals(11.0, polynomialFunction.getValue(params));
  }

  @Test
  void getCoefficients() {
    AbstractTimeFunction[] expectedCoefficients = polynomialFunction.getCoefficients();
    assertNotNull(expectedCoefficients);
    assertEquals(3, expectedCoefficients.length);
  }

  @Test
  void setCoefficients() {
    double[] newCoefficients = {4.0, 5.0, 6.0}; // Nuevos coeficientes
    AbstractTimeFunction[] abstractTimeFunctions = new AbstractTimeFunction[newCoefficients.length];
    for (int i = 0; i < newCoefficients.length; i++) {
      abstractTimeFunctions[i] = new RandomFunction(
          RandomVariateFactory.getInstance("ConstantVariate", newCoefficients[i]));
    }
    polynomialFunction.setCoefficients(abstractTimeFunctions);
    AbstractTimeFunction[] actualCoefficients = polynomialFunction.getCoefficients();
    assertArrayEquals(abstractTimeFunctions, actualCoefficients);

  }

  @Test
  void setParameters() {
    // No errors expected
    double[] newCoefficients = {4.0, 5.0, 6.0};
    AbstractTimeFunction[] abstractTimeFunctions = new AbstractTimeFunction[newCoefficients.length];
    for (int i = 0; i < newCoefficients.length; i++) {
      abstractTimeFunctions[i] = new NextHighFunction(
          new RandomFunction(
              RandomVariateFactory.getInstance(
                  "ConstantVariate",
                  newCoefficients[i]
             )
          ), 1.0, 0.0);
    }
    polynomialFunction.setParameters((Object[]) abstractTimeFunctions);
    AbstractTimeFunction[] actualCoefficients = polynomialFunction.getCoefficients();
    assertArrayEquals(abstractTimeFunctions, actualCoefficients);

  }
}