package es.ull.simulation.ontology;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Reasoner-backed queries (inferred semantics). The methods defined here do not mutate the ontology. 
 * The reasoner used is obtained from the OntologyContext provided at construction (which should be lazy + invalidated via ctx.markDirty()).
 * Some methods have a "direct" boolean parameter thatfollows OWLReasoner conventions:
 *      - getTypes(ind, direct=false) => direct types only (most specific)
 *      - getTypes(ind, direct=true)  => all (direct + indirect / superclasses)
 * For instances: getInstances(cls, direct=true) => direct instances only
 *                  getInstances(cls, direct=false)=> all instances incl. those of subclasses
 */
public final class ReasonedQuery {
    /** The ontology context */
    private final OntologyContext ctx;

    /**
     * Creates a new ReasonedQuery helper for the specified ontology context.
     * @param ctx The ontology context
     */
    public ReasonedQuery(final OntologyContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
    }

    /**
     * Returns the inferred types of the specified individual.
     * @param includeSuperClasses if true, returns all inferred types (including superclasses);
     *                            if false, returns only the direct inferred types (most specific).
     */
    public Set<IRI> getTypesInferred(final IRI individualIri, final boolean includeSuperClasses) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");

        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));

        // OWLAPI: getTypes(ind, true) -> all types; getTypes(ind, false) -> direct types
        final boolean reasonerDirectFlag = !includeSuperClasses;

        return ctx.getReasoner().getTypes(ind, reasonerDirectFlag).entities()
                .map(OWLClass::getIRI)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns true if the specified individual is an instance of the specified class (inferred)     * 
	 * @param individualIri The IRI of an individual in the ontology
	 * @param classIri The IRI of a class in the ontology
     * @param directOnly if true, checks only for direct types; if false, checks for any type (including superclasses)
     * @return true if the specified individual is an instance of the specified class (inferred).
     */
    public boolean isInstanceOfInferred(final IRI individualIri, final IRI classIri, boolean directOnly) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(classIri, "classIri must not be null");

        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));
        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        // Equivalent to: cls ∈ types(ind, includeSuperClasses=true)
        return ctx.getReasoner().getTypes(ind, directOnly).containsEntity(cls);
    }

    /**
	 * Returns true if the specified individual is an instance of the specified class (or any of its subclasses)
	 * @param individualIri The IRI of an individual in the ontology
	 * @param classIri The IRI of a class in the ontology
	 * @return true if the specified individual is an instance of the specified class (or any of its subclasses).
	 */
    public boolean isInstanceOfInferred(final IRI individualIri, final IRI classIri) {
        return isInstanceOfInferred(individualIri, classIri, false);
    }

    /**
     * Returns the individuals that are instances of the specified class (inferred).
     * @param classIri The IRI of a class in the ontology
     * @param directOnly if true, returns only direct instances of the class;
     *                   if false, includes instances of subclasses.
     * @return The set of IRIs of individuals that are instances of the specified class (inferred).
     */
    public Set<IRI> getIndividualsOfClassInferred(final IRI classIri, final boolean directOnly) {
        Objects.requireNonNull(classIri, "classIri must not be null");

        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        return ctx.getReasoner().getInstances(cls, directOnly).entities()
                .map(OWLNamedIndividual::getIRI)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns the superclasses of the specified class (inferred).
     * @param classIri The IRI of a class in the ontology
     * @param directOnly if true, returns only direct superclasses; if false, returns all superclasses.
     */
    public Set<IRI> getSuperClassesInferred(final IRI classIri, final boolean directOnly) {
        Objects.requireNonNull(classIri, "classIri must not be null");

        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        return ctx.getReasoner().getSuperClasses(cls, directOnly).entities()
                .map(OWLClass::getIRI)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns the subclasses of the specified class (inferred).
     * @param classIri The IRI of a class in the ontology
     * @param directOnly if true, returns only direct subclasses; if false, returns all subclasses.
     */
    public Set<IRI> getSubClassesInferred(final IRI classIri, final boolean directOnly) {
        Objects.requireNonNull(classIri, "classIri must not be null");

        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        return ctx.getReasoner().getSubClasses(cls, directOnly).entities()
                .map(OWLClass::getIRI)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

}

