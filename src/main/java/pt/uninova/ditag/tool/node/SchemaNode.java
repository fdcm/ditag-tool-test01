package pt.uninova.ditag.tool.node;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import pt.uninova.ditag.tool.DTLogger;
import pt.uninova.ditag.tool.semantics.SemanticComparator;

public class SchemaNode {
	
	private UUID ID;
	public UUID getID() {
		return ID;
	}
	
	private SchemaNode dependsOn;
	public SchemaNode getDependsOn() {
		return dependsOn;
	}
	public void setDependsOn(SchemaNode node) {
		this.dependsOn = node;
	}
	public String getConstantOption() {
		if (this.getDependsOn() != null) {
			if (this.getDependsOn().getMatchOptionIndex() != null) {
				return this.getSchemaNodeOptions().get(this.getDependsOn().getMatchOptionIndex()).getValue();
			}
			return null;
		} else if (this.getSchemaNodeOptions().get(0).getValue() != null) {
			return this.getSchemaNodeOptions().get(0).getValue();
		}
		return null;
	}
	
	public int getDepth() {
	    int depth = 0;
	    SchemaNode current = this;
	    while (current.getParent() != null) {
	        depth++;
	        current = current.getParent();
	    }
	    return depth;
	}
	
	private String name;
	public String getName() {
		return name;
	}
    public void setName(String name) {
		this.name = name;
	}
	
	private Integer mdiID;
	public Integer getMdiID() {
		return mdiID;
	}
    public void setMdiID(Integer mdiID) {
		this.mdiID = mdiID;
	}
    
    private Type type;
	public enum Type {
        ELEMENT,
        ATTRIBUTE,
        EXTENSION,
        COMPLEX_TYPE,
        SIMPLE_CONTENT,
        SCHEMA,
        EXTRA_PROPERTY,
        ANNOTATION,
        APP_INFO,
        OTHER,
    }
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
    
	private DataType dataType;
    public enum DataType implements Comparable<DataType> {
        FLOAT, // FLOAT, SHORT, U_SHORT, BYTE, U_BYTE
        DOUBLE, // DOUBLE, FLOAT, INT, U_INT, SHORT, U_SHORT, BYTE, U_BYTE
        LONG, // LONG, INT, U_INT, SHORT, U_SHORT, BYTE, U_BYTE
        INT, // INT, SHORT, U_SHORT, BYTE, U_BYTE
    	SHORT, // SHORT, BYTE, U_BYTE
    	BYTE, 
    	
        NON_NEG_INTEGER, //NON_NEG_INTEGER, POS_INTEGER, U_LONG, U_INT, U_SHORT, U_BYTE
        POS_INTEGER,
        NON_POS_INTEGER, //NON_POS_INTEGER, NEG_INTEGER
        NEG_INTEGER,
        
        U_LONG, // U_LONG, U_INT, U_SHORT, U_BYTE
        U_INT, // U_INT, U_SHORT, U_BYTE
        U_SHORT, // U_SHORT, U_BYTE
        U_BYTE,
        
        STRING, // ALL
        NORMALIZED_STRING, // ALL MINUS STRING
        
        DATE, // DATE, DATE_TIME
        TIME, // TIME, DATE_TIME
        DATE_TIME,
        OTHER, // EVERYTHING ELSE MUST BE EQUAL (EX: DATE_TIME, BOOLEAN, BYTE, U_BYTE)
        NONE;
        
    	private String description;
    	public static DataType OTHER(String description) {
            DataType aux = DataType.OTHER;
            aux.setDescription(description);
            return aux;
        }
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
    }

