package pt.uninova.ditag.tool.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jdom2.Document;
import org.jdom2.Element;

import pt.uninova.ditag.tool.DTLogger;
import pt.uninova.ditag.tool.node.SchemaNode;
import pt.uninova.ditag.tool.node.SchemaNodePair;
import pt.uninova.ditag.tool.node.SchemaNode.DataType;
import pt.uninova.ditag.tool.node.SchemaNodeOption;
import pt.uninova.ditag.tool.semantics.SemanticComparator;

public class SchemaTree implements Iterable<SchemaNode> {
	
	private SchemaNode root;
	public SchemaNode getRoot() {
		return root;
	}
	
	private Map<String, String> csvMappings = new HashMap<>();
	public Map<String, String> getCSVMappings() {
		return this.csvMappings;
	}
	public void parseCSVMappings(Document xsdDocument) {
        csvMappings = new HashMap<>();

        List<Element> csvColumnElements = SchemaProcessor.getElements(xsdDocument, new ArrayList<>(Arrays.asList("csvColumn")));  
        for (Element element : csvColumnElements) {
            String name = element.getAttribute("name").getValue();
            String path = element.getAttribute("path").getValue();

            this.csvMappings.put(name, path);
        }        
	}
	
	public SchemaTree(String rootName) {
        this.root = new SchemaNode();
        this.root.setName(rootName);
    }
	
	public SchemaTree(Document xsdDocument, SemanticComparator semanticComparator) {
        this.root = SchemaTreeBuilder.buildTree(xsdDocument, semanticComparator);

        Iterator<SchemaNode> iterator = this.iterator();
        while (iterator.hasNext()) {
	        SchemaNode node = iterator.next();
	        node.updateNamedParent();
	    }
        
        this.parseCSVMappings(xsdDocument);
    }
	
	public List<SchemaNode> findNodes(String name, Object options, SchemaNode.Type type, SchemaNode.DataType dataType, Integer min, Integer max) {
        List<SchemaNode> nodes = new ArrayList<>();
        findNodes(root, name, options, type, dataType, min, max, nodes);
        return nodes;
    }
	private void findNodes(SchemaNode node, String name, Object options, SchemaNode.Type type, SchemaNode.DataType dataType, Integer min, Integer max, List<SchemaNode> nodes) {
	    if (matchNode(node, name, options, type, dataType, min, max)) {
	    	nodes.add(node);
	    }
	    
	    if (node.getChildren() != null) {
	        node.getChildren().forEach(child -> findNodes(child, name, options, type, dataType, min, max, nodes));
	    }
	}
	
	public List<SchemaNode> findLeaves(String name, Object options, SchemaNode.Type type, SchemaNode.DataType dataType, Integer min, Integer max) {
	    List<SchemaNode> leaves = new ArrayList<>();
	    findLeaves(root, name, options, type, dataType, min, max, leaves);
	    return leaves;
	}
	private void findLeaves(SchemaNode node, String name, Object options, SchemaNode.Type type, SchemaNode.DataType dataType, Integer min, Integer max, List<SchemaNode> leaves) {
	    if (node.getChildren() == null || node.getChildren().isEmpty()) {
	        if (matchNode(node, name, options, type, dataType, min, max)) {
	            leaves.add(node);
	        }
	    } else {
	    	node.getChildren().forEach(child -> findLeaves(child, name, options, type, dataType, min, max, leaves));
	    }
	}
	
	private boolean matchNode(SchemaNode node, String name, Object options, SchemaNode.Type type, SchemaNode.DataType dataType, Integer min, Integer max) {
		return (name == null || node.getName() == null || node.getName().equals(name))
	            && (options == null || node.getSchemaNodeOptions() == null || node.getSchemaNodeOptions().equals(options))
	            && (type == null || node.getType() == null || node.getType().equals(type))
	            && (dataType == null || node.getDataType() == null || node.getDataType().equals(type))
	            && (min == null || node.getMin() == null || node.getMin().equals(min))
	            && (max == null || node.getMax() == null || node.getMax().equals(max));
	}
	
