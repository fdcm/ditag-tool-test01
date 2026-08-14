package pt.uninova.ditag.tool.semantics;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import pt.uninova.ditag.tool.DTLogger;

public class ElkSemanticReasoner implements SemanticReasoner {

    private final OWLReasoner reasoner;
    private final OWLDataFactory factory;
    private final OWLOntology ontology;
    
    public ElkSemanticReasoner(OWLOntology ontology) {
    	
    	this.ontology = ontology;
        this.reasoner = new ElkReasonerFactory().createReasoner(this.ontology);
        this.factory = this.ontology.getOWLOntologyManager().getOWLDataFactory();
        
        ontology.axioms();
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    }

    @Override
    public boolean isEquivalentConcept(String thisConcept, String otherConcept) {
    	OWLClass thisC = factory.getOWLClass(IRI.create(thisConcept));
        OWLClass otherC = factory.getOWLClass(IRI.create(otherConcept));

        return reasoner.getEquivalentClasses(thisC).contains(otherC);
    }

    @Override
    public boolean isSubConcept(String thisConcept, String otherConcept) {
        OWLClass thisC = factory.getOWLClass(IRI.create(thisConcept));
        OWLClass otherC = factory.getOWLClass(IRI.create(otherConcept));

        return reasoner.getSubClasses(thisC).containsEntity(otherC);
    }

    @Override
    public boolean isEquivalentProperty(String thisProperty, String otherProperty) {

        DTLogger.logger.warning("ELK does not support property reasoning.");

        IRI thisIRI = IRI.create(thisProperty);
        IRI otherIRI = IRI.create(otherProperty);

        if (ontology.containsObjectPropertyInSignature(thisIRI) && ontology.containsObjectPropertyInSignature(otherIRI)) {

            OWLObjectProperty thisP = factory.getOWLObjectProperty(thisIRI);
            OWLObjectProperty otherP = factory.getOWLObjectProperty(otherIRI);

            if (ontology.equivalentObjectPropertiesAxioms(thisP).anyMatch(ax -> ax.properties().anyMatch(p -> p.equals(otherP)))) {
                return true;
            }
        }

        if (ontology.containsDataPropertyInSignature(thisIRI) && ontology.containsDataPropertyInSignature(otherIRI)) {

            OWLDataProperty thisP = factory.getOWLDataProperty(thisIRI);
            OWLDataProperty otherP = factory.getOWLDataProperty(otherIRI);

            if (ontology.equivalentDataPropertiesAxioms(thisP).anyMatch(ax -> ax.properties().anyMatch(p -> p.equals(otherP)))) {
                return true;
            }
        }

        return false;
    }
    
    @Override
    public boolean isSubProperty(String thisProperty, String otherProperty) {

        DTLogger.logger.warning("ELK does not support property reasoning.");

        IRI thisIRI = IRI.create(thisProperty);
        IRI otherIRI = IRI.create(otherProperty);

        if (ontology.containsObjectPropertyInSignature(thisIRI) && ontology.containsObjectPropertyInSignature(otherIRI)) {

            OWLObjectProperty thisP = factory.getOWLObjectProperty(thisIRI);
            OWLObjectProperty otherP = factory.getOWLObjectProperty(otherIRI);
            
            if (ontology.objectSubPropertyAxiomsForSubProperty(otherP).anyMatch(ax -> ax.getSuperProperty().equals(thisP))) {
                return true;
            }
        }

        if (ontology.containsDataPropertyInSignature(thisIRI) && ontology.containsDataPropertyInSignature(otherIRI)) {

            OWLDataProperty thisP = factory.getOWLDataProperty(thisIRI);
            OWLDataProperty otherP = factory.getOWLDataProperty(otherIRI);

            if (ontology.dataSubPropertyAxiomsForSubProperty(otherP).anyMatch(ax -> ax.getSuperProperty().equals(thisP))) {
                return true;
            }
        }

        return false;
    }
    @Override
    public boolean isSameIndividual(String thisIndividual, String otherIndividual) {
    	DTLogger.logger.warning("ELK does not support individual reasoning.");

    	OWLNamedIndividual thisI = this.factory.getOWLNamedIndividual(IRI.create(thisIndividual));
    	OWLNamedIndividual otherI = this.factory.getOWLNamedIndividual(IRI.create(otherIndividual));
    		
        return this.ontology.sameIndividualAxioms(thisI).anyMatch(ax -> ax.individuals().anyMatch(i -> i.equals(otherI)));
    }
}