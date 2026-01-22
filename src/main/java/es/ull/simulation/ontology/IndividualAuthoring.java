package es.ull.simulation.ontology;

import java.util.Objects;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Provides methods to create and modify individuals in an ontology, i.e., modify the ABox of the ontology.
 */
public final class IndividualAuthoring {
    /**
     * The ontology context to use.
     */
    private final OntologyContext ctx;

    /**
     * Creates a new IndividualAuthoring instance.
     * @param ctx the ontology context to use (must not be null)
     */
    public IndividualAuthoring(final OntologyContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
    }

    /**
     * Checks whether the given axiom is contained in the ontology (including imports).
     * @param ontology the ontology to check
     * @param ax the axiom to check for
     * @return true if the axiom is contained in the ontology (including imports), false otherwise
     */
    private static boolean containsIncluded(final OWLOntology ontology, final OWLAxiom ax) {
        Objects.requireNonNull(ontology, "ontology must not be null");
        return ontology.containsAxiom(Objects.requireNonNull(ax), Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS);
    }

    /**
     * Checks whether the given axiom is contained locally in the ontology (excluding imports).
     * @param ontology the ontology to check
     * @param ax the axiom to check for
     * @return true if the axiom is contained locally in the ontology (excluding imports), false otherwise
     */
    private static boolean containsLocal(final OWLOntology ontology, final OWLAxiom ax) {
        Objects.requireNonNull(ontology, "ontology must not be null");
        return ontology.containsAxiom(Objects.requireNonNull(ax), Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS);
    }

    /**
     * Adds the given axiom to the root ontology as a local axiom.
     * @param ax the axiom to add
     */
    private void addLocal(final OWLAxiom ax) {
        Objects.requireNonNull(ax, "ax must not be null");
        ctx.getManager().addAxiom(ctx.getOntology(), ax);
        ctx.markDirty();
    }

    /**
     * Removes the given axiom from the root ontology as a local axiom.
     * @param ax the axiom to remove
     */
    private void removeLocal(final OWLAxiom ax) {
        Objects.requireNonNull(ax, "ax must not be null");
        ctx.getOntology().removeAxiom(ax);
        ctx.markDirty();
    }