	@SuppressWarnings("unchecked")
	public Pair<List<Map<String, Object>>, List<Triple<String, String, Boolean>>> match(SchemaTree otherSchemaTree, boolean returnCombinations, boolean returnFirst) throws Exception {
		Map<Integer, List<SchemaNode>> consumerNodesDict = new HashMap<>();
        Map<Integer, List<SchemaNode>> providerNodesDict = new HashMap<>();
		
	    Iterator<SchemaNode> consumerIterator = this.iterator();
	    Integer auxGroupMiss = -1;
	    while (consumerIterator.hasNext()) {
	    	SchemaNode node = consumerIterator.next();
	    	Integer auxGroudId = Integer.MIN_VALUE;
		    
	        if (!node.getSchemaNodeOptions().isEmpty() && node.getName() != null) {
        		auxGroudId = node.getGroup();
                if (!consumerNodesDict.containsKey(auxGroudId)) {
                	consumerNodesDict.put(auxGroudId, new ArrayList<SchemaNode>());
                }
                if (!consumerNodesDict.get(auxGroudId).contains(node)) {
                	consumerNodesDict.get(auxGroudId).add(node);
                }
	        }
	    }
	    List<Pair<Integer, ArrayList<SchemaNode>>> consumerNodeListList = consumerNodesDict.entrySet().stream()
	    	    .map(entry -> {
	    	        List<SchemaNode> sortedNodes = entry.getValue().stream()
	    	            .sorted(Comparator
	    	            	.comparing(SchemaNode::getMin, Comparator.reverseOrder())
	    	            	.thenComparing((SchemaNode n) -> n.getDependsOn() != null)
	    	                .thenComparing(SchemaNode::getMax, Comparator.reverseOrder())) 
	    	            .collect(Collectors.toList());

	    	        return Pair.of(entry.getKey(), new ArrayList<>(sortedNodes));
	    	    })
	    	    .sorted(Comparator.comparing(
	    	        entry -> entry.getValue().stream().mapToInt(n -> n.getMin() + n.getMax()).sum(),
	    	        Comparator.reverseOrder()))
	    	    .collect(Collectors.toList());
	    	    Iterator<SchemaNode> providerIterator = otherSchemaTree.iterator();
	    DTLogger.logger.fine("consumerNodeListList: " + consumerNodeListList);
	    auxGroupMiss = -1;
	    while (providerIterator.hasNext()) {
	    	SchemaNode node = providerIterator.next();
	    	Integer auxGroudId = Integer.MIN_VALUE;
	        if (!node.getSchemaNodeOptions().isEmpty() && node.getName() != null) {
        		auxGroudId = node.getGroup();
                if (!providerNodesDict.containsKey(auxGroudId)) {
                	providerNodesDict.put(auxGroudId, new ArrayList<SchemaNode>());
                }
                if (!providerNodesDict.get(auxGroudId).contains(node)) {
                	providerNodesDict.get(auxGroudId).add(node);
                }     
	        }
	    }
	    List<Pair<Integer, ArrayList<SchemaNode>>> providerNodeListList = providerNodesDict.entrySet().stream()
	    	    .map(entry -> {
	    	        List<SchemaNode> sortedNodes = entry.getValue().stream()
	    	            .sorted(Comparator
	    	            	  .comparing(SchemaNode::getMin, Comparator.reverseOrder())
		    	              .thenComparing((SchemaNode n) -> n.getDependsOn() != null)
		    	              .thenComparing(SchemaNode::getMax, Comparator.reverseOrder())) 
	    	            .collect(Collectors.toList());

	    	        return Pair.of(entry.getKey(), new ArrayList<>(sortedNodes));
	    	    })
	    	    .sorted(Comparator.comparing(
	    	        entry -> entry.getValue().stream().mapToInt(n -> n.getMin() + n.getMax()).sum(),
	    	        Comparator.reverseOrder()))
	    	    .collect(Collectors.toList());
	    
	    DTLogger.logger.fine("providerNodeListList: " + providerNodeListList);
	    
	    List<Map<String,Object>> annotationCombinations = new ArrayList<Map<String,Object>>();
	    Integer providerGroupIndex = 0;
	    Integer consumerGroupIndex = 0;
	    List<Pair<Integer,Integer>> obligatoryGroupPairs = new ArrayList<>();
	    
	    List<Triple<String,String,Boolean>> tryLog = new  ArrayList<Triple<String,String,Boolean>>();
	    
	    boolean allObligatoryGroupsCompatible = this.matchObligatory(providerNodeListList, consumerNodeListList, obligatoryGroupPairs, annotationCombinations, tryLog);

		if (allObligatoryGroupsCompatible) {
			boolean allObligatoryDependentNodesompatible = true;
			if (allObligatoryDependentNodesompatible) { 
				this.matchOptional(providerNodeListList, consumerNodeListList, obligatoryGroupPairs, annotationCombinations, tryLog);
			}
		}
	    
        tryLog.sort(Comparator.comparing(Triple::getLeft));

        StringBuilder auxSB = new StringBuilder();
        auxSB.append(String.format("%-30s%-30s%-10s%n", "Category", "Field", "Value"));
        auxSB.append("-----------------------------------------------------------------\n");
        for (Triple<String, String, Boolean> row : tryLog) {
        	auxSB.append(String.format("%-30s%-30s%-10s%n",
                row.getLeft(),
                row.getMiddle(),
                row.getRight()
            ));
        }
        DTLogger.logger.fine(auxSB.toString().substring(0, auxSB.length() - 2));

	    if (allObligatoryGroupsCompatible && returnCombinations) {
	    	for (Map<String, Object> combination : annotationCombinations) {
	    	    if (combination.containsKey("obligatory-annotation-pairs")) {
	    	        List<Pair<SchemaNode, SchemaNode>> obligatoryPairs = (List<Pair<SchemaNode, SchemaNode>>) combination.get("obligatory-annotation-pairs");
	    	        for (Pair<SchemaNode, SchemaNode> pair : obligatoryPairs) {
	    	            this.matchPair(pair);
	    	        }
	    	    }

	    	    if (combination.containsKey("optional-annotation-pairs")) {
	    	        List<Pair<SchemaNode, SchemaNode>> optionalPairs = (List<Pair<SchemaNode, SchemaNode>>) combination.get("optional-annotation-pairs");
	    	        for (Pair<SchemaNode, SchemaNode> pair : optionalPairs) {
	    	        	this.matchPair(pair);
	    	        }
	    	    }
	    	}
	        return new MutablePair<List<Map<String,Object>>,List <Triple <String, String, Boolean>>>(annotationCombinations, tryLog);
	    } else {
	        return new MutablePair<List<Map<String,Object>>,List<Triple <String, String, Boolean>>>(new ArrayList<Map <String, Object>>(), tryLog);
	    }

	}
	
