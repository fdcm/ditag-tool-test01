package pt.uninova.ditag.tool.translator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pt.uninova.ditag.tool.DTLogger;
import pt.uninova.ditag.tool.coder.SchemaNodeCoder;
import pt.uninova.ditag.tool.coder.SchemaNodeCoderStructurer;
import pt.uninova.ditag.tool.node.SchemaNode;
import pt.uninova.ditag.tool.node.SchemaNodeOption;
import pt.uninova.ditag.tool.schema.SchemaTree;

public class TranslatorCreator {
	
	private String consumerClass;
	private String providerClass;
	
	private String constructors = "";
	
	private String mapCreators = "";
	private String mapPairings = "";
	
	private String map2ConsumerCsv = "";
	
	private SchemaNodeCoder coder = new SchemaNodeCoder();
	
	public TranslatorCreator(String consumerClass, String providerClass, SchemaTree consumerTree, SchemaTree providerTree) {
		super();
		
		this.consumerClass = consumerClass;
		this.providerClass = providerClass;
		
		//DTLogger.logger.fine(consumerTree);
		//DTLogger.logger.fine(providerTree);
		
        generateTranslatorLogic(consumerTree.getRoot());
        generateIndividialPairings(consumerTree);
	    generateCsvMappings(consumerTree);

	}
	
	private void generateIndividialPairings(SchemaTree consumerTree) {
		List<String> createdMDI = new ArrayList<String>();
		List<SchemaNode> nodes = consumerTree.findNodes(null, null, null, null, null, null);
		
		for(SchemaNode node : nodes) {
			if (node.getMatch () != null) {
				SchemaNodeOption option = node.getSchemaNodeOptions().get(node.getMatchOptionIndex());
				if (option.getMapDataInd() != null) {
					for (Map.Entry<String, Object> entry : option.getMapDataInd().entrySet()) {
						String key = entry.getKey();
						
					    String consumerValue = (String) entry.getValue();
					    String providerValue = (String) node.getMatch().getSchemaNodeOptions().getFirst().getIndividualValue(key);
					    
					    String idMDI = String.join("_", node.getPathNames());
					    		
					    if (node.getMatch() != null) {
					    	if (!createdMDI.contains(idMDI)) {
					    		this.mapCreators += String.format("\tMap<String, String> individualMap_%s;\n", idMDI);
					    		this.mapPairings += String.format("\n\t\tthis.individualMap_%s = new HashMap<String, String>();\n\n", idMDI);
					    		createdMDI.add(idMDI);
					    	}
					    	this.mapPairings += String.format("\t\tthis.individualMap_%s.put(\"%s\",\"%s\");\n", idMDI, providerValue, consumerValue);
					    }
					}
				}
			}
		}
	}
	private void generateTranslatorLogic(SchemaNode consumerNode) {
	    if (consumerNode.getName() != null) { 		
	        processLinkNodes(consumerNode);
	        processMatches(consumerNode);
	    }

	    for (SchemaNode child : consumerNode.getChildren()) {
	        generateTranslatorLogic(child);
	    }

	    List<Map.Entry<SchemaNode, String>> toRemove = new ArrayList<>();
	    for (Map.Entry<SchemaNode, String> entry : this.coder.getCloserCode()) {
	        if (entry.getKey().equals(consumerNode)) {
	            this.coder.setIdentIndex(this.coder.getIdentIndex() - 1);
	            this.constructors += entry.getValue();
	            toRemove.add(entry);
	        }
	    }
	    this.coder.getCloserCode().removeAll(toRemove);
	}

