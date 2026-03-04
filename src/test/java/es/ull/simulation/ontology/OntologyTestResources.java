package es.ull.simulation.ontology;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.semanticweb.owlapi.io.StreamDocumentSource;

public interface OntologyTestResources {
    final static String SCHEMA_IRI = "http://example.org/sample";
    final static String SCHEMA_FILE = "/sample-schema.owl";
    final static String DATA_FILE = "/sample-data.owl";
    final static String TEST_MODEL_ELEMENT_CLASS = "ModelElement";
    final static String TEST_DISEASE_CLASS = "Disease";
    final static String TEST_RARE_DISEASE_CLASS = "RareDisease";
    final static String TEST_SUPER_CLASS = "ModelElement";
    final static String TEST_MODEL_CLASS = "SimulationModel";
    final static String TEST_DISEASE_INDIVIDUAL = "T1DM";
    final static String TEST_PRELOADED_DISEASE_INDIVIDUAL = "BiotinidaseDeficiency_General";
    final static String TEST_DISEASE_OBJ_PROPERTY = "includedByModel";
    final static String TEST_DISEASE_DATA_PROPERTY = "hasDescription";
    final static String TEST_DISEASE_DESCRIPTION = "Type 1 Diabetes Mellitus";
    final static String TEST_MODEL_INDIVIDUAL = "T1DM_Model";
    final static String TEST_MODEL_DATA_PROPERTY = "hasStudyYear";
    final static String TEST_MODEL_STUDY_YEAR = "2020";

    public static OWLOntologyWrapper createDefaultOwlOntologyWrapper() {
        OWLOntologyWrapper wrap = assertDoesNotThrow(() -> {
            OntologyLoader loader = new OntologyLoader();
            final File tmp = File.createTempFile("schema", ".owl");
            tmp.deleteOnExit();
            Files.copy(Objects.requireNonNull(loader.getClass().getResourceAsStream(OntologyTestResources.SCHEMA_FILE)), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            final OntologyLoadOptions options = new OntologyLoadOptions.Builder()
                    .addLocalMapping(OntologyTestResources.SCHEMA_IRI + "=" + tmp.getAbsolutePath())
                    .build();
            final LoadedOntology loaded = loader.load(new StreamDocumentSource(Objects.requireNonNull(loader.getClass().getResourceAsStream(OntologyTestResources.DATA_FILE))), options); 
            return new OWLOntologyWrapper(loaded);
        }, "Error loading ontology from test resources");
        return wrap;
    }

}
