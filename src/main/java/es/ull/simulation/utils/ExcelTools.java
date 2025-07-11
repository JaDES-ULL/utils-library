package es.ull.simulation.utils;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/** 
 * @author Iván Castilla Rodríguez
 *
*/

public class ExcelTools {
/**
   * Creates an Excel font which is bold.
   * @param wb Workbook where the font is used.
   * @return A bold Excel font.
   *
 ***/

  public static Font getBoldFont(Workbook wb) {
    final Font boldFont = wb.createFont();
    boldFont.setBold(true);
    return boldFont;
  }

/**
   * Creates an Excel style to remark error cells.
   * @param wb Workbook where the style is used.
   * @return A red and italic font.
**/
  public static CellStyle getErrorStyle(Workbook wb) {
    final Font redFont = wb.createFont();
    //redFont.setColor(HSSFColor.RED.index);
    redFont.setColor(HSSFFont.COLOR_RED);
    redFont.setItalic(true);
    final CellStyle errorStyle = wb.createCellStyle();
    errorStyle.setFont(redFont);
    return errorStyle;
  }

/**
   * Creates an Excel style to remark header cells.
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
      res = String.valueOf((int)r.getCell(column).getNumericCellValue());
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
   * @param row The row where the cell should be retrieved or created.
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
   * Gets or creates a row at the specified index in the given sheet.
   * @param sheet The sheet where the row should be retrieved or created.
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
   * @param sheet The sheet where the style should be applied.
   * @param style The CellStyle to apply to the cells.
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
}