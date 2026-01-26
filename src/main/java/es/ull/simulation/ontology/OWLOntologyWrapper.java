package es.ull.simulation.ontology;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * A wrapper for an ontology in OWL. It works more like a method aggregator from different helper classes. Hence, it shortens the use of OWLApi. 
 * @author Iván Castilla Rodríguez
 */
public class OWLOntologyWrapper {
	/**
	 * Modes for checking or quering instanceOf relationships
	 */
	public enum InstanceCheckMode {
		ASSERTED,
		INFERRED_DIRECT,
		INFERRED_ALL
	}	

	/**
	 * The context for this ontology wrapper
	 */
	private final OntologyContext ctx;
	/**
	 * The ontology IO helper
	 */
	private final OntologyIO ontologyIO;
	/**
	 * The ontology resolution helper
	 */
	private final OntologyResolution ontologyResolution;
	/**
	 * The individual authoring helper
	 */
	private final IndividualAuthoring individualAuthoring;
	/**
	 * The individual query helper 
	 */
	private final IndividualQuery individualQuery;
	/**
	 * The reasoned query helper
	 */
	private final ReasonedQuery reasonedQuery;
	/**
	 * The ontology refactoring helper
	 */
	private final OntologyRefactoring ontologyRefactoring;
	/**
	 * The debug printer helper 
	 */
	private final OntologyDebugPrinter debugPrinter;

	/**
	 * Creates a wrapper for the specified ontology that must be checked against OWL 2 DL profile
	 * @param ontology The OWL ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntologyWrapper(LoadedOntology loaded) throws OWLOntologyCreationException {
		this(loaded, false);
	}

	/**
	 * Creates a wrapper for the specified ontology and optionally skips the check against OWL 2 DL profile
	 * @param ontology The OWL ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntologyWrapper(LoadedOntology loaded, boolean unchecked) throws OWLOntologyCreationException {
		final OWLOntology ont = Objects.requireNonNull(loaded.ontology(), "OWL Ontology should never be null");
		final OWLOntologyManager manager = Objects.requireNonNull(loaded.manager(), "OWLOntologyManager should never be null");
		final PrefixManager pm = buildPrefixManagerFromOntologyFormat(manager, ont);
		final OWLReasonerFactory rf = new StructuralReasonerFactory();

		this.ctx = new OntologyContext(loaded, pm, rf);
		this.ontologyIO = new OntologyIO(ctx);
		this.ontologyResolution = new OntologyResolution(ctx);
		if (!unchecked)
			checkProfile(ont);
		this.individualAuthoring = new IndividualAuthoring(ctx);
		this.individualQuery = new IndividualQuery(ctx);
		this.reasonedQuery = new ReasonedQuery(ctx);
		this.ontologyRefactoring = new OntologyRefactoring(ctx, ontologyResolution, individualQuery);
		this.debugPrinter = new OntologyDebugPrinter(ctx, ontologyResolution, individualQuery);
	}

	/**
	 * Builds a PrefixManager from the ontology format, if possible
	 * @param manager The ontology manager
	 * @param ontology The ontology
	 * @return The PrefixManager
	 */
	private static PrefixManager buildPrefixManagerFromOntologyFormat(OWLOntologyManager manager, OWLOntology ontology) {
		Objects.requireNonNull(manager, "OWLOntologyManager should never be null");
		Objects.requireNonNull(ontology, "OWL Ontology should never be null");
		final PrefixManager pm = new DefaultPrefixManager();

		final OWLDocumentFormat format = manager.getOntologyFormat(ontology);
		if (format != null && format.isPrefixOWLDocumentFormat()) {
			final PrefixDocumentFormat prefixFormat = format.asPrefixOWLDocumentFormat();
			// Copies the prefixes to the local prefix manager
			prefixFormat.getPrefixName2PrefixMap()
					.forEach((name, prefIRI) -> pm.setPrefix(
							Objects.requireNonNull(name, "Prefix name should never be null"),
							Objects.requireNonNull(prefIRI, "Prefix IRI should never be null")));
		}
		return pm;
	}	

	/**
	 * Returns the ontology
	 * @return The ontology
	 */
	public OWLOntology getOntology() {
		return ctx.getOntology();
	}

