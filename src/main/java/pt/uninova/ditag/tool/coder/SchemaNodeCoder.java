package pt.uninova.ditag.tool.coder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import pt.uninova.ditag.tool.node.SchemaNode;
import pt.uninova.ditag.tool.DTLogger;
import pt.uninova.ditag.tool.node.SchemaModelReference.AnnotationElementType;
import pt.uninova.ditag.tool.node.SchemaNode.DataType;
import pt.uninova.ditag.tool.report.Report.ExitCode;
import pt.uninova.ditag.tool.utils.EvalExConverter;
import pt.uninova.ditag.tool.utils.StringMatrix;

public class SchemaNodeCoder {
	
	StringMatrix conversionTable = SchemaNodeCoderConversion.getConversionTable();
	
	private int identIndex = 0;
	private List<Map.Entry<SchemaNode, String>> closerCode = new ArrayList<>();
	
	public SchemaNodeCoder() {
		super();
	}
	
	public int getIdentIndex() {
		return identIndex;
	}
	public void setIdentIndex(int identIndex) {
		this.identIndex = identIndex;
	}
	public List<Map.Entry<SchemaNode, String>> getCloserCode() {
		return closerCode;
	}
	public String getIdentation() {
		return "\t\t" + "\t".repeat(this.identIndex);
	}
	
	public String getConversionFormula(SchemaNode consumerNode) {
		String resultValue = "";
		if (consumerNode.getMatchOptionIndex() != null) {
			if (consumerNode.getSchemaNodeOptions().get(consumerNode.getMatchOptionIndex()).getConversionExpression() != null) {
				String expressionValue = "new Expression(\"" 
					+ consumerNode.getSchemaNodeOptions().get(consumerNode.getMatchOptionIndex()).getConversionExpression().replace("\"", "\\\\\"")
					+ "\").with(\"A3ST_EVAR\", %s).evaluate().getStringValue()";		
				resultValue = EvalExConverter.convertFromString(consumerNode.getDataType(), expressionValue);    
				return "(" + resultValue + ")";
			} 
		}
		return "%s";
	}

	private String GetValueExtension(SchemaNodeCoderStructurer nodeStructure) {	
		if (nodeStructure.isAttributeOfElement()) { 
			return ".getValue()";
		}
		return "";
	}
	
	private String getSeparatorStart(String content) {	
		return "\n" +  this.getIdentation() + "// " + content + "\n";
	}
	private String getSeparatorEnd() {	
		return this.getIdentation() + "// ----------------------------" +  "\n";
	}
	
	public String linkConsumerSoloProviderSolo(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure) {
		String constructor = "";
		
		if (consumerNode.getMatch() == null) {
			if (consumerNode.getNamedParent() != null && consumerNode.getNamedParent().isList()) {
				constructor += this.getSeparatorStart("Link CS <-> PS");
				if (consumerNode.isList()) { // Verificar
					constructor += String.format("%s%s = new %s();\n", this.getIdentation(), 
								   consumerNodeStructure.getInnerVariableName(), 
							       consumerNodeStructure.getCapitalPath());
				} else {
					constructor += String.format("%s%s.%s = new %s();\n", this.getIdentation(), 
								   consumerNodeStructure.getParentInnerVariableName(1), 
								   consumerNodeStructure.getName(), 
							       consumerNodeStructure.getCapitalPath());
				}
				constructor += this.getSeparatorEnd();
			} else if (consumerNode.getChildren().size() > 0) {
				constructor += this.getSeparatorStart("Link CS <-> PS");
				

				constructor += String.format("%s%s = new %s();\n", this.getIdentation(), 
						 										   consumerNodeStructure.getPath(), 
						 										   consumerNodeStructure.getCapitalPath());
				
				constructor += this.getSeparatorEnd();
			}
		}
		
		return constructor;
	}
	
	public String linkConsumerSoloProviderList(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure) {
		String constructor = "";
		
		constructor += this.getSeparatorStart("Link CS <-> PL");
		
		SchemaNode consumerAscendentThatIsList = Stream.iterate(consumerNode, node -> node != null && node.getNamedParent() != null, 
                node -> node.getNamedParent())
               .filter(node -> node.getNamedParent() != null && node.getNamedParent().isList())
               .findFirst()
               .orElse(null);
		
		if (consumerAscendentThatIsList != null) {
			constructor += String.format("%s%s.%s = new %s();\n", this.getIdentation(), 
																  consumerNodeStructure.getParentInnerVariableName(1), 
																  consumerNodeStructure.getName(), 
															      consumerNodeStructure.getCapitalPath());
		} else {
			constructor += String.format("%s%s = new %s();\n", this.getIdentation(), 
															   consumerNodeStructure.getPath(), 
															   consumerNodeStructure.getCapitalPath());
		}
		
		constructor += this.getSeparatorEnd();
		
		return constructor;
	}
	
