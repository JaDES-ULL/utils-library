package es.ull.simulation.ontology;

public record LoadedOntology(
    org.semanticweb.owlapi.model.OWLOntology ontology,
    org.semanticweb.owlapi.model.OWLOntologyManager manager,
    org.semanticweb.owlapi.model.OWLDataFactory dataFactory
) {}