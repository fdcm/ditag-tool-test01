package pt.uninova.ditag.tool.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.uninova.ditag.tool.node.SchemaNode;

public class StringMatrix {
	
	private String[][] data;
    private Map<SchemaNode.DataType, Integer> rows;
    private Map<SchemaNode.DataType, Integer> columns;

    public StringMatrix(int rows, int columns) {
        this.data = new String[rows][columns];
        this.rows = new HashMap<>();
        this.columns = new HashMap<>();
    }
    
    public void setRowName(int index, SchemaNode.DataType dataType) {
    	this.rows.put(dataType, index);
    }
    public void setColumnName(int index, SchemaNode.DataType dataType) {
    	this.columns.put(dataType, index);
    }
    
    public void setValue(int row, int column, String value) {
        if (row >= 0 && row < data.length && column >= 0 && column < data[0].length) {
            this.data[row][column] = value;
        } else {
            throw new IndexOutOfBoundsException("Row or Column Index out of Bounds");
        }
    }
    public void setValue(SchemaNode.DataType rowDataType, SchemaNode.DataType columnDataType, String value) {
        Integer rowIndex = rows.get(rowDataType);
        Integer columnIndex = columns.get(columnDataType);
        if (rowIndex != null && columnIndex != null) {
            setValue(rowIndex, columnIndex, value);
        } else {
            throw new IllegalArgumentException("Invalid Row or Column Data Type");
        }
    }
    
    public String getValue(int row, int column) {
        if (row >= 0 && row < data.length && column >= 0 && column < data[0].length) {
            return this.data[row][column];
        } else {
            throw new IndexOutOfBoundsException("Row or Column Index out of Bounds");
        }
    }
    public String getValue(SchemaNode.DataType rowDataType, SchemaNode.DataType columnDataType) {
        Integer rowIndex = rows.get(rowDataType);
        Integer columnIndex = columns.get(columnDataType);
        if (rowIndex != null && columnIndex != null) {
            return getValue(rowIndex, columnIndex);
        } else {
            throw new IllegalArgumentException("Invalid Row or Column Data Type");
        }
    }
    
    public int getRowIndex(SchemaNode.DataType rowDataType) {
        Integer index = rows.get(rowDataType);
        if (index != null) {
            return index;
        } else {
            throw new IllegalArgumentException("Invalid Row Data Type");
        }
    }
    public int getColumnIndex(SchemaNode.DataType columnDataType) {
        Integer index = columns.get(columnDataType);
        if (index != null) {
            return index;
        } else {
            throw new IllegalArgumentException("Invalid Column Data Type");
        }
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        List<Map.Entry<SchemaNode.DataType, Integer>> sortedColumns = new ArrayList<>(columns.entrySet());
        sortedColumns.sort(Map.Entry.comparingByValue());

        List<Map.Entry<SchemaNode.DataType, Integer>> sortedRows = new ArrayList<>(rows.entrySet());
        sortedRows.sort(Map.Entry.comparingByValue());

        sb.append(String.format("%25s", ""));
        for (Map.Entry<SchemaNode.DataType, Integer> colEntry : sortedColumns) {
            sb.append(String.format("%25s", colEntry.getKey().name()));
        }
        sb.append("\n");

        for (Map.Entry<SchemaNode.DataType, Integer> rowEntry : sortedRows) {
            int rowIndex = rowEntry.getValue();
            sb.append(String.format("%25s", rowEntry.getKey().name()));

            for (Map.Entry<SchemaNode.DataType, Integer> colEntry : sortedColumns) {
                int colIndex = colEntry.getValue();
                String value = data[rowIndex][colIndex];
                sb.append(String.format("%25s", value != null ? value : ""));
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
