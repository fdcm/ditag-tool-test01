package pt.uninova.ditag.tool.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import pt.uninova.ditag.tool.DTLogger;
import pt.uninova.ditag.tool.node.SchemaNode;
import pt.uninova.ditag.tool.node.SchemaNodeOption;
import pt.uninova.ditag.tool.semantics.SemanticComparator;

public class SchemaTreeBuilder {
	
	static SchemaNode root = null;
	static List<Pair<String, SchemaNode>> dependsOnList = null;	
	
    public static SchemaNode buildTree(Document xsdDocument, SemanticComparator semanticComparator) {
        Element rootElement = xsdDocument.getRootElement();
        dependsOnList = new ArrayList<>();	
        
        List<Element> extraIndividuals = SchemaProcessor.getElements(xsdDocument, new ArrayList<>(Arrays.asList("mapDataInd")));
        List<Element> nodeAnnotations = SchemaProcessor.getElements(xsdDocument, new ArrayList<>(Arrays.asList("node-annotations")));

        root = parseElement(rootElement, null, extraIndividuals, nodeAnnotations, semanticComparator);
        
        parseDependsOn(nodeAnnotations);
        return root;
    }

    private static SchemaNode parseElement(Element element, SchemaNode parent, List<Element> extraIndividuals, List<Element> nodeAnnotations, SemanticComparator semanticComparator) {
        if ("mapDataInd".equals(element.getName())) {
            return null;
        }

        SchemaNode schemaNode = new SchemaNode();
        
        parseType(element, schemaNode);
        
        if (schemaNode.getType() == SchemaNode.Type.EXTENSION) {
        	if (parent.getName() == null && parent.getNamedParent() != null) {
            	schemaNode.setName(parent.getNamedParent().getName());
        	} else {
        		schemaNode.setName(parent.getName());
        	}	
        } else {
            schemaNode.setName(element.getAttributeValue("name") != null ? element.getAttributeValue("name") : schemaNode.getName());
        }
        schemaNode.setSemanticComparator(semanticComparator);
        schemaNode.setParent(parent);
        
        parseDataType(element, schemaNode);
        parseOccurrences(element, schemaNode);
        parseAnnotations(element, schemaNode, extraIndividuals, nodeAnnotations, semanticComparator);
        parseGroupAndDependsOn(element, schemaNode, nodeAnnotations);
        
        for (Element childElement : element.getChildren()) {
            SchemaNode childNode = parseElement(childElement, schemaNode, extraIndividuals, nodeAnnotations, semanticComparator);
            if (childNode != null) {
                schemaNode.addChild(childNode);
            }
        }

        return schemaNode;
    }

    private static void parseType(Element element, SchemaNode schemaNode) {
        switch (element.getName()) {
            case "schema":
                schemaNode.setType(SchemaNode.Type.SCHEMA);
                break;
            case "element":
                schemaNode.setType(SchemaNode.Type.ELEMENT);
                break;
            case "extension":
                schemaNode.setType(SchemaNode.Type.EXTENSION);
                break;
            case "complexType":
                schemaNode.setType(SchemaNode.Type.COMPLEX_TYPE);
                break;
            case "simpleContent":
                schemaNode.setType(SchemaNode.Type.SIMPLE_CONTENT);
                break;
            case "attribute":
                schemaNode.setType(SchemaNode.Type.ATTRIBUTE);
                break;
            case "annotation":
                schemaNode.setType(SchemaNode.Type.ANNOTATION);
                break;
            case "appinfo":
                schemaNode.setType(SchemaNode.Type.APP_INFO);
                break;
            default:
                schemaNode.setType(SchemaNode.Type.OTHER);
                break;
        }
    }
    
    private static void parseGroupAndDependsOn(Element element, SchemaNode schemaNode, List<Element> nodeAnnotations) {
    	Namespace ns = Namespace.getNamespace("http://gres.uninova.pt/a3st");
    	String elementGroup = element.getAttributeValue("group", ns);
    	if (elementGroup != null) {
            schemaNode.setGroup(Integer.parseInt(elementGroup));
        } 
    	String dependsOn = element.getAttributeValue("depends-on", ns);
        if (dependsOn != null) {
            dependsOnList.add(Pair.of(dependsOn, schemaNode));
        }
    	if (nodeAnnotations != null && !nodeAnnotations.isEmpty()) {
            for (Element annotation: nodeAnnotations) {
            	Element nodesElement = annotation.getChild("nodes", ns);
                if (nodesElement != null && schemaNode.getName() != null) {
                    for (Element node : nodesElement.getChildren("node", ns)) {  
                        String nodeId = node.getAttributeValue("id", ns);
  
                        if (nodeId != null && nodeId.equals("/" + String.join("/", schemaNode.getPathNames()))) {
                        	 elementGroup = node.getAttributeValue("group", ns);
                             if (elementGroup != null && schemaNode.getGroup() == 0) {
                                 schemaNode.setGroup(Integer.parseInt(elementGroup));
                             }       
                             dependsOn = node.getAttributeValue("depends-on", ns);
                             if (dependsOn != null && schemaNode.getDependsOn() == null) {
                                 dependsOnList.add(Pair.of(dependsOn, schemaNode));
                             }
                             break;
                        }
                    }
                }
            }   
        }
        
    }
    
