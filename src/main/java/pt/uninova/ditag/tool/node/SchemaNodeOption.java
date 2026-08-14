package pt.uninova.ditag.tool.node;

import java.util.HashMap;
import java.util.Map;

import pt.uninova.ditag.tool.semantics.SemanticComparator;
import pt.uninova.ditag.tool.utils.EvalExConverter;

public class SchemaNodeOption {
	
	public SchemaNodeOption() {
		super();
	}
	
	private SchemaNode refNode;
	public void setRefNode(SchemaNode refNode) {
		this.refNode = refNode;
	}
	
	// Model Reference
	
	private SchemaModelReference modelReference;
	public SchemaModelReference getModelReference() {
		return modelReference;
	}
	public void createModelReference(SemanticComparator semanticComparator) {
		this.modelReference = new SchemaModelReference(semanticComparator);
	}
	
	// Constant
	
	private String value;
	public String getValue() {
		if (this.value == null) {
			return this.evaluateValueExpression();
		}
		return value;
	}
	public void setValue(String constant) {
		this.value = constant;
	}
	
	// Conversion Formula
	
	private String conversionExpression;
	public String getConversionExpression() {
		return conversionExpression;
	}
	public void setConversionExpression(String conversionExpression) {
		this.conversionExpression = conversionExpression;
	}
	
	// Map Data Individual (List)
	
	private Map<String, Object> mapDataInd;
	public void createMapDataInd() {
		this.mapDataInd = new HashMap<String, Object>();
	}
	public Map<String, Object> getMapDataInd() {
		return mapDataInd;
	}
	public Object getIndividualValue(String key) {
		return mapDataInd.get(key);
	}
	public void addIndividualValue(String key, Object value) {
		if (!this.mapDataInd.containsKey(key)) {
			this.mapDataInd.put(key, value);
		}
	}
	public void removeIndividualValue(String key) {
		this.mapDataInd.remove(key);
	}
	
	// Default Expression
	private String defaultExpression = null;
	public String getDefaultExpression() {
		return defaultExpression;
	}
	public void setDefaultExpression(String defaultExpression) {
		this.defaultExpression = defaultExpression;
	}
	public String evaluateDefaultExpression() {
		if (defaultExpression != null) {
			String expressionValue = String.format("(new Expression(\"%s\")).evaluate().getStringValue()", defaultExpression.replace("\"", "\\\\\""));
			return "(" + EvalExConverter.convertFromString(refNode.getDataType(), expressionValue) + ")";
		}
		return null;
	}
	
	// Value Expression
	private String valueExpression = null;
	public String getValueExpression() {
		return valueExpression;
	}
	public void setValueExpression(String defaultExpression) {
		this.valueExpression = defaultExpression;
	}
	public String evaluateValueExpression() {
		if (valueExpression != null) {
			String expressionValue = String.format("(new Expression(\"%s\").evaluate().getStringValue()", valueExpression.replace("\"", "\\\\\""));
			return "(" + EvalExConverter.convertFromString(refNode.getDataType(), expressionValue) + ")";
		}
		return null;
	}
		
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder("SchemaNodeOption{");

	    if (modelReference != null) {
	        sb.append("model-reference=").append(modelReference);
	    }

	    if (value != null) {
	        if (sb.length() > 17) sb.append(", ");
	        sb.append("value='").append(value).append("'");
	    }
	    
	    if (conversionExpression != null) {
	        if (sb.length() > 17) sb.append(", ");
	        sb.append("conversion=").append(conversionExpression);
	    }
	    
	    if (mapDataInd != null && !mapDataInd.isEmpty()) {
	        if (sb.length() > 17) sb.append(", ");
	        sb.append("map-data-ind=").append(mapDataInd);
	    }
	    
	    if (defaultExpression != null) {
	        if (sb.length() > 17) sb.append(", ");
	        sb.append("default-expression=").append(defaultExpression);
	    }
	    
	    sb.append("}");
	    return sb.toString();
	}
}
