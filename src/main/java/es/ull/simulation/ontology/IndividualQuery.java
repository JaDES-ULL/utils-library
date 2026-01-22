package es.ull.simulation.ontology;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Asserted (non-reasoned) queries over individuals, with support for Imports.EXCLUDED/INCLUDED.
 *
 * Design notes:
 * - Reads only: never modifies ontology (no markDirty()).
 * - Imports.INCLUDED is implemented by iterating the imports closure when OWLAPI lacks overloads with Imports.
 * - Returns only named individuals (anonymous individuals are ignored).
 */
public final class IndividualQuery {
    /**
     * The ontology context
     */
    private final OntologyContext ctx;

    /**
     * Creates a new IndividualQuery instance.
     * @param ctx The ontology context
     */
    public IndividualQuery(final OntologyContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
    }

    /**
     * Returns all individuals in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of individual IRIs
     */
    public Set<IRI> getIndividualsInSignature(final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> result = new LinkedHashSet<>();
        ctx.getOntology().individualsInSignature(imports)
            .map(ind -> ind.getIRI())
            .forEach(result::add);
        return result;
    }

    /**
     * Returns all classes in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of class IRIs
     */
    public Set<IRI> getClassesInSignature(final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> result = new LinkedHashSet<>();
        ctx.getOntology().classesInSignature(imports)
            .map(cls -> cls.getIRI())
            .forEach(result::add);
        return result;
    }

    /**
     * Returns all object properties in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of object property IRIs
     */
    public Set<IRI> getObjectPropertiesInSignature(final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> result = new LinkedHashSet<>();
        ctx.getOntology().objectPropertiesInSignature(imports)
            .map(prop -> prop.getIRI())
            .forEach(result::add);
        return result;
    }

    /**
     * Returns all data properties in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of data property IRIs
     */
    public Set<IRI> getDataPropertiesInSignature(final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> result = new LinkedHashSet<>();
        ctx.getOntology().dataPropertiesInSignature(imports)
            .map(prop -> prop.getIRI())
            .forEach(result::add);
        return result;
    }