	/**
	 * Returns the reasoner used for this ontology
	 * @return The reasoner used for this ontology
	 */
	public OWLReasoner getReasoner() {
		return ctx.getReasoner();
	}

	/**
	 * Returns the data factory
	 * @return The data factory
	 */
	public OWLDataFactory getDataFactory() {
		return ctx.getFactory();
	}

	/**
	 * Returns the ontology manager
	 * @return The ontology manager
	 */
	public OWLOntologyManager getManager() {
		return ctx.getManager();
	}

	/**
	 * Returns the debug printer of the ontology, with utility methods for debugging
	 * @return The debug printer of the ontology
	 */
	public OntologyDebugPrinter	 getDebugPrinter() {
		return this.debugPrinter;
	}

	/**
	 * Returns the ontology refactoring helper, with utility methods for refactoring
	 * @return The ontology refactoring helper
	 */
	public OntologyRefactoring getOntologyRefactoring() {
		return this.ontologyRefactoring;
	}

	/**
	 * Converts a textual reference to an IRI. Accepts: (1) Absolute IRI: "https://...#X"; (2) - Prefixed name: "osdi:X", ":X"; 
	 * and (3) Short form: "X" (the default prefix of the PrefixManager is assumed).
	 * @param ref The textual reference
	 * @return The corresponding IRI
	 */
	public IRI toIRI(String ref) {
		return this.ontologyResolution.toIRI(ref);
	}

	/**
	 * Converts an IRI to its short form using the PrefixManager
	 * @param iri The IRI to convert
	 * @return The short form of the IRI
	 */
	public String toShortForm(IRI iri) {
    	return this.ontologyResolution.toShortForm(iri);
	}

	/**
	 * Best-effort prefixed name, falling back to short form.
	 * Useful for debugging/logging.
	 * @param iri The IRI to convert
	 * @return The prefixed name or short form of the IRI
	 */
	public String toPrefixedName(final IRI iri) {
		return this.ontologyResolution.toPrefixedName(iri);
	}

	/**
	 * Loads another ontology from the specified source and makes it available in the manager
	 * @param iri The IRI of the new ontology
	 * @return The loaded ontology
	 * @throws OWLOntologyCreationException 
	 */
	public OWLOntology loadOntology(final OntologySource source) throws OWLOntologyCreationException {
		return ontologyIO.loadOntology(source);
	}

	/**
	 * Saves the ontology to the original file
	 * @throws OWLOntologyStorageException If the ontology cannot be saved or if the ontology was not loaded from a file
	 */
	public void save() throws OWLOntologyStorageException {
		ontologyIO.save();
	}

    /** 
	 * Saves the ontology to the specified destination. It also updates the document IRI of the ontology to the new path.
	 * @param destination The destination where the ontology will be saved
	 * @throws OWLOntologyStorageException If the ontology cannot be saved
	 */
    public void saveAs(OntologyDestination destination) throws OWLOntologyStorageException {
		ontologyIO.saveAs(destination);
    }

	/**
	 * Adds an individual of a specified class to the ontology, unless the individual already exists
	 * @param classIri The IRI of the class
	 * @param individualIri The IRI of the new individual
	 * @return True if the individual was created; false otherwise
	 */
	public boolean createIndividual(IRI classIri, IRI individualIri) {
		return individualAuthoring.createIndividual(classIri, individualIri);
	}

	/**
	 * Asserts that the specified individual is an instance of the specified class
	 * @param classIri The IRI of the class
	 * @param individualIri The IRI of the individual
	 * @return True if the assertion was added; false if it already existed
	 */
	public boolean assertIndividualClass(IRI classIri, IRI individualIri) {
		return individualAuthoring.assertType(individualIri, classIri);
	}

	/**
	 * Asserts that the specified subject individual is linked to the object individual via the specified object property
	 * @param subjectIri The IRI of the subject individual
	 * @param propertyIri The IRI of the object property
	 * @param objectIri The IRI of the object individual
	 * @return True if the assertion was added; false if it already existed
	 */
	public boolean assertObjectProperty(IRI subjectIri, IRI propertyIri, IRI objectIri) {
		return individualAuthoring.assertObjectProperty(subjectIri, propertyIri, objectIri);
	}

