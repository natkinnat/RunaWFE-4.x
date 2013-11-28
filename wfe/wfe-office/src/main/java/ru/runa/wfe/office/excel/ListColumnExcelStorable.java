package ru.runa.wfe.office.excel;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import ru.runa.wfe.office.excel.utils.ExcelHelper;
import ru.runa.wfe.var.format.FormatCommons;
import ru.runa.wfe.var.format.VariableFormat;
import ru.runa.wfe.var.format.VariableFormatContainer;

public class ListColumnExcelStorable extends ExcelStorable<ColumnConstraints, List<?>> {

    @Override
    public void load(Workbook workbook) {
        List<Object> list = new ArrayList<Object>();
        int rowIndex = constraints.getRowStartIndex();
        String elementFormatClassName = ((VariableFormatContainer) format).getComponentClassName(0);
        VariableFormat elementFormat = FormatCommons.create(elementFormatClassName);
        while (true) {
            Cell cell = getCell(workbook, rowIndex, false);
            if (ExcelHelper.isCellEmptyOrNull(cell)) {
                break;
            }
            list.add(ExcelHelper.getCellValue(cell, elementFormat));
            rowIndex++;
        }
        setData(list);
    }

    @Override
    public void storeIn(Workbook workbook) {
        List<?> list = data;
        int rowIndex = constraints.getRowStartIndex();
        String elementFormatClassName = ((VariableFormatContainer) format).getComponentClassName(0);
        VariableFormat elementFormat = FormatCommons.create(elementFormatClassName);
        for (Object object : list) {
            Cell cell = getCell(workbook, rowIndex, true);
            ExcelHelper.setCellValue(cell, elementFormat.format(object));
            rowIndex++;
        }
    }

    private Cell getCell(Workbook workbook, int rowIndex, boolean createIfLost) {
        Sheet sheet = ExcelHelper.getSheet(workbook, constraints.getSheetName(), constraints.getSheetIndex());
        Row row = ExcelHelper.getRow(sheet, rowIndex, true);
        Cell cell = ExcelHelper.getCell(row, constraints.getColumnIndex(), createIfLost);
        return cell;
    }

}