	private boolean matchObligatory(List<Pair<Integer, ArrayList<SchemaNode>>> providerNodeListList, 
			List<Pair<Integer, ArrayList<SchemaNode>>> consumerNodeListList,
			List<Pair<Integer,Integer>> obligatoryGroupPairs,
		    List<Map<String,Object>> annotationCombinations,
		    List<Triple<String,String,Boolean>> tryLog) {
		Integer providerGroupIndex = 0;
	    Integer consumerGroupIndex = 0;
	    boolean allObligatoryGroupsCompatible = false;

	    while (!allObligatoryGroupsCompatible && providerGroupIndex < providerNodeListList.size()) {
	    	while (!allObligatoryGroupsCompatible && providerGroupIndex < providerNodeListList.size()) {
	    		final Integer providerGroupIndexAux = providerGroupIndex;
	    		if (consumerNodeListList.get(consumerGroupIndex).getRight().get(0).getMin() == 0) {
	    			allObligatoryGroupsCompatible = true;
	    		} else if (obligatoryGroupPairs.stream().anyMatch(pair -> pair.getRight().equals(providerNodeListList.get(providerGroupIndexAux).getLeft()))) {
	    			DTLogger.logger.fine("group already occupied");
	    			providerGroupIndex++;
	    		} else {
	    			Integer providerAnnotationIndex = 0;
	    			Integer consumerAnnotationIndex = 0;
	    			boolean allAnnotationsCompatible = false;
	    			List<Pair<SchemaNode,SchemaNode>> annotationPairs = new ArrayList<>();
	    			
	    			DTLogger.logger.fine("checking if the groups are compatible");
	    			while (!allAnnotationsCompatible && providerAnnotationIndex < providerNodeListList.get(providerGroupIndex).getRight().size()) { // or the consumer is constant-dependent
	    				while (!allAnnotationsCompatible && providerAnnotationIndex < providerNodeListList.get(providerGroupIndex).getRight().size()) { // or the consumer is constant-dependent
	    					SchemaNode consumerNode = consumerNodeListList.get(consumerGroupIndex).getRight().get(consumerAnnotationIndex);
	    					
	    					DTLogger.logger.fine("size "+consumerNodeListList.get(consumerGroupIndex).getRight().size());
	    					boolean addPair = false;
							SchemaNode providerNode = null;
	    					if (consumerNode.getConstantOption() != null) { // check if the option is constant: if dependent, check using optionIndex; otherwise, check the first option
	    						DTLogger.logger.fine("DEPENDS_ON " + consumerNode.getName() + " " + consumerNode.getSchemaNodeOptions().get(consumerNode.getDependsOn().getMatchOptionIndex()).getValue());
	    						providerNode = new SchemaNode();
	    						providerNode.setName("virtual");
	    						providerNode.addSchemaNodeOption(new SchemaNodeOption());
	    						providerNode.getSchemaNodeOptions().getFirst().setValue(consumerNode.getConstantOption());
	    						providerNode.setDataType(consumerNode.getDataType());
	    						providerNode.setVirtual(true);
    							consumerNode.setMatchOptionIndex(consumerNode.getSchemaNodeOptions().size() - 1); // previously 0
    							DTLogger.logger.fine("constant");
    							addPair = true;
	    					}
	    					else {
		    					providerNode = providerNodeListList.get(providerGroupIndex).getRight().get(providerAnnotationIndex);
		    					
		    					DTLogger.logger.fine(providerNodeListList.get(providerGroupIndex).getRight().toString());
		    	    			DTLogger.logger.fine("\nnumRProv=" + providerNodeListList.get(providerGroupIndex).getRight().size());

		    		    		DTLogger.logger.fine("group_indexes(c,p)=(" + consumerGroupIndex + "," + providerGroupIndex + ")");
		    		    		DTLogger.logger.fine("annot_indexes(c,p)=(" + consumerAnnotationIndex + "," + providerAnnotationIndex + ")");
		    		    		DTLogger.logger.fine(consumerNode.getName() + " " + providerNode.getName());
		    		    		DTLogger.logger.fine(consumerNode.getPathNames() + " " + providerNode.getPathNames());
		    		    		
		    		    		SchemaNode providerNodeAux = providerNode;
		    					if (annotationPairs.stream().anyMatch(pair -> pair.getRight().equals(providerNodeAux))) {		    						
		    						DTLogger.logger.fine("annotation already occupied");
		    					} else if (this.isItemCompatible(consumerNode, providerNode)) {
		    							DTLogger.logger.fine("annotationCompatible");
		    							addPair = true;
		    					} 
		    					if (!addPair && (consumerNode.getSchemaNodeOptions().getLast().getValue() != null
		    							|| (providerAnnotationIndex+1 >= providerNodeListList.get(providerGroupIndex).getRight().size() &&
	    								consumerNode.getSchemaNodeOptions().getLast().getDefaultExpression() != null))) { //constant or default
	    					 		
		    						providerNode = new SchemaNode();
		    						providerNode.setName("virtual");
		    						providerNode.addSchemaNodeOption(new SchemaNodeOption());
		    						if (consumerNode.getSchemaNodeOptions().getLast().getValue() != null) {
		    							providerNode.getSchemaNodeOptions().getFirst().setValue(consumerNode.getSchemaNodeOptions().getLast().getValue());
		    							if (consumerNode.getSchemaNodeOptions().getLast().getValueExpression() != null) {
		    								consumerNode.setDataType(DataType.OTHER);
		    							}
		    						} else {
    		    										providerNode.getSchemaNodeOptions().getLast().setValue(consumerNode.getSchemaNodeOptions().getLast().evaluateDefaultExpression());
    		    						consumerNode.setDataType(DataType.OTHER);
		    						}
		    						providerNode.setDataType(consumerNode.getDataType());
		    						providerNode.setVirtual(true);
	    							consumerNode.setMatchOptionIndex(consumerNode.getSchemaNodeOptions().size() - 1);
	    							DTLogger.logger.fine("constant or default");
	    							addPair = true;
		    					} 
	    					}
	    					if (addPair) {
	    				        tryLog.add(new ImmutableTriple<>(consumerNode.getName(), providerNode.getName(), true));
	    						Pair<SchemaNode,SchemaNode> pair = new MutablePair<>(consumerNode, providerNode);
	    						annotationPairs.add(pair);
	    						providerAnnotationIndex = 0;
	    						consumerAnnotationIndex++;
	    						
	    						// check if a pair was found for all annotations or for all required annotations
	    						if (consumerAnnotationIndex >= consumerNodeListList.get(consumerGroupIndex).getRight().size() || consumerNodeListList.get(consumerGroupIndex).getRight().get(consumerAnnotationIndex).getMin() == 0) {

		    						DTLogger.logger.fine("allObligatoryAnnotationsCompatible");
	    							allAnnotationsCompatible = true;
	    							
	    							Map<String,Object> map = new HashMap<String,Object>();
	    							map.put("obligatory-annotation-pairs", annotationPairs);
	    						    annotationCombinations.add(map);
	    														
	    							obligatoryGroupPairs.add(
	    									new MutablePair<>(
	    											consumerNodeListList.get(consumerGroupIndex).getLeft(), 
	    											providerNodeListList.get(providerGroupIndex).getLeft()
	    											)
	    									);
	    							DTLogger.logger.fine(consumerNodeListList.toString());
	    							providerGroupIndex = 0;
	    						    consumerGroupIndex++;
	    						    
	    						    // check if a pair was found for all groups or for all required groups
	    						    if (consumerGroupIndex >= consumerNodeListList.size() || consumerNodeListList.get(consumerGroupIndex).getRight().get(0).getMin() == 0) {
	    						    	allObligatoryGroupsCompatible = true;
	    								
	    								DTLogger.logger.fine("allObligatoryGroupsCompatible");
	    							}
	    						}
		    							
	    					} else {
	    				        tryLog.add(new ImmutableTriple<>(consumerNode.getName(), providerNode.getName(), false));

	    						providerAnnotationIndex++;
    							DTLogger.logger.fine("incompatible annotation");
	    					}
	    				}


						DTLogger.logger.fine("if it reached here, either (all annotations are compatible) OR (all provider annotations have been tested without success");    				
	    				if (!allAnnotationsCompatible) {
	    					if (consumerAnnotationIndex > 0 && !annotationPairs.getLast().getRight().isVirtual()) { // only removes pair if the provider is not virtual
	    						DTLogger.logger.fine("remove one pair");
	    						DTLogger.logger.fine("annotationPairs.size()="+annotationPairs.size());
	    						DTLogger.logger.fine("indexOf="+providerNodeListList.get(providerGroupIndex).getRight().indexOf(annotationPairs.get(annotationPairs.size() - 1).getRight()));
	    						providerAnnotationIndex = providerNodeListList.get(providerGroupIndex).getRight().indexOf(annotationPairs.get(annotationPairs.size() - 1).getRight()) + 1;
	    						annotationPairs.remove(annotationPairs.size() - 1);
	    						consumerAnnotationIndex--;
	    						DTLogger.logger.fine("given the previous group pairs, no match can be found for this annotation. Undo the previous pair and try to match the remaining ones");
	    					}
	    					else {
	    						DTLogger.logger.fine("unable to find pairs for all required annotations");
	    					}
	    				}
	    			}
	    			
	    			if (!allAnnotationsCompatible) {
	    				providerGroupIndex++;
						DTLogger.logger.fine("incompatible group");
	    			}
	    		}
	    	}
			DTLogger.logger.fine("if it reached here, either all groups are compatible or all provider groups have been tested");    				
	    	if (!allObligatoryGroupsCompatible) {
	    		if (consumerGroupIndex > 0) {
	    			providerGroupIndex = IntStream.range(0, providerNodeListList.size())
	    				    .filter(i -> providerNodeListList.get(i).getLeft() == obligatoryGroupPairs.get(obligatoryGroupPairs.size() - 1).getRight())
	    				    .findFirst()
	    				    .orElse(-1) + 1;
	    			obligatoryGroupPairs.remove(obligatoryGroupPairs.size() - 1);
	    			consumerGroupIndex--;	    			
	    			annotationCombinations.removeLast();
	    			
					DTLogger.logger.fine("given the previous group pairs, a match cannot be found for this group. Reverting the previous pair and trying to match the remaining ones");
	    		}
	    		else {
					DTLogger.logger.fine("unable to find pairs for all required groups");
	    		}
	    	}
	    }
	    return allObligatoryGroupsCompatible;
	}
	
