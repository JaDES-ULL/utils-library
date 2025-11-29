package es.ull.ontology;

import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import es.ull.simulation.ontology.OWLOntologyWrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Set;

public class OntologyTest {
    private final static boolean DEBUG = true;
    private final static String SCHEMA_IRI = "http://example.org/sample";
    private final static String SCHEMA_FILE = "/sample-schema.owl";
    private final static String REMOTE_ONTOLOGY_IRI = "https://w3id.org/ontologies-ULL/OSDi";
    private final static String REMOTE_ONTOLOGY_VERSIONED_IRI = "https://w3id.org/ontologies-ULL/OSDi/1.0";
    private final static String REMOTE_INDIVIDUALS_VERSIONED_IRI = "https://w3id.org/ontologies-ULL/OSDi/1.0/individuals/PBD.owl";
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


    public static void debugPrint(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }
    
    public OWLOntologyWrapper createDefaultOwlOntologyWrapper() throws OWLOntologyCreationException {
        try {
            final File tmp = File.createTempFile("schema", ".owl");
            tmp.deleteOnExit();
            Files.copy(getClass().getResourceAsStream(SCHEMA_FILE), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(getClass().getResourceAsStream(DATA_FILE), SCHEMA_IRI + "=" + tmp.getAbsolutePath());
            debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
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
        debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
    }

    @Test
    public void testRemoteOntologyLoading() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(IRI.create(REMOTE_ONTOLOGY_VERSIONED_IRI));
        final String loadedOntologyIRI = ontologyWrapper.getOntology().getOntologyID().getOntologyIRI().get().getIRIString().trim();
        assertEquals("Loaded ontology IRI (" + loadedOntologyIRI + ") and remote ontology IRI (" + REMOTE_ONTOLOGY_IRI + ") should match", REMOTE_ONTOLOGY_IRI, loadedOntologyIRI);
        debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
    }

    @Test
    public void testRemoteIndividualsLoading() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(IRI.create(REMOTE_INDIVIDUALS_VERSIONED_IRI));
        final Set<String> diseases = ontologyWrapper.getIndividuals(TEST_DISEASE_CLASS);
        assertTrue("The disease individual " + REMOTE_TEST_DISEASE_INDIVIDUAL + " should be present", diseases.contains(REMOTE_TEST_DISEASE_INDIVIDUAL));
        assertTrue("The disease individual " + TEST_DISEASE_INDIVIDUAL + " should not be present", !diseases.contains(TEST_DISEASE_INDIVIDUAL));
        final Set<String> dataItemTypes = ontologyWrapper.getIndividuals(REMOTE_TEST_DATAITEMTYPE_CLASS);
        assertTrue("The DataItemType individual " + REMOTE_TEST_DATAITEMTYPE_INDIVIDUAL + " should be present", dataItemTypes.contains(REMOTE_TEST_DATAITEMTYPE_INDIVIDUAL));
        dataItemTypes.forEach(ind -> debugPrint(" - " + ind));
    }

