package es.ull.simulation.ontology;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * A provider for OWLReasoner instances that ensures the reasoner is updated when the ontology changes.
 */
public final class ReasonerProvider {
    /** The factory used to create OWLReasoner instances */
    private final OWLReasonerFactory reasonerFactory;
    /** The revision tracker for the ontology */
    private final AtomicLong revision;
    /** The current OWLReasoner instance */
    private OWLReasoner reasoner;
    /** The revision of the ontology when the reasoner was last created */
    private long reasonerRevision = -1;

    /**
     * Constructs an OWLReasonerProvider with the specified reasoner factory and revision tracker.
     * 
     * @param reasonerFactory The factory used to create OWLReasoner instances
     * @param revision The revision tracker for the ontology
     */
    public ReasonerProvider(OWLReasonerFactory reasonerFactory, AtomicLong revision) {
        this.reasonerFactory = Objects.requireNonNull(reasonerFactory);
        this.revision = Objects.requireNonNull(revision);
    }

    /**
     * Returns an OWLReasoner instance for the specified ontology, updating it if the ontology has changed.
     * 
     * @param rootOntology The root ontology for which to get the reasoner
     * @return An OWLReasoner instance for the specified ontology
     */
    public synchronized OWLReasoner get(OWLOntology rootOntology) {
        Objects.requireNonNull(rootOntology);

        long current = revision.get();
        if (reasoner == null || reasonerRevision != current) {
            if (reasoner != null) {
                reasoner.dispose();
            }
            reasoner = reasonerFactory.createReasoner(rootOntology);
            reasoner.precomputeInferences();
            reasonerRevision = current;
        }
        return reasoner;
    }

    /**
     * Disposes of the current OWLReasoner instance, if any.
     */
    public synchronized void dispose() {
        if (reasoner != null) {
            reasoner.dispose();
            reasoner = null;
            reasonerRevision = -1;
        }
    }
}
