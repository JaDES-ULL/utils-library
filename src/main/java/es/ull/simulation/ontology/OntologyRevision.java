package es.ull.simulation.ontology;

public record OntologyRevision(
    String id,
    String version,
    java.time.Instant loadedAt
) {
  public static OntologyRevision of(String id, String version) {
    return new OntologyRevision(id, version, java.time.Instant.now());
  }
}
