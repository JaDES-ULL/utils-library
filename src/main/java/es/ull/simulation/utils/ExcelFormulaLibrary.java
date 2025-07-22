package es.ull.simulation.utils;

import java.util.Arrays;

public enum ExcelFormulaLibrary implements ExcelFormula {
    RAND("RAND()"),
    AVERAGE("AVERAGE(%s)"),
    STD_DEV("STDEV(%s)"),
    OFFSET("OFFSET(%s,%s,%s,%s)"),
    PERCENTILE05("PERCENTILE(%s,0.5)"),
    PERCENTILE0025("PERCENTILE(%s,0.025)"),
    PERCENTILE0975("PERCENTILE(%s,0.975)"),
    R2("INDEX(LINEST(%s,%s,TRUE,TRUE),3,1)"),
    BETA_INV("_xlfn.BETA.INV(%s,%s,%s)"),
    GAMMA_INV("_xlfn.GAMMA.INV(%s,%s,%s)"),
    NORMAL_INV("_xlfn.NORM.INV(%s,%s,%s)"),
    POISSON_INV("_xlfn.POISSON.INV(%s,%s,%s)"),
    EXPONENTIAL_INV("-LN(%s / %s)"),
    UNIFORM_INV("%s * (%s - %s) + %s") {
        @Override
        public String getFormula(Object... operands) {
            // The template expects four operands, but the uniform expression only requires rand, min and max.
            Object [] newOperands = new Object[operands.length + 1];
            newOperands = Arrays.copyOf(operands, operands.length + 1);
            // Hence, we replicate the last operand to fill the expected number of operands.
            newOperands[operands.length] = operands[operands.length - 1];
            return super.getFormula(newOperands);
        }
    },
    BERNOULLI_INV("(%s < %s) * 1");

    private final String template;

    private ExcelFormulaLibrary(String template) {
        this.template = template;
    }

    @Override
    public String getTemplate() {
        return template;
    }
}
