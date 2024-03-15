package com.ull.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExcelToolsTest {

  private HSSFWorkbook workbook;

  @BeforeEach
  void setUp() {
    workbook = new HSSFWorkbook();
  }

  @AfterEach
  void tearDown() throws IOException {
    workbook.close();
  }

  @Test
  void getBoldFont() {
    HSSFFont font = ExcelTools.getBoldFont(workbook);
    assertNotNull(font);
    assertTrue(font.getBold());
  }

  @Test
  void getErrorStyle() {
    HSSFCellStyle style = ExcelTools.getErrorStyle(workbook);
    assertNotNull(style);
    HSSFFont font = style.getFont(workbook);
    assertNotNull(font);
    assertTrue(font.getItalic());
    assertEquals(HSSFFont.COLOR_RED, font.getColor());
  }

  @Test
  void getHeadStyle() {
    HSSFCellStyle style = ExcelTools.getHeadStyle(workbook);
    assertNotNull(style);
    HSSFFont font = style.getFont(workbook);
    assertNotNull(font);
    assertTrue(font.getBold());
    assertEquals(64,
                 style.getFillForegroundColor());
    assertEquals(FillPatternType.SOLID_FOREGROUND, style.getFillPattern());
  }

  @Test
  void getString() {
    HSSFSheet sheet = workbook.createSheet();
    HSSFRow row = sheet.createRow(0);
    HSSFCell cell = row.createCell(0);
    cell.setCellValue("Test Value");
    String value = ExcelTools.getString(sheet, 0, (short)0);
    assertEquals("Test Value", value);
  }

  @Test
  void testGetString() {
    HSSFSheet sheet = workbook.createSheet();
    HSSFRow row = sheet.createRow(0);
    HSSFCell cell = row.createCell(0);
    cell.setCellValue("Test Value");
    String value = ExcelTools.getString(row, (short)0);
    assertEquals("Test Value", value);
  }

  @Test
  void validCell() {
    HSSFSheet sheet = workbook.createSheet();
    HSSFRow row = sheet.createRow(0);
    HSSFCell cell = row.createCell(0);
    assertTrue(ExcelTools.validCell(row, (short)0));
    assertFalse(ExcelTools.validCell(row, (short)1));
    assertFalse(ExcelTools.validCell(row, (short)2));
  }
}