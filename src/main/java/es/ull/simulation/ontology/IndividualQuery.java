package es.ull.simulation.ontology;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;

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
     * Returns the asserted named classes for an individual (ClassAssertion axioms).
     * If imports == INCLUDED, traverses the imports closure. If directOnly == false, returns asserted types plus all (named) 
     * superclasses reachable through asserted SubClassOf axioms in the selected scope (local or imports closure). 
     * This method does not use a reasoner, but performs a graph traversal over the asserted subclass hierarchy. 
     * It ignores anonymous superclass expressions (restrictions, intersections, etc.).
     * @param individualIri The IRI of the individual
     * @param directOnly if false, returns all asserted types (including superclasses); if true, returns only the direct asserted types (most specific).
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of named class IRIs
     */
    public Set<IRI> getAssertedTypes(final IRI individualIri, final boolean directOnly, final Imports imports) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final Set<IRI> closure = new LinkedHashSet<>();
        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));

        if (imports == Imports.EXCLUDED) {
            for (OWLClassAssertionAxiom ax : ctx.getOntology().getClassAssertionAxioms(ind)) {
                OWLClassExpression ce = ax.getClassExpression();
                if (!ce.isAnonymous()) {
                    closure.add(ce.asOWLClass().getIRI());
                }
            }
            return closure;
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            for (OWLClassAssertionAxiom ax : o.getClassAssertionAxioms(ind)) {
                OWLClassExpression ce = ax.getClassExpression();
                if (!ce.isAnonymous()) {
                    closure.add(ce.asOWLClass().getIRI());
                }
            }
        }
        if (directOnly || closure.isEmpty()) {
            return closure;
        }
        else {
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
     * @param directOnly if true, checks only for direct assertions; if false, checks for any assertion of the class or its superclasses 
     * (traversing asserted subclass hierarchy as in getAssertedTypesWithSuperclasses)
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return true if the individual is asserted to be an instance of the class; false otherwise
     */
    public boolean isInstanceOfAsserted(final IRI individualIri, final IRI classIri, final boolean directOnly, final Imports imports) {
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

        final Set<OWLClass> directClasses = (imports == Imports.INCLUDED
                ? ctx.getOntology().importsClosure()
                : Stream.of(ctx.getOntology()))
                .flatMap(ont -> ont.getClassAssertionAxioms(ind).stream())
                .map(OWLClassAssertionAxiom::getClassExpression)
                .filter(ce -> !ce.isAnonymous())
                .map(OWLClassExpression::asOWLClass)
                .collect(Collectors.toSet());

        if (directOnly) {
            return directClasses.contains(cls);
        } else {
            return directClasses.stream()
                    .anyMatch(directClass -> directClass.equals(cls) ||
                            getSuperClassesAsserted(directClass.getIRI(), false, imports).contains(classIri));
        }
    }

    /**
     * Returns the superclasses of the specified class (asserted).
     * @param classIri The IRI of a class in the ontology
     * @param directOnly if true, returns only direct superclasses; if false, returns all superclasses (transitive closure).
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     */
    public Set<IRI> getSuperClassesAsserted(final IRI classIri, final boolean directOnly, final Imports imports) {
        Objects.requireNonNull(classIri, "classIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));
        final Stream<OWLOntology> ontologies = imports == Imports.INCLUDED
                ? ctx.getOntology().importsClosure()
                : Stream.of(ctx.getOntology());

        if (directOnly) {
            return ontologies
                    .flatMap(ont -> ont.getSubClassAxiomsForSubClass(cls).stream())
                    .map(OWLSubClassOfAxiom::getSuperClass)
                    .filter(superExpr -> !superExpr.isAnonymous())
                    .map(superExpr -> superExpr.asOWLClass().getIRI())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            final Set<IRI> result = new LinkedHashSet<>();
            final Deque<OWLClass> queue = new ArrayDeque<>();
            queue.add(cls);

            while (!queue.isEmpty()) {
                final OWLClass current = Objects.requireNonNull(queue.poll());
                (imports == Imports.INCLUDED
                        ? ctx.getOntology().importsClosure()
                        : Stream.of(ctx.getOntology()))
                        .flatMap(ont -> ont.getSubClassAxiomsForSubClass(current).stream())
                        .map(OWLSubClassOfAxiom::getSuperClass)
                        .filter(superExpr -> !superExpr.isAnonymous())
                        .map(OWLClassExpression::asOWLClass)
                        .forEach(superClass -> {
                            if (result.add(superClass.getIRI())) {
                                queue.add(superClass);
                            }
                        });
            }
            return result;
        }
    }

    /**
     * Returns named individuals x such that ClassAssertion(class, x) is asserted.
     * If imports == INCLUDED, traverses the imports closure.
     * @param classIri The IRI of the class
     * @param directOnly if true, returns only individuals with a direct ClassAssertion; if false, returns individuals with a ClassAssertion to the class 
     * or any of its subclasses (traversing asserted subclass hierarchy as in getSuperClassesAsserted)
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of named individual IRIs
     */
    public Set<IRI> getIndividualsOfClass(final IRI classIri, final boolean directOnly, final Imports imports) {
        Objects.requireNonNull(classIri, "classIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");

        if (!ctx.getOntology().containsClassInSignature(classIri, imports)) {
            return new LinkedHashSet<>();
        }

        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        final Set<IRI> directIndividuals = (imports == Imports.INCLUDED
                ? ctx.getOntology().importsClosure()
                : Stream.of(ctx.getOntology()))
                .flatMap(ont -> ont.getClassAssertionAxioms(cls).stream())
                .map(OWLClassAssertionAxiom::getIndividual)
                .filter(ind -> !ind.isAnonymous())
                .map(ind -> ind.asOWLNamedIndividual().getIRI())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (directOnly) {
            return directIndividuals;
        } else {
            // Include also individuals whose asserted type hierarchy contains cls
            final Set<IRI> result = new LinkedHashSet<>(directIndividuals);

            // Collect all named individuals in scope
            (imports == Imports.INCLUDED
                    ? ctx.getOntology().importsClosure()
                    : Stream.of(ctx.getOntology()))
                    .flatMap(ont -> ont.individualsInSignature())
                    .map(OWLNamedIndividual::getIRI)
                    .filter(indIri -> !result.contains(indIri))
                    .filter(indIri -> isInstanceOfAsserted(indIri, classIri, false, imports))
                    .forEach(result::add);

            return result;
        }
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
            Objects.requireNonNull(o, "ontology in imports closure must not be null");
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

    /**
     * Returns all asserted annotation values for an individual, grouped by annotation property.
     *
     * @param subjectIri The IRI of the subject individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A map from annotation property IRIs to sets of annotation values
     */
    public Map<IRI, Set<OWLAnnotationValue>> getAllAnnotationValues(final IRI subjectIri, final Imports imports) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(imports, "imports must not be null");
        final Map<IRI, Set<OWLAnnotationValue>> result = new LinkedHashMap<>();

        if (!ctx.getOntology().containsIndividualInSignature(subjectIri, imports)) {
            return result;
        }
        final OWLNamedIndividual subj = ctx.getFactory().getOWLNamedIndividual(subjectIri);

        if (imports == Imports.EXCLUDED) {
            collectAllAnnotationValuesFromOntology(ctx.getOntology(), subj, result);
            return result;
        }
        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            collectAllAnnotationValuesFromOntology(o, subj, result);
        }
        return result;
    }

    /**
     * Helper method to collect all annotation property values from a given ontology
     * for a subject individual (as annotation subject).
     *
     * @param ontology the ontology to query
     * @param subj the subject individual whose annotations are to be collected
     * @param out the output map to populate with annotation property IRIs and their corresponding values
     */
    private static void collectAllAnnotationValuesFromOntology(final OWLOntology ontology, final OWLNamedIndividual subj, final Map<IRI, Set<OWLAnnotationValue>> out) {
        Objects.requireNonNull(ontology, "ontology must not be null");
        Objects.requireNonNull(subj, "subj must not be null");
        Objects.requireNonNull(out, "out must not be null");
        final IRI subjectIri = Objects.requireNonNull(subj.getIRI(), "subjectIri must not be null");
        for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(subjectIri)) {
            final OWLAnnotationProperty prop = ax.getProperty();
            final OWLAnnotationValue value = ax.getValue();
            out.computeIfAbsent(prop.getIRI(), k -> new LinkedHashSet<>()).add(value);
        }
    }

	/**
	 * Returns the label (rdfs:label) of an ontology element in a specific language, if available.
	 * @param elementIRI The IRI of the element in the ontology
	 * @param lang The language code (e.g., "es" or "en")
	 * @return The label in the given language, or an empty Optional if not found
	 */
    public Optional<String> getLabelForIRI(IRI elementIRI, String lang) {
        Objects.requireNonNull(elementIRI, "elementIRI must not be null");
        Objects.requireNonNull(lang, "lang must not be null");

        OWLEntity entity = ctx.getOntology().entitiesInSignature(elementIRI, Imports.INCLUDED)
                .findFirst()
                .orElse(null);

        if (entity == null) {
            return Optional.empty();
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            Objects.requireNonNull(o, "ontology in imports closure must not be null");
            for (OWLAnnotation annotation : EntitySearcher.getAnnotations(entity, o).collect(Collectors.toList())) {
                if (!annotation.getProperty().isLabel() || annotation.getValue().asLiteral().isEmpty()) {
                    continue;
                }
                OWLLiteral literal = annotation.getValue().asLiteral().get();
                if (literal.hasLang(lang)) {
                    return Optional.of(literal.getLiteral());
                }
            }
        }
        return Optional.empty();
    }

	/**
	 * Returns the comment (rdfs:comment) of an ontology element in a specific language, if available.
	 * @param elementIRI The IRI of the element in the ontology
	 * @param lang The language code (e.g., "es" or "en")
	 * @return The label in the given language, or an empty Optional if not found
	 */
    public Optional<String> getCommentForIRI(IRI elementIRI, String lang) {
        Objects.requireNonNull(elementIRI, "elementIRI must not be null");
        Objects.requireNonNull(lang, "lang must not be null");

        OWLEntity entity = ctx.getOntology().entitiesInSignature(elementIRI, Imports.INCLUDED)
                .findFirst()
                .orElse(null);

        if (entity == null) {
            return Optional.empty();
        }

        for (OWLOntology o : ctx.getOntology().getImportsClosure()) {
            Objects.requireNonNull(o, "ontology in imports closure must not be null");
            for (OWLAnnotation annotation : EntitySearcher.getAnnotations(entity, o).collect(Collectors.toList())) {
                if (!annotation.getProperty().isComment() || annotation.getValue().asLiteral().isEmpty()) {
                    continue;
                }
                OWLLiteral literal = annotation.getValue().asLiteral().get();
                if (literal.hasLang(lang)) {
                    return Optional.of(literal.getLiteral());
                }
            }
        }
        return Optional.empty();
    }
}
