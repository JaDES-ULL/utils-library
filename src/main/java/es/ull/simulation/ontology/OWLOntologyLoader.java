package es.ull.simulation.ontology;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

/**
 * A class to load OWL ontologies from files or IRIs with optional local IRI mappings
 */
public class OWLOntologyLoader {
	/** 
	 * The OWL ontology manager 
	 */
	private final OWLOntologyManager manager;
    /** 
	 * The OWL ontology 
	 */
	private final OWLOntology ontology;

    /**
     * Private constructor to enforce the use of static factory methods
     * @param manager The OWL ontology manager
     * @param ontology The OWL ontology
     */
    private OWLOntologyLoader(OWLOntologyManager manager, OWLOntology ontology) {
        this.manager = manager;
        this.ontology = ontology;
    }

	/**
     * Returns the OWL ontology manager
     * @return the OWL ontology manager
     */
    public OWLOntologyManager getManager() {
        return manager;
    }

    /**
     * Returns the OWL ontology
     * @return the OWL ontology
     */
    public OWLOntology getOntology() {
        return ontology;
    }

    /**
     * Loads an ontology from a file with optional local IRI mappings
     * @param file The ontology file
     * @param localMappings The local mappings in the form IRI=localPath
     * @return an OWLOntologyLoader instance
     * @throws OWLOntologyCreationException if there is an error loading the ontology
     */
    public static OWLOntologyLoader fromFile(File file, String... localMappings) throws OWLOntologyCreationException {
		final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		addLocalIRIMappers(manager, localMappings);
		final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
        return new OWLOntologyLoader(manager, ontology);
    }

    /**
     * Loads an ontology from a file path with optional local IRI mappings
     * @param filePath The path to the ontology file
     * @param localMappings The local mappings in the form IRI=localPath
     * @return an OWLOntologyLoader instance
     * @throws OWLOntologyCreationException if there is an error loading the ontology
     */
    public static OWLOntologyLoader fromFile(String filePath, String... localMappings) throws OWLOntologyCreationException {
        return fromFile(new File(filePath), localMappings);
    }
    
    /**
     * Loads an ontology from an IRI with optional local IRI mappings
     * @param iri The IRI of the ontology
     * @param localMappings The local mappings in the form IRI=localPath
     * @return an OWLOntologyLoader instance
     * @throws OWLOntologyCreationException if there is an error loading the ontology
     */
    public static OWLOntologyLoader fromIRI(IRI iri, String... localMappings) throws OWLOntologyCreationException {
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        addLocalIRIMappers(manager, localMappings);
        final OWLOntology ontology = manager.loadOntology(iri);
        return new OWLOntologyLoader(manager, ontology);
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
                    IRI schemaIRI = IRI.create(parts[0]);
                    File schemaFile = new File(parts[1]);
                    manager.getIRIMappers().add(new SimpleIRIMapper(schemaIRI, IRI.create(schemaFile)));		
				}
			}
		}
	}
}
