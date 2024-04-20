package es.ull.simulation.functions;

/**
 * A constant value wrapped by a time function.
 * @author Ivan Castilla Rodriguez
 *
 */
public class ConstantFunction extends AbstractTimeFunction {

    private double constantValue;

    public ConstantFunction(double constantValue) {
        // super();
        this.constantValue = constantValue;
    }

    public void setConstantValueValue(double val) {
        this.constantValue = val;
    }

    public double getValue(TimeFunctionParams params) {
        return constantValue;
    }

    @Override
    public void setParameters(Object... params) {
        if (params.length < 1)
            throw new IllegalArgumentException("Need (value), received " +
                    params.length + " parameters");
        else {
            // setValue(((Number)params[0]).doubleValue());
            setConstantValueValue(((Number)params[0]).doubleValue()); // Sets the value of the constant function.
        }

    }

}
