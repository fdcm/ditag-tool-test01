package pt.uninova.ditag.tool;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ethlo.jsons2xsd.Config;
import com.ethlo.jsons2xsd.Jsons2Xsd;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConverterJSONSchemaToXSD {
	
	@SuppressWarnings("unchecked")
	public static String convert(String jsonPath) {
        	String jsonString;
			try {
				jsonString = new String(Files.readAllBytes(Paths.get(jsonPath)));
				try (Reader r = new StringReader(jsonString))
				{
					ObjectMapper mapper = new ObjectMapper();
					Map<String, Object> jsonDic = mapper.readValue(jsonString, Map.class);
	
					StringWriter writer = new StringWriter();
					
					Config cfg = new Config.Builder()
				    .targetNamespace("")
				    .name("")
				    .build();
					Document doc = Jsons2Xsd.convert(r, cfg);
					
					// Add Schema URLs
					
					Element elementSchema = (Element) doc.getElementsByTagName("schema").item(0);
					elementSchema.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
					elementSchema.setAttribute("xmlns:a3st", "http://gres.uninova.pt/a3st");
					elementSchema.setAttribute("xmlns:sawsdl", "http://www.w3.org/ns/sawsdl");
					
					Element elementAnnotation = doc.createElement("xs:annotation");
					Element elementAppInfo = doc.createElement("xs:appinfo");
					elementAnnotation.appendChild(elementAppInfo);
					elementSchema.appendChild(elementAnnotation);
					elementAnnotation.removeAttribute("xmlns");
					
		            // Add Extra Info 
					
					// Add Ontology Model
					
					Element elementAux = null;
					if (jsonDic.containsKey("a3st")) {
						for (Map<String, Object> entry : (List<Map<String, Object>>) jsonDic.get("a3st")) {
							switch ((String) entry.keySet().toArray()[0]) {
							    case "a3st:model":
							    	elementAux = doc.createElement("a3st:model");
							    	elementAux.setAttribute("a3st:ontology", ((Map<String, String>) entry.get("a3st:model")).get("a3st:ontology"));
							    	elementAppInfo.appendChild(elementAux);
							        break;
							    case "a3st:data-property-value":
							    	elementAux = doc.createElement("a3st:data-property-value");
							    	elementAux.setAttribute("type", ((Map<String, String>) entry.get("a3st:data-property-value")).get("type")
							    			// Convert JSON Type to XSD Type
							    			.replace("number", "xs:double")
							    			.replace("string", "xs:string")
							    			.replace("boolean", "xs:boolean")
							    			.replace("integer", "xs:long")
							    			);
							    	elementAux.setAttribute("a3st:property", ((Map<String, String>) entry.get("a3st:data-property-value")).get("a3st:property"));
							    	elementAux.setAttribute("a3st:value", ((Map<String, String>) entry.get("a3st:data-property-value")).get("a3st:value"));
							    	elementAppInfo.appendChild(elementAux);
							        break;
							    case "a3st:map-data-ind":
							    	elementAux = doc.createElement("a3st:map-data-ind");
							    	elementAux.setAttribute("a3st:mdiId", ((Map<String, String>) entry.get("a3st:map-data-ind")).get("a3st:mdi-id"));
							    	for (Map<String, String> ind : (List<Map<String, String>>) ((Map<String, Object>) entry.get("a3st:map-data-ind")).get("a3st:map-value-ind")) {
							    		Element elementAuxInd = doc.createElement("a3st:map-value-ind");
							    		elementAuxInd.setAttribute("a3st:individual", ind.get("a3st:individual"));
							    		elementAuxInd.setAttribute("a3st:value", ind.get("a3st:value"));
							    		elementAux.appendChild(elementAuxInd);
							    	}
							    	elementAppInfo.appendChild(elementAux);
							        break;
							    default:
							    	break;
							}
						}
					}
					
					// Add Individual					
					List<Map<String, String>> mdiIDOccurrences = searchKeyJSON(jsonString, "a3st:mdiId");
					List<Map<String, String>> individualOccurrences = searchKeyJSON(jsonString, "a3st:individual");
					List<Map<String, String>> valueDOccurrences = searchKeyJSON(jsonString, "a3st:node-value");
					
					List<Map<String, String>> annotations = IntStream.range(0, Math.min(mdiIDOccurrences.size(), Math.min(individualOccurrences.size(), valueDOccurrences.size())))
				            .mapToObj(i -> {
				                Map<String, String> zipped = Map.of(
				                        "path", mdiIDOccurrences.get(i).get("path"),
				                        "mdiId", mdiIDOccurrences.get(i).get("value"),
				                        "individual", individualOccurrences.get(i).get("value"),
				                        "value", valueDOccurrences.get(i).get("value")
				                );
				                return zipped;
				            })
				            .collect(Collectors.toList());
					
					for (Map<String, String> annotation: annotations) {
						elementAux = doc.createElement("a3st:map-data-ind");
						elementAux.setAttribute("a3st:mdiId", annotation.get("mdiId"));
						elementAux.setAttribute("a3st:individual", annotation.get("individual"));
						elementAux.setTextContent(annotation.get("value"));
						elementAppInfo.appendChild(elementAux);
					}
					
					
					// Add Model and MDI References
					
			        List<Map<String, String>> modelReferenceOccurrences = searchKeyJSON(jsonString, "modelReference");
			        List<Map<String, String>> MDIRefOccurrences = searchKeyJSON(jsonString, "a3st:mdiRef");
			        
			        NodeList nodes = doc.getElementsByTagName("*");
		            for (int i = 0; i < nodes.getLength(); i++) {
		                Node node = nodes.item(i);
		                if (node.getNodeType() == Node.ELEMENT_NODE) {
		                    Element element = (Element) node;
		                    String path = extractPath(element).replaceAll("/+", "/").replaceAll("^/|/$", "");
		                    
		                    for (Map<String, String> occurence : modelReferenceOccurrences) {
		                    	if (occurence.get("path").equals(path)) {
							        element.setAttribute("sawsdl:modelReference", occurence.get("value"));
		                    	}
		                    }
		                    for (Map<String, String> occurence : MDIRefOccurrences) {
		                    	if (occurence.get("path").equals(path)) {
							        element.setAttribute("a3st:mdiRef", occurence.get("value"));
		                    	}
		                    }
		                }
		            }
		            
					
					TransformerFactory tf = TransformerFactory.newInstance();
				    Transformer transformer = tf.newTransformer();
				    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
				    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
				    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	
				    transformer.transform(new DOMSource(doc), new StreamResult(writer));
				    
				    String outXSD = writer.toString();
				    outXSD = outXSD.replace("type=\"decimal\"", "type=\"xs:float\"");
				    
					return outXSD;
				} catch (IOException | TransformerException e) {
					e.printStackTrace();
					return "";
				}
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
    }
	
	// *********************** //
	
	private static List<Map<String, String>> searchKeyJSON(String jsonString, String key) {
        List<Map<String, String>> occurrences = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.readTree(jsonString);
            searchNodeJSON(rootNode, key, "", occurrences);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return occurrences;
    }

    private static void searchNodeJSON(JsonNode node, String key, String path, List<Map<String, String>> occurrences) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                String newPath = path.isEmpty() ? fieldName : path + "/" + fieldName;
                searchNodeJSON(fieldValue, key, newPath, occurrences);
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                String newPath = path.isEmpty() ? "[" + i + "]" : path + "[" + i + "]";
                searchNodeJSON(arrayNode.get(i), key, newPath, occurrences);
            }
        } else {
            if (node.isValueNode()) {
                if (path.endsWith(key)) {
                    Map<String, String> occurrenceMap = new HashMap<>();
                    occurrenceMap.put("value", node.asText());
                    occurrenceMap.put("path", path.substring(0, path.lastIndexOf("/")).replace("properties/", ""));
                    occurrences.add(occurrenceMap);
                }
            }
        }
    }
    
    // *********************** //
    
    private static String extractPath(Element element) {
        String path = "";
        Node parentNode = element.getParentNode();
        if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
            path += extractPath((Element) parentNode);
        }
        path += "/" + element.getAttribute("name");
        return path;
    }
    
}