	public String linkConsumerListProviderSolo(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure) {
		String constructor = "";
		
		constructor += this.getSeparatorStart("Link CL <-> PS");
		
		SchemaNode consumerAscendentThatIsList = Stream.iterate(consumerNode, node -> node != null && node.getNamedParent() != null, 
                node -> node.getNamedParent())
               .filter(node -> node.getNamedParent() != null && node.getNamedParent().isList())
               .findFirst()
               .orElse(null);
		
		if (consumerAscendentThatIsList == null) {
			constructor += String.format("%s%s = new ArrayList<%s>();\n", this.getIdentation(), 
																		  consumerNodeStructure.getPath(), 
																		  consumerNodeStructure.getCapitalPath());
		}
		
		constructor += String.format("%s%s %s = new %s();\n", this.getIdentation(), 
																consumerNodeStructure.getCapitalPath(), 
																consumerNodeStructure.getInnerVariableName(), 
																consumerNodeStructure.getCapitalPath());
		
		constructor += String.format("\n%s%s.add(%s);\n", this.getIdentation(), 
		  		   consumerNodeStructure.getPath(),
		  		   consumerNodeStructure.getInnerVariableName());
		
		constructor += this.getSeparatorEnd();
		
		return constructor;
	}
	
	public String linkConsumerListProviderList(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure, boolean isLast) {
		String constructor = "";
		
		constructor += this.getSeparatorStart("Link CL <-> PL");
		
		if (providerNode.getNamedParent() != null && providerNode.getNamedParent().isList()) { // If Provider Parent is List
//			constructor += String.format("%s%s.%s = new ArrayList<%s>();\n", this.getIdentation(), // aviso, se um dos acendentes forem lists (o que o primeiro ascendente é), não se coloca nova lista
//																		     consumerNodeStructure.getParentInnerVariableName(1), 
//																		     consumerNodeStructure.getName(), 
//																			 consumerNodeStructure.getCapitalPath());
			
			constructor += String.format("%sfor (%s %s : %s.%s) {\n",	this.getIdentation(), 
																		providerNodeStructure.getCapitalPath(), 
																		providerNodeStructure.getInnerVariableName(),
																		providerNodeStructure.getParentInnerVariableName(1), 
																		providerNodeStructure.getName(), 
																		providerNodeStructure.getPath());
			
			constructor += String.format("\t%sif (%s.size() >= %s) { break; }\n", this.getIdentation(), 
																				  consumerNodeStructure.getPath(),
																			      consumerNode.getEffectiveMax());
			if (isLast) {
				this.closerCode.add(Map.entry(consumerNode, String.format("\t%s%s.%s.add(%s);\n%s}\n", this.getIdentation(), 
												  		                                               consumerNodeStructure.getParentInnerVariableName(1),
												  		                                               consumerNodeStructure.getName().replace("_", ""),
												  		                                               consumerNodeStructure.getInnerVariableName(),
												  		                                               this.getIdentation())));
				this.identIndex += 1;
	
				constructor += String.format("%s%s %s = new %s();\n",	this.getIdentation(), 
																		consumerNodeStructure.getCapitalPath(), 
																		consumerNodeStructure.getInnerVariableName(), 
						                                                consumerNodeStructure.getCapitalPath());
			} else {
				this.closerCode.add(Map.entry(consumerNode, String.format("\n%s}\n", this.getIdentation(), this.getIdentation())));
				this.identIndex += 1;
			}
			
		} else { // If Provider Parent is Solo
			constructor += String.format("%s%s = new ArrayList<%s>();\n", this.getIdentation(), 
																		  consumerNodeStructure.getPath(), 
					                                                      consumerNodeStructure.getCapitalPath());
			
			constructor += String.format("%sfor (%s %s : %s) {\n",	this.getIdentation(), 
																	providerNodeStructure.getCapitalPath(), 
																	providerNodeStructure.getInnerVariableName(), 
					                                                providerNodeStructure.getPath());
			
			constructor += String.format("\t%sif (%s.size() >= %s) { break; }\n", this.getIdentation(), 
																				  consumerNodeStructure.getPath(),
																			      consumerNode.getEffectiveMax());
			
			if (isLast) {
				
				this.closerCode.add(Map.entry(consumerNode, String.format("\t%s%s.%s.add(%s);\n%s}\n", this.getIdentation(), 
						   												 	    consumerNodeStructure.getParentInnerVariableName(1),
						   												        consumerNodeStructure.getName().replace("_", ""),
						   												        consumerNodeStructure.getInnerVariableName(),
						   												        this.getIdentation())));
				this.identIndex += 1;
				
				constructor += String.format("%s%s %s = new %s();\n",	this.getIdentation(), 
																		consumerNodeStructure.getCapitalPath(), 
																		consumerNodeStructure.getInnerVariableName(), 
						                                                consumerNodeStructure.getCapitalPath());
			} else {
				this.closerCode.add(Map.entry(consumerNode, String.format("\n%s}\n", this.getIdentation(), this.getIdentation())));
				this.identIndex += 1;
			}
		}
		
		constructor += this.getSeparatorEnd();
		
		return constructor;
	}
	