	private void processLinkNodes(SchemaNode consumerNode) {
		SchemaNodeCoderStructurer consumerNodeStructure = new SchemaNodeCoderStructurer(consumerNode, "consumer", false);
		if (consumerNode.getLinks().size() > 0) {
			DTLogger.logger.fine(consumerNode.getName() + "|||||||" + consumerNode.getLinks());
			for (SchemaNode providerLinkNode : consumerNode.getLinks()) {
				SchemaNodeCoderStructurer providerNodeStructure = new SchemaNodeCoderStructurer(providerLinkNode, "provider", false);
				DTLogger.logger.fine(providerLinkNode.getName());
				if (!consumerNode.isList() && !providerLinkNode.isList()) { // Consumer is Solo, Provider is Solo
					this.constructors += coder.linkConsumerSoloProviderSolo(consumerNode, providerLinkNode, consumerNodeStructure, providerNodeStructure);
				} else if (!consumerNode.isList() && providerLinkNode.isList()) { // Consumer is Solo, Provider is List
					this.constructors += coder.linkConsumerSoloProviderList(consumerNode, providerLinkNode, consumerNodeStructure, providerNodeStructure);
				} else if (consumerNode.isList() && !providerLinkNode.isList()) { // Consumer is List, Provider is Solo
					if (providerLinkNode == consumerNode.getLinks().getLast()) { // aviso, adicionaod para nao repetir os contrutores para cada link
						this.constructors += coder.linkConsumerListProviderSolo(consumerNode, providerLinkNode, consumerNodeStructure, providerNodeStructure);	
					}
				} else if (consumerNode.isList() && providerLinkNode.isList()) { // Consumer is List, Provider is List
					this.constructors += coder.linkConsumerListProviderList(consumerNode, providerLinkNode, consumerNodeStructure, providerNodeStructure, (providerLinkNode == consumerNode.getLinks().getLast()));
				}
			}
		} else {
			boolean hasMatch = false;
		    
			List<SchemaNode> nodesToCheck = new ArrayList<>();
		    nodesToCheck.add(consumerNode);
		    while (!nodesToCheck.isEmpty() && !hasMatch) {
		        SchemaNode currentNode = nodesToCheck.remove(0);
		        if (currentNode.getMatch() != null) {
		            hasMatch = true;
		            break;
		        }
		        nodesToCheck.addAll(currentNode.getChildren());
		    }

		    if (hasMatch && consumerNode.getChildren().size() > 0) {
				this.constructors += coder.linkEscrow(consumerNode, consumerNodeStructure);

		    }
		}
	}
	private void processMatches(SchemaNode consumerNode) {
		SchemaNode providerNode = consumerNode.getMatch();
		
		SchemaNodeCoderStructurer consumerNodeStructure = new SchemaNodeCoderStructurer(consumerNode, "consumer", true);
		if (providerNode != null) {		
			SchemaNodeCoderStructurer providerNodeStructure = new SchemaNodeCoderStructurer(providerNode, "provider", true);
			if (!consumerNode.isList() && !providerNode.isList()) { // Consumer is Solo, Provider is Solo
				this.constructors += coder.matchConsumerSoloProviderSolo(consumerNode, providerNode, consumerNodeStructure, providerNodeStructure);
			} else if (!consumerNode.isList() && providerNode.isList()) { // Consumer is Solo, Provider is List
				this.constructors += coder.matchConsumerSoloProviderList(consumerNode, providerNode, consumerNodeStructure, providerNodeStructure);
			} else if (consumerNode.isList() && !providerNode.isList()) { // Consumer is List, Provider is Solo
				this.constructors += coder.matchConsumerListProviderSolo(consumerNode, providerNode, consumerNodeStructure, providerNodeStructure);
			} else if (consumerNode.isList() && providerNode.isList()) { // Consumer is List, Provider is List
				this.constructors += coder.matchConsumerListProviderList(consumerNode, providerNode, consumerNodeStructure, providerNodeStructure);
			}
		}
	}
	private void generateCsvMappings(SchemaTree consumerTree) {
		if(consumerTree.getCSVMappings().size() > 0) {
			this.map2ConsumerCsv = "\tMap<String, String> consumerCsvMappings = Map.ofEntries(\n";
			for (Map.Entry<String, String> entry : consumerTree.getCSVMappings().entrySet()) {
			    String name = entry.getKey();
			    String path = entry.getValue();
			    
			    this.map2ConsumerCsv += String.format("\tentry(\"%s\", \"%s\"),\n", name, path);
			}
			this.map2ConsumerCsv = this.map2ConsumerCsv.substring(0, this.map2ConsumerCsv.length() - 2) + "\n);\n";
		} else {
			this.map2ConsumerCsv = "\tMap<String, String> consumerCsvMappings = Map.ofEntries();\n";
		}
	}
	
	public String generate() {
	    StringBuilder result = new StringBuilder();

	    InputStream in = TranslatorCreator.class.getResourceAsStream("/Translator.txt");
	    if (in == null) {
	        in = TranslatorCreator.class.getResourceAsStream("/resources/Translator.txt");
	    }
	    if (in == null) {
	        throw new IllegalStateException("Translator.txt not found on classpath");
	    }
	    
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
	            StringWriter writer = new StringWriter()) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            line = line.replaceAll("\\[CONSUMER CLASS\\]", this.consumerClass);
	            line = line.replaceAll("\\[PROVIDER CLASS\\]", this.providerClass);
	            line = line.replaceAll("\\[MAPS\\]", this.mapCreators);
	            line = line.replaceAll("\\[MAP2CCSV\\]", this.map2ConsumerCsv).replaceAll("\n", "\n\t");
	            line = line.replaceAll("\\[LOGIC\\]", this.constructors.replaceAll("\n", "\n\t"));
	            line = line.replaceAll("\\[LOGIC RAW\\]", this.constructors.replaceAll("\n", "\n\t"));
	            line = line.replaceAll("\\[MAP PAIRINGS\\]", this.mapPairings);
	            writer.write(line + "\n");
	        }
	        result.append(writer.toString());
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    
	    return result.toString();
	}
	
//	public void generate(String outPath) {
//		try (BufferedReader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("Translator.txt")));
//            BufferedWriter writer = new BufferedWriter(new FileWriter(outPath + "/Translator.java"))) {
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                line = line.replaceAll("\\[CONSUMER CLASS\\]", this.consumerClass);
//                line = line.replaceAll("\\[PROVIDER CLASS\\]", this.providerClass);
//                line = line.replaceAll("\\[MAPS\\]", this.mapCreators);
//                line = line.replaceAll("\\[LOGIC\\]", this.constructors);
//                line = line.replaceAll("\\[LOGIC RAW\\]", this.constructors.replaceAll("\n", "\n\t"));
//                line = line.replaceAll("\\[MAP PAIRINGS\\]", this.mapPairings);
//                writer.write(line + "\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//	}
}