    private static void parseDependsOn(List<Element> nodeAnnotations) {
    	for (Pair<String, SchemaNode> pair : dependsOnList) {
    	    String dependencyPath = pair.getLeft();        
    	    SchemaNode dependantNode = pair.getRight(); 
    	    
    	    SchemaTreeIterator iterator = new SchemaTreeIterator(root);
    	    while (iterator.hasNext()) {
    	        SchemaNode node = iterator.next();
    	        
    	        if (node != null && dependencyPath.equals("/" + String.join("/", node.getPathNames()))) {
    	        	dependantNode.setDependsOn(node);
                }
    	    }
    	}
    }
    
    private static void parseDataType(Element element, SchemaNode schemaNode) {
    	String elementType = element.getAttributeValue("type");
    	if (elementType == null) {
    		Element complexTypeElement = element.getChild("complexType", Namespace.getNamespace("http://www.w3.org/2001/XMLSchema"));
            if (complexTypeElement != null) {
                Element simpleContentElement = complexTypeElement.getChild("simpleContent", Namespace.getNamespace("http://www.w3.org/2001/XMLSchema"));
                if (simpleContentElement != null) {
                    Element extensionElement = simpleContentElement.getChild("extension", Namespace.getNamespace("http://www.w3.org/2001/XMLSchema"));
                    if (extensionElement != null) {
                        String baseType = extensionElement.getAttributeValue("base");
                        if (baseType != null) {
                        	elementType = baseType;
                        }
                    }
                }
            }
    	}
        if (elementType != null) {
        	String elementTypeAux = elementType;
        	if (elementType.contains(":")) {
        		elementTypeAux = elementType.split(":")[1];
        	}
            switch (elementTypeAux) {
            	case "float":
                	schemaNode.setDataType(SchemaNode.DataType.FLOAT);
                	break;
	            case "double":
	            case "decimal": // UNBOUND
                	schemaNode.setDataType(SchemaNode.DataType.DOUBLE);
                	break;
	            case "integer":
                case "long": // 64 Bits
                    schemaNode.setDataType(SchemaNode.DataType.LONG);
                    break;
                case "int": // 32 Bits
                    schemaNode.setDataType(SchemaNode.DataType.INT);
                    break;
                case "short": // 16 Bits
                    schemaNode.setDataType(SchemaNode.DataType.SHORT);
                    break;
                case "byte": // 8 Bits
                    schemaNode.setDataType(SchemaNode.DataType.BYTE); 
                    break;
                case "nonNegativeInteger":
                    schemaNode.setDataType(SchemaNode.DataType.NON_NEG_INTEGER);	// JAVA LONG
                    break;
                case "positiveInteger":
                    schemaNode.setDataType(SchemaNode.DataType.POS_INTEGER);	// JAVA LONG
                    break;
                case "nonPositiveInteger":
                    schemaNode.setDataType(SchemaNode.DataType.NON_POS_INTEGER);	// JAVA LONG
                    break;
                case "negativeInteger":
                    schemaNode.setDataType(SchemaNode.DataType.NEG_INTEGER);	// JAVA LONG
                    break;
                case "unsignedLong":
                    schemaNode.setDataType(SchemaNode.DataType.U_LONG);		// JAVA LONG
                    break;
                case "unsignedInt":
                    schemaNode.setDataType(SchemaNode.DataType.U_INT);		// JAVA LONG
                    break;
                case "unsignedShort":
                    schemaNode.setDataType(SchemaNode.DataType.U_SHORT);	// JAVA INT
                    break;
                case "unsignedByte":
                    schemaNode.setDataType(SchemaNode.DataType.U_BYTE);		// JAVA SHORT
                    break;
                case "string":
                    schemaNode.setDataType(SchemaNode.DataType.STRING);
                    break;
                case "normalizedString":
                    schemaNode.setDataType(SchemaNode.DataType.NORMALIZED_STRING);	// JAVA STRING
                    break;
                case "date":
                    schemaNode.setDataType(SchemaNode.DataType.DATE);
                    break;
                case "time":
                    schemaNode.setDataType(SchemaNode.DataType.TIME);
                    break;
                case "dateTime":
                    schemaNode.setDataType(SchemaNode.DataType.DATE_TIME);
                    break;
                default:
                    schemaNode.setDataType(SchemaNode.DataType.OTHER(elementType));
                    break;
            }
        } else {
            schemaNode.setDataType(SchemaNode.DataType.NONE);
        }
    }

