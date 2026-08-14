package pt.uninova.ditag.tool.coder;

import java.util.EnumSet;

import pt.uninova.ditag.tool.node.SchemaNode;
import pt.uninova.ditag.tool.node.SchemaNode.DataType;
import pt.uninova.ditag.tool.utils.StringMatrix;

public class SchemaNodeCoderConversion {
	
	static StringMatrix conversionTable;
	
    static {
    	int size = SchemaNode.DataType.values().length;
        conversionTable = new StringMatrix(size, size);
        
        int i = 0;
        for (SchemaNode.DataType dataType : SchemaNode.DataType.values()) {
        	conversionTable.setRowName(i, dataType);
        	conversionTable.setColumnName(i, dataType);
        	i++;
        }
        
        for (int j = 0; j < size; j++) {
        	for (int k = 0; k < size; k++) {
        		conversionTable.setValue(j, k, "%s");
            }
        }
        
        // PROVIDER -> CONSUMER
        // FLOAT -> FLOAT
        conversionTable.setValue(SchemaNode.DataType.FLOAT, SchemaNode.DataType.FLOAT, "(float) %s"); 
        // ... -> STRING
        for (SchemaNode.DataType dataType : SchemaNode.DataType.values()) {
        	if (!dataType.equals(DataType.STRING)) {
        		conversionTable.setValue(dataType, SchemaNode.DataType.STRING, "String.valueOf(%s)");
        	}
        }
        // ... -> NORMALIZED STRING
        for (SchemaNode.DataType dataType : SchemaNode.DataType.values()) {
        	if (!EnumSet.of(DataType.NORMALIZED_STRING, DataType.STRING, DataType.DATE, DataType.TIME, DataType.DATE_TIME).contains(dataType)) {
        	    conversionTable.setValue(dataType, SchemaNode.DataType.NORMALIZED_STRING, "String.valueOf(%s)");
        	}
        }
        // DATETIME -> DATE
        conversionTable.setValue(SchemaNode.DataType.DATE_TIME, SchemaNode.DataType.DATE, "%s.toLocalDate()");
        // DATETIME -> TIME
        conversionTable.setValue(SchemaNode.DataType.DATE_TIME, SchemaNode.DataType.TIME, "%s.toLocalTime()");
    }
    
    public static StringMatrix getConversionTable() {
        return conversionTable;
    }
	    
}
