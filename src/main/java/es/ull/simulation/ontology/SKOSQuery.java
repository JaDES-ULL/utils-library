package es.ull.simulation.ontology;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

public final class SKOSQuery {
    private final OntologyContext ctx;
    private final IndividualQuery individualQuery;
    
    public static final String SKOS_IRI_PREFIX = "http://www.w3.org/2004/02/skos/core#";

    // SKOS IRIs
    private final IRI BROADER_IRI = Objects.requireNonNull(IRI.create(SKOS_IRI_PREFIX + "broader"));
    private final IRI IN_SCHEME_IRI = Objects.requireNonNull(IRI.create(SKOS_IRI_PREFIX + "inScheme"));
    private final IRI CONCEPT_IRI = Objects.requireNonNull(IRI.create(SKOS_IRI_PREFIX + "Concept"));
    private final IRI SCHEME_IRI = Objects.requireNonNull(IRI.create(SKOS_IRI_PREFIX + "ConceptScheme"));
    private final IRI PREF_LABEL = Objects.requireNonNull(IRI.create(SKOS_IRI_PREFIX + "prefLabel"));

    public SKOSQuery(final OntologyContext ctx, final IndividualQuery individualQuery) {
        this.ctx = Objects.requireNonNull(ctx);
        this.individualQuery = Objects.requireNonNull(individualQuery);
    }

    /**
     * Returns the prefLabel of a concept in a specific language, or null if it is not defined. If the concept does not have a prefLabel in the specified language, 
     * it falls back to the generic label of the individual (if any).
     * @return The prefLabel of the concept in the specified language, or null if it is not defined.     
     */
    public String getSKOSPrefLabel(IRI conceptIri, String lang) {
        Objects.requireNonNull(conceptIri, "conceptIri cannot be null");
        // Define the property explicitly
        final OWLAnnotationProperty prefLabelAnnotation = ctx.getFactory()
            .getOWLAnnotationProperty(PREF_LABEL);

        return org.semanticweb.owlapi.search.EntitySearcher.getAnnotations(
                ctx.getFactory().getOWLNamedIndividual(conceptIri), 
                ctx.getOntology(), 
                prefLabelAnnotation)
            .map(OWLAnnotation::getValue)
            .filter(v -> v.asLiteral().isPresent())
            .map(v -> v.asLiteral().get())
            .filter(lit -> lit.hasLang(lang))
            .map(OWLLiteral::getLiteral)
            .findFirst()
            .orElseGet(() -> individualQuery.getLabelForIRI(conceptIri, lang).orElse(null)); 
    }

    /**
     * Gets the broader concepts of a given concept.
     * @param conceptIri The IRI of the concept for which to get the broader concepts.
     * @param imports The imports to consider when querying the ontology.
     * @return A set of IRIs of the broader concepts of the given concept.
     */
    public Set<IRI> getBroader(IRI conceptIri, Imports imports) {
        return individualQuery.getObjectPropertyValues(conceptIri, BROADER_IRI, imports);
    }

    /**
     * Gets the concept schemes defined in the ontology.
     * @param imports The imports to consider when querying the ontology.
     * @return A set of IRIs of the concept schemes defined in the ontology.
     */
    public Set<IRI> getConceptSchemes(Imports imports) {
        return individualQuery.getIndividualsInSignature(imports).stream()
                .filter(iri -> individualQuery.isInstanceOfAsserted(iri, SCHEME_IRI, false, imports))
                .collect(Collectors.toSet());
    }

    /**
     * Gets the concepts that belong to a given concept scheme.
     * @param schemeIri The IRI of the concept scheme.
     * @param imports The imports to consider when querying the ontology.
     * @return A set of IRIs of the concepts that belong to the given concept scheme.
     */
    public Set<IRI> getConceptsInScheme(IRI schemeIri, Imports imports) {
        return individualQuery.getIndividualsInSignature(imports).stream()
                .filter(iri -> individualQuery.getObjectPropertyValues(iri, IN_SCHEME_IRI, imports).contains(schemeIri))
                .collect(Collectors.toSet());
    }

    /**
     * Determines if a concept belongs to a category by checking if it is the same concept or if it has a broader path to the target category.
     * @param candidateIri The IRI of the concept to check.
     * @param targetIri The IRI of the target category.
     * @param imports The imports to consider when querying the ontology.
     * @return true if the candidate concept is the same as the target category or if it has a broader path to the target category, false otherwise.
     */
    public boolean belongsToCategory(IRI candidateIri, IRI targetIri, Imports imports) {
        Objects.requireNonNull(candidateIri, "candidateIri no puede ser nulo");
        Objects.requireNonNull(targetIri, "targetIri no puede ser nulo");

        // Base case: if the candidate concept is the same as the target category, it belongs to the category
        if (candidateIri.equals(targetIri)) {
            return true;
        }

        // Search the broader path using a breadth-first search (BFS) approach
        // To avoid infinite loops in case of malformed ontologies, we keep track of visited IRIs  
        Set<IRI> visited = new HashSet<>();
        Deque<IRI> queue = new ArrayDeque<>();        
        queue.add(candidateIri);
        visited.add(candidateIri);
        while (!queue.isEmpty()) {
            IRI current = queue.poll();
            Set<IRI> broaders = individualQuery.getObjectPropertyValues(current, BROADER_IRI, imports);            
            for (IRI broader : broaders) {
                if (broader.equals(targetIri)) {
                    return true; 
                }
                if (visited.add(broader)) {
                    queue.add(broader);
                }
            }
        }

        return false;
    }    
}