package es.ull.simulation.ontology;

import java.util.Objects;
import java.util.Optional;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

/**
 * A placeholder class for ontology resolution functionality, i.e., solve IRI references within ontologies, provide ontology elements, etc.
 */
public class OntologyResolution {
    /** The context for this ontology resolution */
    private final OntologyContext ctx;

    public OntologyResolution(final OntologyContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
    }

	/**
	 * Converts a textual reference to an IRI. Accepts: (1) Absolute IRI: "https://...#X"; (2) - Prefixed name: "osdi:X", ":X"; 
	 * and (3) Short form: "X" (the default prefix of the PrefixManager is assumed).
	 * @param ref The textual reference
	 * @return The corresponding IRI
	 */
	public IRI toIRI(String ref) {
		Objects.requireNonNull(ref, "ref should not be null");

		// Simple heuristic: if it looks like an absolute IRI, use it as is
		if (ref.startsWith("http://") || ref.startsWith("https://")) {
			return IRI.create(ref);
		}

		// If it has ':', we let the PrefixManager resolve the prefix
		if (ref.contains(":")) {
			return ctx.getPrefixManager().getIRI(ref);
		}

		// If it is short form, we attach it to the default prefix
		String defaultPrefix = ctx.getPrefixManager().getDefaultPrefix();
		if (defaultPrefix == null) {
			throw new IllegalStateException("Default prefix not configured in PrefixManager; cannot resolve short form: " + ref);
		}
		return IRI.create(defaultPrefix + ref);
	}

	/**
	 * Converts an IRI to its short form using the PrefixManager
	 * @param iri The IRI to convert
	 * @return The short form of the IRI
	 */
	public String toShortForm(IRI iri) {
    	return Objects.requireNonNull(iri, "iri should not be null").getShortForm();
	}

    /**
     * Best-effort prefixed name, falling back to short form.
     * Useful for debugging/logging.
     * @param iri The IRI to convert
     * @return The prefixed name or short form of the IRI
     */
    public String toPrefixedName(final IRI iri) {
        Objects.requireNonNull(iri, "iri must not be null");

        // DefaultPrefixManager can render short forms, but PrefixManager interface does not guarantee rendering.
        // We do a simple best-effort: if namespace matches a known prefix, return prefix:local
        // else fall back to short form.
        final PrefixManager pm = ctx.getPrefixManager();
        if (pm instanceof DefaultPrefixManager dpm) {
            String shortForm = dpm.getShortForm(iri);
            return shortForm != null ? shortForm : iri.getShortForm();
        }
        return iri.getShortForm();
    }    

	/**
	 * Returns the OWLClass object for the specified class IRI, independently of whether it exists or not
	 * @param classIri The IRI of the class
	 * @return The OWLClass object for the specified class IRI
	 */
	public OWLClass asOWLClass(IRI classIri) {
		return ctx.getFactory().getOWLClass(Objects.requireNonNull(classIri));
	}
	
	/**
	 * Returns the OWLObjectProperty object for the specified object property IRI, independently of whether it exists or not
	 * @param objectPropIRI The IRI of the object property
	 * @return The OWLObjectProperty object for the specified object property IRI
	 */
	public OWLObjectProperty asOWLObjectProperty(IRI objectPropIRI) {
		return ctx.getFactory().getOWLObjectProperty(Objects.requireNonNull(objectPropIRI));
	}

	/**
	 * Returns the OWLDataProperty object for the specified data property IRI, independently of whether it exists or not
	 * @param dataPropIRI The IRI of the data property
	 * @return The OWLDataProperty object for the specified data property IRI
	 */
	public OWLDataProperty asOWLDataProperty(IRI dataPropIRI) {
		return ctx.getFactory().getOWLDataProperty(Objects.requireNonNull(dataPropIRI));
	}
	
	/**
	 * Returns the OWLIndividual object for the specified individual IRI, independently of whether it exists or not
	 * @param individualIRI The IRI of the individual
	 * @return The OWLIndividual object for the specified individual IRI
	 */
	public OWLNamedIndividual asOWLNamedIndividual(IRI individualIRI) {
		return ctx.getFactory().getOWLNamedIndividual(Objects.requireNonNull(individualIRI));
	}

    /**
     * Returns the OWLAnnotationProperty object for the specified annotation property IRI, independently of whether it exists or not
     * @param propIri The IRI of the annotation property
     * @return The OWLAnnotationProperty object for the specified annotation property IRI
     */
    public OWLAnnotationProperty asOWLAnnotationProperty(final IRI propIri) {
        return ctx.getFactory().getOWLAnnotationProperty(Objects.requireNonNull(propIri, "propIri must not be null"));
    }    

