package es.ull.ontology;

import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import es.ull.simulation.ontology.OWLOntologyWrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

public class OntologyTest {
    private final static boolean DEBUG = true;
    private final static String PREFIX = "http://example.org/sample#";
    private final static String SCHEMA_IRI = "http://example.org/sample";
    private final static String SCHEMA_FILE = "src/test/java/es/ull/ontology/sample-schema.owl";
    private final static String DATA_FILE = "src/test/java/es/ull/ontology/sample-data.owl";
    private final static String TEST_DISEASE_CLASS = "Disease";
    private final static String TEST_SUPER_CLASS = "ModelElement";
    private final static String TEST_MODEL_CLASS = "SimulationModel";
    private final static String TEST_DISEASE_INSTANCE = "T1DM";
    private final static String TEST_PRELOADED_DISEASE_INSTANCE = "BiotinidaseDeficiency_General";
    private final static String TEST_PRELOADED_DISEASE_DESCRIPTION = "Deficiencia de Biotinidasa (DB)";
    private final static String TEST_PRELOADED_DISEASE_OBJ_PROP_VALUE = "BiotinidaseDeficiency_Model";
    private final static String TEST_DISEASE_OBJ_PROPERTY = "includedByModel";
    private final static String TEST_DISEASE_DATA_PROPERTY = "hasDescription";
    private final static String TEST_DISEASE_DESCRIPTION = "Type 1 Diabetes Mellitus";
    private final static String TEST_MODEL_INSTANCE = "T1DM_Model";
    private final static String TEST_MODEL_DATA_PROPERTY = "hasStudyYear";
    private final static String TEST_MODEL_STUDY_YEAR = "2020";
    private final static String TEST_INVALID_CLASS = "InvalidDiseaseClass";
    private final static String TEST_INVALID_DISEASE_INSTANCE = "T2DM";
    private final static String TEST_INVALID_MODEL_INSTANCE = "T2DM_Model";
    private final static String TEST_INVALID_DISEASE_OBJ_PROPERTY = "included";
    private final static String TEST_INVALID_DISEASE_DATA_PROPERTY = "hasSource";
    private final static String TEST_INVALID_MODEL_DATA_PROPERTY = "hasValue";


    public static void debugPrint(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }
    
    public OWLOntologyWrapper createDefaultOwlOntologyWrapper() throws OWLOntologyCreationException {
        final File dataFile = new File(DATA_FILE);
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(dataFile, PREFIX, SCHEMA_IRI + "=" + SCHEMA_FILE);
        debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
        return ontologyWrapper;
    }

    @Test
    public void testOntologyLoading() throws OWLOntologyCreationException {
        final File schemaFile = new File(SCHEMA_FILE);
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(schemaFile, PREFIX);
        assertEquals(SCHEMA_IRI, ontologyWrapper.getOntology().getOntologyID().getOntologyIRI().get().toString());
        debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
    }

