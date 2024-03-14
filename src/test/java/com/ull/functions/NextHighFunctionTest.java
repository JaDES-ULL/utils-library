package com.ull.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ull.tfg.functions.AbstractTimeFunction;
import ull.tfg.functions.ConstantFunction;
import ull.tfg.functions.LinearFunction;
import ull.tfg.functions.NextHighFunction;
import ull.tfg.functions.TimeFunctionParams;

class NextHighFunctionTest {
    NextHighFunction nextHighFunction;
    TimeFunctionParams params;
    Double scale,shift;

    //AbstractTimeFunction scale,shift, newShift, newScale;
    AbstractTimeFunction constantFunction, linearFunction;

    @BeforeEach
    void setUp() {
        scale = 0.23;
        shift = 0.23;
        nextHighFunction = new NextHighFunction();
        nextHighFunction.setScale(scale);
        nextHighFunction.setShift(shift);

        constantFunction = new ConstantFunction(0.23);
        linearFunction = new LinearFunction(0.23,0.23);
        params = () -> 2.0;

    }

    @Test
    void getValue() {

        //Constante
        nextHighFunction.setFunc(constantFunction);
        Assertions.assertEquals(0.30000000000000027,nextHighFunction.getValue(params));

        //Lineal
        nextHighFunction.setFunc(linearFunction);
        Assertions.assertEquals(0.7600000000000002,nextHighFunction.getValue(params));
    }

    @Test
    void getFunc() {
        //Constante
        nextHighFunction.setFunc(constantFunction);
        Assertions.assertEquals(ConstantFunction.class,nextHighFunction.getFunc().getClass());

        //Lineal
        nextHighFunction.setFunc(linearFunction);
        Assertions.assertEquals(LinearFunction.class,nextHighFunction.getFunc().getClass());

    }

    @Test
    void setFunc() {
        //Constante
        nextHighFunction.setFunc(constantFunction);
        Assertions.assertEquals(ConstantFunction.class,nextHighFunction.getFunc().getClass());

        //Lineal
        nextHighFunction.setFunc(linearFunction);
        Assertions.assertEquals(LinearFunction.class,nextHighFunction.getFunc().getClass());
    }

    @Test
    void getScale() {
        Assertions.assertEquals(scale,nextHighFunction.getScale());
    }

    @Test
    void setScale() {
        nextHighFunction.setScale(0.18);
        Assertions.assertEquals(0.18,nextHighFunction.getScale());
    }

    @Test
    void getShift() {
        Assertions.assertEquals(shift,nextHighFunction.getShift());
    }

    @Test
    void setShift() {
        nextHighFunction.setShift(0.11);
        Assertions.assertEquals(0.11,nextHighFunction.getShift());
    }

    @Test
    void setParameters() {
        //funcion constante a 0.11 y 0.23
        nextHighFunction.setParameters(constantFunction,0.11,0.23);
        Assertions.assertEquals(true,
                nextHighFunction.getFunc().getClass() == ConstantFunction.class &&
                        0.11 == nextHighFunction.getScale() &&
                        0.23 == nextHighFunction.getShift());
    }
}