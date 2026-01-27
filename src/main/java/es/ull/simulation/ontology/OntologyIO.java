package es.ull.simulation.ontology;

import java.util.Objects;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class OntologyIO {
    /** 
     * The context for this ontology input/output 
     **/
    private final OntologyContext ctx;
  	/**
	 * An internal loader to add more ontologies to the manager
	 */
	private final OntologyLoader internalLoader;
  

    public OntologyIO(final OntologyContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
        this.internalLoader = new OntologyLoader();
    }

	/**
	 * Loads another ontology from the specified source and makes it available in the manager
	 * @param iri The IRI of the new ontology
	 * @return The loaded ontology
	 * @throws OWLOntologyCreationException 
	 */
	public OWLOntology loadOntology(final OntologySource source) throws OWLOntologyCreationException {
		Objects.requireNonNull(source, "source must not be null");
		final OWLOntology ontology = internalLoader.load(source, ctx.getManager()).ontology();
        ctx.markDirty();
        return ontology;
	}


	/**
	 * Saves the ontology to the original file
	 * @throws OWLOntologyStorageException If the ontology cannot be saved or if the ontology was not loaded from a file
	 */
	public void save() throws OWLOntologyStorageException {
		IRI docIRI = ctx.getManager().getOntologyDocumentIRI(ctx.getOntology());
		if (!"file".equalsIgnoreCase(docIRI.getScheme())) {
			throw new OWLOntologyStorageException(
				"Ontology was not loaded from a file. Use saveAs(...) to specify a local path.");
		}
		ctx.getManager().saveOntology(ctx.getOntology());
	}

    public void saveAs(OntologyDestination destination) throws OWLOntologyStorageException {
        Objects.requireNonNull(destination, "destination must not be null");
        if (destination instanceof OntologyDestination.ToPath p) {
            saveAs(IRI.create(Objects.requireNonNull(p.path().toUri())));
        } else if (destination instanceof OntologyDestination.ToIRI iri) {
            saveAs(Objects.requireNonNull(iri.iri()));
        } else if (destination instanceof OntologyDestination.ToStream s) {
            ctx.getManager().saveOntology(ctx.getOntology(), Objects.requireNonNull(s.outputStream()));
        } else {
            throw new IllegalArgumentException("Unsupported destination: " + destination);
        }
    }

    /**
	 * Saves the ontology to the specified IRI. It also updates the document IRI of the ontology to the new IRI.
	 * @param documentIri The IRI where the ontology will be saved
	 * @throws OWLOntologyStorageException If the ontology cannot be saved
	 */
    private void saveAs(IRI documentIri) throws OWLOntologyStorageException {
		ctx.getManager().setOntologyDocumentIRI(ctx.getOntology(), Objects.requireNonNull(documentIri));
        ctx.getManager().saveOntology(ctx.getOntology());
    }
    
}
