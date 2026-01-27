package es.ull.simulation.ontology;

public sealed interface OntologyDestination permits OntologyDestination.ToPath, OntologyDestination.ToIRI, OntologyDestination.ToStream {

    record ToPath(java.nio.file.Path path) implements OntologyDestination {}
    record ToIRI(org.semanticweb.owlapi.model.IRI iri) implements OntologyDestination {}
    record ToStream(java.io.OutputStream outputStream) implements OntologyDestination {}
}
