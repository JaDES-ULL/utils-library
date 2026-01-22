package es.ull.simulation.ontology;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public final class OntologyContext {
	/** 
	 * The OWL ontology manager 
	 */
    @Nonnull
	private final OWLOntologyManager manager;
	/** 
	 * The prefix manager for the ontology 
	 */
    @Nonnull
	private final PrefixManager pm;
	/**
	 * The OWL data factory
	 */
    @Nonnull
    private final OWLDataFactory factory;
    /** The revision tracker for the ontology */
    @Nonnull
    private final AtomicLong revision = new AtomicLong(0);
    /** The optional reasoner provider */
    private final Optional<ReasonerProvider> reasonerProvider;
    /** 
     * The root OWL ontology 
     */
    @Nonnull
    private OWLOntology rootOntology;

    /**
     * Creates a new OntologyContext with the specified manager, root ontology, prefix manager, and optional reasoner provider.
     * 
     * @param manager The OWL ontology manager
     * @param rootOntology The root OWL ontology
     * @param pm The prefix manager for the ontology
     * @param reasonerFactoryOrNull The optional reasoner factory (can be null)
     */
    public OntologyContext(OWLOntologyManager manager, OWLOntology rootOntology, PrefixManager pm,final OWLReasonerFactory reasonerFactoryOrNull) {
        this.manager = Objects.requireNonNull(manager);
        this.rootOntology = Objects.requireNonNull(rootOntology);
        this.factory = Objects.requireNonNull(manager.getOWLDataFactory());
        this.pm = Objects.requireNonNull(pm);
        this.reasonerProvider = (reasonerFactoryOrNull == null) ? Optional.empty() : Optional.of(new ReasonerProvider(reasonerFactoryOrNull, this.revision));
    }

    /**
     * Returns the OWL ontology manager
     * @return the OWL ontology manager
     */
    @Nonnull
    public OWLOntologyManager getManager() { 
        return manager; 
    }

    /**
     * Returns the OWL data factory
     * @return the OWL data factory
     */
    @Nonnull
    public OWLDataFactory getFactory() { 
        return factory; 
    }

    /**
     * Returns the prefix manager for the ontology
     * @return the prefix manager for the ontology
     */
    @Nonnull
    public PrefixManager getPrefixManager() { 
        return pm; 
    }

    /**
     * Returns the root OWL ontology
     * @return the root OWL ontology
     */
    @Nonnull
    public OWLOntology getOntology() { 
        return rootOntology; 
    }

    /**
     * Returns whether a reasoner is configured for this ontology context
     * @return true if a reasoner is configured, false otherwise
     */
    public boolean hasReasoner() { 
        return reasonerProvider.isPresent(); 
    }

    /**
     * Returns an OWL reasoner for the root ontology
     * @return an OWL reasoner for the root ontology
     * @throws IllegalStateException if no reasoner is configured
     */
    public OWLReasoner getReasoner() {
        return reasonerProvider
                .orElseThrow(() -> new IllegalStateException("No reasoner configured"))
                .get(rootOntology);
    }

    /** 
     * Marks the ontology as dirty, indicating that it has changed. 
     * It must be called after any change to the ontology or addition of imports.
     */
    public void markDirty() {
        revision.incrementAndGet();
    }

    /**
     * Loads an ontology from a file path with optional local IRI mappings
     * @param filePath The file path to load the ontology from
     * @param localMappings Optional local IRI mappings
     * @return The loaded ontology
     * @throws OWLOntologyCreationException If there is an error creating the ontology
     */
    public OWLOntology loadOntologyFromPath(String filePath, String... localMappings) throws OWLOntologyCreationException {
        Objects.requireNonNull(filePath, "filePath must not be null");

        OWLOntologyLoader.addLocalIRIMappers(getManager(), localMappings);
        OWLOntology loaded = OWLOntologyLoader.fromPath(filePath, getManager()).getOntology();

        // Mark as dirty since the ontology has changed
        markDirty();
        return loaded;
    }

    /**
     * Loads an ontology from an IRI with optional local IRI mappings
     * @param iri The IRI to load the ontology from
     * @param localMappings Optional local IRI mappings
     * @return The loaded ontology
     * @throws OWLOntologyCreationException If there is an error creating the ontology
     */
    public OWLOntology loadOntologyFromIRI(IRI iri, String... localMappings) throws OWLOntologyCreationException {
        Objects.requireNonNull(iri, "iri must not be null");

        OWLOntologyLoader.addLocalIRIMappers(getManager(), localMappings);
        OWLOntology loaded = OWLOntologyLoader.fromIRI(iri, getManager()).getOntology();

        markDirty();
        return loaded;
    }

    /**
     * Loads an ontology from an InputStream with optional local IRI mappings
     * @param inputStream The InputStream to load the ontology from
     * @param localMappings Optional local IRI mappings
     * @return The loaded ontology
     * @throws OWLOntologyCreationException If there is an error creating the ontology
     */
    public OWLOntology loadOntologyFromStream(InputStream inputStream, String... localMappings) throws OWLOntologyCreationException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");

        OWLOntologyLoader.addLocalIRIMappers(getManager(), localMappings);
        OWLOntology loaded = OWLOntologyLoader.fromStream(inputStream, getManager()).getOntology();

        markDirty();
        return loaded;
    }
    
    /**
     * Adds an imported ontology to the root ontology's imports closure.
     * @param ontologyToImport The ontology to be imported
     */
    public void importIntoRoot(final OWLOntology ontologyToImport) {
        Objects.requireNonNull(ontologyToImport, "ontologyToImport must not be null");

        final IRI ontologyIri = ontologyToImport.getOntologyID()
                .getOntologyIRI()
                .orElseThrow(() -> new IllegalArgumentException("Ontology has no ontology IRI; cannot add owl:imports"));

        addImportToRoot(ontologyIri); 
    }

    /**
     * Adds a new ontology to the same manager. It does not change the root ontology,
     * but it does change the universe for Imports.INCLUDED.
     * @param newOntology The ontology to be added
     * @return The added ontology
     */
    public OWLOntology incorporateOntology(OWLOntology newOntology) {
        Objects.requireNonNull(newOntology);
        markDirty();
        return newOntology;
    }

    /**
     * Adds an import declaration to the root ontology to make the root "see" the incorporated ontology.
     * @param ontologyIriToImport The IRI of the ontology to import
     */
    public void addImportToRoot(IRI ontologyIriToImport) {
        Objects.requireNonNull(ontologyIriToImport);

        OWLImportsDeclaration decl = Objects.requireNonNull(factory.getOWLImportsDeclaration(ontologyIriToImport));
        manager.applyChange(new AddImport(rootOntology, decl));
        markDirty();
    }

    /**
     * Disposes of the reasoner provider if present.
     */
    public void dispose() {
        reasonerProvider.ifPresent(ReasonerProvider::dispose);
    }

    /**
     * Creates a default PrefixManager with the specified default prefix IRI.
     * @param defaultPrefixIri The default prefix IRI
     * @return a PrefixManager instance
     */
    public static PrefixManager defaultPrefixManager(String defaultPrefixIri) {
        DefaultPrefixManager dpm = new DefaultPrefixManager();
        dpm.setDefaultPrefix(defaultPrefixIri);
        return dpm;
    }
}
