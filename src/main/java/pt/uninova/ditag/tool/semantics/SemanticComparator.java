package pt.uninova.ditag.tool.semantics;

import pt.uninova.ditag.tool.DTLogger;
import pt.uninova.ditag.tool.ontology.Ontology;
import pt.uninova.ditag.tool.semantics.PelletSemanticReasoner.PelletNotAvailableException;

public class SemanticComparator {

	private final SemanticReasoner reasoner;
	private final Ontology ontology;
	
	// *** *** //
	
	public SemanticComparator(String ontologyContent, String reasonerID) throws PelletNotAvailableException {
		super();
		this.ontology = new Ontology(ontologyContent);
		
		switch (reasonerID) {
		    case "pellet":
		    	this.reasoner = new PelletSemanticReasoner(this.ontology);	    	
		    	DTLogger.logger.info("Using Reasoner: Pellet");
		    	break;
		    case "elk":
		    default:
		    	this.reasoner = new ElkSemanticReasoner(this.ontology.getOwlModel());		    	
		    	DTLogger.logger.info("Using Reasoner: ELK");
		}
	}
	
	// *** *** //
	
	public String parseElementNamespace(String element) {
	    String prefix;
	    String name;

	    if (element.contains(":")) {
	        String[] parts = element.split(":", 2);

	        prefix = this.ontology.getNSMappings().get(parts[0]);
	        name = parts[1];
	    } else if (element.contains("#")) {
	        prefix = this.ontology.getNSMappings().get("base");
	        name = element;
	    } else {
	        prefix = this.ontology.getNSMappings().get("xmlns");
	        name = element;
	    }

	    return prefix + name;
	}
	
	// *** *** //
	
	public boolean isConceptRelated(String providerConcept, String consumerConcept) {
		 boolean isConceptSemanticallyRelated = false;
		 
		 providerConcept = this.parseElementNamespace(providerConcept);
		 consumerConcept = this.parseElementNamespace(consumerConcept);
		 
		 // Check if Concept is Equal		 
		 if (consumerConcept.equals(providerConcept)) {
			 isConceptSemanticallyRelated = true;
		 } else // Check if Concept is Equivalent
		 if (this.reasoner.isEquivalentConcept(providerConcept, consumerConcept)) {
			 isConceptSemanticallyRelated = true;
		 } else // Check if Concept is a Subclass
		 if (this.reasoner.isSubConcept(providerConcept, consumerConcept)) {
			 isConceptSemanticallyRelated = true;
		 }
		 
		 return isConceptSemanticallyRelated;
	}
	
	public boolean isPropertyRelated(String providerProperty, String consumerProperty) {
		 boolean isPropertySemanticallyRelated = false;
		 
		 providerProperty = this.parseElementNamespace(providerProperty);
		 consumerProperty = this.parseElementNamespace(consumerProperty);
		 
		 // Check if Property is Equal		 
		 if (consumerProperty.equals(providerProperty)) {
			 isPropertySemanticallyRelated = true;
		 } else // Check if Property is Equivalent
		 if (this.reasoner.isEquivalentProperty(providerProperty, consumerProperty)) {
			 isPropertySemanticallyRelated = true;
		 } else // Check if Property is a Sub-property
		 if (this.reasoner.isSubProperty(providerProperty, consumerProperty)) {
			 isPropertySemanticallyRelated = true;
		 }
		 
		 return isPropertySemanticallyRelated;
	}
	
	public boolean isIndividualRelated(String providerIndividual, String consumerIndividual) {
		 boolean isIndividualSemanticallyRelated = false;
		 
		 providerIndividual = this.parseElementNamespace(providerIndividual);
		 consumerIndividual = this.parseElementNamespace(consumerIndividual);
		 
		 // Check if Individual is Equal
		 if (consumerIndividual.equals(providerIndividual)) {
			 isIndividualSemanticallyRelated = true;
		 } else // Check if Individual is the Same
		 if (this.reasoner.isSameIndividual(providerIndividual, consumerIndividual)) {
			 isIndividualSemanticallyRelated = true;
		 }
		 
		 return isIndividualSemanticallyRelated;
	}
}
