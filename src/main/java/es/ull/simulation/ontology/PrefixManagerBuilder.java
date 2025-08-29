package es.ull.simulation.ontology;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PrefixManagerBuilder {

    /**
     * Construye un DefaultPrefixManager para la ontología raíz y todas sus importadas.
     * Respeta los prefijos declarados si existen, y detecta namespace automáticamente si no.
     */
    public static DefaultPrefixManager buildPrefixManager(OWLOntologyManager manager, OWLOntology root) {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        Map<String, String> usedPrefixes = new LinkedHashMap<>();
        AtomicInteger counter = new AtomicInteger(1);

        for (OWLOntology ont : root.importsClosure().toList()) {

            // Obtener el formato moderno
            OWLDocumentFormat format = manager.getOntologyFormat(ont);

            // 1. Prefijos declarados
            if (format instanceof PrefixDocumentFormat) {
                PrefixDocumentFormat prefixFormat = (PrefixDocumentFormat) format;
                prefixFormat.getPrefixName2PrefixMap().forEach((prefix, iri) -> {
                    String unique = uniquePrefix(prefix.replace(":", ""), usedPrefixes) + ":";
                    pm.setPrefix(unique, iri);
                    usedPrefixes.put(unique, iri);
                });
            }

            // 2. Si no hay prefijos, detectar namespace a partir de entidades
            if (!(format instanceof PrefixDocumentFormat) || ((PrefixDocumentFormat) format).getPrefixName2PrefixMap().isEmpty()) {
                String namespace = detectNamespaceFromEntities(ont)
                        .orElse(ont.getOntologyID().getOntologyIRI().map(i -> i + "#").orElse("urn:default#"));

                String candidate = "ns" + counter.getAndIncrement();
                String unique = uniquePrefix(candidate, usedPrefixes) + ":";
                pm.setPrefix(unique, namespace);
                usedPrefixes.put(unique, namespace);
            }
        }

        return pm;
    }

    // Detecta namespace de la ontología a partir de cualquier entidad presente
    private static Optional<String> detectNamespaceFromEntities(OWLOntology ont) {
        return ont.classesInSignature()
                .map(c -> c.getIRI().toString())
                .findFirst()
                .map(PrefixManagerBuilder::extractNamespace)
                .or(() -> ont.objectPropertiesInSignature()
                        .map(p -> p.getIRI().toString())
                        .findFirst()
                        .map(PrefixManagerBuilder::extractNamespace))
                .or(() -> ont.dataPropertiesInSignature()
                        .map(p -> p.getIRI().toString())
                        .findFirst()
                        .map(PrefixManagerBuilder::extractNamespace));
    }

    // Extrae el namespace de un IRI
    private static String extractNamespace(String fullIRI) {
        int hash = fullIRI.lastIndexOf('#');
        int slash = fullIRI.lastIndexOf('/');
        int cut = Math.max(hash, slash);
        return cut > 0 ? fullIRI.substring(0, cut + 1) : fullIRI;
    }

    // Garantiza prefijos únicos
    private static String uniquePrefix(String base, Map<String, String> used) {
        String candidate = base;
        int i = 1;
        while (used.containsKey(candidate + ":")) {
            candidate = base + i++;
        }
        return candidate;
    }
}
