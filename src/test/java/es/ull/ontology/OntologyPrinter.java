package es.ull.ontology;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.parameters.Imports;

import es.ull.simulation.ontology.LoadedOntology;
import es.ull.simulation.ontology.OWLOntologyWrapper;
import es.ull.simulation.ontology.OntologyLoadOptions;
import es.ull.simulation.ontology.OntologyLoader;

public class OntologyPrinter {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar " + OntologyPrinter.class.getSimpleName() + ".jar <ontology file/IRI> <mode>");
			System.out.println("Mode can be 1 to print individuals, 2 to print classes and properties, and 3 to print classes and properties to be used in an enum");
			return;
		}
		boolean isURI = true;
		String ontologyFileOrIRI = Objects.requireNonNull(args[0], "Ontology file/IRI must not be null");
		Objects.requireNonNull(args[1], "Mode must not be null");
		try {
			Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Mode must be an integer (1, 2, or 3)");
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
			OWLOntologyDocumentSource source;
			if (isURI) {
				source = new IRIDocumentSource(Objects.requireNonNull(IRI.create(ontologyFileOrIRI)));
			}
			else {
				final Path path = Paths.get(ontologyFileOrIRI);
				if (!Files.exists(path) || !Files.isRegularFile(path)) {
					System.err.println("The specified ontology file does not exist or is not a regular file: " + ontologyFileOrIRI);
					return;
				}
				source = new FileDocumentSource(Objects.requireNonNull(path.toFile()));
			}
            final OWLOntologyLoaderConfiguration cfg = Objects.requireNonNull(new OWLOntologyLoaderConfiguration().setRepairIllegalPunnings(false)); 
            final OntologyLoadOptions options = new OntologyLoadOptions.Builder()
                    .owlConfig(cfg)
                    .build();
			final LoadedOntology loadedOntology = loader.load(source, options);
			wrapper = new OWLOntologyWrapper(loadedOntology);
			switch (mode) {
				case 1:
					wrapper.getDebugPrinter().printTabulatedIndividuals(Imports.INCLUDED);
					break;
				case 2:
					System.out.println("---------------- CLASSES ----------------");
					wrapper.getDebugPrinter().printClasses(Imports.EXCLUDED);
                    System.out.println();
					System.out.println("---------------- DATA PROPS ----------------");
					wrapper.getDebugPrinter().printDataProperties(Imports.EXCLUDED);
                    System.out.println();
					System.out.println("---------------- OBJECT PROPS ----------------");
					wrapper.getDebugPrinter().printObjectProperties(Imports.EXCLUDED);
                    System.out.println();
					break;
				case 3:
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
