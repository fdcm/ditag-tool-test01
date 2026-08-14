package pt.uninova.ditag.tool.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.uninova.ditag.tool.DTLogger;
import pt.uninova.ditag.tool.semantics.SemanticComparator;

public class SchemaModelReference {

    private List<SemanticAnnotation> semanticAnnotations;
    public List<SemanticAnnotation> getSemanticAnnotations() {
		return semanticAnnotations;
	}
    public List<SemanticAnnotation> getSemanticAnnotations(AnnotationElementType annotationElementType) {
        return semanticAnnotations.stream()
            .filter(annotation -> {
                List<SemanticAnnotationElement> elements = annotation.annotationElements;
                if (elements.isEmpty()) {
                    return false;
                }
                SemanticAnnotationElement lastElement = elements.get(elements.size() - 1);
                return lastElement.getType() == annotationElementType;
            })
            .collect(Collectors.toList());
    }
    public void addSemanticAnnotation(String annotationString) {
    	SemanticAnnotation semanticAnnotation = new SemanticAnnotation(annotationString);
    	if (!this.semanticAnnotations.contains(semanticAnnotation)) {
    		this.semanticAnnotations.add(semanticAnnotation);
    	}
	}
	public void removeSemanticAnnotation(String annotationString) {
		Iterator<SemanticAnnotation> iterator = this.semanticAnnotations.iterator();
	    
	    while (iterator.hasNext()) {
	        SemanticAnnotation auxSemanticAnnotation = iterator.next();
	        if (auxSemanticAnnotation.getAnnotation().equals(annotationString)) {
	            iterator.remove();
	            break;
	        }
	    }
	}

	private SemanticComparator semanticComparator;
	public void setSemanticComparator(SemanticComparator semanticComparator) {
		this.semanticComparator = semanticComparator;
	}
	
    public SchemaModelReference(SemanticComparator semanticComparator) {
		super();
		this.semanticComparator = semanticComparator;
		this.semanticAnnotations = new ArrayList<SemanticAnnotation>();
	}
    
	public enum AnnotationElementType {
        CONCEPT,
        PROPERTY, 
        INDIVIDUAL
    }
	
	public class SemanticAnnotationElement {
    	private String element;
		public String getElement() {
			return element;
		}
		public void setElement(String element) {
			this.element = element;
		}
		
        private AnnotationElementType type;
		public AnnotationElementType getType() {
			return type;
		}
		public void setType(AnnotationElementType type) {
			this.type = type;
		}
		
		public SemanticAnnotationElement() {
			super();
		}
		public SemanticAnnotationElement(String element, AnnotationElementType type) {
			super();
			this.element = element;
			this.type = type;
		}
		
		 @Override
	     public String toString() {
			 return Map.of(
			            "element", this.element,
			            "type", this.type
			        ).toString();
	     }
    }
	
    public class SemanticAnnotation {
    	private String fullAnnotation;
        private List<SemanticAnnotationElement> annotationElements;
        
        public SemanticAnnotation(String annotation) {
        	this.fullAnnotation = annotation;
        	annotationElements = new ArrayList<SemanticAnnotationElement>();
        	
            List<String> annotationParts = new ArrayList<>(Arrays.asList(annotation.split("/")));
            if (annotationParts.get(0).equals("")) {
            	annotationParts = annotationParts.subList(1, annotationParts.size());
            }
            
    		Integer annotationIndex = 1;
        	for (String annotationPart: annotationParts) {
        		
        		String individual = null;
        		int start = annotationPart.indexOf('{');
                int end = annotationPart.indexOf('}');
        		if (start != -1 && end != -1 && start < end) {
        			individual = annotationPart.substring(start + 1, end);
        			annotationPart = annotationPart.substring(0, start);	
        		}

        		AnnotationElementType annotationType = null;
        		if (annotationIndex % 2 == 1) {
    				annotationType = AnnotationElementType.CONCEPT;
    			} else {
    				annotationType = AnnotationElementType.PROPERTY;
    			}
        		SemanticAnnotationElement elem = new SemanticAnnotationElement(annotationPart, annotationType);
        		this.annotationElements.add(elem);
        		
        		if (annotationType == AnnotationElementType.CONCEPT && individual != null) {
        			elem = new SemanticAnnotationElement(individual, AnnotationElementType.INDIVIDUAL);
            		this.annotationElements.add(elem);
            		
            		annotationIndex += 1;
        		}
        		
        		annotationIndex += 1;
        	}
        	
        }
 
        public String getAnnotation() {
            return this.fullAnnotation;
        }
        public List<SemanticAnnotationElement> getAnnotationElements() {
            return this.annotationElements;
        }
        public SemanticAnnotationElement getAnnotationPart(Integer index) {
            return this.annotationElements.get(index);
        }
		
        @Override
        public String toString() {
        	StringBuilder result = new StringBuilder();

            for (int i = 0; i < annotationElements.size(); i++) {
                SemanticAnnotationElement elem = annotationElements.get(i);
                String elementStr;

                if (elem.getType() == AnnotationElementType.INDIVIDUAL) {
                    elementStr = "{" + elem.getElement() + "}";
                } else {
                    elementStr = elem.getElement();
                }

                if (i > 0) {
                    SemanticAnnotationElement prevElem = annotationElements.get(i - 1);
                    if (elem.getType() != AnnotationElementType.INDIVIDUAL &&
                        prevElem.getType() != AnnotationElementType.INDIVIDUAL) {
                        result.append("/");
                    }
                }

                result.append(elementStr);
            }

            return result.toString();
        }
        
