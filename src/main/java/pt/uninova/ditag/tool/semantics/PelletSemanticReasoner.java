package pt.uninova.ditag.tool.semantics;

// import com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory; // Uncomment for Pellet
// import com.hp.hpl.jena.query.Query; // Uncomment for Pellet
// import com.hp.hpl.jena.query.QueryExecution; // Uncomment for Pellet
// import com.hp.hpl.jena.query.QueryFactory; // Uncomment for Pellet
// import com.hp.hpl.jena.query.ResultSet; // Uncomment for Pellet

import pt.uninova.ditag.tool.ontology.Ontology;
import pt.uninova.ditag.tool.ontology.OntologyQuery;

public class PelletSemanticReasoner implements SemanticReasoner {
	
	private final Ontology ontology;
    private final OntologyQuery ontologyQuery;

    public PelletSemanticReasoner(Ontology ontology) throws PelletNotAvailableException {
        this.ontology = ontology;
        this.ontologyQuery = new OntologyQuery(ontology.getNSMappings().get("base"));
        
        try {
            Class.forName("com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory");
            Class.forName("com.hp.hpl.jena.query.Query");
            Class.forName("com.hp.hpl.jena.query.QueryExecution");
            Class.forName("com.hp.hpl.jena.query.QueryFactory");
            Class.forName("com.hp.hpl.jena.query.ResultSet");
        } catch (ClassNotFoundException e) {
        	throw new PelletNotAvailableException("Pellet and its Jena dependencies are not available on the classpath.", e);
        }
    }
    
    @Override
    public boolean isEquivalentConcept(String thisConcept, String otherConcept) {
        return semanticTest(ontologyQuery.isEquivalentConcept(thisConcept, otherConcept));
    }

    @Override
    public boolean isSubConcept(String thisConcept, String otherConcept) {
        return semanticTest(ontologyQuery.isSubConcept(thisConcept, otherConcept));
    }

    @Override
    public boolean isEquivalentProperty(String thisProperty, String otherProperty) {
        return semanticTest(ontologyQuery.isEquivalentProperty(thisProperty, otherProperty));
    }

    @Override
    public boolean isSubProperty(String thisProperty, String otherProperty) {
        return semanticTest(ontologyQuery.isSubProperty(thisProperty, otherProperty));
    }

    @Override
    public boolean isSameIndividual(String thisIndividual, String otherIndividual) {
        return semanticTest(ontologyQuery.isSameIndividual(thisIndividual, otherIndividual));
    }
	
    private boolean semanticTest(String semanticQuery) {
	    boolean result = false;	 
//	    Query query = QueryFactory.create(semanticQuery); // Uncomment for Pellet
//	    try { // Uncomment for Pellet
//	    	QueryExecution queryExecution = SparqlDLExecutionFactory.create(query, this.ontology.getJenaModel()); // Uncomment for Pellet
//		    ResultSet resultSet = queryExecution.execSelect(); // Uncomment for Pellet
//		    result = resultSet.hasNext(); // Uncomment for Pellet
//	    } catch(Exception e) { // Uncomment for Pellet
//	    	System.out.println(e); // Uncomment for Pellet
//	    }	// Uncomment for Pellet
		return result;
	}
    
    public class PelletNotAvailableException extends Exception {
        public PelletNotAvailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