    @Test
    public void testOntologyIndividuals() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        // If the individual was created correctly, this call should return false
        assertTrue(!ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        // And this should be false too, since it was already present
        assertTrue(!ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_PRELOADED_DISEASE_INDIVIDUAL));
        debugPrint("Individuals in the ontology:");
        ontologyWrapper.getIndividuals(TEST_DISEASE_CLASS).forEach(ind -> debugPrint(" - " + ind));
    }

    @Test
    public void testAddObjectPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        assertTrue(ontologyWrapper.addIndividual(TEST_MODEL_CLASS, TEST_MODEL_INDIVIDUAL));

        // Valid case: T1DM is included by model T1DM_Model
        boolean added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY, TEST_MODEL_INDIVIDUAL);
        assertTrue("The object property value should be added", added);

        // Invalid case: "T2DM" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_INVALID_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY, TEST_MODEL_INDIVIDUAL);
        assertTrue("The object property value should not be added", !added);
        // Invalid case: "T2DM_Model" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY, TEST_INVALID_MODEL_INDIVIDUAL);
        assertTrue("The object property value should not be added", !added);
        // Invalid case: "included" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_INVALID_DISEASE_OBJ_PROPERTY, TEST_MODEL_INDIVIDUAL);
        assertTrue("The object property value should not be added", !added);
        Set<String> includedByModel = ontologyWrapper.getObjectPropertyValues(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_OBJ_PROPERTY);
        debugPrint("Property values for '" + TEST_DISEASE_OBJ_PROPERTY + "' of " + TEST_DISEASE_INDIVIDUAL + ":");
        includedByModel.forEach(ind -> debugPrint(" - " + ind));
        assertTrue("The includedByModel property should contain " + TEST_MODEL_INDIVIDUAL, includedByModel.contains(TEST_MODEL_INDIVIDUAL));
        assertTrue("The includedByModel property should have exactly one value", includedByModel.size() == 1);
    }

    @Test
    public void testAddDataPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INDIVIDUAL));
        assertTrue(ontologyWrapper.addIndividual(TEST_MODEL_CLASS, TEST_MODEL_INDIVIDUAL));

        // Valid case: T1DM has a description
        boolean added = ontologyWrapper.addDataPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue("The data property value should be added", added);
        // Invalid case: "T2DM" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_INVALID_DISEASE_INDIVIDUAL, TEST_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue("The data property value should not be added", !added);
        // Invalid case: "hasSource" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_DISEASE_INDIVIDUAL, TEST_INVALID_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue("The data property value should not be added", !added);

        // Valid case: T1DM_Model has a year
        added = ontologyWrapper.addDataPropertyValue(TEST_MODEL_INDIVIDUAL, TEST_MODEL_DATA_PROPERTY, TEST_MODEL_STUDY_YEAR, OWL2Datatype.XSD_INTEGER);
        assertTrue("The data property value should be added", added);
        // Invalid case: "T2DM_Model" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_INVALID_MODEL_INDIVIDUAL, TEST_MODEL_DATA_PROPERTY, "2021", OWL2Datatype.XSD_INTEGER);
        assertTrue("The data property value should not be added", !added);
        // Invalid case: "hasValue" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_MODEL_INDIVIDUAL, TEST_INVALID_MODEL_DATA_PROPERTY, "23", OWL2Datatype.XSD_INTEGER);
        assertTrue("The data property value should not be added", !added);

        ArrayList<String> prop = ontologyWrapper.getDataPropertyValues(TEST_DISEASE_INDIVIDUAL, TEST_DISEASE_DATA_PROPERTY);
        debugPrint("Property values for '" + TEST_DISEASE_DATA_PROPERTY + "' of " + TEST_DISEASE_INDIVIDUAL + ":");
        prop.forEach(ind -> debugPrint(" - " + ind));
        assertTrue("The " + TEST_DISEASE_DATA_PROPERTY + " property should have exactly one value", prop.size() == 1);
        assertTrue("The " + TEST_DISEASE_DATA_PROPERTY + " property should contain '" + TEST_DISEASE_DESCRIPTION + "'", prop.contains(TEST_DISEASE_DESCRIPTION));
        prop = ontologyWrapper.getDataPropertyValues(TEST_MODEL_INDIVIDUAL, TEST_MODEL_DATA_PROPERTY);
        debugPrint("Property values for '" + TEST_MODEL_DATA_PROPERTY + "' of " + TEST_MODEL_INDIVIDUAL + ":");
        prop.forEach(ind -> debugPrint(" - " + ind));
        assertTrue("The " + TEST_MODEL_DATA_PROPERTY + " property should have exactly one value", prop.size() == 1);
        assertTrue("The " + TEST_MODEL_DATA_PROPERTY + " property should contain '" + TEST_MODEL_STUDY_YEAR + "'", prop.contains(TEST_MODEL_STUDY_YEAR));
    }

    @Test
    public void testOntologyRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        // Checking individuals
        // These should be ok...
        assertNotNull("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist", ontologyWrapper.getOWLIndividual(TEST_PRELOADED_DISEASE_INDIVIDUAL));
        assertNotNull("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist", ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INDIVIDUAL));
        // This should be ok...
        assertNull("The individual " + TEST_DISEASE_INDIVIDUAL + " should not exist", ontologyWrapper.getOWLIndividualIfExists(TEST_DISEASE_INDIVIDUAL));
        // ... but this too
        assertNotNull("The individual " + TEST_DISEASE_INDIVIDUAL + " should not exist but this method should work anyway", ontologyWrapper.getOWLIndividual(TEST_DISEASE_INDIVIDUAL));
        debugPrint("Access to individuals checked");
        ontologyWrapper.individualsToString().forEach(ind -> debugPrint(" - " + ind));
        // Checking classes
        // These should be ok...
        assertNotNull("The class " + TEST_DISEASE_CLASS + " should exist", ontologyWrapper.getOWLClass(TEST_DISEASE_CLASS));
        assertNotNull("The class " + TEST_MODEL_CLASS + " should exist", ontologyWrapper.getOWLClassIfExists(TEST_MODEL_CLASS));
        // This should be ok...
        assertNull("The class " + TEST_INVALID_CLASS + " should not exist", ontologyWrapper.getOWLClassIfExists(TEST_INVALID_CLASS));
        // ... but this too
        assertNotNull("The class " + TEST_INVALID_CLASS + " should not exist but this method should work anyway", ontologyWrapper.getOWLClass(TEST_INVALID_CLASS));
        debugPrint("Access to classes checked");
        ontologyWrapper.classesToString().forEach(cls -> debugPrint(" - " + cls));
        // Checking dataProperties
        // These should be ok...
        assertNotNull("The data property " + TEST_DISEASE_DATA_PROPERTY + " should exist", ontologyWrapper.getOWLDataProperty(TEST_DISEASE_DATA_PROPERTY));
        assertNotNull("The data property " + TEST_DISEASE_DATA_PROPERTY + " should exist", ontologyWrapper.getOWLDataPropertyIfExists(TEST_DISEASE_DATA_PROPERTY));
        // This should be ok...
        assertNull("The data property " + TEST_INVALID_DISEASE_DATA_PROPERTY + " should not exist", ontologyWrapper.getOWLDataPropertyIfExists(TEST_INVALID_DISEASE_DATA_PROPERTY));
        // ... but this too
        assertNotNull("The data property " + TEST_INVALID_DISEASE_DATA_PROPERTY + " should not exist but this method should work anyway", ontologyWrapper.getOWLDataProperty(TEST_INVALID_DISEASE_DATA_PROPERTY));
        debugPrint("Access to data properties checked");
        ontologyWrapper.dataPropertiesToString().forEach(dp -> debugPrint(" - " + dp));
        // Checking objectProperties
        // These should be ok...
        assertNotNull("The object property " + TEST_DISEASE_OBJ_PROPERTY + " should exist", ontologyWrapper.getOWLObjectProperty(TEST_DISEASE_OBJ_PROPERTY));
        assertNotNull("The object property " + TEST_DISEASE_OBJ_PROPERTY + " should exist", ontologyWrapper.getOWLObjectPropertyIfExists(TEST_DISEASE_OBJ_PROPERTY));
        // This should be ok...
        assertNull("The object property " + TEST_INVALID_DISEASE_OBJ_PROPERTY + " should not exist", ontologyWrapper.getOWLObjectPropertyIfExists(TEST_INVALID_DISEASE_OBJ_PROPERTY));
        // ... but this too
        assertNotNull("The object property " + TEST_INVALID_DISEASE_OBJ_PROPERTY + " should not exist but this method should work anyway", ontologyWrapper.getOWLObjectProperty(TEST_INVALID_DISEASE_OBJ_PROPERTY));
        debugPrint("Access to object properties checked");
        ontologyWrapper.objectPropertiesToString().forEach(op -> debugPrint(" - " + op));
    }

    @Test
    public void testOntologyStructure() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should be a subclass of " + TEST_DISEASE_CLASS,
            ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INDIVIDUAL, TEST_DISEASE_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should be a subclass of " + TEST_SUPER_CLASS,
            ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INDIVIDUAL, TEST_SUPER_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should not be a subclass of " + TEST_MODEL_CLASS,
            !ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INDIVIDUAL, TEST_MODEL_CLASS));

        Set<String> clazzes = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INDIVIDUAL, true);
        debugPrint("Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        clazzes.forEach(c -> debugPrint(" - " + c));
        assertTrue(TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to 3 classes", clazzes.size() == 3);
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_DISEASE_CLASS,
            clazzes.contains(TEST_DISEASE_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_SUPER_CLASS,
            clazzes.contains(TEST_SUPER_CLASS));
        clazzes = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        debugPrint("DIRECT Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        clazzes.forEach(c -> debugPrint(" - " + c));
        assertTrue(TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to 1 class", clazzes.size() == 1);
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_DISEASE_CLASS,
            clazzes.contains(TEST_DISEASE_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should not (directly) belong to class " + TEST_SUPER_CLASS,
            !clazzes.contains(TEST_SUPER_CLASS));
    }

    @Test
    public void testLabelRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        String label = ontologyWrapper.getLabelForIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL, "en");
        debugPrint("Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in English: " + label);
        assertEquals("Label should match", "Biotinidase deficiency", label);
        label = ontologyWrapper.getLabelForIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL, "es");
        debugPrint("Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in Spanish: " + label);
        assertEquals("Label should match", "Deficiencia de biotinidasa", label);
    }

    @Test
    public void testOntologyMerging() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(getClass().getResourceAsStream(SCHEMA_FILE));
        debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());

        ontologyWrapper.mergeOtherOntology(getClass().getResourceAsStream(DATA_FILE));
        final OWLOntology mergedOntology = ontologyWrapper.getOntology();
        debugPrint("Data ontology merged. New ontology: " + mergedOntology.getOntologyID());
        // Mostrar individuos en la ontología mergeada
        mergedOntology.individualsInSignature().forEach(ind -> debugPrint(" - " + ind));
        assertNotNull("The class " + TEST_MODEL_CLASS + " should exist", ontologyWrapper.getOWLClassIfExists(TEST_MODEL_CLASS));
        assertNotNull("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist", ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INDIVIDUAL));
    }

    @Test
    public void testOntologyDoubleLoading() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(getClass().getResourceAsStream(SCHEMA_FILE));
        debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());

        ontologyWrapper.addOntology(getClass().getResourceAsStream(DATA_FILE));
        final OWLOntology mergedOntology = ontologyWrapper.getOntology();
        debugPrint("Another ontology loaded. New ontology: " + mergedOntology.getOntologyID());
        // Mostrar individuos en la ontología mergeada
        mergedOntology.individualsInSignature().forEach(ind -> debugPrint(" - " + ind));
        assertNotNull("The class " + TEST_MODEL_CLASS + " should exist", ontologyWrapper.getOWLClassIfExists(TEST_MODEL_CLASS));
        assertNotNull("The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist", ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INDIVIDUAL));
        Set<String> diseaseClasses = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        debugPrint("Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        diseaseClasses.forEach(c -> debugPrint(" - " + c));
    }

    @Test
    public void testNonCompliantOntology() throws OWLOntologyCreationException {
        assertThrows("Loading " + WRONG_DATA_FILE + ", which is a non-compliant ontology, should throw exception", OWLOntologyCreationException.class, () -> {
            new OWLOntologyWrapper(getClass().getResourceAsStream(WRONG_DATA_FILE));
        });
    }
}
