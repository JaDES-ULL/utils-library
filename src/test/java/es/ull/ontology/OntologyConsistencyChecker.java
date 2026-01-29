package es.ull.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;

import es.ull.simulation.ontology.LoadedOntology;
import es.ull.simulation.ontology.OWLOntologyWrapper;
import es.ull.simulation.ontology.OntologyLoadOptions;
import es.ull.simulation.ontology.OntologyLoader;

public class OntologyConsistencyChecker {
    private enum EntityType {
        ANNOTATION,
        OBJECT_PROPERTY,
        DATA_PROPERTY,
        CLASS,
        INDIVIDUAL
    }
    public record CheckResult(int missingClasses, int misusedProperties, List<String> issues) {}
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OntologyConsistencyChecker.class);
    private final OWLOntologyWrapper refWrapper;
    private final Map<IRI, EntityType> refEntityTypes = new HashMap<>();

    public OntologyConsistencyChecker(OWLOntologyWrapper refWrapper) {
        this.refWrapper = refWrapper;
        fillEntityTypes();
    }

    private void fillEntityTypes() {
        OWLOntology referenceOntology = refWrapper.getOntology();
        Set<IRI> refClasses = referenceOntology.classesInSignature(Imports.INCLUDED)
                        .map(OWLEntity::getIRI).collect(Collectors.toSet());
        for (IRI iri : refClasses) {
            refEntityTypes.put(iri, EntityType.CLASS);
        }
        Set<IRI> refObjProps = referenceOntology.objectPropertiesInSignature(Imports.INCLUDED)
                        .map(OWLEntity::getIRI).collect(Collectors.toSet());
        for (IRI iri : refObjProps) {
            refEntityTypes.put(iri,  EntityType.OBJECT_PROPERTY);
        }
        Set<IRI> refDataProps = referenceOntology.dataPropertiesInSignature(Imports.INCLUDED)
                        .map(OWLEntity::getIRI).collect(Collectors.toSet());
        for (IRI iri : refDataProps) {
            refEntityTypes.put(iri,  EntityType.DATA_PROPERTY);
        }
        Set<IRI> refAnnProps = referenceOntology.annotationPropertiesInSignature(Imports.INCLUDED)
                        .map(OWLEntity::getIRI).collect(Collectors.toSet());
        for (IRI iri : refAnnProps) {
            refEntityTypes.put(iri,  EntityType.ANNOTATION);
        }
        Set<IRI> refIndividuals = referenceOntology.individualsInSignature(Imports.INCLUDED)
                        .map(OWLEntity::getIRI).collect(Collectors.toSet());
        for (IRI iri : refIndividuals) {
            refEntityTypes.put(iri,  EntityType.INDIVIDUAL);
        }
    }

    private int[] check(OWLOntologyWrapper checkedWrapper, List<String> issues) {
        int[] results = new int[2];
        LOGGER.info("Checking ontology consistency between reference and checked ontologies.");
        results[0] = checkClasses(checkedWrapper, issues);
        results[1] = countWrongProperties(checkedWrapper, issues);
        return results;
    }

    public int countWrongAnnotationsForIndividual(OWLOntologyWrapper checkedWrapper, IRI individualIRI, List<String> issues) {
        Objects.requireNonNull(individualIRI, "individualIRI must not be null");
        int errors = 0;
        for (OWLAnnotationAssertionAxiom ax : checkedWrapper.getOntology().getAnnotationAssertionAxioms(individualIRI)) {
            if (ax.getSubject().isIRI() && ax.getSubject().asIRI().get().equals(individualIRI)) {
                final IRI propIri = ax.getProperty().getIRI();                
                final EntityType refType = refEntityTypes.get(propIri);
                if (refType != EntityType.ANNOTATION) {
                    issues.add("MISUSED PROPERTY IN ASSERTION: property " + propIri + " used in annotation for individual " + individualIRI + " should be an annotation property.");
                    errors++;
                }
            }
        }
        return errors;
    }

    public int countWrongObjectPropertiesForIndividual(OWLOntologyWrapper checkedWrapper, IRI individualIRI, List<String> issues) {
        Objects.requireNonNull(individualIRI, "individualIRI must not be null");
        int errors = 0;
        OWLNamedIndividual individual = Objects.requireNonNull(checkedWrapper.asOWLIndividual(individualIRI));
        final Set<OWLObjectPropertyAssertionAxiom> objProps = checkedWrapper.getOntology().getObjectPropertyAssertionAxioms(individual);
        for (OWLObjectPropertyAssertionAxiom ax : objProps) {
            final IRI propIri = ax.getProperty().asOWLObjectProperty().getIRI();                
            final EntityType refType = refEntityTypes.get(propIri);
            if (refType != EntityType.OBJECT_PROPERTY) {
                issues.add("MISUSED PROPERTY IN ASSERTION: property " + propIri + " used for individual " + individualIRI + " should be an object property.");
                errors++;
            }
            final IRI valueIRI = ax.getObject().asOWLNamedIndividual().getIRI();
            if (!checkedWrapper.getIndividualsInSignature(Imports.INCLUDED).contains(valueIRI)) {
                issues.add("INVALID OBJECT VALUE IN ASSERTION: value " + valueIRI + " of object property " + propIri + " is not an individual.");
                errors++;
            }            
        }
        return errors;
    }

    public int countWrongDataPropertiesForIndividual(OWLOntologyWrapper checkedWrapper, IRI individualIRI, List<String> issues) {
        Objects.requireNonNull(individualIRI, "individualIRI must not be null");
        int errors = 0;
        OWLNamedIndividual individual = Objects.requireNonNull(checkedWrapper.asOWLIndividual(individualIRI));
        final Set<OWLDataPropertyAssertionAxiom> dataProps = checkedWrapper.getOntology().getDataPropertyAssertionAxioms(individual);
        for (OWLDataPropertyAssertionAxiom ax : dataProps) {
            final IRI propIri = ax.getProperty().asOWLDataProperty().getIRI();                
            final EntityType refType = refEntityTypes.get(propIri);
            if (refType != EntityType.DATA_PROPERTY) {
                issues.add("MISUSED PROPERTY IN ASSERTION: property " + propIri + " used for individual " + individualIRI + " should be a data property.");
                errors++;
            }          
        }
        return errors;
    }

    public int countWrongTypesForIndividual(OWLOntologyWrapper checkedWrapper, IRI individualIRI, List<String> issues) {
        Objects.requireNonNull(individualIRI, "individualIRI must not be null");
        int errors = 0;
        Set<IRI> assertedClasses = checkedWrapper.getAssertedTypes(individualIRI, false, Imports.EXCLUDED);
        for (IRI classIRI : assertedClasses) {
            final EntityType refType = refEntityTypes.get(classIRI);
            if (refType != EntityType.CLASS) {
                issues.add("MISUSED TYPE IN ASSERTION: type " + classIRI + " asserted for individual " + individualIRI + " should be a class.");
                errors++;
            }
        }
        return errors;
    }

    public boolean checkEmptyTypeForIndividual(OWLOntologyWrapper checkedWrapper, IRI individualIRI, List<String> issues) {
        Objects.requireNonNull(individualIRI, "individualIRI must not be null");
        Set<IRI> assertedClasses = checkedWrapper.getAssertedTypes(individualIRI, false, Imports.EXCLUDED);
        return assertedClasses.isEmpty();
    }

    private int countWrongProperties(OWLOntologyWrapper checkedWrapper, List<String> issues) {
        int misusedProperties = 0;
        for (OWLAxiom ax : checkedWrapper.getOntology().getAxioms()) {
            if (ax instanceof OWLAnnotationAssertionAxiom) {
                final OWLAnnotationAssertionAxiom ann = (OWLAnnotationAssertionAxiom) ax;

                final IRI propIri = ann.getProperty().getIRI();
                boolean declaredAsAnnotation = checkedWrapper.getOntology()
                                .annotationPropertiesInSignature()
                                .anyMatch(p -> p.getIRI().equals(propIri));
                boolean declaredAsObject = checkedWrapper.getOntology()
                                .objectPropertiesInSignature()
                                .anyMatch(p -> p.getIRI().equals(propIri));
                boolean declaredAsData = checkedWrapper.getOntology()
                                .dataPropertiesInSignature()
                                .anyMatch(p -> p.getIRI().equals(propIri));
                
                final EntityType refType = refEntityTypes.get(propIri);
                if (refType == null) {
                    misusedProperties++;
                    issues.add("Property " + propIri + " in checked ontology is not present in reference ontology.");
                }
                else {
                    switch (refType) {
                        case OBJECT_PROPERTY:
                            if (!declaredAsObject) {
                                issues.add("MISUSED PROPERTY IN ASSERTION: property " + propIri + " should be an object property.");
                                misusedProperties++;
                            }
                            else if (declaredAsData || declaredAsAnnotation) {
                                issues.add("PUNNING DETECTED IN ASSERTION: property " + propIri + " is declared also as data or annotation property.");
                                misusedProperties++;
                            }
                            break;
                        case DATA_PROPERTY:
                            if (!declaredAsData) {
                                issues.add("MISUSED PROPERTY IN ASSERTION: property " + propIri + " should be a data property.");
                                misusedProperties++;
                            }
                            else if (declaredAsObject || declaredAsAnnotation) {
                                issues.add("PUNNING DETECTED IN ASSERTION: property " + propIri + " is declared also as object or annotation property.");
                                misusedProperties++;
                            }
                            break;
                        case ANNOTATION:
                            if (!declaredAsAnnotation) {
                                issues.add("MISUSED PROPERTY IN ASSERTION: property " + propIri + " should be an annotation property.");
                                misusedProperties++;
                            }
                            else if (declaredAsObject || declaredAsData) {
                                issues.add("PUNNING DETECTED IN ASSERTION: property " + propIri + " is declared also as object or data property.");
                                misusedProperties++;
                            }
                            break;
                        default:
                            break;
                    }
                }
                if (ann.getValue().isIRI()) { 
                    final IRI valueIRI = ann.getValue().asIRI().get();
                    if (refEntityTypes.containsKey(valueIRI)) {
                        final EntityType valueRefType = refEntityTypes.get(valueIRI);
                        if (valueRefType != EntityType.CLASS && valueRefType != EntityType.INDIVIDUAL) {
                            issues.add("MISUSED VALUE IN ASSERTION: value " + valueIRI + " of annotation property " + propIri + " should be a class or individual.");
                            misusedProperties++;
                        }
                    }
                }
            }
        }
        return misusedProperties;
    }

    private int checkClasses(OWLOntologyWrapper checkedWrapper, List<String> issues) {
        Set<IRI> refClasses = refWrapper.getClassesInSignature(Imports.INCLUDED);
        Set<IRI> checkedClasses = checkedWrapper.getClassesInSignature(Imports.INCLUDED);
        Set<IRI> notPresentClasses = new HashSet<>(checkedClasses);
        notPresentClasses.removeAll(refClasses);
        for (IRI notPresentClass : notPresentClasses) {
            issues.add("Class " + notPresentClass + " in checked ontology is not present in reference ontology.");
        }
        return notPresentClasses.size();
    }
    
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar " + OntologyConsistencyChecker.class.getSimpleName() + ".jar <reference ontology file/IRI> <checked ontology file/IRI>");
			return;
		}
		String refOntologyFileOrIRI = Objects.requireNonNull(args[0], "Ontology file/IRI must not be null");
		String checkedOntologyFileOrIRI = Objects.requireNonNull(args[1], "Checked ontology file/IRI must not be null");
        OntologyLoader loader = new OntologyLoader();
        try {
            final LoadedOntology loadedRefOntology = loader.load(refOntologyFileOrIRI);
            OWLOntologyWrapper refWrapper = new OWLOntologyWrapper(loadedRefOntology);
            final OWLOntologyLoaderConfiguration cfg = Objects.requireNonNull(new OWLOntologyLoaderConfiguration().setRepairIllegalPunnings(false)); 
            final OntologyLoadOptions options = new OntologyLoadOptions.Builder()
                    .owlConfig(cfg)
                    .build();
            final LoadedOntology loadedCheckOntology = loader.load(checkedOntologyFileOrIRI, options);
            OWLOntologyWrapper checkedWrapper = new OWLOntologyWrapper(loadedCheckOntology);
            checkedWrapper.getDebugPrinter().printTabulatedIndividuals(Imports.EXCLUDED);
            OntologyConsistencyChecker checker = new OntologyConsistencyChecker(refWrapper);
            List<String> issues = new ArrayList<>();
            int[] errors = checker.check(checkedWrapper, issues);
            System.out.println("Number of missing classes: " +  errors[0]);
            System.out.println("Number of misused properties: " +  errors[1]);
            for (String issue : issues) {
                System.out.println(issue);
            }
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
	}
}
