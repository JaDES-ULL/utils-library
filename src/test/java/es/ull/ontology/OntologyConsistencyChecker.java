package es.ull.ontology;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
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
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OntologyConsistencyChecker.class);
    private final OWLOntologyWrapper refWrapper;
    private final OWLOntologyWrapper checkedWrapper;
    private final Map<IRI, EntityType> refEntityTypes = new HashMap<>();
    private final List<String> issues = new ArrayList<>();
    private int missingClasses = 0;
    private int missingProperties = 0;
    private int misusedProperties = 0;

    private OntologyConsistencyChecker(OWLOntologyWrapper refWrapper, OWLOntologyWrapper checkedWrapper) {
        this.refWrapper = refWrapper;
        this.checkedWrapper = checkedWrapper;
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

    public List<String> getIssues() {
        return List.copyOf(issues);
    }

    public int getMissingClassesCount() {
        return missingClasses;
    }
    
    public int getMissingPropertiesCount() {
        return missingProperties;
    }
    
    public int getMisusedPropertiesCount() {
        return misusedProperties;
    }

    private void check() {
        LOGGER.info("Checking ontology consistency between reference and checked ontologies.");
        checkClasses();
        checkProperties();
    }

    private void checkProperties() {
        for (OWLAxiom ax : checkedWrapper.getOntology().getAxioms()) {

            if (ax instanceof OWLAnnotationAssertionAxiom) {
                OWLAnnotationAssertionAxiom ann = (OWLAnnotationAssertionAxiom) ax;

                IRI propIri = ann.getProperty().getIRI();
                if (propIri.toString().contains("Freq")) {
                    LOGGER.info("Debug Freq property found: {}", ann);
                }
                boolean declaredAsAnnotation = checkedWrapper.getOntology()
                                .annotationPropertiesInSignature()
                                .anyMatch(p -> p.getIRI().equals(propIri));
                boolean declaredAsObject = checkedWrapper.getOntology()
                                .objectPropertiesInSignature()
                                .anyMatch(p -> p.getIRI().equals(propIri));
                boolean declaredAsData = checkedWrapper.getOntology()
                                .dataPropertiesInSignature()
                                .anyMatch(p -> p.getIRI().equals(propIri));
                
                EntityType refType = refEntityTypes.get(propIri);
                if (refType == null) {
                    missingProperties++;
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
            }
        }
    }

    private void checkClasses() {
        Set<IRI> refClasses = refWrapper.getClassesInSignature(Imports.INCLUDED);
        Set<IRI> checkedClasses = checkedWrapper.getClassesInSignature(Imports.INCLUDED);
        Set<IRI> notPresentClasses = new HashSet<>(checkedClasses);
        notPresentClasses.removeAll(refClasses);
        for (IRI notPresentClass : notPresentClasses) {
            issues.add("Class " + notPresentClass + " in checked ontology is not present in reference ontology.");
        }
        missingClasses = notPresentClasses.size();
    }

    private static OWLOntologyWrapper loadReferenceOntology(String refOntologyFileOrIRI) {
        Objects.requireNonNull(refOntologyFileOrIRI, "Reference ontology file/IRI must not be null");
        boolean isURI = true;
        OWLOntologyWrapper wrapper = null;
        try {
            new URI(refOntologyFileOrIRI);
        } catch (Exception e) {
            isURI = false;
        }
        
        try {
            OntologyLoader loader = new OntologyLoader();
            OWLOntologyDocumentSource source;
            if (isURI) {
                source = new IRIDocumentSource(Objects.requireNonNull(IRI.create(refOntologyFileOrIRI)));
            }
            else {
                final Path path = Paths.get(refOntologyFileOrIRI);
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    System.err.println("The specified ontology file does not exist or is not a regular file: " + refOntologyFileOrIRI);
                    return null;
                }
                source = new FileDocumentSource(Objects.requireNonNull(path.toFile()));
            }
            final LoadedOntology loadedOntology = loader.load(source);
            wrapper = new OWLOntologyWrapper(loadedOntology);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return wrapper;
    }

    private static OWLOntologyWrapper loadCheckedOntology(String checkedOntologyFileOrIRI) {
        Objects.requireNonNull(checkedOntologyFileOrIRI, "Checked ontology file/IRI must not be null");
        boolean isURI = true;
        OWLOntologyWrapper wrapper = null;
        try {
            new URI(checkedOntologyFileOrIRI);
        } catch (Exception e) {
            isURI = false;
        }
        
         try {
            OntologyLoader loader = new OntologyLoader();
            OWLOntologyDocumentSource source;
            if (isURI) {
                source = new IRIDocumentSource(Objects.requireNonNull(IRI.create(checkedOntologyFileOrIRI)));
            }
            else {
                final Path path = Paths.get(checkedOntologyFileOrIRI);
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    System.err.println("The specified ontology file does not exist or is not a regular file: " + checkedOntologyFileOrIRI);
                    return null;
                }
                source = new FileDocumentSource(Objects.requireNonNull(path.toFile()));
            }
            final OWLOntologyLoaderConfiguration cfg = Objects.requireNonNull(new OWLOntologyLoaderConfiguration().setRepairIllegalPunnings(false)); 
            final OntologyLoadOptions options = new OntologyLoadOptions.Builder()
                    .owlConfig(cfg)
                    .build();

            final LoadedOntology loadedOntology = loader.load(source, options);
            wrapper = new OWLOntologyWrapper(loadedOntology);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return wrapper;
    }
    
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar " + OntologyConsistencyChecker.class.getSimpleName() + ".jar <reference ontology file/IRI> <checked ontology file/IRI>");
			return;
		}
		String refOntologyFileOrIRI = Objects.requireNonNull(args[0], "Ontology file/IRI must not be null");
		String checkedOntologyFileOrIRI = Objects.requireNonNull(args[1], "Checked ontology file/IRI must not be null");
        OWLOntologyWrapper refWrapper = loadReferenceOntology(refOntologyFileOrIRI);
        OWLOntologyWrapper checkedWrapper = loadCheckedOntology(checkedOntologyFileOrIRI);
        checkedWrapper.getDebugPrinter().printTabulatedIndividuals(Imports.EXCLUDED);
        OntologyConsistencyChecker checker = new OntologyConsistencyChecker(refWrapper, checkedWrapper);
        checker.check();
	}
}
