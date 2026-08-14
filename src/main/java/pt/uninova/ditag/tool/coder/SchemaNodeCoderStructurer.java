package pt.uninova.ditag.tool.coder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pt.uninova.ditag.tool.node.SchemaNode;
import pt.uninova.ditag.tool.node.SchemaNode.Type;
import pt.uninova.ditag.tool.utils.StringMatrix;

public class SchemaNodeCoderStructurer {

	SchemaNode node;
	
	String rootName;
	Boolean forMatch;
	
	StringMatrix conversionTable;
	
	private static final List<String> FORBIDDEN = Arrays.asList("package", "Package");
	
	public SchemaNodeCoderStructurer(SchemaNode node, String rootName, Boolean forMatch) {
		super();
		this.node = node;
		this.rootName = rootName;
		this.forMatch = forMatch;
		this.conversionTable = SchemaNodeCoderConversion.getConversionTable();
	}
	
	private SchemaNode getAncestorAtLevel(int level) {
	    SchemaNode current = this.node;
	    for (int i = 0; i < level && current != null; i++) {
	        current = current.getNamedParent();
	    }
	    return current;
	}
	
	public String getName() {
		String aux = Pattern.compile("(?<=\\d|_)([a-zA-Z])").matcher(node.getName()).replaceAll(match -> match.group().toUpperCase());
		return this.checkForbiddenName(aux.substring(0, 1).toLowerCase() + aux.substring(1));
	}
	public String getParentName(int level) {
	    SchemaNode ancestor = this.getAncestorAtLevel(level);
	    if (ancestor != null) {
	        return this.checkForbiddenName(ancestor.getName());
	    }
	    return null;
	}
	private String checkForbiddenName(String name) {
		if (FORBIDDEN.contains(name)) {
			return "_" + name;
		} 
		return name;
	}
	
	public ArrayList<String> getPathArray() {
		return this.getCleanPathArray(this.node);
	}
	public ArrayList<String> getParentPathArray(int level) {
	    SchemaNode ancestor = this.getAncestorAtLevel(level);
	    return this.getCleanPathArray(ancestor);
	}
	private ArrayList<String> getCleanPathArray(SchemaNode node) {
		if (node != null) {
			ArrayList<String> path = node.getPathNames();
			if (path.size() > 0) { 
				path.remove(0);
				path.add(0, rootName); // Replace First Item w/ "consumer" or "provider"
			}
			if (this.forMatch) {
				if (!path.isEmpty() && path.size() > 1) {  
					path.remove(path.size() - 1); // Delete Last Item of Path
		        }
			}
			for (int i = 0; i < path.size(); i++) {
	            path.set(i, path.get(i).replace("_", ""));
	        }
		return path;
		}
		return new ArrayList<String>();
	}
	
	private ArrayList<String> getPathWithCorrectedCapitals(ArrayList<String> path) {
		for (int j = 0; j < path.size(); j++) {
		    String item = path.get(j);

		    if (item.equals(item.toUpperCase())) {
		        path.set(j, item.toLowerCase());
		    } else if (item.length() <= 2) {
		        path.set(j, item.toLowerCase());
		    } else {
		        StringBuilder newItem = new StringBuilder(item.length());
		        
		        boolean stop = false;
		        for (int i = 0; i < item.length(); i++) {
		            char c = item.charAt(i);
		            if (i == 0) {
		                c = Character.toLowerCase(c);
		            } else if (!stop && i + 1 < item.length() && Character.isUpperCase(item.charAt(i + 1))) {
		                c = Character.toLowerCase(c);
		            } else {
		                if (i == item.length() - 1 && Character.isUpperCase(c)) {
		                    stop = true;
		                } else if (i == item.length() - 1) {
		                    c = Character.toLowerCase(c);
		                }
		                stop = true;
		            }
		            newItem.append(c);
		        }

		        path.set(j, newItem.toString());
		    }
		}
		
		path.replaceAll(item -> FORBIDDEN.contains(item) ? "_" + item : item);
		return path;
	}
	public String getPath() {
		return String.join(".", this.getPathWithCorrectedCapitals(this.getPathArray()));
	}
	public String getParentPath(int level) {
	    return String.join(".", this.getPathWithCorrectedCapitals(this.getParentPathArray(level)));
	}

	public String getCapitalPath() {
		ArrayList<String> path = this.getPathArray();
		return String.join(".", path.stream()
      			.map(str -> str.substring(0, 1).toUpperCase() + str.substring(1))
      			.collect(Collectors.toList()));
	}
	public String getParentCapitalPath(int level) {
	    ArrayList<String> path = this.getParentPathArray(level);
	    return String.join(".", path.stream()
	            .map(str -> str.substring(0, 1).toUpperCase() + str.substring(1))
	            .collect(Collectors.toList()));
	}
	
	private String getCleanMethodName(String name) {
		String aux = "";
		if (name != null) {
			aux = Pattern.compile("(?<=\\d|_)([a-zA-Z])").matcher(name).replaceAll(match -> match.group().toUpperCase()).replace("_", "");
			aux = Character.toUpperCase(aux.charAt(0)) + aux.substring(1);
		}
		return aux;
	}
	public String getMethodName() {
		if (node.getType() == SchemaNode.Type.EXTENSION) {
			return "Value";
		} else {
			return this.getCleanMethodName(this.node.getName());
		}
	}
	public String getParentMethodName(int level) {
	    SchemaNode ancestor = this.getAncestorAtLevel(level);
	    if (ancestor != null) {
	        return this.getCleanMethodName(ancestor.getName());
	    }
	    return null;
	}
	
	public String getInnerVariableName() {
		return String.join(".", String.join("_" + this.rootName.charAt(0) + "_", this.getPathArray()));
	}
	public String getParentInnerVariableName(int level) {
	    ArrayList<String> path = this.getParentPathArray(level);
	    return String.join(".", String.join("_" + this.rootName.charAt(0) + "_", path));
	}

	public Boolean isAttributeOfElement() {//aviso adicionamos this.node.getType() != null
		if (this.node != null &&  this.node.getType() != null && this.node.getType().equals(Type.ELEMENT) &&
				this.node.getChildren().stream()
			        .filter(child -> child.getType().equals(Type.COMPLEX_TYPE))
			        .anyMatch(complexChild -> complexChild.getChildren().stream()
			            .filter(grandChild -> grandChild.getType().equals(Type.SIMPLE_CONTENT)) 
			            .anyMatch(simpleContentChild -> simpleContentChild.getChildren().stream()
			                .anyMatch(greatGrandChild -> greatGrandChild.getType().equals(Type.EXTENSION))))) { 
			return true;
		}
		return false;
	}
}
