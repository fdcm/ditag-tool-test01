package pt.uninova.ditag.tool;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pt.uninova.ditag.tool.report.Report.ExitCode;
import pt.uninova.ditag.tool.schema.SchemaDocument;
import pt.uninova.ditag.tool.schema.SchemaProcessor;
import pt.uninova.ditag.tool.semantics.PelletSemanticReasoner.PelletNotAvailableException;
import pt.uninova.ditag.tool.semantics.SemanticComparator;
import pt.uninova.ditag.tool.translator.SemanticPOJOCreator;
import java.io.FileInputStream;

public class Executor 
{
	private static SemanticEngine semanticEngine;
	
	public static void main( String[] args )
    {
		Map<String, String> arguments = parseArguments(args);

        if (!arguments.containsKey("providerData") ||
            !arguments.containsKey("providerType") ||
            !arguments.containsKey("consumerData") ||
            !arguments.containsKey("consumerType") ||
            !arguments.containsKey("outType")) {
        		throw new IllegalArgumentException("MISSING <REQUIRED> ARGUMENTS: Executor --providerData=<data> --providerType=<file/raw> --consumerData=<data> --consumerType=<file/raw> --ontologyData=<data> --ontologyType=<file/raw> --outType=<full/simple> [--outDirectory=<directory>]");
        }
        
        String providerData = arguments.get("providerData");
        String providerType = arguments.get("providerType");
        String consumerData = arguments.get("consumerData");
        String consumerType = arguments.get("consumerType");
        String ontologyData = arguments.get("ontologyData");
        String ontologyType = arguments.get("ontologyType");
        String outType = arguments.get("outType");
        String outDirectory = arguments.get("outDirectory");
        String reasoner = arguments.getOrDefault("reasoner", "pellet");

        int logLevel = 0;
        try {
            logLevel = Integer.parseInt(arguments.getOrDefault("logLevel", "0").toString());
        } catch (NumberFormatException e) {}
        
        run(providerData,providerType,consumerData,consumerType,ontologyData,ontologyType,outType,outDirectory,logLevel,reasoner);
        
        DTLogger.logger.info(String.format("Exit Code: %s", semanticEngine.report.getExitCode().getCode()));
    }
	
