 package pt.uninova.ditag.tool.ontology;

import pt.uninova.ditag.tool.DTLogger;

public class OntologyQuery {

    private String prefixTemplate = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
                              "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"+
                              "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"+
                              "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

    private String sufixTemplate = "    }";
    private String queryTemplate = "SELECT ?object \n" +
                                   "    WHERE { \n" +
                                   "        <%s> %s ?object \n" +
                                   "            FILTER (?object = <%s>) \n";

    // *** *** //

    public OntologyQuery(String base) {

        super();

        this.prefixTemplate += "PREFIX base: <" + base + ">\n";

    }

    // *** Concept (Class) *** //

    public String isSubConcept(String providerConcept, String consumerConcept){

        String query = String.format(queryTemplate, consumerConcept, "rdfs:subClassOf", providerConcept);
        
        DTLogger.logger.fine(query);
        
        return prefixTemplate + query + sufixTemplate;

    }

    public String isEquivalentConcept(String providerConcept, String consumerConcept){

        String query =    String.format(queryTemplate, providerConcept, "owl:equivalentClass", consumerConcept);
        
        DTLogger.logger.fine(query);
        
        return prefixTemplate + query + sufixTemplate;

    }
	
	// *** Property *** //
	
	public String isSubProperty(String providerProperty, String consumerProperty){
		String query =	String.format(queryTemplate, consumerProperty, "rdfs:subPropertyOf", providerProperty);
		
		DTLogger.logger.fine(query);
		
		return prefixTemplate + query + sufixTemplate;
	}
	
	public String isEquivalentProperty(String providerProperty, String consumerProperty){
		String query =	String.format(queryTemplate, providerProperty, "owl:equivalentProperty", consumerProperty);
		
		DTLogger.logger.fine(query);
		
		return prefixTemplate + query + sufixTemplate;
	}
	
	// *** Individual *** //
	
	public String isSameIndividual(String providerIndividual, String consumerIndividual){
		String query =	String.format(queryTemplate, providerIndividual, "owl:sameAs", consumerIndividual);
		
		DTLogger.logger.fine(query);
		
		return prefixTemplate + query + sufixTemplate;
	}
}
