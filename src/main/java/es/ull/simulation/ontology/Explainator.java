package es.ull.simulation.ontology;

import java.util.Objects;
import java.util.Set;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

/**
 * This is an experimental class for explaining reasoning results (inconsistencies, inferred types, etc.). It is not working yet.
 */
public class Explainator {

    /**
     * Explain the ontology by providing insights into its structure and inconsistencies.
     * @author Adapted from HermiT's example
     */
    public static void explain(OWLOntology ontology) {
        Objects.requireNonNull(ontology, "ontology must not be null");
        
        ReasonerFactory factory = new ReasonerFactory();
        // We don't want HermiT to thrown an exception for inconsistent ontologies because then we 
        // can't explain the inconsistency. This can be controlled via a configuration setting.  
        Configuration configuration = new Configuration();
        configuration.throwInconsistentOntologyException = false;
        OWLReasoner reasoner = Objects.requireNonNull(factory.createReasoner(ontology, configuration));
        // Ok, here we go. Let's see why the ontology is inconsistent. 
        System.out.println("Computing explanations for the inconsistency...");
        factory = new ReasonerFactory() {
            protected OWLReasoner createHermiTOWLReasoner(org.semanticweb.HermiT.Configuration configuration,OWLOntology ontology) {
                // don't throw an exception since otherwise we cannot compte explanations 
                configuration.throwInconsistentOntologyException=false;
                return new Reasoner(configuration,ontology);
            }  
        };
        // TODO: Implement different processing when there is a global inconsistency. Currently, a simple issue (wrong datatype for a single individual) produces all classes to print an explanation.
        // I tried first with the global inconsistency (getExplanations(dataFactory.getOWLNothing()) but the resulting set was empty.
        // I was also trying with the GlassBoxExplanation, but throws a null pointer exception
/*        if (!reasoner.isConsistent()) {
            ExplanationGeneratorFactory<OWLAxiom> genFac = ExplanationManager.createExplanationGeneratorFactory(factory, () -> ontology.getOWLOntologyManager());
            ExplanationGenerator<OWLAxiom> gen = genFac.createExplanationGenerator(ontology);
            OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
            Set<Explanation<OWLAxiom>> explanations = gen.getExplanations(dataFactory.getOWLSubClassOfAxiom(dataFactory.getOWLThing(), dataFactory.getOWLNothing()), 1);
            for (Explanation<OWLAxiom> explanation : explanations) {
                System.out.println("------------------");
                System.out.println("Axioms causing the unsatisfiability: ");
                for (OWLAxiom causingAxiom : explanation.getAxioms()) {
                    System.out.println(" - " + causingAxiom);
                }
                System.out.println("------------------");
            }
        } 
        else {*/
            BlackBoxExplanation exp = new BlackBoxExplanation(ontology, factory, reasoner);
            HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);
            // Now we can get explanations for the inconsistency
            //Set<Set<OWLAxiom>> explanations = multExplanator.getExplanations(dataFactory.getOWLNothing());
            for (OWLClass cls : reasoner.getUnsatisfiableClasses()) {
                if (!cls.isOWLNothing()) {
                    Set<Set<OWLAxiom>> explanations = multExplanator.getExplanations(cls, 1);
                    for (Set<OWLAxiom> explanation : explanations) {
                        System.out.println("Explanation for " + cls + ":");
                        explanation.forEach(ax -> System.out.println(" - " + ax));
                    }
                }
            }
        //}
    }

	/**
	 * Returns the set of inferred axioms in the ontology using a reasoner (HermiT).
	 * @return A set of inferred OWLAxioms
	 */
    public static Set<OWLAxiom> getInferredAxioms(OWLOntology ontology) {
        Objects.requireNonNull(ontology, "ontology must not be null");
        OWLReasonerFactory reasonerFactory = new ReasonerFactory(); // HermiT
        OWLReasoner reasoner = Objects.requireNonNull(reasonerFactory.createReasoner(ontology));

        // Inference generator
        InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner);

        // Temporal ontology to store inferred axioms
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = Objects.requireNonNull(manager.getOWLDataFactory());
        OWLOntology inferredOnt;
        try {
            inferredOnt = Objects.requireNonNull(manager.createOntology());
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        try {
            iog.fillOntology(dataFactory, inferredOnt);
        } catch (Exception e) {
            e.printStackTrace();
        }

        reasoner.dispose();

        // Returns all inferred axioms
        return inferredOnt.getAxioms();
    }
}