	public String linkEscrow(SchemaNode consumerNode, SchemaNodeCoderStructurer consumerNodeStructure) {
		String constructor = "";
		
		constructor += this.getSeparatorStart("Link E");
		
		constructor += String.format("%s%s = new %s();\n", this.getIdentation(), 
                										   consumerNodeStructure.getPath(), 
                										   consumerNodeStructure.getCapitalPath());;
		
        constructor += this.getSeparatorEnd();
                										   
		return constructor;
	}
	
	/// aviso: separar logica e ditag (tool e lib)
	
	public String matchConsumerSoloProviderSolo(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure) {
		String constructor = "";
		
		constructor += this.getSeparatorStart("Match CS <-> PS");
		
		String conversionFormattedString = String.format(this.conversionTable.getValue(providerNode.getDataType(), consumerNode.getDataType()), this.getConversionFormula(consumerNode));
		
		if (providerNode.getSchemaNodeOptions().getFirst().getValue() == null) {
			String consumerFix = (String) consumerNodeStructure.getPath();
			String providerFix = (String) providerNodeStructure.getPath();
			
			SchemaNode consumerAscendentThatIsList = Stream.iterate(consumerNode, node -> node != null && node.getNamedParent() != null, 
                    node -> node.getNamedParent())
                   .filter(node -> node.getNamedParent() != null && node.getNamedParent().isList())
                   .findFirst()
                   .orElse(null);
			
			SchemaNode providerAscendentThatIsList = Stream.iterate(providerNode, node -> node != null && node.getNamedParent() != null, 
                    node -> node.getNamedParent())
                   .filter(node -> node.getNamedParent() != null && node.getNamedParent().isList())
                   .findFirst()
                   .orElse(null);
			
			if (consumerAscendentThatIsList != null) {
				SchemaNodeCoderStructurer auxNodeStructure = new SchemaNodeCoderStructurer(consumerAscendentThatIsList, "consumer", true);
				if (consumerAscendentThatIsList != consumerNode) {
					consumerFix = (String) auxNodeStructure.getInnerVariableName() + "." + auxNodeStructure.getName().substring(0,1).toLowerCase() + auxNodeStructure.getName().substring(1);
				} else {
					consumerFix = (String) auxNodeStructure.getInnerVariableName();
				}
			} 
			if (providerNode.getNamedParent() != null && providerNode.getNamedParent().isList()) {
				providerFix = (String) providerNodeStructure.getInnerVariableName();
			} else if (providerNode.getParent() != null  && providerNode.getParent().getType() == SchemaNode.Type.EXTENSION) {
				providerFix = (String) providerFix.substring(0, providerFix.lastIndexOf("."));
			} else if (providerAscendentThatIsList != null) {
			    SchemaNodeCoderStructurer auxNodeStructure = new SchemaNodeCoderStructurer(providerAscendentThatIsList, "provider", true);
			    String providerInnerVariableName = auxNodeStructure.getInnerVariableName();
			    providerFix = "";
			    for (SchemaNode providerParent = providerNode.getNamedParent(); providerParent != null; providerParent = providerParent.getNamedParent()) {
			        auxNodeStructure = new SchemaNodeCoderStructurer(providerParent, "provider", true);
			        providerFix = ".get" + auxNodeStructure.getMethodName() + "()" + providerFix;
			        if (providerParent == providerAscendentThatIsList) break;
			    }
			    providerFix = providerInnerVariableName + providerFix;
			}
			String auxConvertedValue = String.format(conversionFormattedString, "%s.get%s()%s");
			auxConvertedValue = String.format(auxConvertedValue, providerFix, providerNodeStructure.getMethodName(), this.GetValueExtension(providerNodeStructure));
			DTLogger.logger.fine(auxConvertedValue);
            if (!providerNode.isVirtual() && !providerNode.getSchemaNodeOptions().getFirst().getModelReference().getSemanticAnnotations(AnnotationElementType.INDIVIDUAL).isEmpty() && providerNode.getSchemaNodeOptions().getFirst().getMapDataInd() != null && !providerNode.getSchemaNodeOptions().getFirst().getMapDataInd().isEmpty()) {
				auxConvertedValue = String.format("this.individualMap_%s.get(%s)", String.join("_", consumerNode.getPathNames()), auxConvertedValue);
				DTLogger.logger.info(String.format("Exit Code: %s", ExitCode.ONTOLOGY_MISMATCH));
			}
			constructor += String.format("%s%s.set%s(%s);\n", this.getIdentation(), 
															  consumerFix, 
															  consumerNodeStructure.getMethodName(), 
															  auxConvertedValue);
		 } else {
			String preSetCode = "";
			String literalValue = (String) providerNode.getSchemaNodeOptions().getFirst().getValue();
			if (consumerNode.getDataType() == DataType.STRING) {
				literalValue = "\"" + literalValue + "\"";
			}
			if (literalValue != null && !literalValue.equals(""))  {
				if (consumerNode.getNamedParent() != null && consumerNode.getNamedParent().isList()) {
					preSetCode = consumerNodeStructure.getInnerVariableName();
				} else {
					preSetCode = consumerNodeStructure.getPath();
				}
				constructor += String.format("%s%s.set%s(%s);\n", this.getIdentation(), 
						  preSetCode, 
						  consumerNodeStructure.getMethodName(), 
						  String.format(conversionFormattedString, literalValue));
			}
		}
		
		constructor += this.getSeparatorEnd();
		
		return constructor;
	}
	
