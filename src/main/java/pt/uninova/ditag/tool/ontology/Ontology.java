package pt.uninova.ditag.tool.ontology;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
// import org.mindswap.pellet.PelletOptions; // Uncomment for Pellet
// import org.mindswap.pellet.jena.PelletReasonerFactory; // Uncomment for Pellet
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import pt.uninova.ditag.tool.DTLogger;

public class Ontology {

    private String ontologyContent;
    private OntModel jenaModel;
    private OWLOntology owlModel;
    
    public Ontology(String ontologyContent) {
        super();
        this.ontologyContent = ontologyContent;
        loadOntologyFromString();
    }

    public String getOntologyContent() {
        return ontologyContent;
    }

    public void setOntologyContent(String ontologyContent) {
        this.ontologyContent = ontologyContent;
        loadOntologyFromString();
    }

    public OntModel getJenaModel() {
        return jenaModel;
    }
    public OWLOntology getOwlModel() {
        return owlModel;
    }
    
    private Map<String, String> nsMappings = new HashMap<>();
	public Map<String, String> getNSMappings() {
		return this.nsMappings;
	}

	private void parseNSMap() {
	    nsMappings.clear();

	    if (owlModel == null) {
	        return;
	    }

	    OWLDocumentFormat format = owlModel.getOWLOntologyManager().getOntologyFormat(owlModel);
	    if (!format.isPrefixOWLDocumentFormat()) {
	        return;
	    }

	    PrefixDocumentFormat prefixFormat = format.asPrefixOWLDocumentFormat();
	    for (Map.Entry<String, String> entry : prefixFormat.getPrefixName2PrefixMap().entrySet()) {
	        String key = entry.getKey();

	        if (key == null || key.isEmpty() || key.equals(":")) {
	            key = "xmlns";
	        } else if (key.endsWith(":")) {
	            key = key.substring(0, key.length() - 1);
	        }

	        nsMappings.put(key, entry.getValue());
	    }

	    String xmlnsNs = nsMappings.get("xmlns");
	    if (xmlnsNs != null) {
	        if (xmlnsNs.endsWith("#")) {
	            nsMappings.put("base", xmlnsNs.substring(0, xmlnsNs.length() - 1));
	        } else {
	            nsMappings.put("base", xmlnsNs);
	        }
	    }
	}
	
    private void loadOntologyFromString() {    	
    	// PelletOptions.TREAT_ALL_VARS_DISTINGUISHED = false; // Uncomment for Pellet
        // this.jenaModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC); // Uncomment for Pellet
        if (this.ontologyContent != null && !this.ontologyContent.isEmpty()) {
        	ByteArrayInputStream inputStream = new ByteArrayInputStream(ontologyContent.getBytes(StandardCharsets.UTF_8));
            if (this.jenaModel != null) {
            	try {
            	    this.jenaModel.read(inputStream, null);
            	} catch (Throwable t) {
            	    System.out.println(t.toString());
            	    System.exit(0);
            	}
            }
            try {
            	inputStream = new ByteArrayInputStream(ontologyContent.getBytes(StandardCharsets.UTF_8));
	            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	        	this.owlModel = manager.loadOntologyFromOntologyDocument(inputStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            this.jenaModel = null;
            this.owlModel = null;
        }
        
        DTLogger.logger.info(String.format("Ontology Size: %s", this.owlModel.getAxiomCount()));
        
        this.parseNSMap();
        DTLogger.logger.info(String.format("Namespaces Found: %s", this.nsMappings.size()));
    }
    
    
}