	/**
	 * Asserts a data property value for the specified individual.
	 * @param subjectIri The IRI of the individual
	 * @param dataPropertyIri The IRI of the data property
	 * @param value The value to be assigned to the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	public boolean assertDataProperty(IRI subjectIri, IRI dataPropertyIri, OWLLiteral value) {
		return individualAuthoring.assertDataProperty(subjectIri, dataPropertyIri, value);
	}

	/**
	 * Asserts a data property value for the specified individual, given a lexical value and a datatype.
	 * @param subjectIri The IRI of the individual
	 * @param dataPropertyIri The IRI of the data property
	 * @param lexicalValue The lexical value of the data property
	 * @param datatype The datatype of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	public boolean assertDataProperty(IRI subjectIri, IRI dataPropertyIri, String lexicalValue, OWL2Datatype datatype) {
		Objects.requireNonNull(lexicalValue, "lexicalValue must not be null");
		Objects.requireNonNull(datatype, "datatype must not be null");

		final OWLLiteral lit = getDataFactory().getOWLLiteral(lexicalValue, datatype);
		return assertDataProperty(subjectIri, dataPropertyIri, lit);
	}

	/**
	 * Asserts a data property value for the specified individual, given a lexical value (xsd:string datatype is assumed).
	 * @param subjectIri The IRI of the individual
	 * @param dataPropertyIri The IRI of the data property
	 * @param lexicalValue The lexical value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	public boolean assertDataProperty(IRI subjectIri, IRI dataPropertyIri, String lexicalValue) {
		Objects.requireNonNull(lexicalValue, "lexicalValue must not be null");
		// xsd:string is the default datatype
		final OWLLiteral lit = getDataFactory().getOWLLiteral(lexicalValue);
		return assertDataProperty(subjectIri, dataPropertyIri, lit);
	}

	/**
	 * Asserts a data property value for the specified individual, given a lexical value and a language tag.
	 * @param subjectIri The IRI of the individual
	 * @param dataPropertyIri The IRI of the data property
	 * @param lexicalValue The lexical value of the data property
	 * @param lang The language tag
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	public boolean assertDataProperty(IRI subjectIri, IRI dataPropertyIri, String lexicalValue, String lang) {
		Objects.requireNonNull(lexicalValue, "lexicalValue must not be null");
		Objects.requireNonNull(lang, "lang must not be null");

		final OWLLiteral lit = getDataFactory().getOWLLiteral(lexicalValue, lang);
		return assertDataProperty(subjectIri, dataPropertyIri, lit);
	}

	/**
	 * Asserts a data property value for the specified individual, given an integer value.
	 * @param subjectIri The IRI of the individual
	 * @param dataPropertyIri The IRI of the data property
	 * @param value The integer value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	public boolean assertDataProperty(IRI subjectIri, IRI dataPropertyIri, int value) {
		final OWLLiteral lit = getDataFactory().getOWLLiteral(value);
		return assertDataProperty(subjectIri, dataPropertyIri, lit);
	}

	/**
	 * Asserts a data property value for the specified individual, given a double value.
	 * @param subjectIri The IRI of the individual
	 * @param dataPropertyIri The IRI of the data property
	 * @param value The double value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	public boolean assertDataProperty(IRI subjectIri, IRI dataPropertyIri, double value) {
		final OWLLiteral lit = getDataFactory().getOWLLiteral(value);
		return assertDataProperty(subjectIri, dataPropertyIri, lit);
	}

	/**
	 * Asserts a data property value for the specified individual, given a boolean value.
	 * @param subjectIri The IRI of the individual
	 * @param dataPropertyIri The IRI of the data property
	 * @param value The boolean value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	public boolean assertDataProperty(IRI subjectIri, IRI dataPropertyIri, boolean value) {
		final OWLLiteral lit = getDataFactory().getOWLLiteral(value);
		return assertDataProperty(subjectIri, dataPropertyIri, lit);
	}

	/**
	 * Returns the OWLClass object for the specified class IRI, independently of whether it exists or not
	 * @param classIri The IRI of the class
	 * @return The OWLClass object for the specified class IRI
	 */
	public OWLClass asOWLClass(IRI classIri) {
		return this.ontologyResolution.asOWLClass(Objects.requireNonNull(classIri));
	}
	
