package com.ull.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ull.functions.AbstractTimeFunction;
import com.ull.functions.ConstantFunction;
import com.ull.functions.LinearFunction;
import com.ull.functions.TimeFunctionParams;

class LinearFunctionTest {
    LinearFunction linearFunction;
    TimeFunctionParams params;
    AbstractTimeFunction scale,shift, newShift, newScale;



    @BeforeEach
    void setUp() {

        scale = new ConstantFunction(0.23);
        shift = new ConstantFunction(0.11);
        linearFunction = new LinearFunction(scale, shift);
        params = () -> 2.0;
    }


    @Test
    void SetParameters() {

        linearFunction.setParameters(scale,shift);
        Assertions.assertEquals(scale, linearFunction.getScale());
        Assertions.assertEquals(shift, linearFunction.getShift());
    }

    @Test
    void setScale() {
        newScale= new ConstantFunction(0.5);
        linearFunction.setScale(newScale);
        Assertions.assertEquals(newScale, linearFunction.getScale());

        newScale= new LinearFunction();
        newScale.setParameters(scale,shift);
        linearFunction.setScale(newScale);
        Assertions.assertEquals(newScale, linearFunction.getScale());
    }

    @Test
    void setShift() {

        newShift= new ConstantFunction(0.18);
        linearFunction.setShift(newShift);
        Assertions.assertEquals(newShift, linearFunction.getShift());

        newShift= new LinearFunction(scale,shift);
        linearFunction.setShift(shift);
        Assertions.assertEquals(shift, linearFunction.getShift());
    }

    // Buscar alternativas al churro ese
    @Test
    void getValue() {
        Assertions.assertEquals(0.5700000000000001, linearFunction.getValue(params));

    }

    @Test
    void getScale() {
        linearFunction.setParameters(scale,shift);
        Assertions.assertEquals(scale, linearFunction.getScale());
    }

    @Test
    void getShift() {
        linearFunction.setParameters(scale,shift);
        Assertions.assertEquals(shift, linearFunction.getShift());
    }

}