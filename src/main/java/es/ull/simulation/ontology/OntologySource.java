package es.ull.simulation.ontology;

public sealed interface OntologySource permits OntologySource.FromPath, OntologySource.FromIRI, OntologySource.FromStream {

  record FromPath(java.nio.file.Path path) implements OntologySource {}
  record FromIRI(org.semanticweb.owlapi.model.IRI iri) implements OntologySource {}
  record FromStream(java.io.InputStream inputStream) implements OntologySource {}
}