	/**
	 * Returns the OWLObjectProperty object for the specified object property IRI, independently of whether it exists or not
	 * @param objectPropIRI The IRI of the object property
	 * @return The OWLObjectProperty object for the specified object property IRI
	 */
	public OWLObjectProperty asOWLObjectProperty(IRI objectPropIRI) {
		return this.ontologyResolution.asOWLObjectProperty(Objects.requireNonNull(objectPropIRI));
	}

	/**
	 * Returns the OWLDataProperty object for the specified data property IRI, independently of whether it exists or not
	 * @param dataPropIRI The IRI of the data property
	 * @return The OWLDataProperty object for the specified data property IRI
	 */
	public OWLDataProperty asOWLDataProperty(IRI dataPropIRI) {
		return this.ontologyResolution.asOWLDataProperty(Objects.requireNonNull(dataPropIRI));
	}
	
	/**
	 * Returns the OWLIndividual object for the specified individual IRI, independently of whether it exists or not
	 * @param individualIRI The IRI of the individual
	 * @return The OWLIndividual object for the specified individual IRI
	 */
	public OWLNamedIndividual asOWLIndividual(IRI individualIRI) {
		return this.ontologyResolution.asOWLNamedIndividual(Objects.requireNonNull(individualIRI));
	}

	/**
	 * Returns the OWLAnnotationProperty object for the specified annotation property IRI, independently of whether it exists or not
	 * @param annotationPropIRI The IRI of the annotation property
	 * @return The OWLAnnotationProperty object for the specified annotation property IRI
	 */
	public OWLAnnotationProperty asOWLAnnotationProperty(IRI annotationPropIRI) {
		return this.ontologyResolution.asOWLAnnotationProperty(Objects.requireNonNull(annotationPropIRI));
	}

	/**
	 * Returns the OWLClass object for the specified class IRI, only if it is already defined
	 * @param classIri The IRI of the class
	 * @return The OWLClass object for the specified class IRI, wrapped in an Optional
	 */
	public Optional<OWLClass> findOWLClass(IRI classIri) {
		return this.ontologyResolution.findOWLClass(classIri, Imports.INCLUDED);
	}

	/**
	 * Returns the OWLObjectProperty object for the specified object property IRI, only if it is already defined
	 * @param propIri The IRI of the object property
	 * @return The OWLObjectProperty object for the specified object property IRI, wrapped in an Optional
	 */
	public Optional<OWLObjectProperty> findOWLObjectProperty(IRI propIri) {
		return this.ontologyResolution.findOWLObjectProperty(propIri, Imports.INCLUDED);
	}

	/**
	 * Returns the OWLDataProperty object for the specified data property IRI, only if it is already defined
	 * @param propIri The IRI of the data property
	 * @return The OWLDataProperty object for the specified data property IRI, wrapped in an Optional
	 */
	public Optional<OWLDataProperty> findOWLDataProperty(IRI propIri) {
		return this.ontologyResolution.findOWLDataProperty(propIri, Imports.INCLUDED);
	}

	/**
	 * Returns the OWLIndividual object for the specified individual IRI, only if it is already defined
	 * @param indIri The IRI of the individual
	 * @return The OWLIndividual object for the specified individual IRI, wrapped in an Optional
	 */
	public Optional<OWLNamedIndividual> findOWLIndividual(IRI indIri) {
		return this.ontologyResolution.findOWLNamedIndividual(indIri, Imports.INCLUDED);
	}

    /**
     * Returns all individuals in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of individual IRIs
     */
    public Set<IRI> getIndividualsInSignature(final Imports imports) {
		return this.individualQuery.getIndividualsInSignature(imports);
    }

    /**
     * Returns all classes in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of class IRIs
     */
    public Set<IRI> getClassesInSignature(final Imports imports) {
		return this.individualQuery.getClassesInSignature(imports);
    }

    /**
     * Returns all object properties in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of object property IRIs
     */
    public Set<IRI> getObjectPropertiesInSignature(final Imports imports) {
        return this.individualQuery.getObjectPropertiesInSignature(imports);
    }

    /**
     * Returns all data properties in the ontology signature.
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A set of data property IRIs
     */
    public Set<IRI> getDataPropertiesInSignature(final Imports imports) {
        return this.individualQuery.getDataPropertiesInSignature(imports);
    }
	
