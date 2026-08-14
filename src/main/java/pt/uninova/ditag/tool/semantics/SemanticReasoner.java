package pt.uninova.ditag.tool.semantics;

public interface SemanticReasoner {
	
	boolean isEquivalentConcept(String thisConcept, String otherConcept);

    boolean isSubConcept(String thisConcept, String otherConcept);

    boolean isEquivalentProperty(String thisProperty, String otherProperty);

    boolean isSubProperty(String thisProperty, String otherProperty);

    boolean isSameIndividual(String thisIndividual, String otherIndividual);
    
}