        public boolean isCompatibleWith(SemanticAnnotation otherSemanticAnnotation) {
            if (this == otherSemanticAnnotation) {
                return true;
            }
            if (otherSemanticAnnotation == null || getClass() != otherSemanticAnnotation.getClass()) {
                return false;
            }
            
            if (this.annotationElements.size() == otherSemanticAnnotation.annotationElements.size()) {
            	for (int i = 0; i < this.annotationElements.size(); i++) {
            		if (this.getAnnotationPart(i).getType().equals(otherSemanticAnnotation.getAnnotationPart(i).getType())) {
                    	switch (this.getAnnotationPart(i).getType()) {
                        case CONCEPT:
                            if(!semanticComparator.isConceptRelated(this.getAnnotationPart(i).getElement(), otherSemanticAnnotation.getAnnotationPart(i).getElement())) {
                            	return false;
                            }
                            break;
                        case PROPERTY:
                        	if (!semanticComparator.isPropertyRelated(this.getAnnotationPart(i).getElement(), otherSemanticAnnotation.getAnnotationPart(i).getElement())) {
                        		return false;
                        	}
                        	break;
                        case INDIVIDUAL:
                        	if (!semanticComparator.isIndividualRelated(this.getAnnotationPart(i).getElement(), otherSemanticAnnotation.getAnnotationPart(i).getElement())) {
                        		return false;
                        	}
                        	break;
                    	}
                    } else {
                    	return false;
                    }
            	}
            } else {
            	return false;
            }
        	return true;
        }
    }
    
	public boolean isSemanticAnnotationCompatible(SchemaModelReference otherSchemaModelReference) {
		if (semanticComparator == null) {
	        throw new NullPointerException("No Semantic Comparator Defined.");
		} else {
			List<SemanticAnnotation> thisConcepts = this.getSemanticAnnotations(AnnotationElementType.CONCEPT);
			List<SemanticAnnotation> otherConcepts = otherSchemaModelReference.getSemanticAnnotations(AnnotationElementType.CONCEPT);

			DTLogger.logger.fine("thisConcepts: " + thisConcepts);
			DTLogger.logger.fine("otherConcepts: " + otherConcepts);

			boolean conceptsCompatible = ((!thisConcepts.isEmpty() && !otherConcepts.isEmpty()) || (thisConcepts.isEmpty() && otherConcepts.isEmpty())) &&
					thisConcepts.stream()
	                .allMatch(concept -> otherConcepts.stream().anyMatch(otherConcept -> concept.isCompatibleWith(otherConcept)));

			List<SemanticAnnotation> thisProperties = this.getSemanticAnnotations(AnnotationElementType.PROPERTY);
			List<SemanticAnnotation> otherProperties = otherSchemaModelReference.getSemanticAnnotations(AnnotationElementType.PROPERTY);
			
	        boolean propertiesCompatible = ((!thisProperties.isEmpty() && !otherProperties.isEmpty()) || (thisProperties.isEmpty() && otherProperties.isEmpty())) &&
	        		thisProperties.stream()
	                .allMatch(property -> otherProperties.stream().anyMatch(otherProperty -> property.isCompatibleWith(otherProperty)));
	        
			List<SemanticAnnotation> thisIndividuals = this.getSemanticAnnotations(AnnotationElementType.INDIVIDUAL);
			List<SemanticAnnotation> otherIndividuals = otherSchemaModelReference.getSemanticAnnotations(AnnotationElementType.INDIVIDUAL);

			boolean individualsCompatible = ((!thisIndividuals.isEmpty() && !otherIndividuals.isEmpty()) || (thisIndividuals.isEmpty() && otherIndividuals.isEmpty())) &&
					otherIndividuals.stream()
		            .allMatch(otherIndividual -> thisIndividuals.stream().anyMatch(thisIndividual -> thisIndividual.isCompatibleWith(otherIndividual)));
			
			DTLogger.logger.fine("thisConcepts: " + thisConcepts);
			DTLogger.logger.fine("otherConcepts: " + otherConcepts);
			
			DTLogger.logger.fine("thisProperties: " + thisProperties);
			DTLogger.logger.fine("otherProperties: " + otherProperties);

			DTLogger.logger.fine("conceptsCompatible: " + conceptsCompatible + " | propertiesCompatible: " + propertiesCompatible + " | individualsCompatible: " + individualsCompatible);
			return conceptsCompatible && propertiesCompatible && individualsCompatible;
		}
	}
	
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder("SchemaModelReference{");

	    if (semanticAnnotations != null && !semanticAnnotations.isEmpty()) {
	        String annotationsStr = semanticAnnotations.stream()
	                .map(SemanticAnnotation::toString)
	                .collect(Collectors.joining(", "));
	        sb.append("semantic-annotations=[").append(annotationsStr).append("]");
	    } else {
	        sb.append("semantic-annotations=[]");
	    }

	    sb.append("}");
	    return sb.toString();
	}
}