	/**
	 * Returns true if the specified individual is an instance of the specified class, according to the specified mode
	 * @param individualIri The IRI of an individual in the ontology
	 * @param classIri The IRI of a class in the ontology
	 * @param importsForAsserted Whether to consider only the local ontology (Imports.EXCLUDED) or the imports closure (Imports.INCLUDED) when checking asserted axioms
	 * @param mode The instance check mode (ASSERTED, INFERRED_DIRECT, INFERRED_ALL)
	 * @return true if the specified individual is an instance of the specified class, according to the specified mode.
	 */
	public boolean isInstanceOf(IRI individualIri, IRI classIri, Imports importsForAsserted, InstanceCheckMode mode) {
		Objects.requireNonNull(mode, "mode must not be null");

		return switch (mode) {
			case ASSERTED -> this.individualQuery.isInstanceOfAsserted(individualIri, classIri, importsForAsserted);
			case INFERRED_DIRECT -> this.reasonedQuery.isInstanceOfInferred(individualIri, classIri, true);
			case INFERRED_ALL -> this.reasonedQuery.isInstanceOfInferred(individualIri, classIri);
		};
	}
	
	/**
	 * Returns true if the ontology contains the specified individual
	 * @param individualIri The IRI of an individual in the ontology
	 * @param imports Whether to consider only the local ontology (Imports.EXCLUDED) or the imports closure (Imports.INCLUDED)
	 * @return true if the ontology contains the specified individual
	 */
	public boolean existsIndividual(IRI individualIri, Imports imports) {
		return this.individualQuery.existsIndividual(individualIri, imports);
	}

	/**
	 * Returns a set of individuals belonging at the same time to ALL the specified classes or any of its subclasses
	 * @param classIRIs A collection of IRIs for classes in the ontology
	 * @return a set of individuals belonging at the same time to ALL the specified classes or any of its subclasses
	 */
	public Set<IRI> getIndividuals(List<IRI> classIRIs) {
		if (classIRIs.size() == 0)
			return new TreeSet<>();
		final Set<IRI> set = getIndividualsOfClass(classIRIs.get(0), Imports.INCLUDED, InstanceCheckMode.ASSERTED);
		for (int i = 1; i < classIRIs.size(); i++)
			set.retainAll(getIndividualsOfClass(classIRIs.get(i), Imports.INCLUDED, InstanceCheckMode.ASSERTED));
		return set;
	}

	/**
	 * Returns a set of individuals belonging to the specified class, according to the specified mode
	 * @param classIri The IRI of a class in the ontology
	 * @param imports Whether to consider only the local ontology (Imports.EXCLUDED) or the imports closure (Imports.INCLUDED) when checking asserted axioms
	 * @param mode The instance check mode (ASSERTED, INFERRED_DIRECT, INFERRED_ALL)
	 * @return a set of individuals belonging to the specified class, according to the specified mode.
	 */
	public Set<IRI> getIndividualsOfClass(IRI classIri, Imports imports, InstanceCheckMode mode) {
		return switch (mode) {
			case ASSERTED ->
				this.individualQuery.getIndividualsOfClass(classIri, imports);
			case INFERRED_DIRECT ->
				this.reasonedQuery.getIndividualsOfClassInferred(classIri, true);
			case INFERRED_ALL ->
				this.reasonedQuery.getIndividualsOfClassInferred(classIri, false);
		};
	}	
	
	/**
	 * Returns a set of IRIs representing the asserted types of a specified individual. If includeSuperClasses is true, 
	 * the set will include all superclasses as well (extracted from asserted SubClassOf axioms and not by using a reasoner).
	 * @param individualIRI An individual in the ontology
	 * @param includeSuperClasses If true, the set will include all superclasses as well
	 * @param imports Whether to consider only the local ontology (Imports.EXCLUDED) or the imports closure (Imports.INCLUDED)
	 * @return a set of IRIs representing the asserted types of a specified individual.
	 */
	public Set<IRI> getAssertedTypes(IRI individualIRI, boolean includeSuperClasses,Imports imports) {
		if (includeSuperClasses)
			return individualQuery.getAssertedTypesWithSuperclasses(individualIRI, imports);
		return individualQuery.getTypes(individualIRI, imports);
	}