    private static void parseOccurrences(Element element, SchemaNode schemaNode) {
        schemaNode.setMin(element.getAttributeValue("minOccurs") != null ? Integer.parseInt(element.getAttributeValue("minOccurs")) : schemaNode.getMin());
        schemaNode.setMax(element.getAttributeValue("maxOccurs") != null ? (element.getAttributeValue("maxOccurs").equals("unbounded") ? Integer.MAX_VALUE : Integer.parseInt(element.getAttributeValue("maxOccurs"))) : schemaNode.getMax());
    }
    
    private static void parseAnnotations(Element element, SchemaNode schemaNode, List<Element> extraIndividuals, List<Element> nodeAnnotations, SemanticComparator semanticComparator) {
    	parseInnerAnnotations(element, schemaNode, extraIndividuals, semanticComparator);
        parseOuterAnnotations(element, schemaNode, nodeAnnotations, semanticComparator);
    }
    
    private static void parseInnerAnnotations(Element element, SchemaNode schemaNode, List<Element> extraIndividuals, SemanticComparator semanticComparator) {
    	SchemaNodeOption option = new SchemaNodeOption();
    	option.setRefNode(schemaNode);
    	String rawAnnotation = null;
    	if (element.getAttributeValue("modelReference", Namespace.getNamespace("http://www.w3.org/ns/sawsdl")) != null) {
    		rawAnnotation = element.getAttributeValue("modelReference", Namespace.getNamespace("http://www.w3.org/ns/sawsdl"));
    	} //else if (element.getAttributeValue("property", Namespace.getNamespace("http://gres.uninova.pt/a3st")) != null) {
    	//	rawAnnotation = element.getAttributeValue("property", Namespace.getNamespace("http://gres.uninova.pt/a3st")) ;
    	//} 
    	if(element.getAttributeValue("value", Namespace.getNamespace("http://gres.uninova.pt/a3st")) != null) {
    		option.setValue(element.getAttributeValue("value", Namespace.getNamespace("http://gres.uninova.pt/a3st")));
    	}
    	if(element.getAttributeValue("default", Namespace.getNamespace("http://gres.uninova.pt/a3st")) != null) {
    		option.setDefaultExpression(element.getAttributeValue("default", Namespace.getNamespace("http://gres.uninova.pt/a3st")));
    	}
    	if(element.getAttributeValue("conversion", Namespace.getNamespace("http://gres.uninova.pt/a3st")) != null) {
    		option.setConversionExpression(element.getAttributeValue("conversion", Namespace.getNamespace("http://gres.uninova.pt/a3st")));
    	}
    	if(element.getAttributeValue("expression", Namespace.getNamespace("http://gres.uninova.pt/a3st")) != null) {
    		option.setValueExpression(element.getAttributeValue("expression", Namespace.getNamespace("http://gres.uninova.pt/a3st")));
    	}
        if (rawAnnotation != null) {
            ArrayList<String> unwrappedAnnotations = SchemaProcessor.unwrapAnnotation(rawAnnotation);

            ArrayList<String> finalAnnotations = new ArrayList<>(unwrappedAnnotations);
            String refMDI = element.getAttributeValue("mdiRef", Namespace.getNamespace("http://gres.uninova.pt/a3st"));
            if (refMDI != null) {
                ArrayList<Integer> refsMDI = Arrays.stream(refMDI.split(";"))
                        .map(Integer::parseInt)
                        .collect(Collectors.toCollection(ArrayList::new));
                
                for (Element extraIndividualGroup : extraIndividuals) {
                    String idMDI = extraIndividualGroup.getAttributeValue("mdiId", Namespace.getNamespace("http://gres.uninova.pt/a3st"));
                    if (idMDI != null && refsMDI.contains(Integer.parseInt(idMDI))) {
                        schemaNode.setMdiID(Integer.valueOf(refMDI));
                    	for (Element extraIndividual : extraIndividualGroup.getChildren()) {
                    		String individual = extraIndividual.getAttributeValue("individual", Namespace.getNamespace("http://gres.uninova.pt/a3st"));
                    		finalAnnotations.addAll(unwrappedAnnotations.stream()
                                    .map(str -> str + String.format("[{%s}]", individual))
                                    .collect(Collectors.toList()));
                            if (extraIndividual.getText() != null) {
                            	if (option.getMapDataInd() == null) {
                            		option.createMapDataInd();
                            	}
                            	option.addIndividualValue(individual, extraIndividual.getAttributeValue("value", Namespace.getNamespace("http://gres.uninova.pt/a3st")));
                            }
                    	}
                    }
                }
            } else {
                finalAnnotations = unwrappedAnnotations;
            }
            if (!finalAnnotations.isEmpty()) {
            	option.createModelReference(semanticComparator);
            }
            finalAnnotations.forEach(annotation -> option.getModelReference().addSemanticAnnotation(annotation));
            
            schemaNode.addSchemaNodeOption(option);
        }
 
    }
    