	public String matchConsumerSoloProviderList(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure) {
		String constructor = "";
		
		constructor += this.getSeparatorStart("Match CS <-> PL");
		
		String conversionFormattedString = String.format(this.conversionTable.getValue(providerNode.getDataType(), consumerNode.getDataType()), this.getConversionFormula(consumerNode));
		
		if (providerNode.getNamedParent() != null && providerNode.getNamedParent().isList()) { // Provider Parent is List
			String auxConvertedValue = String.format(conversionFormattedString, "%s.get%s().get(0)");
			auxConvertedValue = String.format(auxConvertedValue, providerNodeStructure.getInnerVariableName(), providerNodeStructure.getMethodName());
			if (!providerNode.getSchemaNodeOptions().getFirst().getModelReference().getSemanticAnnotations(AnnotationElementType.INDIVIDUAL).isEmpty()) {
				auxConvertedValue = String.format("this.individualMap_%s.get(%s)", String.join("_", consumerNode.getPathNames()), auxConvertedValue);
			}
			constructor += String.format("%s%s.set%s(%s);\n", this.getIdentation(), 
															  consumerNodeStructure.getInnerVariableName(), 
															  consumerNodeStructure.getMethodName(), 
															  auxConvertedValue);
		} else { // Provider Parent is Solo
			constructor += String.format("%sfor (%s %s : %s) {\n",	this.getIdentation(), 
																	providerNodeStructure.getCapitalPath(), 
					                                                providerNodeStructure.getInnerVariableName(), 
					                                                providerNodeStructure.getPath());
			
			this.closerCode.add(Map.entry(consumerNode, String.format("%s}\n", this.getIdentation())));
			this.identIndex += 1;

			String auxConvertedValue = String.format(conversionFormattedString, "provider%s.get%s()%s");
			auxConvertedValue = String.format(auxConvertedValue, providerNodeStructure.getParentInnerVariableName(1), providerNodeStructure.getInnerVariableName(), this.GetValueExtension(providerNodeStructure));
            if (!providerNode.isVirtual() && !providerNode.getSchemaNodeOptions().getFirst().getModelReference().getSemanticAnnotations(AnnotationElementType.INDIVIDUAL).isEmpty() && !providerNode.getSchemaNodeOptions().getFirst().getMapDataInd().isEmpty()) {
				auxConvertedValue = String.format("this.individualMap_%s.get(%s)", String.join("_", consumerNode.getPathNames()), auxConvertedValue);
			}
			constructor += String.format("%s%s.set%s(%s);\n", this.getIdentation(), 
															  consumerNodeStructure.getParentInnerVariableName(1),
					                                          consumerNodeStructure.getInnerVariableName(), 
					                                          auxConvertedValue);
		}
		
		constructor += this.getSeparatorEnd();
		
		return constructor;
	}
	