	/**
	 * Returns a set of IRIs representing the inferred types of a specified individual. If includeSuperClasses is true, 
	 * the set will include all superclasses as well.
	 * FIXME: This method is not consistent with other methods that use InstanceCheckMode, should be refactored. 
	 * @param individualIRI An individual in the ontology
	 * @param includeSuperClasses If true, the set will include all superclasses as well
	 * @param imports Whether to consider only the local ontology (Imports.EXCLUDED) or the imports closure (Imports.INCLUDED)
	 * @return a set of IRIs representing the inferred types of a specified individual.
	 */
	public Set<IRI> getInferredTypes(IRI individualIRI, boolean includeSuperClasses, Imports imports) {
		return reasonedQuery.getTypesInferred(individualIRI, includeSuperClasses);
	}

    /**
     * Returns all asserted object property values for an individual, grouped by property.
     * @param subjectIri The IRI of the subject individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A map from object property IRIs to sets of object individual IRIs
     */
    public Map<IRI, Set<IRI>> getAllObjectPropertyValues(final IRI subjectIri, final Imports imports) {
		return this.individualQuery.getAllObjectPropertyValues(subjectIri, imports);
	}
	
    /**
     * Returns asserted data property values for an individual.
     * @param subjectIri The IRI of the subject individual
     * @param dataPropertyIri The IRI of the data property
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of data property values
     */
    public Set<OWLLiteral> getDataPropertyValues(final IRI subjectIri, final IRI dataPropertyIri, final Imports imports) {
		return this.individualQuery.getDataPropertyValues(subjectIri, dataPropertyIri, imports);
	}

    /**
     * Returns all asserted data property values for an individual, grouped by property.
     * @param subjectIri The IRI of the subject individual
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return A map from data property IRIs to sets of data property values
     */
    public Map<IRI, Set<OWLLiteral>> getAllDataPropertyValues(final IRI subjectIri, final Imports imports) {
		return this.individualQuery.getAllDataPropertyValues(subjectIri, imports);
	}

    /**
     * Returns asserted object property values for an individual.
     * @param subjectIri The IRI of the subject individual
     * @param objectPropertyIri The IRI of the object property
     * @param imports The imports setting (INCLUDED or EXCLUDED)
     * @return The set of object individual IRIs
     */
    public Set<IRI> getObjectPropertyValues(final IRI subjectIri, final IRI objectPropertyIri, final Imports imports) {
		return this.individualQuery.getObjectPropertyValues(subjectIri, objectPropertyIri, imports);
	}
	
	/**
	 * Returns a set containing solely those individuals from the passed collection that belongs to the specified class
	 * @param individuals A collection of individual names 
	 * @param classIRI The IRI of a class in the ontology
	 * @return a set containing solely those individuals from the passed collection that belongs to the specified class
	 */
	public Set<IRI> pickIndividualsSubclassOf(Collection<IRI> individuals, IRI classIRI) {
		final Set<IRI> result = getIndividualsOfClass(classIRI, Imports.INCLUDED, InstanceCheckMode.ASSERTED); 
		result.retainAll(individuals);
		return result;
	}
	
	/**
	 * Returns the label (rdfs:label) of an ontology element in a specific language, if available.
	 * @param elementIRI The IRI of the element in the ontology
	 * @param lang The language code (e.g., "es" or "en")
	 * @return The label in the given language, or an empty Optional if none is found
	 */
	public Optional<String> getLabelForIRI(IRI elementIRI, String lang) {
		return this.individualQuery.getLabelForIRI(elementIRI, lang);
	}

	/**
	 * Checks if the ontology complies with the OWL 2 DL profile.
	 * @param ontology The ontology to be checked
	 * @throws OWLOntologyCreationException if the ontology does not comply with the OWL 2 DL profile
	 */
    public void checkProfile(OWLOntology ontology) throws OWLOntologyCreationException {
		Objects.requireNonNull(ontology, "ontology must not be null");
        final OWLProfile profile = new OWL2DLProfile(); // o el perfil que te interese
        final OWLProfileReport report = profile.checkOntology(ontology);

        if (!report.isInProfile()) {
            throw new OWLOntologyCreationException("Ontology does NOT comply with OWL 2 DL. Violations: " + report.getViolations().toString());
        } 
    }
}