	public DataType getDataType() {
		return dataType;
	}
	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}
	
	private static Map<DataType, Set<DataType>> dataCompatibilityMap = new EnumMap<>(DataType.class);
    static {
    	// 	CONSUMER, PROVIDER
    	
        dataCompatibilityMap.put(DataType.FLOAT, EnumSet.of(DataType.FLOAT, DataType.SHORT, DataType.U_SHORT, DataType.BYTE, DataType.U_BYTE));
        dataCompatibilityMap.put(DataType.DOUBLE, EnumSet.of(DataType.DOUBLE, DataType.FLOAT, DataType.INT, DataType.U_INT, DataType.SHORT, DataType.U_SHORT, DataType.BYTE, DataType.U_BYTE));
        dataCompatibilityMap.put(DataType.LONG, EnumSet.of(DataType.LONG, DataType.INT, DataType.U_INT, DataType.SHORT, DataType.U_SHORT, DataType.BYTE, DataType.U_BYTE, DataType.POS_INTEGER));
    	dataCompatibilityMap.put(DataType.INT, EnumSet.of(DataType.INT, DataType.SHORT, DataType.U_SHORT, DataType.BYTE, DataType.U_BYTE));
    	dataCompatibilityMap.put(DataType.SHORT, EnumSet.of(DataType.SHORT, DataType.BYTE, DataType.U_BYTE));
    	dataCompatibilityMap.put(DataType.BYTE, EnumSet.of(DataType.BYTE));
    	
    	dataCompatibilityMap.put(DataType.NON_NEG_INTEGER, EnumSet.of(DataType.NON_NEG_INTEGER, DataType.POS_INTEGER, DataType.U_LONG, DataType.U_INT, DataType.U_SHORT, DataType.U_BYTE));
    	dataCompatibilityMap.put(DataType.POS_INTEGER, EnumSet.of(DataType.POS_INTEGER));
    	dataCompatibilityMap.put(DataType.NON_POS_INTEGER, EnumSet.of(DataType.NON_POS_INTEGER, DataType.NEG_INTEGER));
    	dataCompatibilityMap.put(DataType.NEG_INTEGER, EnumSet.of(DataType.NEG_INTEGER));
    	
    	dataCompatibilityMap.put(DataType.U_LONG, EnumSet.of(DataType.U_LONG, DataType.U_INT, DataType.U_SHORT, DataType.U_BYTE));
        dataCompatibilityMap.put(DataType.U_INT, EnumSet.of(DataType.U_INT, DataType.U_SHORT, DataType.U_BYTE));
        dataCompatibilityMap.put(DataType.U_SHORT, EnumSet.of(DataType.U_SHORT, DataType.U_BYTE));
        dataCompatibilityMap.put(DataType.U_BYTE, EnumSet.of(DataType.U_BYTE));
        
        
        dataCompatibilityMap.put(DataType.STRING, EnumSet.allOf(DataType.class));
        dataCompatibilityMap.put(DataType.NORMALIZED_STRING, EnumSet.complementOf(EnumSet.of(DataType.STRING, DataType.DATE, DataType.TIME, DataType.DATE_TIME)));
        
        dataCompatibilityMap.put(DataType.DATE, EnumSet.of(DataType.DATE,DataType.DATE_TIME));
        dataCompatibilityMap.put(DataType.TIME, EnumSet.of(DataType.TIME, DataType.DATE_TIME));
        dataCompatibilityMap.put(DataType.DATE_TIME, EnumSet.of(DataType.DATE_TIME));
        
        dataCompatibilityMap.put(DataType.OTHER, EnumSet.of(DataType.OTHER));
    }
	public boolean isDataTypeCompatible(SchemaNode otherSchemaNode) {
		if (this.dataType == DataType.NONE) {
			return this.dataType.equals(otherSchemaNode.getDataType());
		} else if (this.dataType == DataType.OTHER && otherSchemaNode.getDataType() == DataType.OTHER) {
			return this.dataType.getDescription().equals(otherSchemaNode.getDataType().getDescription());
		} else {
			DTLogger.logger.fine(String.format("%s | %s | %s", this.dataType, otherSchemaNode.getDataType(), dataCompatibilityMap.get(this.dataType).contains(otherSchemaNode.getDataType())));
			return dataCompatibilityMap.get(this.dataType).contains(otherSchemaNode.getDataType());
		}
	}
	
	private Integer min = 1;
    public Integer getMin() {
		return min;
	}
	public void setMin(Integer min) {
		this.min = min;
	}
	public Integer getEffectiveMin() {
		Integer effectiveMin = 1;
	    for (SchemaNode node = this; node != null; node = node.getNamedParent()) {
	        if (node.getName() != null && node.getMin() != null) {
	        	effectiveMin *= node.getMin();
	        }
	    }
		return effectiveMin;
	}
	
	private Integer max = 1;
	public Integer getMax() {
		return max;
	}
	public void setMax(Integer max) {
		this.max = max;
	}
	public Integer getEffectiveMax() {
		Integer effectiveMax = 1;
	    for (SchemaNode node = this; node != null; node = node.getNamedParent()) {
	        if (node.getName() != null && node.getMax() != null) {
	        	effectiveMax *= node.getMax();
	        }
	    }
		return effectiveMax;
	}
	
	public boolean isFulfillmentCompatible(SchemaNode otherSchemaNode) {
		if (otherSchemaNode.getEffectiveMin() >= this.getEffectiveMin()) {
			return true;
		} else {
			return false;
		}
	}
	public Integer computeObligatoryFulfillment(SchemaNode otherSchemaNode) {
		if (this.getMin() > 0 && otherSchemaNode.getMin() >= this.getMin()) {
			return this.getMin();
		} else {
			return 0;
		}
	}
	public Integer computeOptionalFulfillment(SchemaNode otherSchemaNode) {
		if (otherSchemaNode.getMin() >= this.getMin()) {
			if (otherSchemaNode.getMin() <= this.getMax()) {
				return otherSchemaNode.getMin() - this.getMin();
			} else {
				return this.getMax() - this.getMin();
			}
		} else {
			return 0;
		}
	}
	
	public boolean isList() {
		return (Math.max(this.getMin(), this.getMax()) > 1) ? true : false;
	}
	
    private Integer group = 0;
	public Integer getGroup() {
		return group;
	}
	public void setGroup(Integer group) {
		this.group = group;
	}
	
	private Boolean virtual = false;
	public Boolean isVirtual() {
		return virtual;
	}
	public void setVirtual(Boolean virtual) {
		this.virtual = virtual;
	}
	
	public SemanticComparator semanticComparator;
	public void setSemanticComparator(SemanticComparator semanticComparator) {
		this.semanticComparator = semanticComparator;
	}
	
	private List<SchemaNodeOption> schemaNodeOptions = new ArrayList<SchemaNodeOption>();
	public List<SchemaNodeOption> getSchemaNodeOptions() {
		return this.schemaNodeOptions;
	}
	public void addSchemaNodeOption(SchemaNodeOption option) {
		schemaNodeOptions.add(option);
	}
	public boolean hasSemanticAnnotations() {
		for (SchemaNodeOption schemaNodeOption : this.getSchemaNodeOptions()) {
			SchemaModelReference modelReference = schemaNodeOption.getModelReference();
			if (modelReference != null) {
				return true;
			}
		}
		return false;
	}
	//public enum MatchType { NONE, MODEL_REFERENCE, CONSTANT }
	public boolean hasSemanticAnnotationCompatible(SchemaNode otherNode) {
		Integer i = 0;
		for (SchemaNodeOption thisSchemaNodeOption : this.getSchemaNodeOptions()) {
			SchemaNodeOption otherSchemaNodeOption = otherNode.getSchemaNodeOptions().getFirst();
			if (thisSchemaNodeOption.getModelReference() != null && thisSchemaNodeOption.getModelReference().isSemanticAnnotationCompatible(otherSchemaNodeOption.getModelReference())) {
				this.matchOptionIndex = i;
				return true;
			}
			i += 1;
		}
		return false;
	}
	
	private SchemaNode parent;
	private SchemaNode namedParent;
	public SchemaNode getParent() {
		return parent;
	}
	public SchemaNode getNamedParent() {
		return namedParent;
	}
	public void setParent(SchemaNode parent) {
		this.parent = parent;	
		this.updateNamedParent();
	}
	public void updateNamedParent() {
		SchemaNode parentNode = this.getParent();
		while (parentNode != null && parentNode.getName() == null) {
			parentNode = parentNode.getParent();
		}
		this.namedParent = parentNode;
	}
	
    private List<SchemaNode> children;    
    public List<SchemaNode> getChildren() {
		return children;
	}
    public void addChild(SchemaNode child) {
    	if (!this.children.contains(child)) {
    		this.children.add(child);
    	}
	}
	public void removeChild(SchemaNode child) {
		this.children.remove(child);
	}
	
    private SchemaNode match;
    public SchemaNode getMatch() {
		return match;
	}
    public void setMatch(SchemaNode match) {
		this.match = match;
	}
    
	private Integer matchOptionIndex;
    public void setMatchOptionIndex(Integer matchOptionIndex) {
		this.matchOptionIndex = matchOptionIndex;
	}
	public Integer getMatchOptionIndex() {
		return matchOptionIndex;
	}

	private List<SchemaNode> links;    
    public List<SchemaNode> getLinks() {
		return links;
	}
    public void addLink(SchemaNode link) {
    	if (!this.links.contains(link)) {
    		this.links.add(link);
    	}
	}
	public void removeLink(SchemaNode link) {
		this.links.remove(link);
	}
	
    public SchemaNode() {
		super();
		
		this.ID = UUID.randomUUID();
		
		this.children = new ArrayList<SchemaNode>();
		this.links = new ArrayList<SchemaNode>();
	}
    
	public ArrayList<SchemaNode> getPathNodes() {
    	ArrayList<SchemaNode> path = new ArrayList<SchemaNode>();

    	SchemaNode node = this;  	

    	while (node != null) {
    		if (node.name != null || path.size() == 0) {
    			path.add(0, node);
    		}
            node = node.parent;
        }
        
		return path;
    }
    public ArrayList<String> getPathNames() {
    	ArrayList<String> path = new ArrayList<String>();

    	SchemaNode node = this;  	

    	while (node != null) {
    		if (node.name != null) {
    			//if (node.getType() == Type.ATTRIBUTE) { // aviso, @ a aparecer no translator
        			//path.add(0, "@" + node.name);
    			//} else {
        			path.add(0, node.name);
    			//}
    		}
            node = node.parent;
        }
        
		return path;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final SchemaNode other = (SchemaNode) obj;
        if (!this.ID.equals(other.getID())) {
            return false;
        }

        return true;
    }
    
    @Override
    public int hashCode() {
        return this.ID.hashCode();
    }
    
    public Map<String, Object> toMap() {
    	Map<String, Object> variables = new HashMap<>();

        Optional.ofNullable(name).ifPresent(ref -> variables.put("name", ref));
        Optional.ofNullable(mdiID).ifPresent(ref -> variables.put("mdi-id", ref));
        Optional.ofNullable(type).ifPresent(ref -> variables.put("type", ref));
        Optional.ofNullable(dataType).ifPresent(ref -> variables.put("data-type", ref));
        Optional.ofNullable(min).ifPresent(ref -> variables.put("min", ref));
        Optional.ofNullable(max).ifPresent(ref -> variables.put("max", ref));
        Optional.ofNullable(schemaNodeOptions).ifPresent(ref -> variables.put("options", ref));
        Optional.ofNullable(match).ifPresent(ref -> variables.put("match", ref));
        Optional.ofNullable(links).ifPresent(ref -> variables.put("links", ref));
        Optional.ofNullable(group).ifPresent(ref -> variables.put("group", ref));
        Optional.ofNullable(dependsOn).ifPresent(ref -> variables.put("depends-on", ref));
        
        if (!this.getPathNames().isEmpty()) variables.put("path", String.join("/", this.getPathNames()));
        
        variables.put("parent", (this.parent) != null ? 1 : 0);
        variables.put("named-parent", (this.namedParent) != null ? 1 : 0);
        variables.put("children", (this.children) != null ? this.children.size() : 0);
        
        return variables;
    }
    
    @Override
    public String toString() {
        return this.toMap().toString();
    }
    
    
}