	public String matchConsumerListProviderSolo(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure) {
		String constructor = "";
	
		constructor += this.getSeparatorStart("Match CL <-> PS");
		
		String conversionFormattedString = String.format(this.conversionTable.getValue(providerNode.getDataType(), consumerNode.getDataType()), this.getConversionFormula(consumerNode));
		
		String auxConvertedValue = String.format(conversionFormattedString, "%s.get%s()%s");
		auxConvertedValue = String.format(auxConvertedValue, providerNodeStructure.getInnerVariableName(), providerNodeStructure.getMethodName(), this.GetValueExtension(providerNodeStructure));
		
        if (!providerNode.isVirtual() && !providerNode.getSchemaNodeOptions().getFirst().getModelReference().getSemanticAnnotations(AnnotationElementType.INDIVIDUAL).isEmpty() && !providerNode.getSchemaNodeOptions().getFirst().getMapDataInd().isEmpty()) {
			auxConvertedValue = String.format("this.individualMap_%s.get(%s)", String.join("_", consumerNode.getPathNames()), auxConvertedValue);
		}
		
		constructor += String.format("%s%s.%s = new ArrayList<%s>(Arrays.asList(%s));\n", this.getIdentation(), 
																						      consumerNodeStructure.getInnerVariableName(), 
																							  consumerNodeStructure.getName(), 
																							  consumerNode.getDataType().toString().substring(0, 1) +  consumerNode.getDataType().toString().substring(1).toLowerCase(),
																							  auxConvertedValue);
		
		constructor += this.getSeparatorEnd();
		
		return constructor;
	}
	
	public String matchConsumerListProviderList(SchemaNode consumerNode, SchemaNode providerNode, SchemaNodeCoderStructurer consumerNodeStructure, SchemaNodeCoderStructurer providerNodeStructure) {
		String constructor = "";
		
		constructor += this.getSeparatorStart("Match CL <-> PL");
		
		String conversionFormattedString = String.format(this.conversionTable.getValue(providerNode.getDataType(), consumerNode.getDataType()), this.getConversionFormula(consumerNode));
		
		if (providerNode.getNamedParent() != null && providerNode.getNamedParent().isList()) { // Provider Parent is List
			String auxConvertedValue = String.format(conversionFormattedString, "%s.%s");
			auxConvertedValue = String.format(auxConvertedValue, providerNodeStructure.getInnerVariableName(), providerNodeStructure.getName());
			
			if (!providerNode.getSchemaNodeOptions().getFirst().getModelReference().getSemanticAnnotations(AnnotationElementType.INDIVIDUAL).isEmpty()) {
				auxConvertedValue = String.format("this.individualMap_%s.get(%s)", String.join("_", consumerNode.getPathNames()), auxConvertedValue);
			}
			
			constructor += String.format("%s%s.%s = %s;\n", this.getIdentation(), 
															   consumerNodeStructure.getInnerVariableName(), 
															   consumerNodeStructure.getName(), 
															   auxConvertedValue);
		} else { // Provider Parent is Solo
			constructor += String.format("%s%s = new ArrayList<%s>();\n",	this.getIdentation(), 
					                                                        consumerNodeStructure.getPath(), 
					                                                        consumerNodeStructure.getCapitalPath());
			
			constructor += String.format("%sfor (%s %s : %s) {\n",	this.getIdentation(),
					                                                providerNodeStructure.getCapitalPath(), 
					                                                providerNodeStructure.getInnerVariableName(), 
					                                                providerNodeStructure.getPath());
			this.closerCode.add(Map.entry(consumerNode,String.format("%s}\n", this.getIdentation())));
			this.identIndex += 1;
			
			String auxConvertedValue = String.format(conversionFormattedString, "provider%s.get%s()%s");
			auxConvertedValue = String.format(auxConvertedValue, providerNodeStructure.getPath(), providerNodeStructure.getInnerVariableName(), this.GetValueExtension(providerNodeStructure));
			
            if (!providerNode.isVirtual() && !providerNode.getSchemaNodeOptions().getFirst().getModelReference().getSemanticAnnotations(AnnotationElementType.INDIVIDUAL).isEmpty() && !providerNode.getSchemaNodeOptions().getFirst().getMapDataInd().isEmpty()) {
				auxConvertedValue = String.format("this.individualMap_%s.get(%s)", String.join("_", consumerNode.getPathNames()), auxConvertedValue);
			}
			
			constructor += String.format("%s%s.set%s();\n", this.getIdentation(), 
					                                        consumerNodeStructure.getPath(), 
					                                        consumerNodeStructure.getInnerVariableName(), 
					                                        auxConvertedValue);
		}
		
		constructor += this.getSeparatorEnd();
		
		return constructor;
	}
}
