package es.ull.ontology;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.Imports;

import es.ull.simulation.ontology.LoadedOntology;
import es.ull.simulation.ontology.OWLOntologyWrapper;
import es.ull.simulation.ontology.OntologyLoader;
import es.ull.simulation.ontology.OntologySource;

public class OntologyPrinter {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar " + OntologyPrinter.class.getSimpleName() + ".jar <ontology file/IRI> <mode>");
			System.out.println("Mode can be 1 to print individuals, 2 to print classes and properties to be used in an enum");
			return;
		}
		boolean isURI = true;
		String ontologyFileOrIRI = Objects.requireNonNull(args[0], "Ontology file/IRI must not be null");
		Objects.requireNonNull(args[1], "Mode must not be null");
		try {
			Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Mode must be an integer (1 or 2)");
			return;
		}
		int mode = Integer.parseInt(args[1]);

		OWLOntologyWrapper wrapper = null;
		try {
			new URI(ontologyFileOrIRI);
		} catch (Exception e) {
			isURI = false;
		}
		
		try {
			OntologyLoader loader = new OntologyLoader();
			OntologySource source;
			if (isURI) {
				source = new OntologySource.FromIRI(IRI.create(ontologyFileOrIRI));
			}
			else {
				final Path path = Paths.get(ontologyFileOrIRI);
				if (!Files.exists(path) || !Files.isRegularFile(path)) {
					System.err.println("The specified ontology file does not exist or is not a regular file: " + ontologyFileOrIRI);
					return;
				}
				source = new OntologySource.FromPath(path);
			}
			final LoadedOntology loadedOntology = loader.load(source);
			wrapper = new OWLOntologyWrapper(loadedOntology);
			switch (mode) {
				case 1:
					wrapper.getDebugPrinter().printTabulatedIndividuals(Imports.INCLUDED);
					break;
				case 2:
					System.out.println("---------------- CLASSES ----------------");
					wrapper.getDebugPrinter().printClassesAsEnum(Imports.EXCLUDED);
                    System.out.println();
					System.out.println("---------------- DATA PROPS ----------------");
					wrapper.getDebugPrinter().printDataPropertiesAsEnum(Imports.EXCLUDED);
                    System.out.println();
					System.out.println("---------------- OBJECT PROPS ----------------");
					wrapper.getDebugPrinter().printObjectPropertiesAsEnum(Imports.EXCLUDED);
                    System.out.println();
					break;
				default:
					System.out.println("Invalid mode. Use 1 or 2.");
					break;
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

}
