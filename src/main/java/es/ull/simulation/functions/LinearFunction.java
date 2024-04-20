package es.ull.simulation.functions;

/**
 * Represents the linear function: Ax + B. Thus, two parameters are required: A (scale) and B (shift).
 * @author Iván Castilla Rodríguez
 */
public class LinearFunction extends AbstractTimeFunction {
    /** Scale */
    private AbstractTimeFunction scale;
    /** Shift */
    private AbstractTimeFunction shift;

    /**
     * Creates a new linear function with two parameters A (scale) and B (factor), which can be also
     * defined as other time functions.
     * @param scale Scale
     * @param shift Factor
     */
    public LinearFunction(AbstractTimeFunction scale, AbstractTimeFunction shift) {
        this.scale = scale;
        this.shift = shift;
    }

    /**
     * Creates a new linear function with two parameters A (scale) and B (factor), which are defined
     * as constants.
     * @param scale Scale
     * @param shift Factor
     */
    public LinearFunction(double scale, double shift) {
        this.scale = new ConstantFunction(scale);
        this.shift = new ConstantFunction(shift);
    }

    /**
     * Creates a non-set linear function. This constructor must be used together with the
     * <code>setParameters</code> method.
     */
    public LinearFunction() {
    }

    /**
     * Returns the scale.
     * @return Returns the scale.
     */
    public AbstractTimeFunction getScale() {
        return scale;
    }

    /**
     * Returns the shift
     * @return Returns the shift.
     */
    public AbstractTimeFunction getShift() {
        return shift;
    }

    /**
     * Sets a new value for the scale.
     * @param scale The scale to set.
     */
    public void setScale(AbstractTimeFunction scale) {
        this.scale = scale;
    }

    /**
     * Sets a new value for the shift
     * @param shift The shift to set.
     */
    public void setShift(AbstractTimeFunction shift) {
        this.shift = shift;
    }

    public double getValue(TimeFunctionParams params) {
        return scale.getValue(params) * params.getTime() + shift.getValue(params);
    }

    /**
     * Requires two parameters: scale and shift.
     * @param params Parameters required by this method.
     */
    @Override
    public void setParameters(Object... params) {
        if (params.length < 2)
            throw new IllegalArgumentException("Need (Scale, Shift), received " +
                    params.length + " parameters");
        else {
            setScale((AbstractTimeFunction) params[0]);
            setShift((AbstractTimeFunction) params[1]);
        }

    }
}
