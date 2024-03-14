package com.ull.functions;

/**
 * An abstract class to return values corresponding to a specified function, which can depend on time.
 * @author Ivan Castilla Rodriguez
 */
public abstract class AbstractTimeFunction {
    /**
     * Returns a value as indicated by the definition of this function.
     * @param params The parameters which can be used to determine the value to be returned.
     * @return A value as indicated by the definition of this function.
     */
    public abstract double getValue(TimeFunctionParams params);

    /**
     * Sets the parameters of this function.
     * @param params Parameters required by this function.
     */
    public abstract void setParameters(Object... params);

}