	public static String generateTranslator(
			InputStream providerStream, 
			InputStream consumerStream, 
			InputStream ontologyStream, 
			JSONObject outputJson,
			int logLevel,
			String reasoner
			)
    {
		DTLogger.setLevel(logLevel == 1 ? Level.INFO :
						  logLevel == 2 ? Level.FINE :
                          Level.OFF);

		String providerXSD = null;
		String consumerXSD = null;
		String ontology = null;
		String returnString = "";
		
		try {
			providerXSD = new String(providerStream.readAllBytes(), StandardCharsets.UTF_8);
			consumerXSD = new String(consumerStream.readAllBytes(), StandardCharsets.UTF_8);
			if (ontologyStream != null) {
				ontology = new String(ontologyStream.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (semanticEngine == null) {
			semanticEngine = new SemanticEngine();
		}
		
        SchemaDocument providerDocument = new SchemaDocument(providerXSD);
        SchemaDocument consumerDocument = new SchemaDocument(consumerXSD);
        
        semanticEngine.setProviderDocument(providerDocument);
        semanticEngine.setConsumerDocument(consumerDocument);
		
		String consumerOntology = null;
        String providerOntology = null;
        for (Element entry : SchemaProcessor.getElements(consumerDocument.getData(), new ArrayList<>(Arrays.asList("metadata")))) {
        	consumerOntology = entry.getAttributeValue("ontology", Namespace.getNamespace("http://gres.uninova.pt/a3st"));
        }
        for (Element entry : SchemaProcessor.getElements(providerDocument.getData(), new ArrayList<>(Arrays.asList("metadata")))) {
        	providerOntology = entry.getAttributeValue("ontology", Namespace.getNamespace("http://gres.uninova.pt/a3st"));
        }
        
        if (consumerOntology == null || providerOntology == null || (!consumerOntology.equals(providerOntology))) {
        	semanticEngine.getReport().setExitCode(ExitCode.ONTOLOGY_MISMATCH);
        }
        
        if (semanticEngine.getReport().getExitCode() != ExitCode.ONTOLOGY_MISMATCH) {
	        if (ontology == null) {
	        	try {
	        		InputStream auxOntologyStream = getInputStream(providerOntology, "url");
	        		if (auxOntologyStream != null) {
	        			ontology = new String(auxOntologyStream.readAllBytes(), StandardCharsets.UTF_8);
	        		}
				} catch (IOException e) {
					e.printStackTrace();
				} 
	        }
	        if (ontology != null) {
	        	SemanticComparator semanticComparator = null;
	        	try {
	        		semanticComparator = new SemanticComparator(ontology, reasoner);

	        	
			        semanticEngine.setSemanticComparator(semanticComparator);//
			        
					String consumerClass = "";
			        String providerClass = "";
			        
			        try {
			        	consumerClass = SemanticPOJOCreator.fromXSD(consumerXSD, "Consumer");
			        	providerClass = SemanticPOJOCreator.fromXSD(providerXSD, "Provider");
					} catch (IOException e) {
						e.printStackTrace();
					}
			        semanticEngine.setConsumerClass(consumerClass);
			        semanticEngine.setProviderClass(providerClass);
					
					semanticEngine.runEngine();
					returnString = semanticEngine.translatorCreator.generate(); 
	        	} catch (PelletNotAvailableException e) {
	            	semanticEngine.getReport().setExitCode(ExitCode.PELLET_NOT_AVAILABLE);
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
	        }
		    
        }
	    
        return returnString;	
    }
	
	public static String run(
		    String providerData,
		    String providerType,
		    String consumerData,
		    String consumerType,
		    String ontologyData,
		    String ontologyType,
		    String outType,
		    String outDirectory,
		    int logLevel,
		    String reasoner)
    { 	
		semanticEngine = new SemanticEngine();
		
		InputStream providerXSD = getInputStream(providerData, providerType); 
		InputStream consumerXSD = getInputStream(consumerData, consumerType); 
		if (providerXSD == null || consumerXSD == null) {
			semanticEngine.getReport().setExitCode(ExitCode.IMPROPER_ARGUMENTS);
		}
		
		InputStream ontology = null;
		if (ontologyData != null && ontologyType != null) {
			ontology = getInputStream(ontologyData, ontologyType); 
		}
		providerXSD = validateInputStream(providerXSD);
		consumerXSD = validateInputStream(consumerXSD);
		
		JSONObject report = new JSONObject();
		
		String outTranslator = "";
		if (semanticEngine.getReport().getExitCode() == ExitCode.SUCCESS || semanticEngine.getReport().getExitCode() == ExitCode.DEFAULT) {
	        outTranslator = generateTranslator(providerXSD, consumerXSD, ontology, report, logLevel, reasoner);
		}
		
		switch (outType) {
			case "file":
				if (outTranslator != "") {			
					if(semanticEngine.report.getMatch() == null) {
						semanticEngine.report.setExitCode(ExitCode.NO_MATCH);
					} else {
						Path filePath = Paths.get(outDirectory, "Translator.java");
						try {
						    Files.write(filePath, outTranslator.getBytes(StandardCharsets.UTF_8));
						} catch (IOException e) {
						    e.printStackTrace();
						}
						
						semanticEngine.report.setExitCode(ExitCode.SUCCESS);
					}
				}
				JSONObject reportJson = semanticEngine.getReport().toJSONObject();

				report.clear();
			    reportJson.keySet().forEach(key -> report.put(key, reportJson.get(key)));
			    
				exportReportAsJSON(report, outDirectory);
				break;
			case "text":
				return outTranslator;
		}
		return semanticEngine.report.getExitCode().getMessage();
    }
	
	private static InputStream downloadFile(String URL) {
	    try {
	        HttpClient client = HttpClient.newHttpClient();
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(URL))
	                .build();

	        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

	        if (response.statusCode() != 200) {
	            semanticEngine.getReport().setExitCode(ExitCode.FILE_NOT_REACHABLE);
	            return null;
	        }

	        return response.body();
	    } catch (IOException | InterruptedException e) {
	    	e.printStackTrace();
	        return null;
	    }
	}
	
	private static InputStream getInputStream(String data, String type) {
		try {
	        switch (type.toLowerCase()) {
	            case "file":
	            	return new FileInputStream(data);
	            case "owl":
	            case "raw":
	            	return new ByteArrayInputStream(data.getBytes());
	            case "url":
	                return downloadFile(data);
	            default:
	                return null;
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return null;
    }

	private static InputStream validateInputStream(InputStream stream) {
	    try {
	        String content = new String(stream.readAllBytes());

	        switch (SchemaValidator.validate(content)) {
	            case 1:
	                return new ByteArrayInputStream(content.getBytes());

	            case 2:
	                String converted = ConverterJSONSchemaToXSD.convert(content);
	                return new ByteArrayInputStream(converted.getBytes());
	            case 0:
	            default:
	                semanticEngine.getReport().setExitCode(ExitCode.INPUT_NOT_VALID);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    return null;
	}
	
	private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    arguments.put(parts[0], parts[1]);
                }
            }
        }
        
		return arguments;
	}
	
	private static void exportReportAsJSON(JSONObject report, String directory) {
	    try {
	        File dir = new File(directory);
	        if (!dir.exists()) {
	            dir.mkdirs();
	        }
	        File outputFile = new File(dir, "report.json");

	        ObjectMapper objectMapper = new ObjectMapper();
	        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

	        String jsonString = objectMapper.writeValueAsString(report.toMap());

	        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
	            writer.write(jsonString);
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}