package es.ull.ontology;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ull.simulation.ontology.LoadedOntology;
import es.ull.simulation.ontology.OntologyLoader;
import es.ull.simulation.ontology.OWLOntologyWrapper;
import es.ull.simulation.ontology.OntologySource;
import es.ull.simulation.ontology.OWLOntologyWrapper.InstanceCheckMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
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
    final private OntologyLoader loader = new OntologyLoader();

    public OWLOntologyWrapper createDefaultOwlOntologyWrapper() throws OWLOntologyCreationException {
        try {
            final File tmp = File.createTempFile("schema", ".owl");
            tmp.deleteOnExit();
            Files.copy(getClass().getResourceAsStream(SCHEMA_FILE), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            final LoadedOntology loaded = loader.load(new OntologySource.FromStream(getClass().getResourceAsStream(DATA_FILE)), SCHEMA_IRI + "=" + tmp.getAbsolutePath()); 
            final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(loaded);
            log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
            return ontologyWrapper;
        } catch (IOException e) {
            throw new OWLOntologyCreationException("Error copying schema file to temp location", e);
        }
    }

    @Test
    public void testOntologyLoading() throws OWLOntologyCreationException {
        final InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_FILE);
        final LoadedOntology loaded = loader.load(new OntologySource.FromStream(schemaStream));
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(loaded);
        assertEquals(SCHEMA_IRI, ontologyWrapper.getOntology().getOntologyID().getOntologyIRI().get().toString());
        log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
    }

    @Test
    public void testRemoteOntologyLoading() throws OWLOntologyCreationException {
        final LoadedOntology loaded = loader.load(new OntologySource.FromIRI(IRI.create(REMOTE_ONTOLOGY_VERSIONED_IRI)));
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(loaded);
        final String loadedOntologyIRI = ontologyWrapper.getOntology().getOntologyID().getOntologyIRI().get().getIRIString().trim();
        assertEquals(REMOTE_ONTOLOGY_IRI, loadedOntologyIRI, "Loaded ontology IRI (" + loadedOntologyIRI + ") and remote ontology IRI (" + REMOTE_ONTOLOGY_IRI + ") should match");
        log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
    }

    @Test
    public void testRemoteIndividualsLoading() throws OWLOntologyCreationException {
        final LoadedOntology loaded = loader.load(new OntologySource.FromIRI(IRI.create(REMOTE_INDIVIDUALS_VERSIONED_IRI)));
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(loaded);
        final IRI classDiseaseIRI = ontologyWrapper.toIRI(TEST_DISEASE_CLASS);
        final IRI individualDiseaseIRI = ontologyWrapper.toIRI(REMOTE_TEST_DISEASE_INDIVIDUAL);
        final IRI wrongIndividualDiseaseIRI = ontologyWrapper.toIRI(TEST_DISEASE_INDIVIDUAL);
        final Set<IRI> diseaseIRIs = ontologyWrapper.getIndividualsOfClass(classDiseaseIRI, Imports.INCLUDED, InstanceCheckMode.INFERRED_ALL);
        assertTrue(diseaseIRIs.contains(individualDiseaseIRI), "The disease individual " + REMOTE_TEST_DISEASE_INDIVIDUAL + " should be present");
        assertTrue(!diseaseIRIs.contains(wrongIndividualDiseaseIRI), "The disease individual " + TEST_DISEASE_INDIVIDUAL + " should not be present");
        final IRI dataItemTypeIRI = ontologyWrapper.toIRI(REMOTE_TEST_DATAITEMTYPE_CLASS);
        final Set<IRI> dataItemTypeIRIs = ontologyWrapper.getIndividualsOfClass(dataItemTypeIRI, Imports.INCLUDED, InstanceCheckMode.INFERRED_ALL);
        assertTrue(dataItemTypeIRIs.contains(ontologyWrapper.toIRI(REMOTE_TEST_DATAITEMTYPE_INDIVIDUAL)), "The DataItemType individual " + REMOTE_TEST_DATAITEMTYPE_INDIVIDUAL + " should be present");
        log.debug("Individuals of class " + REMOTE_TEST_DATAITEMTYPE_CLASS + ":");
        dataItemTypeIRIs.forEach(ind -> log.debug(" - " + ind));
    }

    @Test
    public void testOntologyIndividuals() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        final IRI classDiseaseIRI = ontologyWrapper.toIRI(TEST_DISEASE_CLASS);
        final IRI individualDiseaseIRI = ontologyWrapper.toIRI(TEST_DISEASE_INDIVIDUAL);
        final IRI preloadedIndividualDiseaseIRI = ontologyWrapper.toIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        assertTrue(ontologyWrapper.createIndividual(classDiseaseIRI, individualDiseaseIRI));
        // If the individual was created correctly, this call should return false
        assertTrue(!ontologyWrapper.createIndividual(classDiseaseIRI, individualDiseaseIRI));
        // And this should be false too, since it was already present
        assertTrue(!ontologyWrapper.createIndividual(classDiseaseIRI, preloadedIndividualDiseaseIRI));
        log.debug("Individuals in the ontology (inferred):");
        ontologyWrapper.getIndividualsOfClass(classDiseaseIRI, Imports.INCLUDED, InstanceCheckMode.INFERRED_ALL).forEach(ind -> log.debug(" - " + ind));
    }

    @Test
    public void testAddObjectPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        final IRI diseaseClassIRI = ontologyWrapper.toIRI(TEST_DISEASE_CLASS);
        final IRI modelClassIRI = ontologyWrapper.toIRI(TEST_MODEL_CLASS);
        final IRI diseaseIndividualIRI = ontologyWrapper.toIRI(TEST_DISEASE_INDIVIDUAL);
        final IRI modelIndividualIRI = ontologyWrapper.toIRI(TEST_MODEL_INDIVIDUAL);
        assertTrue(ontologyWrapper.createIndividual(diseaseClassIRI, diseaseIndividualIRI));
        assertTrue(ontologyWrapper.createIndividual(modelClassIRI, modelIndividualIRI));

        final IRI includedByModelPropertyIRI = ontologyWrapper.toIRI(TEST_DISEASE_OBJ_PROPERTY);
        final IRI invalidObjectPropertyIRI = ontologyWrapper.toIRI(TEST_INVALID_DISEASE_OBJ_PROPERTY);
        final IRI invalidDiseaseIndividualIRI = ontologyWrapper.toIRI(TEST_INVALID_DISEASE_INDIVIDUAL);
        final IRI invalidModelIndividualIRI = ontologyWrapper.toIRI(TEST_INVALID_MODEL_INDIVIDUAL);
        // Valid case: T1DM is included by model T1DM_Model
        boolean added = ontologyWrapper.assertObjectProperty(diseaseIndividualIRI, includedByModelPropertyIRI, modelIndividualIRI);
        assertTrue(added, "The object property value should be added");

        // Invalid case: "T2DM" does not exist
        assertThrows(IllegalArgumentException.class, () -> {
            ontologyWrapper.assertObjectProperty(invalidDiseaseIndividualIRI, includedByModelPropertyIRI, modelIndividualIRI);
        }, "Asserting an object property for a non-existing individual should throw exception");
        added = ontologyWrapper.assertObjectProperty(diseaseIndividualIRI, includedByModelPropertyIRI, modelIndividualIRI);
        assertTrue(!added, "The object property value should not be added because it already exists");
        // Invalid case: "T2DM_Model" does not exist
        assertThrows(IllegalArgumentException.class, () -> {
            ontologyWrapper.assertObjectProperty(diseaseIndividualIRI, includedByModelPropertyIRI, invalidModelIndividualIRI);
        }, "Asserting an object property for a non-existing individual should throw exception");
        // Invalid case: "included" does not exist
        assertThrows(IllegalArgumentException.class, () -> {
            ontologyWrapper.assertObjectProperty(diseaseIndividualIRI, invalidObjectPropertyIRI, modelIndividualIRI);
        }, "Asserting an object property for a non-existing property should throw exception");
        Set<IRI> includedByModelIris = ontologyWrapper.getObjectPropertyValues(diseaseIndividualIRI, includedByModelPropertyIRI, Imports.INCLUDED);
        log.debug("Property values for '" + TEST_DISEASE_OBJ_PROPERTY + "' of " + TEST_DISEASE_INDIVIDUAL + ":");
        includedByModelIris.forEach(ind -> log.debug(" - " + ind));
        assertTrue(includedByModelIris.contains(modelIndividualIRI), "The includedByModel property should contain " + TEST_MODEL_INDIVIDUAL);
        assertTrue(includedByModelIris.size() == 1, "The includedByModel property should have exactly one value");
    }

    @Test
    public void testAddDataPropertyValue() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        final IRI diseaseClassIRI = ontologyWrapper.toIRI(TEST_DISEASE_CLASS);
        final IRI modelClassIRI = ontologyWrapper.toIRI(TEST_MODEL_CLASS);
        final IRI diseaseIndividualIRI = ontologyWrapper.toIRI(TEST_DISEASE_INDIVIDUAL);
        final IRI modelIndividualIRI = ontologyWrapper.toIRI(TEST_MODEL_INDIVIDUAL);

        assertTrue(ontologyWrapper.createIndividual(diseaseClassIRI, diseaseIndividualIRI));
        assertTrue(ontologyWrapper.createIndividual(modelClassIRI, modelIndividualIRI));

        final IRI invalidDiseaseIndividualIRI = ontologyWrapper.toIRI(TEST_INVALID_DISEASE_INDIVIDUAL);
        final IRI invalidModelIndividualIRI = ontologyWrapper.toIRI(TEST_INVALID_MODEL_INDIVIDUAL);
        final IRI diseaseDataPropertyIRI = ontologyWrapper.toIRI(TEST_DISEASE_DATA_PROPERTY);
        final IRI invalidDiseaseDataPropertyIRI = ontologyWrapper.toIRI(TEST_INVALID_DISEASE_DATA_PROPERTY);
        final IRI modelDataPropertyIRI = ontologyWrapper.toIRI(TEST_MODEL_DATA_PROPERTY);
        final IRI invalidModelDataPropertyIRI = ontologyWrapper.toIRI(TEST_INVALID_MODEL_DATA_PROPERTY);
        // Valid case: T1DM has a description
        boolean added = ontologyWrapper.assertDataProperty(diseaseIndividualIRI, diseaseDataPropertyIRI, TEST_DISEASE_DESCRIPTION);
        assertTrue(added, "The data property value should be added");
        // Invalid case: "T2DM" does not exist
        assertThrows(IllegalArgumentException.class, () -> {
            ontologyWrapper.assertDataProperty(invalidDiseaseIndividualIRI, diseaseDataPropertyIRI, TEST_DISEASE_DESCRIPTION);
        }, "Asserting a data property for a non-existing individual should throw exception");
        added = ontologyWrapper.assertDataProperty(diseaseIndividualIRI, diseaseDataPropertyIRI, TEST_DISEASE_DESCRIPTION);
        assertTrue(!added, "The data property value should not be added because it already exists");
        // Invalid case: "hasSource" does not exist
        assertThrows(IllegalArgumentException.class, () -> {
            ontologyWrapper.assertDataProperty(diseaseIndividualIRI, invalidDiseaseDataPropertyIRI, TEST_DISEASE_DESCRIPTION);
        }, "Asserting a data property for a non-existing property should throw exception");

        // Valid case: T1DM_Model has a year
        added = ontologyWrapper.assertDataProperty(modelIndividualIRI, modelDataPropertyIRI, TEST_MODEL_STUDY_YEAR, OWL2Datatype.XSD_INTEGER);
        assertTrue(added, "The data property value should be added");
        // Invalid case: "T2DM_Model" does not exist
        assertThrows(IllegalArgumentException.class, () -> {
            ontologyWrapper.assertDataProperty(invalidModelIndividualIRI, modelDataPropertyIRI, "2021", OWL2Datatype.XSD_INTEGER);
        }, "Asserting a data property for a non-existing individual should throw exception");
        // Invalid case: "hasValue" does not exist
        assertThrows(IllegalArgumentException.class, () -> {
            ontologyWrapper.assertDataProperty(modelIndividualIRI, invalidModelDataPropertyIRI, "23", OWL2Datatype.XSD_INTEGER);
        }, "Asserting a data property for a non-existing property should throw exception");

        Set<OWLLiteral> dataPropertyValues = ontologyWrapper.getDataPropertyValues(diseaseIndividualIRI, diseaseDataPropertyIRI, Imports.INCLUDED);
        log.debug("Property values for '" + TEST_DISEASE_DATA_PROPERTY + "' of " + TEST_DISEASE_INDIVIDUAL + ":");
        dataPropertyValues.forEach(ind -> log.debug(" - " + ind));
        assertTrue(dataPropertyValues.size() == 1, "The " + TEST_DISEASE_DATA_PROPERTY + " property should have exactly one value");
        OWLLiteral descLiteral = ontologyWrapper.getDataFactory().getOWLLiteral(TEST_DISEASE_DESCRIPTION);
        assertTrue(dataPropertyValues.contains(descLiteral), "The " + TEST_DISEASE_DATA_PROPERTY + " property should contain '" + TEST_DISEASE_DESCRIPTION + "'");
        dataPropertyValues = ontologyWrapper.getDataPropertyValues(modelIndividualIRI, modelDataPropertyIRI, Imports.INCLUDED);
        log.debug("Property values for '" + TEST_MODEL_DATA_PROPERTY + "' of " + TEST_MODEL_INDIVIDUAL + ":");
        dataPropertyValues.forEach(ind -> log.debug(" - " + ind));
        assertTrue(dataPropertyValues.size() == 1, "The " + TEST_MODEL_DATA_PROPERTY + " property should have exactly one value");
        assertTrue(dataPropertyValues.contains(ontologyWrapper.getDataFactory().getOWLLiteral(TEST_MODEL_STUDY_YEAR, OWL2Datatype.XSD_INTEGER)), "The " + TEST_MODEL_DATA_PROPERTY + " property should contain '" + TEST_MODEL_STUDY_YEAR + "'");
    }

    @Test
    public void testOntologyRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        // Checking individuals
        // These should be ok...
        final IRI testPreloadedDiseaseIndividualIRI = ontologyWrapper.toIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        final IRI testDiseaseIndividualIRI = ontologyWrapper.toIRI(TEST_DISEASE_INDIVIDUAL);
        assertNotNull(ontologyWrapper.asOWLIndividual(testPreloadedDiseaseIndividualIRI), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist");
        assertTrue(ontologyWrapper.findOWLIndividual(testPreloadedDiseaseIndividualIRI).isPresent(), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist");
        // This should be ok...
        assertTrue(ontologyWrapper.findOWLIndividual(testDiseaseIndividualIRI).isEmpty(), "The individual " + TEST_DISEASE_INDIVIDUAL + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.asOWLIndividual(testDiseaseIndividualIRI), "The individual " + TEST_DISEASE_INDIVIDUAL + " should not exist but this method should work anyway");
        log.debug("Access to individuals checked");
        ontologyWrapper.getIndividualsInSignature(Imports.INCLUDED).forEach(ind -> log.debug(" - " + ind));
        // Checking classes
        // These should be ok...
        final IRI testDiseaseClassIRI = ontologyWrapper.toIRI(TEST_DISEASE_CLASS);
        final IRI testModelClassIRI = ontologyWrapper.toIRI(TEST_MODEL_CLASS);
        final IRI testInvalidClassIRI = ontologyWrapper.toIRI(TEST_INVALID_CLASS);
        assertNotNull(ontologyWrapper.asOWLClass(testDiseaseClassIRI), "The class " + TEST_DISEASE_CLASS + " should exist");
        assertTrue(ontologyWrapper.findOWLClass(testModelClassIRI).isPresent(), "The class " + TEST_MODEL_CLASS + " should exist");
        // This should be ok...
        assertTrue(ontologyWrapper.findOWLClass(testInvalidClassIRI).isEmpty(), "The class " + TEST_INVALID_CLASS + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.asOWLClass(testInvalidClassIRI), "The class " + TEST_INVALID_CLASS + " should not exist but this method should work anyway");
        log.debug("Access to classes checked");
        ontologyWrapper.getClassesInSignature(Imports.INCLUDED).forEach(cls -> log.debug(" - " + cls));
        // Checking dataProperties
        // These should be ok...
        final IRI testDiseaseDataPropertyIRI = ontologyWrapper.toIRI(TEST_DISEASE_DATA_PROPERTY);
        final IRI testInvalidDiseaseDataPropertyIRI = ontologyWrapper.toIRI(TEST_INVALID_DISEASE_DATA_PROPERTY);
        assertNotNull(ontologyWrapper.asOWLDataProperty(testDiseaseDataPropertyIRI), "The data property " + TEST_DISEASE_DATA_PROPERTY + " should exist");
        assertTrue(ontologyWrapper.findOWLDataProperty(testDiseaseDataPropertyIRI).isPresent(), "The data property " + TEST_DISEASE_DATA_PROPERTY + " should exist");
        // This should be ok...
        assertTrue(ontologyWrapper.findOWLDataProperty(testInvalidDiseaseDataPropertyIRI).isEmpty(), "The data property " + TEST_INVALID_DISEASE_DATA_PROPERTY + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.asOWLDataProperty(testInvalidDiseaseDataPropertyIRI), "The data property " + TEST_INVALID_DISEASE_DATA_PROPERTY + " should not exist but this method should work anyway");
        log.debug("Access to data properties checked");
        ontologyWrapper.getDataPropertiesInSignature(Imports.INCLUDED).forEach(dp -> log.debug(" - " + dp));
        // Checking objectProperties
        // These should be ok...
        final IRI testDiseaseObjectPropertyIRI = ontologyWrapper.toIRI(TEST_DISEASE_OBJ_PROPERTY);
        final IRI testInvalidDiseaseObjectPropertyIRI = ontologyWrapper.toIRI(TEST_INVALID_DISEASE_OBJ_PROPERTY);
        assertNotNull(ontologyWrapper.asOWLObjectProperty(testDiseaseObjectPropertyIRI), "The object property " + TEST_DISEASE_OBJ_PROPERTY + " should exist");
        assertTrue(ontologyWrapper.findOWLObjectProperty(testDiseaseObjectPropertyIRI).isPresent(), "The object property " + TEST_DISEASE_OBJ_PROPERTY + " should exist");
        // This should be ok...
        assertTrue(ontologyWrapper.findOWLObjectProperty(testInvalidDiseaseObjectPropertyIRI).isEmpty(), "The object property " + TEST_INVALID_DISEASE_OBJ_PROPERTY + " should not exist");
        // ... but this too
        assertNotNull(ontologyWrapper.asOWLObjectProperty(testInvalidDiseaseObjectPropertyIRI), "The object property " + TEST_INVALID_DISEASE_OBJ_PROPERTY + " should not exist but this method should work anyway");
        log.debug("Access to object properties checked");
        ontologyWrapper.getObjectPropertiesInSignature(Imports.INCLUDED).forEach(op -> log.debug(" - " + op));
    }

    @Test
    public void testOntologyStructure() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        final IRI testPreloadedDiseaseIndividualIRI = ontologyWrapper.toIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        final IRI testDiseaseClassIRI = ontologyWrapper.toIRI(TEST_DISEASE_CLASS);
        final IRI testSuperClassIRI = ontologyWrapper.toIRI(TEST_SUPER_CLASS);
        final IRI testModelClassIRI = ontologyWrapper.toIRI(TEST_MODEL_CLASS);
        assertTrue(ontologyWrapper.isInstanceOf(testPreloadedDiseaseIndividualIRI, testDiseaseClassIRI, Imports.INCLUDED, InstanceCheckMode.ASSERTED),
            "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should be a subclass of " + TEST_DISEASE_CLASS); 
        assertTrue(!ontologyWrapper.isInstanceOf(testPreloadedDiseaseIndividualIRI, testSuperClassIRI, Imports.INCLUDED, InstanceCheckMode.ASSERTED),
            "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should not be determined to be a subclass of " + TEST_SUPER_CLASS + " without inference");
        assertTrue(ontologyWrapper.isInstanceOf(testPreloadedDiseaseIndividualIRI, testSuperClassIRI, Imports.INCLUDED, InstanceCheckMode.INFERRED_ALL),
            "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should be a subclass of " + TEST_SUPER_CLASS + " with inference");
        assertTrue(!ontologyWrapper.isInstanceOf(testPreloadedDiseaseIndividualIRI, testModelClassIRI, Imports.INCLUDED, InstanceCheckMode.ASSERTED),
            "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should not be a subclass of " + TEST_MODEL_CLASS);

        Set<IRI> classIRIs = ontologyWrapper.getAssertedTypes(testPreloadedDiseaseIndividualIRI, true, Imports.INCLUDED);
        log.debug("Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        classIRIs.forEach(c -> log.debug(" - " + c));
        assertTrue(classIRIs.size() == 2, TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to 2 classes");
        assertTrue(classIRIs.contains(testDiseaseClassIRI), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_DISEASE_CLASS);
        assertTrue(classIRIs.contains(testSuperClassIRI), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_SUPER_CLASS);
        classIRIs = ontologyWrapper.getAssertedTypes(testPreloadedDiseaseIndividualIRI, false, Imports.INCLUDED);
        log.debug("DIRECT Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        classIRIs.forEach(c -> log.debug(" - " + c));
        assertTrue(classIRIs.size() == 1, TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to 1 class");
        assertTrue(classIRIs.contains(testDiseaseClassIRI), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should belong to class " + TEST_DISEASE_CLASS);
        assertTrue(!classIRIs.contains(testSuperClassIRI), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should not (directly) belong to class " + TEST_SUPER_CLASS);
    }

    @Test
    public void testLabelRetrieval() throws OWLOntologyCreationException {
        final OWLOntologyWrapper ontologyWrapper = createDefaultOwlOntologyWrapper();
        final IRI testPreloadedDiseaseIndividualIRI = ontologyWrapper.toIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        Optional<String> label = ontologyWrapper.getLabelForIRI(testPreloadedDiseaseIndividualIRI, "en");
        assertTrue(label.isPresent(), "Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in English should be present");
        log.debug("Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in English: " + label.get());
        assertEquals("Biotinidase deficiency", label.get(), "Label should match");
        label = ontologyWrapper.getLabelForIRI(testPreloadedDiseaseIndividualIRI, "es");
        assertTrue(label.isPresent(), "Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in Spanish should be present");
        log.debug("Label for " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " in Spanish: " + label.get());
        assertEquals("Deficiencia de biotinidasa", label.get(), "Label should match");
    }

    @Test
    public void testOntologyDoubleLoading() throws OWLOntologyCreationException {
        final LoadedOntology loaded = loader.load(new OntologySource.FromStream(getClass().getResourceAsStream(SCHEMA_FILE)));
        final URL url = Objects.requireNonNull(getClass().getResource(SCHEMA_FILE));
        final IRI localSchemaFileIRI = Objects.requireNonNull(IRI.create(url));
        log.debug("Schema file IRI: " + localSchemaFileIRI);
        final IRI schemaFileVersionIRI = Objects.requireNonNull(loaded.ontology().getOntologyID().getOntologyIRI().get());
        SimpleIRIMapper mapper = new SimpleIRIMapper(schemaFileVersionIRI, localSchemaFileIRI);
        loaded.manager().getIRIMappers().add(mapper);

        final LoadedOntology loadedWithMapping = loader.load(new OntologySource.FromStream(getClass().getResourceAsStream(DATA_FILE)), loaded.manager());
        final OWLOntologyWrapper ontologyWrapper = new OWLOntologyWrapper(loadedWithMapping);
        log.debug("Ontology loaded: " + ontologyWrapper.getOntology().getOntologyID());
        
        final OWLOntology mergedOntology = ontologyWrapper.loadOntology(new OntologySource.FromStream(getClass().getResourceAsStream(DATA_FILE)));
        log.debug("Another ontology loaded. New ontology: " + mergedOntology.getOntologyID());
        // Show individuals in the merged ontology
        mergedOntology.individualsInSignature().forEach(ind -> log.debug(" - " + ind));

        final IRI testModelClassIRI = ontologyWrapper.toIRI(TEST_MODEL_CLASS);
        final IRI testPreloadedDiseaseIndividualIRI = ontologyWrapper.toIRI(TEST_PRELOADED_DISEASE_INDIVIDUAL);
        log.debug("IRI to check: " + testPreloadedDiseaseIndividualIRI);
        assertTrue(ontologyWrapper.findOWLClass(testModelClassIRI).isPresent(), "The class " + TEST_MODEL_CLASS + " should exist");
        assertTrue(ontologyWrapper.findOWLIndividual(testPreloadedDiseaseIndividualIRI).isPresent(), "The individual " + TEST_PRELOADED_DISEASE_INDIVIDUAL + " should exist");
        Set<IRI> diseaseClasseIRIs = ontologyWrapper.getAssertedTypes(testPreloadedDiseaseIndividualIRI, true, Imports.INCLUDED);
        log.debug("Classes for individual '" + TEST_PRELOADED_DISEASE_INDIVIDUAL + "':");
        diseaseClasseIRIs.forEach(c -> log.debug(" - " + c));
    }

    @Test
    public void testNonCompliantOntology() throws OWLOntologyCreationException {
        assertThrows(OWLOntologyCreationException.class, () -> {
            final LoadedOntology loaded = loader.load(new OntologySource.FromStream(getClass().getResourceAsStream(WRONG_DATA_FILE)));
            new OWLOntologyWrapper(loaded);
        }, "Loading " + WRONG_DATA_FILE + ", which is a non-compliant ontology, should throw exception");
    }
}
