package es.ull.simulation.utils;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

/**
 * A set of utility methods for working with Excel files.
 * 
 * @author Iván Castilla Rodríguez
 */

public class ExcelTools {
    /**
     * Creates an Excel font which is bold.
     * 
     * @param wb Workbook where the font is used.
     * @return A bold Excel font.
     *
     **/
    public static Font getBoldFont(Workbook wb) {
        final Font boldFont = wb.createFont();
        boldFont.setBold(true);
        return boldFont;
    }

    /**
     * Creates an Excel style to remark error cells.
     * 
     * @param wb Workbook where the style is used.
     * @return A red and italic font.
     **/
    public static CellStyle getErrorStyle(Workbook wb) {
        final Font redFont = wb.createFont();
        // redFont.setColor(HSSFColor.RED.index);
        redFont.setColor(HSSFFont.COLOR_RED);
        redFont.setItalic(true);
        final CellStyle errorStyle = wb.createCellStyle();
        errorStyle.setFont(redFont);
        return errorStyle;
    }

    /**
     * Creates an Excel style to remark header cells.
     * 
     * @param wb Workbook where the style is used.
     * @return A grey background and a bold font.
     **/
    public static CellStyle getHeadStyle(Workbook wb) {
        final CellStyle headStyle = wb.createCellStyle();
        // headStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        headStyle.setFillForegroundColor(HSSFColorPredefined.GREY_25_PERCENT.getColor());
        // headStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headStyle.setWrapText(true);
        headStyle.setFont(getBoldFont(wb));
        return headStyle;
    }

    public static String getString(Sheet s, int row, int column) {
        return s.getRow(row).getCell(column).getRichStringCellValue().getString();
    }

    public static String getString(Row r, int column) {
        String res = "";
        if (r.getCell(column).getCellType() == CellType.NUMERIC)
            res = String.valueOf((int) r.getCell(column).getNumericCellValue());
        else
            res = r.getCell(column).getRichStringCellValue().getString();
        return res;

    }

    public static boolean validCell(Row r, int column) {
        if (r.getCell(column) == null)
            return false;
        if (r.getCell(column).getCellType() == CellType.NUMERIC)
            return false;
        return true;
    }

    /**
     * Gets or creates a cell at the specified index in the given row.
     * 
     * @param row      The row where the cell should be retrieved or created.
     * @param colIndex The index of the column to retrieve or create.
     * @return The Cell at the specified index.
     */
    public static Cell getOrCreateCell(Row row, int colIndex) {
        // Get or create a cell at the specified column index
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        return cell;
    }

    /**
     * Gets or creates a cell at the specified column and row in the given sheet.
     * 
     * @param sheet    The sheet where the row should be retrieved or created.
     * @param rowIndex The index of the row where the cell should be retrieved or created.
     * @param colIndex The index of the column to retrieve or create.
     * @return The Cell at the specified index.
     */
    public static Cell getOrCreateCell(Sheet sheet, int rowIndex, int colIndex) {
        Row row = getOrCreateRow(sheet, rowIndex);
        return getOrCreateCell(row, colIndex);
    }

    /**
     * Gets or creates a row at the specified index in the given sheet.
     * 
     * @param sheet    The sheet where the row should be retrieved or created.
     * @param rowIndex The index of the row to retrieve or create.
     * @return The XSSFRow at the specified index.
     */
    public static Row getOrCreateRow(Sheet sheet, int rowIndex) {
        // Get or create a row at the specified index
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row;
    }

    /**
     * Applies a given style to a range of cells in the specified sheet.
     * 
     * @param sheet  The sheet where the style should be applied.
     * @param style  The CellStyle to apply to the cells.
     * @param iniRow The starting row index (inclusive).
     * @param endRow The ending row index (inclusive).
     * @param iniCol The starting column index (inclusive).
     * @param endCol The ending column index (inclusive).
     */
    public static void applyStyleToRange(Sheet sheet, CellStyle style, int iniRow, int endRow, int iniCol, int endCol) {
        for (int i = iniRow; i <= endRow; i++) {
            final Row row = getOrCreateRow(sheet, i);

            for (int j = iniCol; j <= endCol; j++) {
                final Cell cell = getOrCreateCell(row, j);
                cell.setCellStyle(style);
            }
        }
    }

