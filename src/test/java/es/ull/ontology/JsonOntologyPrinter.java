package es.ull.ontology;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFJsonLDDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;

import es.ull.simulation.ontology.LoadedOntology;
import es.ull.simulation.ontology.OWLOntologyWrapper;

/**
 * A printer for ontologies that forces JSON format.
 */
public class JsonOntologyPrinter {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: java -jar " + JsonOntologyPrinter.class.getSimpleName() + ".jar <ontology file>");
			return;
		}
		String ontologyFile = Objects.requireNonNull(args[0], "Ontology file must not be null");

		OWLOntologyWrapper wrapper = null;
		
		try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            final Path path = Paths.get(ontologyFile);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                System.err.println("The specified ontology file does not exist or is not a regular file: " + ontologyFile);
                return;
            }
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(
                new FileDocumentSource(Objects.requireNonNull(path.toFile(), "Ontology file must not be null"), new RDFJsonLDDocumentFormat())
            );
            LoadedOntology loadedOntology = new LoadedOntology(ontology, manager, manager.getOWLDataFactory());
			wrapper = new OWLOntologyWrapper(loadedOntology, true);
			wrapper.getDebugPrinter().printTabulatedIndividuals(Imports.INCLUDED);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

}