    private static void parseOuterAnnotations(Element element, SchemaNode schemaNode, List<Element> nodeAnnotations, SemanticComparator semanticComparator) {
    	Namespace ns = Namespace.getNamespace("http://gres.uninova.pt/a3st");
        if (nodeAnnotations != null && !nodeAnnotations.isEmpty()) {
        	for (Element nodeAnnotation : nodeAnnotations) {
	            Element nodesElement = nodeAnnotation.getChild("nodes", ns);
	            Element optionsElement = nodeAnnotation.getChild("options", ns);
	            if (nodesElement != null) {
	                for (Element node : nodesElement.getChildren("node", ns)) {
	                    String nodeId = node.getAttributeValue("id", ns);
	                    String schemaNodeId = "/" + String.join("/", schemaNode.getPathNames());

	                    if (nodeId != null && schemaNodeId != null && nodeId.equals(schemaNodeId)) {
	                    	if (optionsElement != null) {
	                    		schemaNode.getSchemaNodeOptions().clear();
	                    		for (Element optionElement : optionsElement.getChildren("option", ns)) {
	                    			SchemaNodeOption option = new SchemaNodeOption();
	                    			option.setRefNode(schemaNode);
	                    			
	                    			if(optionElement.getAttributeValue("default", Namespace.getNamespace("http://gres.uninova.pt/a3st")) != null) {
	                    	    		option.setDefaultExpression(optionElement.getAttributeValue("default", Namespace.getNamespace("http://gres.uninova.pt/a3st")));
	                    	    	}
	                    			
	                    			String nodeModelReferenceRaw = optionElement.getAttributeValue("modelReference", Namespace.getNamespace("http://www.w3.org/ns/sawsdl"));  
	                    			if (nodeModelReferenceRaw != null) {
	                    				option.createModelReference(semanticComparator);
		                    			ArrayList<String> nodeModelReferences = SchemaProcessor.unwrapAnnotation(nodeModelReferenceRaw);
		                    			for (String nodeModelReference : nodeModelReferences) {		             
		                    				//option.getModelReference().getSemanticAnnotations().clear(); // aviso
		                    				DTLogger.logger.fine(schemaNode.getName());
		                    				option.getModelReference().addSemanticAnnotation(nodeModelReference);
			                    				
		                    				if (optionElement.getChildren("mapDataInd", ns) != null) {
			                    				option.createMapDataInd();
			                        			for (Element individualElement : optionElement.getChildren("mapDataInd", ns)) {
			                        				String nodeModelReferenceAux = nodeModelReference + "[{" + individualElement.getAttributeValue("individual", ns) + "}]";	     
				                    				option.getModelReference().addSemanticAnnotation(nodeModelReferenceAux);
				                    				option.addIndividualValue(individualElement.getAttributeValue("individual", ns), individualElement.getAttributeValue("value", ns));
			                        			}
		                    				}
		
			                    		} 
	                    			}
	                    			
                    				// Constant/Value
	                    			Element constantElement = optionElement.getChild("constant", ns);
	                    		    if (constantElement != null) {
	                    		        String constantValue = constantElement.getTextTrim();
	                    		        if (!constantValue.isEmpty()) {
	                    		            option.setValue(constantValue);
	                    		        }
	                    		    }
	
                    				// Formulas
	                    		    String conversionExpression = optionElement.getAttributeValue("conversion", ns);
	                    		    if (conversionExpression != null) {
	                    		        option.setConversionExpression(conversionExpression);
		                    			schemaNode.addSchemaNodeOption(option);
	                    		    }
	                    		    
	                    			schemaNode.addSchemaNodeOption(option);
                    			} 
	                    	}
	                        break;
	                    }
	                }
	            }
        	}
        }
    }
}