package es.ull.simulation.utils;

import org.apache.poi.ss.util.CellReference;

public interface ExcelFormula {
    public default String getFormula(Object... operands) {
        // Preprocess the operands to ensure they are formatted correctly
        for (int i = 0; i < operands.length; i++) {
            if (operands[i] instanceof CellReference) {
                operands[i] = ((CellReference)operands[i]).formatAsString();
            } 
        }
        return String.format(getTemplate(), operands);
    }
    public String getTemplate();
}