    /**
     * Gets the style from a named range in the workbook.
     * 
     * @param workbook   The workbook containing the named range.
     * @param namedRange The name of the named range to retrieve the style from. It
     *                   should refer to a cell in the workbook.
     * @return The CellStyle associated with the named range, or null if the named
     *         range does not exist or does not refer to a valid cell.
     */
    public static CellStyle getStyleFromNamedRange(Workbook workbook, String namedRange) {
        // Get the named range from the workbook
        final Name name = workbook.getName(namedRange);
        if (name == null)
            return null;
        final String formula = name.getRefersToFormula(); // Ej: "Hoja1!$B$4"
        if (formula == null)
            return null;
        final CellReference ref = new CellReference(formula);
        Sheet sheet = workbook.getSheet(ref.getSheetName());
        if (sheet == null)
            return null;

        Row row = sheet.getRow(ref.getRow());
        if (row == null)
            return null;

        return row.getCell(ref.getCol()).getCellStyle();
    }

    /**
     * Copies a range of cells from one sheet to another.
     * Copies the cell styles and values, including formulas.
     * @param sheet The source sheet from which to copy the cells.
     * @param startRow The starting row index of the range to copy (inclusive).
     * @param startCol The starting column index of the range to copy (inclusive).
     * @param endRow The ending row index of the range to copy (inclusive).
     * @param endCol The ending column index of the range to copy (inclusive).
     * @param targetSheet The target sheet where the cells will be copied to.
     * @param targetStartRow The starting row index in the target sheet where the copied cells will be placed (inclusive).
     * @param targetStartCol The starting column index in the target sheet where the copied cells will be placed (inclusive).
     */
    public static void copyCellRange(Sheet sheet, int startRow, int startCol, int endRow, int endCol,
            Sheet targetSheet, int targetStartRow, int targetStartCol) {
        for (int r = 0; r <= endRow - startRow; r++) {
            Row sourceRow = getOrCreateRow(sheet, startRow + r);
            Row targetRow = getOrCreateRow(targetSheet, targetStartRow + r);

            for (int c = 0; c <= endCol - startCol; c++) {
                Cell sourceCell = getOrCreateCell(sourceRow, startCol + c);
                Cell targetCell = getOrCreateCell(targetRow, targetStartCol + c);
                targetCell.setCellStyle(sourceCell.getCellStyle());

                // Copiar tipo y contenido
                switch (sourceCell.getCellType()) {
                    case STRING:
                        targetCell.setCellValue(sourceCell.getRichStringCellValue());
                        break;
                    case NUMERIC:
                        targetCell.setCellValue(sourceCell.getNumericCellValue());
                        break;
                    case BOOLEAN:
                        targetCell.setCellValue(sourceCell.getBooleanCellValue());
                        break;
                    case FORMULA:
                        targetCell.setCellFormula(sourceCell.getCellFormula());
                        break;
                    case ERROR:
                        targetCell.setCellErrorValue(sourceCell.getErrorCellValue());
                        break;
                    case BLANK:
                        targetCell.setBlank();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Copies a named range from the workbook to a target sheet at a specified position.
     * This method retrieves the named range from the workbook and copies its content
     * to the target sheet starting at the specified row and column.
     * @param rangeName The name of the range to copy.
     * @param targetSheet The target sheet where the named range will be copied.
     * @param targetStartRow The starting row index in the target sheet where the named range will be copied.
     * @param targetStartCol The starting column index in the target sheet where the named range will be copied.
     * @throws IllegalArgumentException if the named range does not exist in the workbook.
     */
    public static void copyCellRange(String rangeName, Sheet targetSheet, int targetStartRow, int targetStartCol) {
        Workbook workbook = targetSheet.getWorkbook();
        Name namedRange = workbook.getName(rangeName);
        if (namedRange == null) {
            throw new IllegalArgumentException("Named range '" + rangeName + "' does not exist in the workbook.");
        }
        String formula = namedRange.getRefersToFormula(); 
        AreaReference area = new AreaReference(formula, workbook.getSpreadsheetVersion());
        CellReference firstCell = area.getFirstCell();
        CellReference lastCell = area.getLastCell();        
        Sheet sourceSheet = workbook.getSheet(firstCell.getSheetName());
        copyCellRange(sourceSheet, firstCell.getRow(), firstCell.getCol(), lastCell.getRow(), lastCell.getCol(), targetSheet, targetStartRow, targetStartCol);
    }
}