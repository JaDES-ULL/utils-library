package com.ull.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simkit.random.RandomVariate;
import simkit.random.RandomVariateFactory;
import ull.tfg.functions.RandomFunction;
import ull.tfg.functions.TimeFunctionParams;

class RandomFunctionTest {
  RandomFunction randomFunction;
  TimeFunctionParams params;

  @BeforeEach
  void setUp() {
    RandomVariate rnd = RandomVariateFactory.getInstance(
        "ConstantVariate", 5.0);
    randomFunction = new RandomFunction(rnd);
    params = () -> 2.0;
  }

  @Test
  void getValue() {
    assertEquals(5.0, randomFunction.getValue(params));
  }

  @Test
  void getRandom() {
    RandomVariate expectedRnd = randomFunction.getRandom();
    assertNotNull(expectedRnd);
  }

  @Test
  void setRandom() {
    RandomVariate newRnd = RandomVariateFactory.getInstance(
        "ConstantVariate", 10.0);
    randomFunction.setRandom(newRnd);
    RandomVariate actualRnd = randomFunction.getRandom();
    assertEquals(newRnd, actualRnd);
  }

  @Test
  void setParameters() {
    RandomVariate newRnd = RandomVariateFactory.getInstance(
        "ConstantVariate", 10.0);
    randomFunction.setParameters(newRnd);
    RandomVariate actualRnd = randomFunction.getRandom();
    assertEquals(newRnd, actualRnd);
  }
}