	private void matchOptional(List<Pair<Integer, ArrayList<SchemaNode>>> providerNodeListList, 
			List<Pair<Integer, ArrayList<SchemaNode>>> consumerNodeListList,
			List<Pair<Integer,Integer>> obligatoryGroupPairs,
		    List<Map<String,Object>> annotationCombinations,
		    List<Triple<String,String,Boolean>> tryLog) {
		Integer providerGroupIndex = 0;
	    Integer consumerGroupIndex = 0;
	    
	    DTLogger.logger.fine("handling optional items for required groups");

	    // for each required group pair, analyze optional annotations
    	for (Pair<Integer, Integer> groupPair: obligatoryGroupPairs) {
			List<Pair<SchemaNode,SchemaNode>> annotationPairs = new ArrayList<>();
			
		    providerGroupIndex = IntStream.range(0, providerNodeListList.size())
		    	    .filter(i -> providerNodeListList.get(i).getLeft().equals(groupPair.getRight()))
		    	    .findFirst()
		    	    .orElse(-1);

		    consumerGroupIndex = IntStream.range(0, consumerNodeListList.size())
		    	    .filter(i -> consumerNodeListList.get(i).getLeft().equals(groupPair.getLeft()))
		    	    .findFirst()
		    	    .orElse(-1);
		    
		    // for each optional annotation (consumerNode) in this group, look for an available annotation in the provider's group
			Integer providerAnnotationIndex = 0;
			Integer consumerAnnotationIndex = 0;
			boolean compatibleAnnotationFound = false;
		    for (consumerAnnotationIndex = 0; consumerAnnotationIndex < consumerNodeListList.get(consumerGroupIndex).getRight().size(); consumerAnnotationIndex++) { // for each consumer annotation
	    		SchemaNode consumerNode = consumerNodeListList.get(consumerGroupIndex).getRight().get(consumerAnnotationIndex);
	    		if (consumerNode.getMin() > 0) {
    				continue;
				}
	    		
				boolean addPair = false;
				SchemaNode providerNode = null;
				if (consumerNode.getConstantOption() != null) {
					DTLogger.logger.fine("DEPENDS_ON " + consumerNode.getName() + " " + consumerNode.getSchemaNodeOptions().get(consumerNode.getDependsOn().getMatchOptionIndex()).getValue());
					providerNode = new SchemaNode();
					providerNode.setName("virtual");
					providerNode.addSchemaNodeOption(new SchemaNodeOption());
					providerNode.getSchemaNodeOptions().getFirst().setValue(consumerNode.getConstantOption());
					providerNode.setDataType(consumerNode.getDataType());
					providerNode.setVirtual(true);
					consumerNode.setMatchOptionIndex(consumerNode.getSchemaNodeOptions().size() - 1); // previously 0
					DTLogger.logger.fine("opAnnot - constant");
					addPair = true;
				}
				else {
					for (providerAnnotationIndex = 0; providerAnnotationIndex < providerNodeListList.get(providerGroupIndex).getRight().size(); providerAnnotationIndex++) { // for each provider annotation
						providerNode = providerNodeListList.get(providerGroupIndex).getRight().get(providerAnnotationIndex);
						
			    		DTLogger.logger.fine("\nopAnnot - group_indexes(c,p)=(" + consumerGroupIndex + "," + providerGroupIndex + ")");
			    		DTLogger.logger.fine("opAnnot - annot_indexes(c,p)=(" + consumerNodeListList.get(consumerGroupIndex).getRight().indexOf(consumerNode) + "," + providerAnnotationIndex + ")");
			    		DTLogger.logger.fine(consumerNode.getName() + " " + providerNode.getName());	    	
			    		DTLogger.logger.fine(consumerNode.getPathNames() + " " + providerNode.getPathNames());

			    		SchemaNode providerNodeAux = providerNode;
			    		if (annotationPairs.stream().anyMatch(pair -> pair.getRight().equals(providerNodeAux)) ||
								annotationCombinations.stream()
						    	    .map(combinationMap -> combinationMap.get("obligatory-annotation-pairs")) 
						    	    .filter(Objects::nonNull)
						    	    .flatMap(list -> ((List<Pair<SchemaNode, SchemaNode>>) list).stream())
						    	    .anyMatch(annotation -> annotation.getRight().equals(providerNodeAux))) {
							DTLogger.logger.fine("opAnnot - annotation already occupied");
						} else if (this.isItemCompatible(consumerNode, providerNode)) {
	    					DTLogger.logger.fine("opAnnot - annotationCompatible");
	    					addPair = true;
    						break;
						} else if (consumerNode.getSchemaNodeOptions().getLast().getValue() != null) {
							// check if has default value
							providerNode = new SchemaNode();
    						providerNode.setName("virtual");
    						providerNode.addSchemaNodeOption(new SchemaNodeOption());
    						providerNode.getSchemaNodeOptions().getFirst().setValue(consumerNode.getSchemaNodeOptions().getLast().getValue());
    						providerNode.setDataType(consumerNode.getDataType());
    						providerNode.setVirtual(true);
							consumerNode.setMatchOptionIndex(consumerNode.getSchemaNodeOptions().size() - 1);
							DTLogger.logger.fine("constant");
							addPair = true;
							break;
						}
					}
				}
				if (addPair) {
					tryLog.add(new ImmutableTriple<>(consumerNode.getName(), providerNode.getName(), true));    						
					Pair<SchemaNode,SchemaNode> pair = new MutablePair<>(consumerNode, providerNode);
					annotationPairs.add(pair);
					
					compatibleAnnotationFound = true;
				} else {
			        tryLog.add(new ImmutableTriple<>(consumerNode.getName(), providerNode.getName(), false));
					DTLogger.logger.fine("opAnnot - this optional annotation is incompatible");
				}
			}
		    if (compatibleAnnotationFound) {
		    	annotationCombinations.getFirst().put("optional-annotation-pairs", annotationPairs);
		    }
    	}
    	
    	DTLogger.logger.fine("for each optional group, search for a provider group");
    	
    	// for each optional group, search for a provider group
    	List<Integer> providerGroupsOccupiedByOptionalConsumers = new ArrayList<Integer>();
    	// foreach consumerGroup checks if is optional, and if it is optional then process it
    	for (consumerGroupIndex = 0; consumerGroupIndex < consumerNodeListList.size(); consumerGroupIndex++) { // for each optional consumer group
    		if (consumerNodeListList.get(consumerGroupIndex).getRight().getFirst().getMin() > 0) {
    			continue;
    		}
    		// look for a provider group that is not yet used in obligatoryPairs or in the optionalPairs currently being created
    		for (providerGroupIndex = 0; providerGroupIndex < providerNodeListList.size(); providerGroupIndex++) { // for each unused provider group
    			final Integer providerGroupIndexAux = providerGroupIndex;
    			if (obligatoryGroupPairs.stream().anyMatch(pair -> pair.getRight().equals(providerNodeListList.get(providerGroupIndexAux).getLeft()) ||
    					providerGroupsOccupiedByOptionalConsumers.contains(providerGroupIndexAux))) {
	    			DTLogger.logger.fine("opGroup - group already occupied");
	    			continue;
    			}

    			// if at least one annotation is compatible, pair the groups; otherwise, check if the next group is...
    			Integer providerAnnotationIndex = 0;
    			Integer consumerAnnotationIndex = 0;
    			boolean compatibleAnnotationFound = false;
    			List<Pair<SchemaNode,SchemaNode>> annotationPairs = new ArrayList<>();
    			for (consumerAnnotationIndex = 0; consumerAnnotationIndex < consumerNodeListList.get(consumerGroupIndex).getRight().size(); consumerAnnotationIndex++) { // for each consumer annotation
		    		SchemaNode consumerNode = consumerNodeListList.get(consumerGroupIndex).getRight().get(consumerAnnotationIndex);
		    		
		    		boolean addPair = false;
					SchemaNode providerNode = null;
					if (consumerNode.getConstantOption() != null) {
						DTLogger.logger.fine("DEPENDS_ON " + consumerNode.getName() + " " + consumerNode.getSchemaNodeOptions().get(consumerNode.getDependsOn().getMatchOptionIndex()).getValue());
						providerNode = new SchemaNode();
						providerNode.setName("virtual");
						providerNode.addSchemaNodeOption(new SchemaNodeOption());
						providerNode.getSchemaNodeOptions().getFirst().setValue(consumerNode.getConstantOption());
						providerNode.setDataType(consumerNode.getDataType());
						providerNode.setVirtual(true);
						consumerNode.setMatchOptionIndex(consumerNode.getSchemaNodeOptions().size() - 1); // aviso antes estava 0
						DTLogger.logger.fine("opGroup - constant");
						addPair = true;
					}
					else {
						for (providerAnnotationIndex = 0; providerAnnotationIndex < providerNodeListList.get(providerGroupIndex).getRight().size(); providerAnnotationIndex++) { // for each provider annotation
	    					providerNode = providerNodeListList.get(providerGroupIndex).getRight().get(providerAnnotationIndex);
	    					
	    		    		DTLogger.logger.fine("\nopGroup - group_indexes(c,p)=(" + consumerGroupIndex + "," + providerGroupIndex + ")");
	    		    		DTLogger.logger.fine("opGroup - annot_indexes(c,p)=(" + consumerNodeListList.get(consumerGroupIndex).getRight().indexOf(consumerNode) + "," + providerAnnotationIndex + ")");
	    		    		DTLogger.logger.fine(consumerNode.getName() + " " + providerNode.getName());	 
	    		    		DTLogger.logger.fine(consumerNode.getPathNames() + " " + providerNode.getPathNames());

				    		SchemaNode providerNodeAux = providerNode;
	    					if (annotationPairs.stream().anyMatch(pair -> pair.getRight().equals(providerNodeAux)) ||
	    							annotationCombinations.stream()
							    	    .map(combinationMap -> combinationMap.get("obligatory-annotation-pairs")) 
							    	    .filter(Objects::nonNull)
							    	    .flatMap(list -> ((List<Pair<SchemaNode, SchemaNode>>) list).stream())
							    	    .anyMatch(annotation -> annotation.getRight().equals(providerNodeAux))) {
	    						DTLogger.logger.fine("opGroup - annotation already occupied");
	    					} else {
	    						AtomicReference<String> constantValue = new AtomicReference<>();
	    						if (this.isItemCompatible(consumerNode, providerNode)) {
	    	    					DTLogger.logger.fine("opGroup - annotationCompatible");
	    							addPair = true;
		    						break; // already found a compatible provider, move on to the consumer's next annotation
		    					} else if (consumerNode.getSchemaNodeOptions().getLast().getValue() != null) {
	    							// check if it has default value
	    							providerNode = new SchemaNode();
		    						providerNode.setName("virtual");
		    						providerNode.addSchemaNodeOption(new SchemaNodeOption());
		    						providerNode.getSchemaNodeOptions().getFirst().setValue(consumerNode.getSchemaNodeOptions().getLast().getValue());
		    						providerNode.setDataType(consumerNode.getDataType());
		    						providerNode.setVirtual(true);
	    							consumerNode.setMatchOptionIndex(consumerNode.getSchemaNodeOptions().size() - 1);
	    							DTLogger.logger.fine("constant");
	    							addPair = true;
	    							break;
	    						}
	    					}
		    			}
					}
					if (addPair) {
				        tryLog.add(new ImmutableTriple<>(consumerNode.getName(), providerNode.getName(), true));	    						
						Pair<SchemaNode,SchemaNode> pair = new MutablePair<>(consumerNode, providerNode);
						annotationPairs.add(pair);
						
						compatibleAnnotationFound = true;
					} else {
				        tryLog.add(new ImmutableTriple<>(consumerNode.getName(), providerNode.getName(), false));
						DTLogger.logger.fine("opGroup - this optional annotation is incompatible");
					}		    		
				}
				// if at least one anot is compatible then add combinations and breaks this for
				if (compatibleAnnotationFound) {
					if (annotationCombinations.isEmpty()) {
						annotationCombinations.add(new HashMap<String, Object>());
					}
					if (annotationCombinations.getFirst().containsKey("optional-annotation-pairs")) {
						((List<Pair<SchemaNode,SchemaNode>>) annotationCombinations.getFirst().get("optional-annotation-pairs")).addAll(annotationPairs);
					} else {
						annotationCombinations.getFirst().put("optional-annotation-pairs", annotationPairs);
					}
					break;
				}
	    	}
    	}
	    
	}
	
