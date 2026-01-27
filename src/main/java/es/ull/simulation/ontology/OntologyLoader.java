package es.ull.simulation.ontology;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

/**
 * A class to load OWL ontologies from files or IRIs with optional local IRI mappings
 */
public class OntologyLoader {

    /**
     * Loads an ontology from the specified source with optional local IRI mappings
     * @param source The source of the ontology
     * @param localMappings The local mappings in the form IRI=localPath
     * @return an instance of LoadedOntology containing the loaded ontology, its manager, and data factory
     * @throws OWLOntologyCreationException if there is an error loading the ontology
     */
    public LoadedOntology load(OntologySource source, String... localMappings) throws OWLOntologyCreationException {
        return load(source, OWLManager.createOWLOntologyManager(), localMappings);
    }

    /**
     * Loads an ontology from the specified source with optional local IRI mappings and using the provided manager
     * @param source The source of the ontology
     * @param manager A previously created OWL ontology manager
     * @param localMappings The local mappings in the form IRI=localPath
     * @return an instance of LoadedOntology containing the loaded ontology, its manager, and data factory
     * @throws OWLOntologyCreationException if there is an error loading the ontology
     */
    public LoadedOntology load(OntologySource source, OWLOntologyManager manager, String... localMappings) throws OWLOntologyCreationException {
        addLocalIRIMappers(manager, localMappings);
        final OWLDataFactory df = manager.getOWLDataFactory();

        final OWLOntology ontology;
        if (source instanceof OntologySource.FromPath p) {
            final Path path = Objects.requireNonNull(p.path(), "Path should never be null");
            final File file = Objects.requireNonNull(path.toFile(), "File should never be null");
            ontology = manager.loadOntologyFromOntologyDocument(file);
        } else if (source instanceof OntologySource.FromIRI i) {
            final IRI iri = Objects.requireNonNull(i.iri(), "IRI should never be null");
            ontology = manager.loadOntology(iri);
        } else if (source instanceof OntologySource.FromStream s) {
            final InputStream inputStream = Objects.requireNonNull(s.inputStream(), "InputStream should never be null");
            ontology = manager.loadOntologyFromOntologyDocument(inputStream);
        } else {
            throw new IllegalArgumentException("Unsupported source: " + source);
        }
        return new LoadedOntology(ontology, manager, df);
    }

	/**
	 * Adds a set of local path mappings for IRIs to the ontology manager. It is static so it can be used before loading ontologies.
	 * @param manager The OWL ontology manager
     * @param localMappings The local mappings in the form IRI=localPath
	 */
	public static void addLocalIRIMappers(OWLOntologyManager manager, String... localMappings) {
		if (localMappings.length > 0) {
			for (String mapping : localMappings) {
				String[] parts = mapping.split("=");
				if (parts.length == 2) {
                    IRI schemaIRI = IRI.create(Objects.requireNonNull(parts[0], "IRI in local mapping should never be null"));
                    File schemaFile = new File(Objects.requireNonNull(parts[1], "Local path in local mapping should never be null"));
                    if (schemaFile.exists() == false) {
                        throw new IllegalArgumentException("The local path in the mapping does not exist: " + schemaFile.getAbsolutePath());
                    }
                    manager.getIRIMappers().add(
                        new SimpleIRIMapper(
                            Objects.requireNonNull(schemaIRI, "Schema IRI should never be null"), 
                            Objects.requireNonNull(IRI.create(schemaFile), "Schema file IRI should never be null")));		
				}
			}
		}
	}
}
