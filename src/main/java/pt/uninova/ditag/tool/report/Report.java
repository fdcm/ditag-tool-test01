package pt.uninova.ditag.tool.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;

import pt.uninova.ditag.tool.node.SchemaNode;
import pt.uninova.ditag.tool.node.SchemaNodeOption;
import pt.uninova.ditag.tool.schema.SchemaTree;

public class Report {

	ExitCode exitCode = ExitCode.DEFAULT;
	public ExitCode getExitCode() {
        return exitCode;
    }
    public void setExitCode(ExitCode exitCode) {
        this.exitCode = exitCode;
    }
	public enum ExitCode {
	    SUCCESS(0, "Operation Completed Successfully"),
	    ERROR_UNKNOWN(1, "Unknown Error Occurred"),
	    INVALID_INPUT(2, "Invalid Input Parameter"),
	    ONTOLOGY_MISMATCH(3, "Consumer and Provider Ontologies are Different"),
	    FILE_NOT_REACHABLE(4, "File Download Failed"),
	    NO_MATCH(5, "No Match Found Between Consumer and Provider"),
		IMPROPER_ARGUMENTS(6, "Argument Options Not Supported"),
		INPUT_NOT_VALID(6, "Input Format Not Supported"),
		PELLET_NOT_AVAILABLE(7, "Required Pellet/Jena classes are not available. Please uncomment imports and logic from Ontology and PelletSemanticReasoner classes, as well as the Pellet dependency declaration in the pom.xml file."),
		DEFAULT(-1, "Default Exit Code");
		
	    private final int code;
	    private final String message;

	    ExitCode(int code, String message) {
	        this.code = code;
	        this.message = message;
	    }

	    public int getCode() {
	        return code;
	    }

	    public String getMessage() {
	        return message;
	    }
	    
	    public Map<String, Object> toMap() {
	    	return Map.of(
	    	        "code", this.getCode(),
	    	        "message", this.getMessage()
	    	    );
	    }
	}

	private SchemaTree consumerTree;
	public SchemaTree getConsumerTree() {
		return consumerTree;
	}
	public void setConsumerTree(SchemaTree consumerTree) {
		this.consumerTree = consumerTree;
		
		Iterator<SchemaNode> iterator = this.consumerTree.iterator();
	    while (iterator.hasNext()) {
	        SchemaNode node = iterator.next();
	        if (!node.getSchemaNodeOptions().isEmpty()) {
	        	consumerElements.add(node.toMap());
	        }
	    }
	}

	private SchemaTree providerTree;
	public SchemaTree getProviderTree() {
		return providerTree;
	}
	public void setProviderTree(SchemaTree providerTree) {
		this.providerTree = providerTree;
		
		Iterator<SchemaNode> iterator = this.providerTree.iterator();
	    while (iterator.hasNext()) {
	        SchemaNode node = iterator.next();
	        if (!node.getSchemaNodeOptions().isEmpty()) {
	        	providerElements.add(node.toMap());
	        }
	    }
	}
	
	private List<Map<String, Object>> consumerElements = new ArrayList<Map<String, Object>>();
	public List<Map<String, Object>> getConsumerElements() {
		return consumerElements;
	}

	private List<Map<String, Object>> providerElements = new ArrayList<Map<String, Object>>();
	public List<Map<String, Object>> getProviderElements() {
		return providerElements;
	}
	
	private List<Map<String, Object>> combinations;
	private Map<String, Object> match;
	public List<Map<String, Object>> getCombinations() {
		return combinations;
	}
	public Map<String, Object> getMatch() {
		return match;
	}
	@SuppressWarnings("serial")
	public void setCombinations(List<Map<String, Object>> combinations) {
		this.combinations = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < combinations.size(); i++) {
			Map <String, Object> aux = new HashMap<String, Object>();
			
			@SuppressWarnings("unchecked")
			List<Pair<SchemaNode,SchemaNode>> obligatoryMatches = (List<Pair<SchemaNode, SchemaNode>>) combinations.get(i).get("obligatory-annotation-pairs");
			@SuppressWarnings("unchecked")
			List<Pair<SchemaNode,SchemaNode>> optionalMatches = (List<Pair<SchemaNode, SchemaNode>>) combinations.get(i).get("optional-annotation-pairs");

			
			aux.put("obligatory-matches", 
		            Optional.ofNullable(obligatoryMatches).orElse(List.of())
		                .stream().map(p -> p.getLeft().getName() + " - " + p.getRight().getName())
		                .toList());

		    aux.put("optional-matches", 
		            Optional.ofNullable(optionalMatches).orElse(List.of())
		                .stream().map(p -> p.getLeft().getName() + " - " + p.getRight().getName())
		                .toList());
	
			List<Map<String, String>> auxPairs = new ArrayList<Map<String, String>>();
		
			List<Pair<SchemaNode,SchemaNode>> pairs = new ArrayList<Pair<SchemaNode,SchemaNode>>();
			if (obligatoryMatches != null) {
				pairs.addAll(obligatoryMatches);
			}
			if (optionalMatches != null) {
				pairs.addAll(optionalMatches);
			}
			
			for (Pair<SchemaNode,SchemaNode> pair : pairs) {
				auxPairs.add(new HashMap<String, String>() {{
			        put("provider", String.join("/", pair.getRight().getPathNames()));
			        put("consumer", String.join("/", pair.getLeft().getPathNames()));
			    }});
			}
			
			aux.put("pairs", auxPairs);
			
			if (i == 0) {
				this.match = aux;
			}
			this.combinations.add(aux);
		}
	}
	
	private List<Map<String, String>> tryLog = new ArrayList<Map<String, String>>();
	public List<Map<String, String>> getTryLog() {
		return tryLog;
	}
	public void setTryLog(List<Triple <String, String, Boolean>> tryLog) {        
        for (Triple<String, String, Boolean> log : tryLog) {
        	//if (log.getRight()) 
        	{
	            Map<String, String> logMap = new HashMap<>();
	            logMap.put("consumer", log.getLeft());
	            logMap.put("provider", log.getMiddle());
	            logMap.put("compatible", log.getRight().toString());
	            this.tryLog.add(logMap);
        	}
        }
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> toMap() {
		Map<String, Object> report = new HashMap<String, Object>();
		
		report.put("response", this.exitCode.toMap());
		
		if (this.exitCode == ExitCode.SUCCESS) {
			report.put("matches", this.getCombinations());
			report.put("numberOfMatches", ((List<Object>) this.getCombinations().get(0).get("obligatory-matches")).size() +
					((List<Object>) this.getCombinations().get(0).get("optional-matches")).size());
			report.put("match", this.getMatch());
		}
		
		report.put("tryLog", this.getTryLog());
		report.put("numberOfTries", this.getTryLog().size());

		return report;
	}
	
	public JSONObject toJSONObject() {
	    return new JSONObject(this.toMap());
	}
}	
