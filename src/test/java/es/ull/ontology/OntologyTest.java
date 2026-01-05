package es.ull.ontology;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ull.simulation.ontology.OWLOntologyWrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Set;

public class OntologyTest {
    private static final Logger log = LoggerFactory.getLogger(OntologyTest.class);
    private final static String SCHEMA_IRI = "http://example.org/sample";
    private final static String SCHEMA_FILE = "/sample-schema.owl";
    private final static String REMOTE_ONTOLOGY_IRI = "https://w3id.org/ontologies-ULL/OSDi";
    private final static String REMOTE_ONTOLOGY_VERSIONED_IRI = "https://w3id.org/ontologies-ULL/OSDi/1.0";
    private final static String REMOTE_INDIVIDUALS_VERSIONED_IRI = "https://w3id.org/ontologies-ULL/OSDi/1.0/individuals/PBD.ttl";
    private final static String REMOTE_TEST_DISEASE_INDIVIDUAL = "PBD_ProfoundBiotinidaseDeficiency";
    private final static String REMOTE_TEST_DATAITEMTYPE_CLASS = "DataItemType";
    private final static String REMOTE_TEST_DATAITEMTYPE_INDIVIDUAL = "DI_StandardDeviation";
    private final static String DATA_FILE = "/sample-data.owl";
    private final static String WRONG_DATA_FILE = "/wrong-sample-data.owl";
    private final static String TEST_DISEASE_CLASS = "Disease";
    private final static String TEST_SUPER_CLASS = "ModelElement";
    private final static String TEST_MODEL_CLASS = "SimulationModel";
    private final static String TEST_DISEASE_INDIVIDUAL = "T1DM";
    private final static String TEST_PRELOADED_DISEASE_INDIVIDUAL = "BiotinidaseDeficiency_General";
    private final static String TEST_DISEASE_OBJ_PROPERTY = "includedByModel";
    private final static String TEST_DISEASE_DATA_PROPERTY = "hasDescription";
    private final static String TEST_DISEASE_DESCRIPTION = "Type 1 Diabetes Mellitus";
    private final static String TEST_MODEL_INDIVIDUAL = "T1DM_Model";
    private final static String TEST_MODEL_DATA_PROPERTY = "hasStudyYear";
    private final static String TEST_MODEL_STUDY_YEAR = "2020";
    private final static String TEST_INVALID_CLASS = "InvalidDiseaseClass";
    private final static String TEST_INVALID_DISEASE_INDIVIDUAL = "T2DM";
    private final static String TEST_INVALID_MODEL_INDIVIDUAL = "T2DM_Model";
    private final static String TEST_INVALID_DISEASE_OBJ_PROPERTY = "included";
    private final static String TEST_INVALID_DISEASE_DATA_PROPERTY = "hasSource";
    private final static String TEST_INVALID_MODEL_DATA_PROPERTY = "hasValue";

    public OWLOntologyWrapper createDefaultOwlOntologyWrapper() throws OWLOntologyCreationException {
        try {
            final File tmp = File.createTempFile("schema", ".owl");
            tmp.deleteOnExit();
            Files.copy(getClass().getResourceAsStream(SCHEMA_FILE), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(getClass().getResourceAsStream(DATA_FILE), SCHEMA_IRI + "=" + tmp.getAbsolutePath());
            log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
            return ontologyWrapper;
        } catch (IOException e) {
            throw new OWLOntologyCreationException("Error copying schema file to temp location", e);
        }
    }

    @Test
    public void testOntologyLoading() throws OWLOntologyCreationException {
        final InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_FILE);
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(schemaStream);
        assertEquals(SCHEMA_IRI, ontologyWrapper.getOntology().getOntologyID().getOntologyIRI().get().toString());
        log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
    }