    /**
     * Checks whether a class with the specified IRI exists in the ontology
     * @param classIri The IRI of the class
     * @param imports The imports setting
     * @return True if the class exists, false otherwise
     */
    public boolean existsOWLClass(final IRI classIri, final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsClassInSignature(Objects.requireNonNull(classIri), imports);
    }

    /**
     * Checks whether an object property with the specified IRI exists in the ontology
     * @param propIri The IRI of the object property
     * @param imports The imports setting
     * @return True if the object property exists, false otherwise
     */
    public boolean existsOWLObjectProperty(final IRI propIri, final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsObjectPropertyInSignature(Objects.requireNonNull(propIri), imports);
    }

    /**
     * Checks whether a data property with the specified IRI exists in the ontology
     * @param propIri The IRI of the data property
     * @param imports The imports setting
     * @return True if the data property exists, false otherwise
     */
    public boolean existsOWLDataProperty(final IRI propIri, final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsDataPropertyInSignature(Objects.requireNonNull(propIri), imports);
    }

    /**
     * Checks whether an annotation property with the specified IRI exists in the ontology
     * @param propIri The IRI of the annotation property
     * @param imports The imports setting
     * @return True if the annotation property exists, false otherwise
     */
    public boolean existsOWLAnnotationProperty(final IRI propIri, final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsAnnotationPropertyInSignature(Objects.requireNonNull(propIri), imports);
    }

    /**
     * Checks whether an individual with the specified IRI exists in the ontology
     * @param individualIri The IRI of the individual
     * @param imports The imports setting
     * @return True if the individual exists, false otherwise
     */
    public boolean existsOWLNamedIndividual(final IRI individualIri, final Imports imports) {
        Objects.requireNonNull(imports, "imports must not be null");
        return ctx.getOntology().containsIndividualInSignature(Objects.requireNonNull(individualIri), imports);
    }

    /**
     * Finds an OWLClass by its IRI if it exists in the ontology
     * @param classIri The IRI of the class
     * @param imports The imports setting
     * @return An Optional containing the OWLClass if it exists, or empty otherwise
     */
    public Optional<OWLClass> findOWLClass(final IRI classIri, final Imports imports) {
        return existsOWLClass(classIri, imports) ? Optional.of(asOWLClass(classIri)) : Optional.empty();
    }

    /**
     * Finds an OWLObjectProperty by its IRI if it exists in the ontology
     * @param propIri The IRI of the object property
     * @param imports The imports setting
     * @return An Optional containing the OWLObjectProperty if it exists, or empty otherwise
     */
    public Optional<OWLObjectProperty> findOWLObjectProperty(final IRI propIri, final Imports imports) {
        return existsOWLObjectProperty(propIri, imports) ? Optional.of(asOWLObjectProperty(propIri)) : Optional.empty();
    }

    /**
     * Finds an OWLDataProperty by its IRI if it exists in the ontology
     * @param propIri The IRI of the data property
     * @param imports The imports setting
     * @return An Optional containing the OWLDataProperty if it exists, or empty otherwise
     */
    public Optional<OWLDataProperty> findOWLDataProperty(final IRI propIri, final Imports imports) {
        return existsOWLDataProperty(propIri, imports) ? Optional.of(asOWLDataProperty(propIri)) : Optional.empty();
    }

    /**
     * Finds an OWLAnnotationProperty by its IRI if it exists in the ontology
     * @param propIri The IRI of the annotation property
     * @param imports The imports setting
     * @return An Optional containing the OWLAnnotationProperty if it exists, or empty otherwise
     */
    public Optional<OWLAnnotationProperty> findOWLAnnotationProperty(final IRI propIri, final Imports imports) {
        return existsOWLAnnotationProperty(propIri, imports) ? Optional.of(asOWLAnnotationProperty(propIri)) : Optional.empty();
    }

    /**
     * Finds an OWLNamedIndividual by its IRI if it exists in the ontology
     * @param individualIri The IRI of the individual
     * @param imports The imports setting
     * @return An Optional containing the OWLNamedIndividual if it exists, or empty otherwise
     */
    public Optional<OWLNamedIndividual> findOWLNamedIndividual(final IRI individualIri, final Imports imports) {
        return existsOWLNamedIndividual(individualIri, imports) ? Optional.of(asOWLNamedIndividual(individualIri)) : Optional.empty();
    }    
}
