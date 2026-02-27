package es.ull.ontology;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;

import es.ull.simulation.ontology.LoadedOntology;
import es.ull.simulation.ontology.OWLOntologyWrapper;
import es.ull.simulation.ontology.OntologyLoadOptions;
import es.ull.simulation.ontology.OntologyLoader;

public class BatchOntologyChecker {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BatchOntologyChecker.class);
    private static final String SEP_STRING = "\t";

    private static OWLOntologyWrapper loadCheckedOntology(String checkedOntologyFileOrIRI, OntologyLoader loader) throws OWLOntologyCreationException {
        final OWLOntologyLoaderConfiguration cfg = Objects.requireNonNull(new OWLOntologyLoaderConfiguration().setRepairIllegalPunnings(false)); 
        final OntologyLoadOptions options = new OntologyLoadOptions.Builder()
                .owlConfig(cfg)
                .build();
        final LoadedOntology loadedCheckOntology = loader.load(checkedOntologyFileOrIRI, options);
        return new OWLOntologyWrapper(loadedCheckOntology);
    }

    private static void printHeader(PrintStream out) {
        out.println("CaseID" + SEP_STRING + "Individual" + SEP_STRING + "Classes" + SEP_STRING + "ObjectProperties" + SEP_STRING + "DataProperties" + SEP_STRING + "Annotations" +
                SEP_STRING + "MissingType" + SEP_STRING + "WrongClasses" + SEP_STRING + "WrongProperties"
        );
    }

    private static void printResults(PrintStream out, String caseId, String[] entityInfo, boolean missingType, int wrongClasses, int wrongProperties) {
        out.println(caseId + SEP_STRING + entityInfo[0] + SEP_STRING + entityInfo[1] + SEP_STRING + entityInfo[2] + SEP_STRING + entityInfo[3] + SEP_STRING + entityInfo[4] +
                SEP_STRING + (missingType ? "TRUE" : "FALSE") +
                SEP_STRING + wrongClasses + SEP_STRING + wrongProperties
        );
    }
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar " + BatchOntologyChecker.class.getSimpleName() + ".jar <reference ontology file/IRI> <checked ontologies folder> [<results file>]");
			return;
		}
		final String refOntologyFileOrIRI = Objects.requireNonNull(args[0], "Ontology file/IRI must not be null");
        final String checkedOntologiesFolder = Objects.requireNonNull(args[1], "Checked ontologies folder must not be null");
        PrintStream out = System.out;
        if (args.length >= 3) {
            try {
                out = new PrintStream(Files.newOutputStream(Paths.get(args[2])));
            } catch (IOException e) {
                LOGGER.error("Error when opening the results file for writing: " + e.getMessage());
                return;
            }
        }
        
        final OntologyLoader loader = new OntologyLoader();
        OWLOntologyWrapper refWrapper = null;

        try {
            final LoadedOntology loadedRefOntology = loader.load(refOntologyFileOrIRI);
            refWrapper = new OWLOntologyWrapper(loadedRefOntology);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        if (refWrapper == null) {
            LOGGER.error("Failed to load the reference ontology. Exiting.");
            return;
        }
        final OntologyConsistencyChecker checker = new OntologyConsistencyChecker(refWrapper);
        Path dir = Paths.get(checkedOntologiesFolder);
        printHeader(out);
        try {

            for (Path path : Files.newDirectoryStream(dir)) {
                if (Files.isRegularFile(path)) {
                    LOGGER.info("Checking ontology file: " + path.getFileName());
                    try {
                        final String fileNameOnly = FilenameUtils.getBaseName(path.toString());
                        OWLOntologyWrapper checkedWrapper = loadCheckedOntology(path.toString(), loader);
                        List<String[]> ontEntities = checkedWrapper.getDebugPrinter().getTabulatedIndividuals(Imports.EXCLUDED);
                        for (String[] entityData : ontEntities) {
                            IRI entityIRI = refWrapper.toIRI(entityData[0]);
                            List<String> issues = new ArrayList<>();
                            boolean missingType = checker.checkEmptyTypeForIndividual(checkedWrapper, entityIRI, issues);
                            int wrongTypes = checker.countWrongTypesForIndividual(checkedWrapper, entityIRI, issues);
                            int wrongObjProperties = checker.countWrongObjectPropertiesForIndividual(checkedWrapper, entityIRI, issues);
                            int wrongDataProperties = checker.countWrongDataPropertiesForIndividual(checkedWrapper, entityIRI, issues);
                            int wrongAnnotations = checker.countWrongAnnotationsForIndividual(checkedWrapper, entityIRI, issues);
                            if (!refWrapper.getIndividualsInSignature(Imports.EXCLUDED).contains(entityIRI))
                                printResults(out, fileNameOnly, entityData, missingType, wrongTypes, wrongObjProperties + wrongDataProperties + wrongAnnotations);
                            if (!issues.isEmpty()) {
                                LOGGER.debug("Issues for individual " + entityData[0] + " in ontology " + path.getFileName() + ":");
                                for (String issue : issues) {
                                    LOGGER.debug(" - " + issue);
                                }
                            }
                        }
//                        checker.check(checkedWrapper);
                    } catch (OWLOntologyCreationException e) {
                        LOGGER.debug("Not an ontology or malformed ontology: " + path.getFileName());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error when accessing the folder: " + e.getMessage());
        }
    }

}