    /**
     * Checks whether an individual with the given IRI exists in the ontology signature.
     * @param individualIri The IRI of the individual to check
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return true if the individual exists; false otherwise
     */
    public boolean existsIndividual(final IRI individualIri, final Imports imports) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsIndividualInSignature(individualIri, imports);
    }

    /**
     * Checks whether a class with the given IRI exists in the ontology signature.
     * @param classIri The IRI of the class to check
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return true if the class exists; false otherwise
     */
    public boolean existsClass(final IRI classIri, final Imports imports) {
        Objects.requireNonNull(classIri, "classIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsClassInSignature(classIri, imports);
    }

    /**
     * Checks whether an object property with the given IRI exists in the ontology signature.
     * @param propIri The IRI of the object property to check
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return true if the object property exists; false otherwise
     */
    public boolean existsObjectProperty(final IRI propIri, final Imports imports) {
        Objects.requireNonNull(propIri, "propIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsObjectPropertyInSignature(propIri, imports);
    }

    /**
     * Checks whether a data property with the given IRI exists in the ontology signature.
     * @param propIri The IRI of the data property to check
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return true if the data property exists; false otherwise
     */
    public boolean existsDataProperty(final IRI propIri, final Imports imports) {
        Objects.requireNonNull(propIri, "propIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsDataPropertyInSignature(propIri, imports);
    }

    /**
     * Returns the asserted named classes for an individual (ClassAssertion axioms).
     * If imports == INCLUDED, traverses the imports closure.
     * @param individualIri The IRI of the individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of named class IRIs
     */
    public Set<IRI> getTypes(final IRI individualIri, final Imports imports) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> result = new LinkedHashSet<>();
        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));

        if (imports == Imports.EXCLUDED) {
            for (OWLClassAssertionAxiom ax : ctx.getOntology().getClassAssertionAxioms(ind)) {
                OWLClassExpression ce = ax.getClassExpression();
                if (!ce.isAnonymous()) {
                    result.add(ce.asOWLClass().getIRI());
                }
            }
            return result;
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            for (OWLClassAssertionAxiom ax : o.getClassAssertionAxioms(ind)) {
                OWLClassExpression ce = ax.getClassExpression();
                if (!ce.isAnonymous()) {
                    result.add(ce.asOWLClass().getIRI());
                }
            }
        }
        return result;
    }

    /**
     * Returns asserted types plus all (named) superclasses reachable through asserted SubClassOf
     * axioms in the selected scope (local or imports closure). This method does not use a reasoner, 
     * but performs a graph traversal over the asserted subclass hierarchy. It ignores anonymous superclass 
     * expressions (restrictions, intersections, etc.).
     * @param individualIri The IRI of the individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of named class and superclass IRIs
     */
    public Set<IRI> getAssertedTypesWithSuperclasses(final IRI individualIri, final Imports imports) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        // Start from asserted named types (direct)
        final Set<IRI> closure = new LinkedHashSet<>(getTypes(individualIri, imports));
        if (closure.isEmpty()) {
            return closure;
        }

        // Worklist/BFS over superclass edges
        final Deque<IRI> queue = new ArrayDeque<>(closure);
        while (!queue.isEmpty()) {
            final IRI currentClassIri = Objects.requireNonNull(queue.removeFirst());
            final OWLClass current = ctx.getFactory().getOWLClass(currentClassIri);

            if (imports == Imports.EXCLUDED) {
                enqueueNamedSuperclasses(ctx.getOntology(), current, closure, queue);
            } else {
                for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
                    enqueueNamedSuperclasses(o, current, closure, queue);
                }
            }
        }

        return closure;
    }

    /**
     * Helper method to enqueue named superclasses of a given subclass.
     * @param ontology the ontology to query
     * @param subClass the subclass
     * @param closure the set of IRIs representing the closure of superclasses found so far
     * @param queue the queue used for breadth-first traversal of superclasses
     */
    private static void enqueueNamedSuperclasses(final OWLOntology ontology, final OWLClass subClass, final Set<IRI> closure, final Deque<IRI> queue) {
        Objects.requireNonNull(subClass, "subClass must not be null");
        Objects.requireNonNull(ontology, "ontology must not be null");
        Objects.requireNonNull(closure, "closure must not be null");
        Objects.requireNonNull(queue, "queue must not be null");
        for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(subClass)) {
            final OWLClassExpression sup = ax.getSuperClass();

            // Only follow named superclasses (no restrictions/anonymous expressions)
            if (sup.isAnonymous()) {
                continue;
            }
            final IRI supIri = sup.asOWLClass().getIRI();
            if (closure.add(supIri)) {
                queue.addLast(supIri);
            }
        }
    }

    /**
     * Asserted instance check: true iff there is an explicit ClassAssertion(class, individual)
     * in the selected scope (local or imports closure).
     * @param individualIri The IRI of the individual
     * @param classIri The IRI of the class
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return true if the individual is asserted to be an instance of the class; false otherwise
     */
    public boolean isInstanceOfAsserted(final IRI individualIri, final IRI classIri, final Imports imports) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(classIri, "classIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        if (!ctx.getOntology().containsIndividualInSignature(individualIri, imports)) {
            return false;
        }
        if (!ctx.getOntology().containsClassInSignature(classIri, imports)) {
            return false;
        }

        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));
        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        if (imports == Imports.EXCLUDED) {
            return ctx.getOntology().getClassAssertionAxioms(ind).stream()
                    .anyMatch(ax -> {
                        OWLClassExpression ce = ax.getClassExpression();
                        return !ce.isAnonymous() && ce.asOWLClass().equals(cls);
                    });
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            for (OWLClassAssertionAxiom ax : o.getClassAssertionAxioms(ind)) {
                OWLClassExpression ce = ax.getClassExpression();
                if (!ce.isAnonymous() && ce.asOWLClass().equals(cls)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns named individuals x such that ClassAssertion(class, x) is asserted.
     * If imports == INCLUDED, traverses the imports closure.
     * @param classIri The IRI of the class
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of named individual IRIs
     */
    public Set<IRI> getIndividualsOfClass(final IRI classIri, final Imports imports) {
        Objects.requireNonNull(classIri, "classIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> result = new LinkedHashSet<>();

        // If the class is not in the scope signature, there cannot be asserted instances (in that scope).
        if (!ctx.getOntology().containsClassInSignature(classIri, imports)) {
            return result;
        }

        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        if (imports == Imports.EXCLUDED) {
            for (OWLClassAssertionAxiom ax : ctx.getOntology().getClassAssertionAxioms(cls)) {
                OWLIndividual ind = ax.getIndividual();
                if (!ind.isAnonymous()) {
                    result.add(ind.asOWLNamedIndividual().getIRI());
                }
            }
            return result;
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            for (OWLClassAssertionAxiom ax : o.getClassAssertionAxioms(cls)) {
                OWLIndividual ind = ax.getIndividual();
                if (!ind.isAnonymous()) {
                    result.add(ind.asOWLNamedIndividual().getIRI());
                }
            }
        }

        return result;
    }

    /**
     * Returns asserted object property values for an individual.
     * @param subjectIri The IRI of the subject individual
     * @param objectPropertyIri The IRI of the object property
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of object individual IRIs
     */
    public Set<IRI> getObjectPropertyValues(final IRI subjectIri, final IRI objectPropertyIri, final Imports imports) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(objectPropertyIri, "objectPropertyIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> result = new LinkedHashSet<>();

        if (!ctx.getOntology().containsIndividualInSignature(subjectIri, imports)) {
            return result;
        }
        if (!ctx.getOntology().containsObjectPropertyInSignature(objectPropertyIri, imports)) {
            return result;
        }

        final OWLNamedIndividual subj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(subjectIri));
        final OWLObjectProperty prop = Objects.requireNonNull(ctx.getFactory().getOWLObjectProperty(objectPropertyIri));

        if (imports == Imports.EXCLUDED) {
            for (OWLObjectPropertyAssertionAxiom ax : ctx.getOntology().getObjectPropertyAssertionAxioms(subj)) {
                if (ax.getProperty().isAnonymous()) continue;
                if (!ax.getProperty().asOWLObjectProperty().equals(prop)) continue;

                OWLIndividual obj = ax.getObject();
                if (!obj.isAnonymous()) {
                    result.add(obj.asOWLNamedIndividual().getIRI());
                }
            }
            return result;
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            for (OWLObjectPropertyAssertionAxiom ax : o.getObjectPropertyAssertionAxioms(subj)) {
                if (ax.getProperty().isAnonymous()) continue;
                if (!ax.getProperty().asOWLObjectProperty().equals(prop)) continue;

                OWLIndividual obj = ax.getObject();
                if (!obj.isAnonymous()) {
                    result.add(obj.asOWLNamedIndividual().getIRI());
                }
            }
        }

        return result;
    }

    /**
     * Checks if an individual has a specific object property value asserted.
     * @param subjectIri The IRI of the subject individual
     * @param objectPropertyIri The IRI of the object property
     * @param objectIri The IRI of the object individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return True if the object property value is asserted for the individual, false otherwise
     */
    public boolean hasObjectPropertyValue(final IRI subjectIri, final IRI objectPropertyIri, final IRI objectIri, final Imports imports) {
        Objects.requireNonNull(objectIri, "objectIri must not be null");
        return getObjectPropertyValues(subjectIri, objectPropertyIri, imports).contains(objectIri);
    }

    /**
     * Returns asserted data property values for an individual.
     * @param subjectIri The IRI of the subject individual
     * @param dataPropertyIri The IRI of the data property
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of data property values
     */
    public Set<OWLLiteral> getDataPropertyValues(final IRI subjectIri, final IRI dataPropertyIri, final Imports imports) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(dataPropertyIri, "dataPropertyIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<OWLLiteral> result = new LinkedHashSet<>();

        if (!ctx.getOntology().containsIndividualInSignature(subjectIri, imports)) {
            return result;
        }
        if (!ctx.getOntology().containsDataPropertyInSignature(dataPropertyIri, imports)) {
            return result;
        }

        final OWLNamedIndividual subj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(subjectIri));
        final OWLDataProperty prop = ctx.getFactory().getOWLDataProperty(dataPropertyIri);

        if (imports == Imports.EXCLUDED) {
            for (OWLDataPropertyAssertionAxiom ax : ctx.getOntology().getDataPropertyAssertionAxioms(subj)) {
                if (ax.getProperty().isAnonymous()) continue;
                if (!ax.getProperty().asOWLDataProperty().equals(prop)) continue;
                result.add(ax.getObject());
            }
            return result;
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            for (OWLDataPropertyAssertionAxiom ax : o.getDataPropertyAssertionAxioms(subj)) {
                if (ax.getProperty().isAnonymous()) continue;
                if (!ax.getProperty().asOWLDataProperty().equals(prop)) continue;
                result.add(ax.getObject());
            }
        }

        return result;
    }

    /**
     * Checks if an individual has a specific data property value asserted.
     * @param subjectIri The IRI of the subject individual
     * @param dataPropertyIri The IRI of the data property
     * @param value The data property value to check
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return True if the data property value is asserted for the individual, false otherwise
     */
    public boolean hasDataPropertyValue(final IRI subjectIri, final IRI dataPropertyIri, final OWLLiteral value, final Imports imports) {
        Objects.requireNonNull(value, "value must not be null");
        return getDataPropertyValues(subjectIri, dataPropertyIri, imports).contains(value);
    }

    /**
     * Returns all asserted object property values for an individual, grouped by property.
     * @param subjectIri The IRI of the subject individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A map from object property IRIs to sets of object individual IRIs
     */
    public Map<IRI, Set<IRI>> getAllObjectPropertyValues(final IRI subjectIri, final Imports imports) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final Map<IRI, Set<IRI>> result = new LinkedHashMap<>();

        if (!ctx.getOntology().containsIndividualInSignature(subjectIri, imports)) {
            return result;
        }

        final OWLNamedIndividual subj = ctx.getFactory().getOWLNamedIndividual(subjectIri);

        if (imports == Imports.EXCLUDED) {
            collectAllObjectPropertyValuesFromOntology(ctx.getOntology(), subj, result);
            return result;
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            collectAllObjectPropertyValuesFromOntology(o, subj, result);
        }
        return result;
    }

    /**
     * Helper method to collect all object property values from a given ontology for a subject individual.
     * @param ontology the ontology to query
     * @param subj the subject individual
     * @param out the output map to populate with property IRIs and their corresponding object individual IRIs
     */
    private static void collectAllObjectPropertyValuesFromOntology(final OWLOntology ontology, final OWLNamedIndividual subj, final Map<IRI, Set<IRI>> out) {
        Objects.requireNonNull(ontology, "ontology must not be null");
        Objects.requireNonNull(subj, "subj must not be null");
        for (OWLObjectPropertyAssertionAxiom ax : ontology.getObjectPropertyAssertionAxioms(subj)) {
            if (ax.getProperty().isAnonymous()) continue;

            final OWLObjectProperty prop = ax.getProperty().asOWLObjectProperty();
            final OWLIndividual obj = ax.getObject();
            if (obj.isAnonymous()) continue;

            out.computeIfAbsent(prop.getIRI(), k -> new LinkedHashSet<>())
               .add(obj.asOWLNamedIndividual().getIRI());
        }
    }

    /**
     * Returns all asserted data property values for an individual, grouped by property.
     * @param subjectIri The IRI of the subject individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A map from data property IRIs to sets of data property values
     */
    public Map<IRI, Set<OWLLiteral>> getAllDataPropertyValues(final IRI subjectIri, final Imports imports) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final Map<IRI, Set<OWLLiteral>> result = new LinkedHashMap<>();

        if (!ctx.getOntology().containsIndividualInSignature(subjectIri, imports)) {
            return result;
        }

        final OWLNamedIndividual subj = ctx.getFactory().getOWLNamedIndividual(subjectIri);

        if (imports == Imports.EXCLUDED) {
            collectAllDataPropertyValuesFromOntology(ctx.getOntology(), subj, result);
            return result;
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            collectAllDataPropertyValuesFromOntology(o, subj, result);
        }
        return result;
    }

    /**
     * Helper method to collect all data property values from a given ontology for a subject individual.
     * @param ontology the ontology to query
     * @param subj the subject individual
     * @param out the output map to populate with property IRIs and their corresponding data property values
     */
    private static void collectAllDataPropertyValuesFromOntology(final OWLOntology ontology, final OWLNamedIndividual subj, final Map<IRI, Set<OWLLiteral>> out) {
        Objects.requireNonNull(ontology, "ontology must not be null");
        Objects.requireNonNull(subj, "subj must not be null");
        for (OWLDataPropertyAssertionAxiom ax : ontology.getDataPropertyAssertionAxioms(subj)) {
            if (ax.getProperty().isAnonymous()) continue;

            final OWLDataProperty prop = ax.getProperty().asOWLDataProperty();
            out.computeIfAbsent(prop.getIRI(), k -> new LinkedHashSet<>())
               .add(ax.getObject());
        }
    }
}
