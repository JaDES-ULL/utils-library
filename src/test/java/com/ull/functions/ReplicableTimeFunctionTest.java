package com.ull.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simkit.random.RandomVariate;
import simkit.random.RandomVariateFactory;
import ull.tfg.functions.AbstractTimeFunction;
import ull.tfg.functions.RandomFunction;
import ull.tfg.functions.ReplicableTimeFunction;
import ull.tfg.functions.TimeFunctionParams;

class ReplicableTimeFunctionTest {

  ReplicableTimeFunction replicableTimeFunction;
  TimeFunctionParams params;

  @BeforeEach
  void setUp() {
    // Configuración inicial para cada prueba
    RandomVariate rnd = RandomVariateFactory.getInstance(
        "ConstantVariate", 5.0);
    AbstractTimeFunction randomFunction = new RandomFunction(rnd);
    replicableTimeFunction = new ReplicableTimeFunction(randomFunction);
    params = () -> 2.0; // Ejemplo de parámetros de tiempo
  }

  @Test
  void reStart() {
    // Works if not RandomFunction
    replicableTimeFunction.reStart();
    //assertEquals(0, replicableTimeFunction.getValue(params));
    assertEquals(5, replicableTimeFunction.getValue(params));
  }

  @Test
  void getValue() {
    assertEquals(5.0, replicableTimeFunction.getValue(params));
  }

  @Test
  void setParameters() {
    RandomVariate newRnd = RandomVariateFactory.getInstance(
        "ConstantVariate", 10.0);
    AbstractTimeFunction newRandomFunction = new RandomFunction(newRnd);
    replicableTimeFunction.setParameters(newRandomFunction);
    AbstractTimeFunction actualInnerFunction = replicableTimeFunction.getInnerTimeFunction();
    assertEquals(newRandomFunction, actualInnerFunction);
  }

  @Test
  void getInnerTimeFunction() {
    AbstractTimeFunction actualInnerFunction = replicableTimeFunction.getInnerTimeFunction();
    assertNotNull(actualInnerFunction);
  }

  @Test
  void setInnerTimeFunction() {
    RandomVariate newRnd = RandomVariateFactory.getInstance(
        "ConstantVariate", 10.0);
    AbstractTimeFunction newRandomFunction = new RandomFunction(newRnd);
    replicableTimeFunction.setInnerTimeFunction(newRandomFunction);
    AbstractTimeFunction actualInnerFunction = replicableTimeFunction.getInnerTimeFunction();
    assertEquals(newRandomFunction, actualInnerFunction);
  }
}