    /**
     * Creates a new named individual and types it as the given class.
     *
     * Policy: returns false if the individual already exists in Imports.INCLUDED.
     * Writes to the root ontology: Declaration + ClassAssertion.
     * @param classIri the IRI of the class to type the individual as
     * @param individualIri the IRI of the individual to create
     * @return true if the individual was created, false if it already existed
     */
    public boolean createIndividual(final IRI classIri, final IRI individualIri) {
        Objects.requireNonNull(classIri, "classIri must not be null");
        Objects.requireNonNull(individualIri, "individualIri must not be null");

        if (ctx.getOntology().containsIndividualInSignature(individualIri, Imports.INCLUDED)) {
            return false;
        }
        if (!ctx.getOntology().containsClassInSignature(classIri, Imports.INCLUDED)) {
            throw new IllegalArgumentException("Class IRI not in signature (Imports.INCLUDED): " + classIri);
        }

        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));
        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));

        final OWLDeclarationAxiom decl = Objects.requireNonNull(ctx.getFactory().getOWLDeclarationAxiom(ind));
        if (!containsIncluded(ctx.getOntology(), decl)) {
            addLocal(decl);
        }

        final OWLClassAssertionAxiom typeAx = ctx.getFactory().getOWLClassAssertionAxiom(cls, ind);
        if (!containsIncluded(ctx.getOntology(), typeAx)) {
            addLocal(typeAx);
        }

        return true;
    }

    /**
     * Asserts ClassAssertion(class, individual). Does NOT create individuals.
     * @param individualIri the IRI of the individual
     * @param classIri the IRI of the class
     * @return true if the assertion was added, false if already present in Imports.INCLUDED.
     */
    public boolean assertType(final IRI individualIri, final IRI classIri) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(classIri, "classIri must not be null");

        if (!ctx.getOntology().containsIndividualInSignature(individualIri, Imports.INCLUDED)) {
            throw new IllegalStateException("Individual does not exist (Imports.INCLUDED): " + individualIri);
        }
        if (!ctx.getOntology().containsClassInSignature(classIri, Imports.INCLUDED)) {
            throw new IllegalArgumentException("Class IRI not in signature (Imports.INCLUDED): " + classIri);
        }

        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));
        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));
        final OWLClassAssertionAxiom ax = Objects.requireNonNull(ctx.getFactory().getOWLClassAssertionAxiom(cls, ind));

        if (containsIncluded(ctx.getOntology(), ax)) {
            return false;
        }
        addLocal(ax);
        return true;
    }

    /**
     * Retracts the explicit local ClassAssertion(class, individual) from the root ontology.
     * Returns false if the axiom is not present locally.
     */
    public boolean retractType(final IRI individualIri, final IRI classIri) {
        Objects.requireNonNull(individualIri, "individualIri must not be null");
        Objects.requireNonNull(classIri, "classIri must not be null");

        final OWLNamedIndividual ind = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(individualIri));
        final OWLClass cls = Objects.requireNonNull(ctx.getFactory().getOWLClass(classIri));
        final OWLClassAssertionAxiom ax = Objects.requireNonNull(ctx.getFactory().getOWLClassAssertionAxiom(cls, ind));

        if (!containsLocal(ctx.getOntology(), ax)) {
            return false;
        }
        removeLocal(ax);
        return true;
    }

    /**
     * Asserts ObjectPropertyAssertion(prop, subject, object). Does NOT create individuals.
     * @param subjectIri the IRI of the subject individual
     * @param propertyIri the IRI of the object property
     * @param objectIri the IRI of the object individual
     * @return true if the assertion was added, false if already present in Imports.INCLUDED.
     */
    public boolean assertObjectProperty(final IRI subjectIri, final IRI propertyIri, final IRI objectIri) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(propertyIri, "propertyIri must not be null");
        Objects.requireNonNull(objectIri, "objectIri must not be null");

        if (!ctx.getOntology().containsIndividualInSignature(subjectIri, Imports.INCLUDED)) {
            throw new IllegalStateException("Subject individual does not exist (Imports.INCLUDED): " + subjectIri);
        }
        if (!ctx.getOntology().containsIndividualInSignature(objectIri, Imports.INCLUDED)) {
            throw new IllegalStateException("Object individual does not exist (Imports.INCLUDED): " + objectIri);
        }
        if (!ctx.getOntology().containsObjectPropertyInSignature(propertyIri, Imports.INCLUDED)) {
            throw new IllegalArgumentException("Object property not in signature (Imports.INCLUDED): " + propertyIri);
        }

        final OWLNamedIndividual subj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(subjectIri));
        final OWLObjectProperty prop = Objects.requireNonNull(ctx.getFactory().getOWLObjectProperty(propertyIri));
        final OWLNamedIndividual obj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(objectIri));
        final OWLObjectPropertyAssertionAxiom ax = Objects.requireNonNull(ctx.getFactory().getOWLObjectPropertyAssertionAxiom(prop, subj, obj));

        if (containsIncluded(ctx.getOntology(), ax)) {
            return false;
        }
        addLocal(ax);
        return true;
    }

    /**
     * Retracts the explicit local ObjectPropertyAssertion(prop, subject, object) from the root ontology.
     * @param subjectIri the IRI of the subject individual
     * @param propertyIri the IRI of the object property
     * @param objectIri the IRI of the object individual
     * @return true if the assertion was retracted, false if not present locally.
     */
    public boolean retractObjectProperty(final IRI subjectIri, final IRI propertyIri, final IRI objectIri) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(propertyIri, "propertyIri must not be null");
        Objects.requireNonNull(objectIri, "objectIri must not be null");

        final OWLNamedIndividual subj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(subjectIri));
        final OWLObjectProperty prop = Objects.requireNonNull(ctx.getFactory().getOWLObjectProperty(propertyIri));
        final OWLNamedIndividual obj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(objectIri));
        final OWLObjectPropertyAssertionAxiom ax = Objects.requireNonNull(ctx.getFactory().getOWLObjectPropertyAssertionAxiom(prop, subj, obj));

        if (!containsLocal(ctx.getOntology(), ax)) {
            return false;
        }
        removeLocal(ax);
        return true;
    }

    /**
     * Asserts DataPropertyAssertion(prop, subject, value). Does NOT create individuals.
     * @param subjectIri the IRI of the subject individual
     * @param dataPropertyIri the IRI of the data property
     * @param value the literal value
     * @return true if the assertion was added, false if it was already present
     */
    public boolean assertDataProperty(final IRI subjectIri, final IRI dataPropertyIri, final OWLLiteral value) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(dataPropertyIri, "dataPropertyIri must not be null");
        Objects.requireNonNull(value, "value must not be null");

        if (!ctx.getOntology().containsIndividualInSignature(subjectIri, Imports.INCLUDED)) {
            throw new IllegalStateException("Subject individual does not exist (Imports.INCLUDED): " + subjectIri);
        }
        if (!ctx.getOntology().containsDataPropertyInSignature(dataPropertyIri, Imports.INCLUDED)) {
            throw new IllegalArgumentException("Data property not in signature (Imports.INCLUDED): " + dataPropertyIri);
        }

        final OWLNamedIndividual subj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(subjectIri));
        final OWLDataProperty prop = Objects.requireNonNull(ctx.getFactory().getOWLDataProperty(dataPropertyIri));
        final OWLDataPropertyAssertionAxiom ax = Objects.requireNonNull(ctx.getFactory().getOWLDataPropertyAssertionAxiom(prop, subj, value));

        if (containsIncluded(ctx.getOntology(), ax)) {
            return false;
        }
        addLocal(ax);
        return true;
    }

    /**
     * Retracts the explicit local DataPropertyAssertion(prop, subject, value) from the root ontology.
     * @param subjectIri the IRI of the subject individual
     * @param dataPropertyIri the IRI of the data property
     * @param value the literal value
     * @return true if the assertion was retracted, false if not present locally.
     */
    public boolean retractDataProperty(final IRI subjectIri, final IRI dataPropertyIri, final OWLLiteral value) {
        Objects.requireNonNull(subjectIri, "subjectIri must not be null");
        Objects.requireNonNull(dataPropertyIri, "dataPropertyIri must not be null");
        Objects.requireNonNull(value, "value must not be null");

        final OWLNamedIndividual subj = Objects.requireNonNull(ctx.getFactory().getOWLNamedIndividual(subjectIri));
        final OWLDataProperty prop = Objects.requireNonNull(ctx.getFactory().getOWLDataProperty(dataPropertyIri));
        final OWLDataPropertyAssertionAxiom ax = Objects.requireNonNull(ctx.getFactory().getOWLDataPropertyAssertionAxiom(prop, subj, value));

        if (!containsLocal(ctx.getOntology(), ax)) {
            return false;
        }
        removeLocal(ax);
        return true;
    }
}
