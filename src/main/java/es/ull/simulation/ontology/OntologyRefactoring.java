package es.ull.simulation.ontology;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLEntityRemover;

public class OntologyRefactoring {
    /** 
     * The context for this ontology refactoring 
     **/
    private final OntologyContext ctx;
    private final OntologyResolution resolution;
    private final IndividualQuery individualQuery;

    public OntologyRefactoring(final OntologyContext ctx, final OntologyResolution resolution, final IndividualQuery query) {
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
        this.resolution = Objects.requireNonNull(resolution, "resolution must not be null");
        this.individualQuery = Objects.requireNonNull(query, "query must not be null");
    }

	/**
	 * Creates a subclass relationship between the specified class and its superclass
	 * @param classIRI The IRI of the class to be created
	 * @param superclassIRI The IRI of the superclass
	 */
	public void createClassSubClassOf(IRI classIRI, IRI superclassIRI) {
		final OWLClassExpression owlSuperClass = Objects.requireNonNull(resolution.asOWLClass(superclassIRI), "Superclass must not be null");
		final OWLClass owlClass = Objects.requireNonNull(resolution.asOWLClass(classIRI), "Class must not be null");
		final OWLSubClassOfAxiom ax = Objects.requireNonNull(ctx.getFactory().getOWLSubClassOfAxiom(owlClass, owlSuperClass), "Axiom must not be null");
		final AddAxiom addAx = new AddAxiom(ctx.getOntology(), ax);
	    ctx.getManager().applyChange(addAx);
	}
	
	/**
	 * Collects all individuals of the specified class and changes them to subclasses of the specified class. 
	 * Adds the prefix to the individual names to create the new class IRIs. 
	 * @param classIRI The IRI of the class whose individuals will be changed to subclasses
	 * @param prefix The prefix to be added to the individual names to create the new class IRIs
	 */
	public void changeInstanceToSubclass(IRI classIRI, String prefix) {
        Objects.requireNonNull(classIRI, "classIRI must not be null");
        Objects.requireNonNull(prefix, "prefix must not be null");
        final Set<IRI> individualsIRI = individualQuery.getIndividualsOfClass(classIRI, Imports.EXCLUDED);
		removeIndividualsOfClass(classIRI);
		for (IRI ind : individualsIRI) {
			createClassSubClassOf(resolution.toIRI(prefix + ind.getShortForm()), classIRI);
		}
	}
	
	/**
	 * Removes all individuals of the specified class from the ontology
	 * @param classIRI The IRI of a class in the ontology
	 */
	public void removeIndividualsOfClass(IRI classIRI) {
        final Set<IRI> individualsIRI = individualQuery.getIndividualsOfClass(classIRI, Imports.EXCLUDED);
		final OWLEntityRemover remover = new OWLEntityRemover(Objects.requireNonNull(Collections.singleton(ctx.getOntology())));
		for (IRI indIRI : individualsIRI) {
			resolution.asOWLNamedIndividual(indIRI).accept(remover);
		}
		ctx.getManager().applyChanges(Objects.requireNonNull(remover.getChanges(), "Remover changes must not be null"));
		remover.reset();
	}

    public void replaceMin0ByOnlyRecursive() {
        final OWLDataFactory factory = ctx.getFactory();
        Set<OWLAxiom> axiomsToRemove = new HashSet<>();
        Set<OWLAxiom> axiomsToAdd = new HashSet<>();

        for (OWLSubClassOfAxiom ax : ctx.getOntology().getAxioms(Objects.requireNonNull(AxiomType.SUBCLASS_OF, "AxiomType must not be null"))) {
            OWLClassExpression subCls = Objects.requireNonNull(ax.getSubClass(), "SubClass must not be null");
            OWLClassExpression superCls = Objects.requireNonNull(ax.getSuperClass(), "SuperClass must not be null");

            OWLClassExpression newSuperCls = replaceMin0InExpression(superCls, factory);
            if (!newSuperCls.equals(superCls)) {
                axiomsToRemove.add(ax);
                axiomsToAdd.add(factory.getOWLSubClassOfAxiom(subCls, newSuperCls));
            }
        }
        ctx.getManager().removeAxioms(ctx.getOntology(), axiomsToRemove);
        ctx.getManager().addAxioms(ctx.getOntology(), axiomsToAdd);
    }

    private OWLClassExpression replaceMin0InExpression(OWLClassExpression expr, OWLDataFactory factory) {
        // Si es una restricción de min cardinality
        if (expr instanceof OWLObjectMinCardinality) {
            OWLObjectMinCardinality minCard = (OWLObjectMinCardinality) expr;
            if (minCard.getCardinality() == 0) {
                return factory.getOWLObjectAllValuesFrom(
                    Objects.requireNonNull(minCard.getProperty().asOWLObjectProperty(), "Object property must not be null"), 
                    Objects.requireNonNull(minCard.getFiller(), "Filler must not be null"));
            }
        }
        else if (expr instanceof OWLDataMinCardinality) {
            OWLDataMinCardinality minCard = (OWLDataMinCardinality) expr;
            if (minCard.getCardinality() == 0) {
                return factory.getOWLDataAllValuesFrom(
                    Objects.requireNonNull(minCard.getProperty().asOWLDataProperty(), "Data property must not be null"), 
                    Objects.requireNonNull(minCard.getFiller(), "Filler must not be null"));
            }
        }

        // Si es una intersección, se procesa recursivamente cada operando
        if (expr instanceof OWLObjectIntersectionOf) {
            List<OWLClassExpression> newOperands = ((OWLObjectIntersectionOf) expr).getOperands().stream()
                    .map(e -> replaceMin0InExpression(e, factory))
                    .collect(Collectors.toList());
            return factory.getOWLObjectIntersectionOf(Objects.requireNonNull(newOperands, "Operands must not be null"));
        }

        // Si es unión, complemento, etc., se puede extender de forma similar
        // Por ahora devolvemos el mismo si no se cumple nada
        return expr;
    }


}