    @Test
    public void testRemoteOntologyLoading() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(IRI.create(REMOTE_ONTOLOGY_VERSIONED_IRI));
        final String loadedOntologyIRI = ontologyWrapper.getOntology().getOntologyID().getOntologyIRI().get().getIRIString().trim();
        assertEquals(REMOTE_ONTOLOGY_IRI, loadedOntologyIRI, "Loaded ontology IRI (" + loadedOntologyIRI + ") and remote ontology IRI (" + REMOTE_ONTOLOGY_IRI + ") should match");
        log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
    }

    @Test
    public void testRemoteIndividualsLoading() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(IRI.create(REMOTE_INDIVIDUALS_VERSIONED_IRI));
        final Set<String> diseases = ontologyWrapper.getIndividuals(TEST_DISEASE_CLASS);
        assertTrue(diseases.contains(REMOTE_TEST_DISEASE_INDIVIDUAL), "The disease individual " + REMOTE_TEST_DISEASE_INDIVIDUAL + " should be present");
        assertTrue(!diseases.contains(TEST_DISEASE_INDIVIDUAL), "The disease individual " + TEST_DISEASE_INDIVIDUAL + " should not be present");
        final Set<String> dataItemTypes = ontologyWrapper.getIndividuals(REMOTE_TEST_DATAITEMTYPE_CLASS);
        assertTrue(dataItemTypes.contains(REMOTE_TEST_DATAITEMTYPE_INDIVIDUAL), "The DataItemType individual " + REMOTE_TEST_DATAITEMTYPE_INDIVIDUAL + " should be present");
        dataItemTypes.forEach(ind -> log.debug(" - " + ind));
    }

    @Test
    public void testOntologyIndividuals() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        // If the individual was created correctly, this call should return false
        assertTrue(!ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        // And this should be false too, since it was already present
        assertTrue(!ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_PRELOADED_DISEASE_INDIVIDUAL));
        log.debug("Individuals in the ontology:");
        ontologyWrapper.getIndividuals(TEST_DISEASE_CLASS).forEach(ind -> log.debug(" - " + ind));
    }

    @Test
    public void testAddObjectPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        assertTrue(ontologyWrapper.addIndividual(TEST_MODEL_CLASS, TEST_MODEL_INDIVIDUAL));

        // Valid case: T1DM is included by model T1DM_Model
        boolean added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY, TEST_MODEL_INDIVIDUAL);
        assertTrue(added, "The object property value should be added");

        // Invalid case: "T2DM" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_INVALID_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY, TEST_MODEL_INDIVIDUAL);
        assertTrue(!added, "The object property value should not be added");
        // Invalid case: "T2DM_Model" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY, TEST_INVALID_MODEL_INDIVIDUAL);
        assertTrue(!added, "The object property value should not be added");
        // Invalid case: "included" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_INVALID_DISEASE_OBJ_PROPERTY, TEST_MODEL_INDIVIDUAL);
        assertTrue(!added, "The object property value should not be added");
        Set<String> includedByModel = ontologyWrapper.getObjectPropertyValues(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY);
        log.debug("Property values for '" + TEST_DISEASE_OBJ_PROPERTY + "' of " + TEST_DISEASE_INDIVIDUAL + ":");
        includedByModel.forEach(ind -> log.debug(" - " + ind));
        assertTrue(includedByModel.contains(TEST_MODEL_INDIVIDUAL), "The includedByModel property should contain " + TEST_MODEL_INDIVIDUAL);
        assertTrue(includedByModel.size() == 1, "The includedByModel property should have exactly one value");
    }

    @Test
    public void testAddDataPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        assertTrue(ontologyWrapper.addIndividual(TEST_MODEL_CLASS, TEST_MODEL_INDIVIDUAL));

        // Valid case: T1DM has a description
        boolean added = ontologyWrapper.addDataPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue(added, "The data property value should be added");
        // Invalid case: "T2DM" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_INVALID_DISEASE_INDIVIDUAL, TEST_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue(!added, "The data property value should not be added");
        // Invalid case: "hasSource" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_INVALID_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue(!added, "The data property value should not be added");

        // Valid case: T1DM_Model has a year
        added = ontologyWrapper.addDataPropertyValue(TEST_MODEL_INDIVIDUAL, TEST_MODEL_DATA_PROPERTY, TEST_MODEL_STUDY_YEAR, OWL2Datatype.XSD_INTEGER);
        assertTrue(added, "The data property value should be added");
        // Invalid case: "T2DM_Model" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_INVALID_MODEL_INDIVIDUAL, TEST_MODEL_DATA_PROPERTY, "2021", OWL2Datatype.XSD_INTEGER);
        assertTrue(!added, "The data property value should not be added");
        // Invalid case: "hasValue" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_MODEL_INDIVIDUAL, TEST_INVALID_MODEL_DATA_PROPERTY, "23", OWL2Datatype.XSD_INTEGER);
        assertTrue(!added, "The data property value should not be added");

        ArrayList<String> prop = ontologyWrapper.getDataPropertyValues(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_DATA_PROPERTY);
        log.debug("Property values for '" + TEST_DISEASE_DATA_PROPERTY + "' of " + TEST_DISEASE_INDIVIDUAL + ":");
        prop.forEach(ind -> log.debug(" - " + ind));
        assertTrue(prop.size() == 1, "The " + TEST_DISEASE_DATA_PROPERTY + " property should have exactly one value");
        assertTrue(prop.contains(TEST_DISEASE_DESCRIPTION), "The " + TEST_DISEASE_DATA_PROPERTY + " property should contain '" + TEST_DISEASE_DESCRIPTION + "'");
        prop = ontologyWrapper.getDataPropertyValues(TEST_MODEL_INDIVIDUAL, TEST_MODEL_DATA_PROPERTY);
        log.debug("Property values for '" + TEST_MODEL_DATA_PROPERTY + "' of " + TEST_MODEL_INDIVIDUAL + ":");
        prop.forEach(ind -> log.debug(" - " + ind));
        assertTrue(prop.size() == 1, "The " + TEST_MODEL_DATA_PROPERTY + " property should have exactly one value");
        assertTrue(prop.contains(TEST_MODEL_STUDY_YEAR), "The " + TEST_MODEL_DATA_PROPERTY + " property should contain '" + TEST_MODEL_STUDY_YEAR + "'");
    }

    @Test
    public void testOntologyRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        // Checking individuals
        // These should be ok...
        assertNotNull(ontologyWrapper.getOWLIndividual(TEST_PRELOADED_DISEASE_INDIVIDUAL), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist");
        assertNotNull(ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INDIVIDUAL), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist");
        // This should be ok...
        assertNull(ontologyWrapper.getOWLIndividualIfExists(TEST_DISEASE_INDIVIDUAL), "The individual " + TEST_DISEASE_INDIVIDUAL + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.getOWLIndividual(TEST_DISEASE_INDIVIDUAL), "The individual " + TEST_DISEASE_INDIVIDUAL + " should not exist but this method should work anyway");
        log.debug("Access to individuals checked");
        ontologyWrapper.individualsToString().forEach(ind -> log.debug(" - " + ind));
        // Checking classes
        // These should be ok...
        assertNotNull(ontologyWrapper.getOWLClass(TEST_DISEASE_CLASS), "The class " + TEST_DISEASE_CLASS + " should exist");
        assertNotNull(ontologyWrapper.getOWLClassIfExists(TEST_MODEL_CLASS), "The class " + TEST_MODEL_CLASS + " should exist");
        // This should be ok...
        assertNull(ontologyWrapper.getOWLClassIfExists(TEST_INVALID_CLASS), "The class " + TEST_INVALID_CLASS + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.getOWLClass(TEST_INVALID_CLASS), "The class " + TEST_INVALID_CLASS + " should not exist but this method should work anyway");
        log.debug("Access to classes checked");
        ontologyWrapper.classesToString().forEach(cls -> log.debug(" - " + cls));
        // Checking dataProperties
        // These should be ok...
        assertNotNull(ontologyWrapper.getOWLDataProperty(TEST_DISEASE_DATA_PROPERTY), "The data property " + TEST_DISEASE_DATA_PROPERTY + " should exist");
        assertNotNull(ontologyWrapper.getOWLDataPropertyIfExists(TEST_DISEASE_DATA_PROPERTY), "The data property " + TEST_DISEASE_DATA_PROPERTY + " should exist");
        // This should be ok...
        assertNull(ontologyWrapper.getOWLDataPropertyIfExists(TEST_INVALID_DISEASE_DATA_PROPERTY), "The data property " + TEST_INVALID_DISEASE_DATA_PROPERTY + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.getOWLDataProperty(TEST_INVALID_DISEASE_DATA_PROPERTY), "The data property " + TEST_INVALID_DISEASE_DATA_PROPERTY + " should not exist but this method should work anyway");
        log.debug("Access to data properties checked");
        ontologyWrapper.dataPropertiesToString().forEach(dp -> log.debug(" - " + dp));
        // Checking objectProperties
        // These should be ok...
        assertNotNull(ontologyWrapper.getOWLObjectProperty(TEST_DISEASE_OBJ_PROPERTY), "The object property " + TEST_DISEASE_OBJ_PROPERTY + " should exist");
        assertNotNull(ontologyWrapper.getOWLObjectPropertyIfExists(TEST_DISEASE_OBJ_PROPERTY), "The object property " + TEST_DISEASE_OBJ_PROPERTY + " should exist");
        // This should be ok...
        assertNull(ontologyWrapper.getOWLObjectPropertyIfExists(TEST_INVALID_DISEASE_OBJ_PROPERTY), "The object property " + TEST_INVALID_DISEASE_OBJ_PROPERTY + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.getOWLObjectProperty(TEST_INVALID_DISEASE_OBJ_PROPERTY), "The object property " + TEST_INVALID_DISEASE_OBJ_PROPERTY + " should not exist but this method should work anyway");
        log.debug("Access to object properties checked");
        ontologyWrapper.objectPropertiesToString().forEach(op -> log.debug(" - " + op));
    }

    @Test
    public void testOntologyStructure() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INDIVIDUAL, TEST_DISEASE_CLASS),
            "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should be a subclass of " + TEST_DISEASE_CLASS); 
        assertTrue(ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INDIVIDUAL, TEST_SUPER_CLASS),
            "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should be a subclass of " + TEST_SUPER_CLASS);
        assertTrue(!ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INDIVIDUAL, TEST_MODEL_CLASS),
            "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should not be a subclass of " + TEST_MODEL_CLASS);

        Set<String> clazzes = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INDIVIDUAL, true);
        log.debug("Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        clazzes.forEach(c -> log.debug(" - " + c));
        assertTrue(clazzes.size() == 3, TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to 3 classes");
        assertTrue(clazzes.contains(TEST_DISEASE_CLASS), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_DISEASE_CLASS);
        assertTrue(clazzes.contains(TEST_SUPER_CLASS), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_SUPER_CLASS);
        clazzes = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        log.debug("DIRECT Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        clazzes.forEach(c -> log.debug(" - " + c));
        assertTrue(clazzes.size() == 1, TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to 1 class");
        assertTrue(clazzes.contains(TEST_DISEASE_CLASS), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_DISEASE_CLASS);
        assertTrue(!clazzes.contains(TEST_SUPER_CLASS), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should not (directly) belong to class " + TEST_SUPER_CLASS);
    }

    @Test
    public void testLabelRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        String label = ontologyWrapper.getLabelForIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL, "en");
        log.debug("Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in English: " + label);
        assertEquals("Biotinidase deficiency", label, "Label should match");
        label = ontologyWrapper.getLabelForIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL, "es");
        log.debug("Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in Spanish: " + label);
        assertEquals("Deficiencia de biotinidasa", label, "Label should match");
    }

    @Test
    public void testOntologyMerging() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(getClass().getResourceAsStream(SCHEMA_FILE));
        log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());

        ontologyWrapper.mergeOtherOntology(getClass().getResourceAsStream(DATA_FILE));
        final OWLOntology mergedOntology = ontologyWrapper.getOntology();
        log.debug("Data ontology merged. New ontology: " + mergedOntology.getOntologyID());
        // Mostrar individuos en la ontología mergeada
        mergedOntology.individualsInSignature().forEach(ind -> log.debug(" - " + ind));
        assertNotNull(ontologyWrapper.getOWLClassIfExists(TEST_MODEL_CLASS), "The class " + TEST_MODEL_CLASS + " should exist");
        assertNotNull(ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INDIVIDUAL), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist");
    }

    @Test
    public void testOntologyDoubleLoading() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(getClass().getResourceAsStream(SCHEMA_FILE));
        log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());

        ontologyWrapper.addOntology(getClass().getResourceAsStream(DATA_FILE));
        final OWLOntology mergedOntology = ontologyWrapper.getOntology();
        log.debug("Another ontology loaded. New ontology: " + mergedOntology.getOntologyID());
        // Mostrar individuos en la ontología mergeada
        mergedOntology.individualsInSignature().forEach(ind -> log.debug(" - " + ind));
        assertNotNull(ontologyWrapper.getOWLClassIfExists(TEST_MODEL_CLASS), "The class " + TEST_MODEL_CLASS + " should exist");
        assertNotNull(ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INDIVIDUAL), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist");
        Set<String> diseaseClasses = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        log.debug("Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        diseaseClasses.forEach(c -> log.debug(" - " + c));
    }

    @Test
    public void testNonCompliantOntology() throws OWLOntologyCreationException {
        assertThrows(OWLOntologyCreationException.class, () -> {
            new OWLOntologyWrapper(getClass().getResourceAsStream(WRONG_DATA_FILE));
        }, "Loading " + WRONG_DATA_FILE + ", which is a non-compliant ontology, should throw exception");
    }
}