	private boolean isItemCompatible(SchemaNode consumer, SchemaNode provider) {	
		if (consumer.isDataTypeCompatible(provider) && consumer.isFulfillmentCompatible(provider)) {
			return consumer.hasSemanticAnnotationCompatible(provider);
    	}
    	return false;
    }
    
    public static List<SchemaNodePair> createPairs(List<SchemaNode> consumers, List<SchemaNode> providers) {
        List<SchemaNodePair> pairs = new ArrayList<>();
        for (int i = 0; i < consumers.size(); i++) {
            pairs.add(new SchemaNodePair(consumers.get(i), providers.get(i)));
        }
        return pairs;
    } 
	
    private void matchPair(Pair<SchemaNode,SchemaNode> pair) {
    	pair.getLeft().setMatch(pair.getRight());
    	
    	ArrayList<SchemaNode> auxConsumerPath = pair.getLeft().getPathNodes();
		ArrayList<SchemaNode> auxProviderPath = pair.getRight().getPathNodes();

		int cIndex = 0;
		int pIndex = 0;
		
		for (int i = 0; i < Math.max(auxConsumerPath.size(), auxProviderPath.size()); i++) {

			if(!auxProviderPath.stream().anyMatch(node -> SchemaNode.Type.OTHER.equals(node.getType())) && 
					!auxProviderPath.stream().anyMatch(node -> node.isVirtual())) { 
				auxConsumerPath.get(cIndex).addLink(auxProviderPath.get(pIndex));
				if (i < auxConsumerPath.size() - 2) {cIndex++;} 
				if (i < auxProviderPath.size() - 2) {pIndex++;} 
			}
        }
    } 
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        printTree(root, sb, "", true, 100);
        return sb.toString();
    }

    private void printTree(SchemaNode node, StringBuilder sb, String prefix, boolean isTail, int maxWidth) {
        Map<String, Object> variables = new LinkedHashMap<>(); // keep insertion order
        Optional.ofNullable(node.getName()).ifPresent(ref -> variables.put("name", ref));
        Optional.ofNullable(node.getMdiID()).ifPresent(ref -> variables.put("mdi-id", ref));
        Optional.ofNullable(node.getType()).ifPresent(ref -> variables.put("type", ref));
        Optional.ofNullable(node.getDataType()).ifPresent(ref -> variables.put("data-type", ref));
        Optional.ofNullable(node.getMin()).ifPresent(ref -> variables.put("min", ref));
        Optional.ofNullable(node.getMax()).ifPresent(ref -> variables.put("max", ref));
        Optional.ofNullable(node.getSchemaNodeOptions()).ifPresent(ref -> variables.put("options", ref));
        Optional.ofNullable(node.getMatch()).ifPresent(ref -> variables.put("match", ref));
        Optional.ofNullable(node.getLinks()).ifPresent(ref -> variables.put("links", ref));
        Optional.ofNullable(node.getGroup()).ifPresent(ref -> variables.put("group", ref));
        Optional.ofNullable(node.getDependsOn()).ifPresent(ref -> variables.put("depends-on", ref.getPathNames()));
        if (!node.getPathNames().isEmpty()) variables.put("path", String.join("/", node.getPathNames()));
        variables.put("parent", node.getParent() != null ? 1 : 0);
        variables.put("named-parent", node.getNamedParent() != null ? 1 : 0);
        variables.put("children", node.getChildren() != null ? node.getChildren().size() : 0);

        String nodeStr = variables.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining("\n"));

        List<String> lines = Arrays.asList(nodeStr.split("\n"));
        for (int i = 0; i < lines.size(); i++) {
            sb.append(prefix)
              .append(i == 0 ? (isTail ? "└─ " : "├─ ") : "   ")
              .append(lines.get(i))
              .append("\n");
        }

        List<SchemaNode> children = node.getChildren();
        for (int i = 0; i < children.size() - 1; i++) {
            printTree(children.get(i), sb, prefix + (isTail ? "    " : "│   "), false, maxWidth);
        }
        if (!children.isEmpty()) {
            printTree(children.get(children.size() - 1), sb, prefix + (isTail ? "    " : "│   "), true, maxWidth);
        }
    }
	
	@Override
    public Iterator<SchemaNode> iterator() {
        return new SchemaTreeIterator(root);
    }
}