    @Test
    public void testOntologyIndividuals() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INSTANCE));
        // If the individual was created correctly, this call should return false
        assertTrue(!ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INSTANCE));
        // And this should be false too, since it was already present
        assertTrue(!ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_PRELOADED_DISEASE_INSTANCE));
        debugPrint("Individuals in the ontology:");
        ontologyWrapper.getIndividuals(TEST_DISEASE_CLASS).forEach(ind -> debugPrint(" - " + ind));
    }

    @Test
    public void testAddObjectPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INSTANCE));
        assertTrue(ontologyWrapper.addIndividual(TEST_MODEL_CLASS, TEST_MODEL_INSTANCE));

        // Valid case: T1DM is included by model T1DM_Model
        boolean added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INSTANCE, TEST_DISEASE_OBJ_PROPERTY, TEST_MODEL_INSTANCE);
        assertTrue("The object property value should be added", added);

        // Invalid case: "T2DM" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_INVALID_DISEASE_INSTANCE, TEST_DISEASE_OBJ_PROPERTY, TEST_MODEL_INSTANCE);
        assertTrue("The object property value should not be added", !added);
        // Invalid case: "T2DM_Model" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INSTANCE, TEST_DISEASE_OBJ_PROPERTY, TEST_INVALID_MODEL_INSTANCE);
        assertTrue("The object property value should not be added", !added);
        // Invalid case: "included" does not exist
        added = ontologyWrapper.addObjectPropertyValue(TEST_DISEASE_INSTANCE, TEST_INVALID_DISEASE_OBJ_PROPERTY, TEST_MODEL_INSTANCE);
        assertTrue("The object property value should not be added", !added);
        Set<String> includedByModel = ontologyWrapper.getObjectPropertyValues(TEST_DISEASE_INSTANCE, TEST_DISEASE_OBJ_PROPERTY);
        debugPrint("Property values for '" + TEST_DISEASE_OBJ_PROPERTY + "' of " + TEST_DISEASE_INSTANCE + ":");
        includedByModel.forEach(ind -> debugPrint(" - " + ind));
        assertTrue("The includedByModel property should contain " + TEST_MODEL_INSTANCE, includedByModel.contains(TEST_MODEL_INSTANCE));
        assertTrue("The includedByModel property should have exactly one value", includedByModel.size() == 1);
    }

    @Test
    public void testAddDataPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        assertTrue(ontologyWrapper.addIndividual(TEST_DISEASE_CLASS, TEST_DISEASE_INSTANCE));
        assertTrue(ontologyWrapper.addIndividual(TEST_MODEL_CLASS, TEST_MODEL_INSTANCE));

        // Valid case: T1DM has a description
        boolean added = ontologyWrapper.addDataPropertyValue(TEST_DISEASE_INSTANCE, TEST_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue("The data property value should be added", added);
        // Invalid case: "T2DM" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_INVALID_DISEASE_INSTANCE, TEST_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue("The data property value should not be added", !added);
        // Invalid case: "hasSource" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_DISEASE_INSTANCE, TEST_INVALID_DISEASE_DATA_PROPERTY, TEST_DISEASE_DESCRIPTION);
        assertTrue("The data property value should not be added", !added);

        // Valid case: T1DM_Model has a year
        added = ontologyWrapper.addDataPropertyValue(TEST_MODEL_INSTANCE, TEST_MODEL_DATA_PROPERTY, TEST_MODEL_STUDY_YEAR, OWL2Datatype.XSD_INTEGER);
        assertTrue("The data property value should be added", added);
        // Invalid case: "T2DM_Model" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_INVALID_MODEL_INSTANCE, TEST_MODEL_DATA_PROPERTY, "2021", OWL2Datatype.XSD_INTEGER);
        assertTrue("The data property value should not be added", !added);
        // Invalid case: "hasValue" does not exist
        added = ontologyWrapper.addDataPropertyValue(TEST_MODEL_INSTANCE, TEST_INVALID_MODEL_DATA_PROPERTY, "23", OWL2Datatype.XSD_INTEGER);
        assertTrue("The data property value should not be added", !added);

        ArrayList<String> prop = ontologyWrapper.getDataPropertyValues(TEST_DISEASE_INSTANCE, TEST_DISEASE_DATA_PROPERTY);
        debugPrint("Property values for '" + TEST_DISEASE_DATA_PROPERTY + "' of " + TEST_DISEASE_INSTANCE + ":");
        prop.forEach(ind -> debugPrint(" - " + ind));
        assertTrue("The " + TEST_DISEASE_DATA_PROPERTY + " property should have exactly one value", prop.size() == 1);
        assertTrue("The " + TEST_DISEASE_DATA_PROPERTY + " property should contain '" + TEST_DISEASE_DESCRIPTION + "'", prop.contains(TEST_DISEASE_DESCRIPTION));
        prop = ontologyWrapper.getDataPropertyValues(TEST_MODEL_INSTANCE, TEST_MODEL_DATA_PROPERTY);
        debugPrint("Property values for '" + TEST_MODEL_DATA_PROPERTY + "' of " + TEST_MODEL_INSTANCE + ":");
        prop.forEach(ind -> debugPrint(" - " + ind));
        assertTrue("The " + TEST_MODEL_DATA_PROPERTY + " property should have exactly one value", prop.size() == 1);
        assertTrue("The " + TEST_MODEL_DATA_PROPERTY + " property should contain '" + TEST_MODEL_STUDY_YEAR + "'", prop.contains(TEST_MODEL_STUDY_YEAR));
    }

    @Test
    public void testOntologyRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        // Checking individuals
        // These should be ok...
        assertNotNull("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should exist", ontologyWrapper.getOWLIndividual(TEST_PRELOADED_DISEASE_INSTANCE));
        assertNotNull("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should exist", ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INSTANCE));
        // This should be ok...
        assertNull("The individual " + TEST_DISEASE_INSTANCE + " should not exist", ontologyWrapper.getOWLIndividualIfExists(TEST_DISEASE_INSTANCE));
        // ... but this too
        assertNotNull("The individual " + TEST_DISEASE_INSTANCE + " should not exist but this method should work anyway", ontologyWrapper.getOWLIndividual(TEST_DISEASE_INSTANCE));
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
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should be a subclass of " + TEST_DISEASE_CLASS,
            ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INSTANCE, TEST_DISEASE_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should be a subclass of " + TEST_SUPER_CLASS,
            ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INSTANCE, TEST_SUPER_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should not be a subclass of " + TEST_MODEL_CLASS,
            !ontologyWrapper.isInstanceOf(TEST_PRELOADED_DISEASE_INSTANCE, TEST_MODEL_CLASS));

        Set<String> clazzes = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INSTANCE, true);
        debugPrint("Classes for individual '" + TEST_PRELOADED_DISEASE_INSTANCE + "':");
        clazzes.forEach(c -> debugPrint(" - " + c));
        assertTrue(TEST_PRELOADED_DISEASE_INSTANCE + " should belong to 3 classes", clazzes.size() == 3);
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should belong to class " + TEST_DISEASE_CLASS,
            clazzes.contains(TEST_DISEASE_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should belong to class " + TEST_SUPER_CLASS,
            clazzes.contains(TEST_SUPER_CLASS));
        clazzes = ontologyWrapper.getIndividualClasses(TEST_PRELOADED_DISEASE_INSTANCE);
        debugPrint("DIRECT Classes for individual '" + TEST_PRELOADED_DISEASE_INSTANCE + "':");
        clazzes.forEach(c -> debugPrint(" - " + c));
        assertTrue(TEST_PRELOADED_DISEASE_INSTANCE + " should belong to 1 class", clazzes.size() == 1);
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should belong to class " + TEST_DISEASE_CLASS,
            clazzes.contains(TEST_DISEASE_CLASS));
        assertTrue("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should not (directly) belong to class " + TEST_SUPER_CLASS,
            !clazzes.contains(TEST_SUPER_CLASS));
    }

    @Test
    public void testLabelRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        String label = ontologyWrapper.getLabelForIRI(TEST_PRELOADED_DISEASE_INSTANCE, "en");
        debugPrint("Label for " + TEST_PRELOADED_DISEASE_INSTANCE + " in English: " + label);
        assertEquals("Label should match", "Biotinidase deficiency", label);
        label = ontologyWrapper.getLabelForIRI(TEST_PRELOADED_DISEASE_INSTANCE, "es");
        debugPrint("Label for " + TEST_PRELOADED_DISEASE_INSTANCE + " in Spanish: " + label);
        assertEquals("Label should match", "Deficiencia de biotinidasa", label);
    }

    @Test
    public void testOntologyMerging() throws OWLOntologyCreationException {
        final File schemaFile = new File(SCHEMA_FILE);
        final File dataFile = new File(DATA_FILE);
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(schemaFile, PREFIX);
        debugPrint("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());

        ontologyWrapper.mergeOtherOntology(dataFile);
        final OWLOntology mergedOntology = ontologyWrapper.getOntology();
        debugPrint("Data ontology merged. New ontology: " + mergedOntology.getOntologyID());
        // Mostrar individuos en la ontología mergeada
        mergedOntology.individualsInSignature().forEach(ind -> debugPrint(" - " + ind));
        assertNotNull("The class " + TEST_MODEL_CLASS + " should exist", ontologyWrapper.getOWLClassIfExists(TEST_MODEL_CLASS));
        assertNotNull("The individual " + TEST_PRELOADED_DISEASE_INSTANCE + " should exist", ontologyWrapper.getOWLIndividualIfExists(TEST_PRELOADED_DISEASE_INSTANCE));
    }
}
