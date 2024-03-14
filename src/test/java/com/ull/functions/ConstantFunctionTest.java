package com.ull.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.ull.functions.ConstantFunction;
import com.ull.functions.TimeFunctionParams;

class ConstantFunctionTest {
    ConstantFunction constantFunction = new ConstantFunction(0d);

    TimeFunctionParams params = () -> 1.0;

    double expectedValue = 10.0;

    @Test
    void testConstructor(){
        ConstantFunction constantFunction = new ConstantFunction(0d);
        Assertions.assertEquals(ConstantFunction.class, constantFunction.getClass());
    }


    @Test
    void testSetConstantValue() {
        constantFunction.setConstantValueValue(0.23);
        Assertions.assertEquals(0.23,constantFunction.getValue(params));
    }

    @Test
    void testGetConstantValue() {
        constantFunction.setConstantValueValue(0.23);
        Assertions.assertEquals(0.23,constantFunction.getValue(params));
    }

    @Test
    void testGetValue() {
        double result = constantFunction.getValue(params);
        Assertions.assertEquals(0d, result);
    }

    @Test
    void testSetParameters() {
        constantFunction.setParameters(expectedValue);
        Assertions.assertEquals(expectedValue,
                constantFunction.getValue(params),
                0.0001);
    }

}
