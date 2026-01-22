package es.ull.simulation.ontology;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

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
	 * The debug printer helper 
	 */
	private final OntologyDebugPrinter debugPrinter;

    /**
	 * Creates a wrapper for the ontology in the input stream
	 * @param inputStream The input stream with the ontology 
	 * @param localMappings Optional local mappings for IRIs, in the form "http://example.org/ontology#=path/to/local/file.owl"
	 * @throws OWLOntologyCreationException If the ontology cannot be opened
	 */
	public OWLOntologyWrapper(InputStream inputStream, String... localMappings) throws OWLOntologyCreationException {
		this(OWLOntologyLoader.fromStream(inputStream, localMappings));
    }

	/**
	 * Creates a wrapper for the ontology in the file with the specified path
	 * @param path Path to the file with the ontology
	 * @param localMappings Optional local mappings for IRIs, in the form "http://example.org/ontology#=path/to/local/file.owl"
	 * @throws OWLOntologyCreationException If the ontology cannot be opened
	 */
	public OWLOntologyWrapper(String path, String... localMappings) throws OWLOntologyCreationException {
		this(OWLOntologyLoader.fromPath(path, localMappings));
	}

	/**
	 * Creates a wrapper for the ontology with the specified IRI
	 * @param iri The IRI of the ontology
	 * @param localMappings Optional local mappings for IRIs, in the form "http://example.org/ontology#=path/to/local/file.owl"
	 * @throws OWLOntologyCreationException If the ontology cannot be opened
	 */
	public OWLOntologyWrapper(IRI iri, String... localMappings) throws OWLOntologyCreationException {
		this(OWLOntologyLoader.fromIRI(iri, localMappings));
	}

	private OWLOntologyWrapper(OWLOntologyLoader loader) throws OWLOntologyCreationException {
		this(loader.getOntology());
	}

	/**
	 * 
	 * @param ontology The OWL ontology
	 * @throws OWLOntologyCreationException
	 */
	@SuppressWarnings("null")
	public OWLOntologyWrapper(OWLOntology ontology) throws OWLOntologyCreationException {
		final OWLOntology ont = Objects.requireNonNull(ontology, "OWL Ontology should never be null");
		final OWLOntologyManager manager = Objects.requireNonNull(ont.getOWLOntologyManager(), "OWLOntologyManager should never be null");
		final PrefixManager pm = buildPrefixManagerFromOntologyFormat(manager, ont);
		final OWLReasonerFactory rf = new StructuralReasonerFactory();

		this.ctx = new OntologyContext(manager, ont, pm, rf);
		this.ontologyResolution = new OntologyResolution(ctx);
		checkProfile(ont);
		this.individualAuthoring = new IndividualAuthoring(ctx);
		this.individualQuery = new IndividualQuery(ctx);
		this.reasonedQuery = new ReasonedQuery(ctx);
		this.debugPrinter = new OntologyDebugPrinter(ctx, ontologyResolution, individualQuery);
	}

	/**
	 * Builds a PrefixManager from the ontology format, if possible
	 * @param manager The ontology manager
	 * @param ontology The ontology
	 * @return The PrefixManager
	 */
	private static PrefixManager buildPrefixManagerFromOntologyFormat(OWLOntologyManager manager, OWLOntology ontology) {
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

	public OntologyDebugPrinter	 getDebugPrinter() {
		return this.debugPrinter;
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
	 * Adds an ontology from the specified IRI. The new ontology becomes the main ontology handled by this wrapper.
	 * @param iri The IRI of the new ontology
	 * @throws OWLOntologyCreationException 
	 */
	public void addOntology(IRI iri) throws OWLOntologyCreationException {
		setOntology(manager.loadOntology(Objects.requireNonNull(iri, "IRI should never be null")));
	}

	/**
	 * Adds an ontology from the specified input stream. The new ontology becomes the main ontology handled by this wrapper.
	 * @param inputStream The input stream containing the ontology
	 * @throws OWLOntologyCreationException 
	 */
	public void addOntology(InputStream inputStream) throws OWLOntologyCreationException {
		setOntology(manager.loadOntologyFromOntologyDocument(Objects.requireNonNull(inputStream, "InputStream should never be null")));
	}

	/**
	 * Adds an ontology from the specified file. The new ontology becomes the main ontology handled by this wrapper.
	 * @param path The path to the file containing the ontology
	 * @throws OWLOntologyCreationException 
	 */
	public void addOntology(String path) throws OWLOntologyCreationException {
		try {
			this.addOntology(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			throw new OWLOntologyCreationException("The ontology file was not found: " + path, e);
		}
	}

	/**
	 * Saves the ontology to the original file
	 * @throws OWLOntologyStorageException If the ontology cannot be saved or if the ontology was not loaded from a file
	 */
	public void save() throws OWLOntologyStorageException {
		IRI docIRI = manager.getOntologyDocumentIRI(ontology);
		if (!"file".equalsIgnoreCase(docIRI.getScheme())) {
			throw new OWLOntologyStorageException(
				"Ontology was not loaded from a file. Use saveAs(...) to specify a local path.");
		}
		manager.saveOntology(ontology);
	}

    /** 
	 * Saves the ontology to the specified path. It also updates the document IRI of the ontology to the new path.
	 * @param path The path to the file where the ontology will be saved
	 * @throws OWLOntologyStorageException If the ontology cannot be saved
	 */
    public void saveAs(Path path) throws OWLOntologyStorageException {
		saveAs(IRI.create(Objects.requireNonNull(path.toUri())));
    }

    /** 
	 * Saves the ontology to the specified file. It also updates the document IRI of the ontology to the new file path.
	 * @param file The file where the ontology will be saved
	 * @throws OWLOntologyStorageException If the ontology cannot be saved
	 */
    public void saveAs(File file) throws OWLOntologyStorageException {
        saveAs(file.toPath());
    }

    /**
	 * Saves the ontology to the specified IRI. It also updates the document IRI of the ontology to the new IRI.
	 * @param documentIri The IRI where the ontology will be saved
	 * @throws OWLOntologyStorageException If the ontology cannot be saved
	 */
    public void saveAs(IRI documentIri) throws OWLOntologyStorageException {
		manager.setOntologyDocumentIRI(ontology, Objects.requireNonNull(documentIri));
        manager.saveOntology(ontology);
    }

	/**
	 * Adds a local path mapping for an IRI
	 * @param iri The IRI of the ontology to map
	 * @param path The path to the local file
	 */
	public void addLocalIRIMapper(IRI iri, String path) {
		final File schemaFile = Objects.requireNonNull(new File(path), "File should never be null");
		manager.getIRIMappers().add(new SimpleIRIMapper(Objects.requireNonNull(iri, "IRI should never be null"), 
			Objects.requireNonNull(IRI.create(schemaFile))));		
	}

	/**
	 * Adds a local path mapping for an IRI expressed as a string
	 * @param strIri The IRI of the ontology to map as a string
	 * @param path The path to the local file
	 */
	public void addLocalIRIMapper(String strIri, String path) {
		this.addLocalIRIMapper(IRI.create(Objects.requireNonNull(strIri, "IRI should never be null")), path);
	}

	/**
	 * Merges another ontology with a previously loaded one
	 * @param inputStream The input stream containing the ontology to merge
	 * @throws OWLOntologyCreationException If the ontology cannot be merged
	 */
	public void mergeOtherOntology(InputStream inputStream) throws OWLOntologyCreationException {
		final OWLOntology otherOntology = manager.loadOntologyFromOntologyDocument(Objects.requireNonNull(inputStream, "InputStream should never be null"));
		final IRI mergedIRI = Objects.requireNonNull(IRI.create(otherOntology.getOntologyID().getOntologyIRI().get() + "-merged"), "Merged IRI should never be null");
		ontology = Objects.requireNonNull(new OWLOntologyMerger(manager).createMergedOntology(manager, mergedIRI), "Merged ontology should never be null");
	}

	/**
	 * Merges another ontology with a previously loaded one
	 * @param path The path to the file containing the ontology to merge
	 * @throws OWLOntologyCreationException If the ontology cannot be merged
	 */
	public void mergeOtherOntology(String path) throws OWLOntologyCreationException {
		try {
			this.mergeOtherOntology(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			throw new OWLOntologyCreationException("The ontology file was not found: " + path, e);
		}
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
	 * Adds an individual of a specified class to the ontology, unless the individual already exists
	 * @param classRef The short name of the class
	 * @param individualRef The short name of the new individual
	 * @return True if the individual was created; false otherwise
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean createIndividual(String classRef, String individualRef) {
		return createIndividual(toIRI(classRef), toIRI(individualRef));
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
	 * Asserts that the specified source individual is linked to the destination individual via the specified object property
	 * @param srcIndividualRef The IRI of the source individual
	 * @param objectPropertyRef The IRI of the object property
	 * @param destIndividualRef The IRI of the destination individual
	 * @return True if the assertion was added; false if it already existed
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean assertObjectProperty(String srcIndividualRef, String objectPropertyRef, String destIndividualRef) {
		return assertObjectProperty(toIRI(srcIndividualRef), toIRI(objectPropertyRef), toIRI(destIndividualRef));
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
	 * Asserts a data property value for the specified individual, given a lexical value (xsd:string datatype is assumed).
	 * @param subjectRef The IRI of the individual as a string
	 * @param dataPropertyRef The IRI of the data property as a string
	 * @param lexicalValue The lexical value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean assertDataProperty(String subjectRef, String dataPropertyRef, String lexicalValue) {
		return assertDataProperty(toIRI(subjectRef), toIRI(dataPropertyRef), lexicalValue);
	}

	/**
	 * Asserts a data property value for the specified individual, given a lexical value and a datatype.
	 * @param subjectRef The IRI of the individual as a string
	 * @param dataPropertyRef The IRI of the data property as a string
	 * @param lexicalValue The lexical value of the data property
	 * @param datatype The datatype of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean assertDataProperty(String subjectRef, String dataPropertyRef, String lexicalValue, OWL2Datatype datatype) {
		return assertDataProperty(toIRI(subjectRef), toIRI(dataPropertyRef), lexicalValue, datatype);
	}

	/**
	 * Asserts a data property value for the specified individual, given a lexical value and a language tag.
	 * @param subjectRef The IRI of the individual as a string
	 * @param dataPropertyRef The IRI of the data property as a string
	 * @param lexicalValue The lexical value of the data property
	 * @param lang The language tag
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean assertDataProperty(String subjectRef, String dataPropertyRef, String lexicalValue, String lang) {
		return assertDataProperty(toIRI(subjectRef), toIRI(dataPropertyRef), lexicalValue, lang);
	}

	/**
	 * Asserts a data property value for the specified individual, given an integer value.
	 * @param subjectRef The IRI of the individual as a string
	 * @param dataPropertyRef The IRI of the data property as a string
	 * @param value The integer value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean assertDataProperty(String subjectRef, String dataPropertyRef, int value) {
		return assertDataProperty(toIRI(subjectRef), toIRI(dataPropertyRef), value);
	}

	/**
	 * Asserts a data property value for the specified individual, given a double value.
	 * @param subjectRef The IRI of the individual as a string
	 * @param dataPropertyRef The IRI of the data property as a string
	 * @param value The double value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean assertDataProperty(String subjectRef, String dataPropertyRef, double value) {
		return assertDataProperty(toIRI(subjectRef), toIRI(dataPropertyRef), value);
	}

	/**
	 * Asserts a data property value for the specified individual, given a boolean value.
	 * @param subjectRef The IRI of the individual as a string
	 * @param dataPropertyRef The IRI of the data property as a string
	 * @param value The boolean value of the data property
	 * @return true if the data property value was added successfully; false if the axiom already exists
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean assertDataProperty(String subjectRef, String dataPropertyRef, boolean value) {
		return assertDataProperty(toIRI(subjectRef), toIRI(dataPropertyRef), value);
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
	 * Returns the OWLClass object for the specified class short name, independently of whether it exists or not
	 * @param classRef The short name of the class
	 * @return The OWLClass object for the specified class short name
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public OWLClass asOWLClass(String classRef) {
		return asOWLClass(toIRI(classRef));
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
	 * Returns the OWLObjectProperty object for the specified object property short name, independently of whether it exists or not
	 * @param objectPropRef The short name of the object property
	 * @return The OWLObjectProperty object for the specified object property short name
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public OWLObjectProperty asOWLObjectProperty(String objectPropRef) {
		return asOWLObjectProperty(toIRI(objectPropRef));
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
	 * Returns the OWLDataProperty object for the specified data property short name, independently of whether it exists or not
	 * @param dataPropRef The short name of the data property
	 * @return The OWLDataProperty object for the specified data property short name
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public OWLDataProperty asOWLDataProperty(String dataPropRef) {
		return asOWLDataProperty(toIRI(dataPropRef));
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
	 * Returns the OWLIndividual object for the specified individual short name, independently of whether it exists or not
	 * @param individualRef The short name of the individual
	 * @return The OWLIndividual object for the specified individual short name
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public OWLNamedIndividual asOWLIndividual(String individualRef) {
		return asOWLIndividual(toIRI(individualRef));
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
	 * Returns the OWLClass object for the specified class short name, only if it is already defined
	 * @param classRef The short name of the class
	 * @return The OWLClass object for the specified class short name, wrapped in an Optional
     */
	@Deprecated(since = "2026-01", forRemoval = true)
    public Optional<OWLClass> findOWLClass(String classRef) {
        return findOWLClass(toIRI(classRef));
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
	 * Returns the OWLObjectProperty object for the specified object property short name, only if it is already defined
	 * @param objectPropRef The short name of the object property
	 * @return The OWLObjectProperty object for the specified object property short name, wrapped in an Optional
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
    public Optional<OWLObjectProperty> findOWLObjectProperty(String objectPropRef) {
		return findOWLObjectProperty(toIRI(objectPropRef));    
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
	 * Returns the OWLDataProperty object for the specified data property short name, only if it is already defined
	 * @param dataPropRef The short name of the data property
	 * @return The OWLDataProperty object for the specified data property short name, wrapped in an Optional
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
    public Optional<OWLDataProperty> findOWLDataProperty(String dataPropRef) {
		return findOWLDataProperty(toIRI(dataPropRef));    
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
	 * Returns the OWLIndividual object for the specified individual IRI, only if it is already defined
	 * @param individualIRI The IRI of the individual
	 * @return The OWLIndividual object for the specified individual IRI, wrapped in an Optional
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
    public Optional<OWLNamedIndividual> findOWLIndividual(String individualIRI) {
		return findOWLIndividual(toIRI(individualIRI));
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
	 * Returns true if the specified individual is an instance of the specified class (or any of its subclasses), considering inferred axioms
	 * @param individualIRI The IRI of an individual in the ontology
	 * @param classIRI The IRI of a class in the ontology
	 * @return true if the specified individual is instance of the specified class (or any of its subclasses), considering inferred axioms.
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public boolean isInstanceOf(String individualIRI, String classIRI) {
		return isInstanceOf(toIRI(individualIRI), toIRI(classIRI), Imports.INCLUDED, InstanceCheckMode.INFERRED_ALL);
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
	 * Returns a set of individuals belonging at the same time to ALL the specified classes or any of its subclasses
	 * @param classIRIs A collection of IRIs for classes in the ontology
	 * @return a set of individuals belonging at the same time to ALL the specified classes or any of its subclasses
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public Set<String> getIndividuals(ArrayList<String> classIRIs) {
		if (classIRIs.size() == 0)
			return new TreeSet<>();
		final Set<String> set = getIndividuals(classIRIs.get(0));
		for (int i = 1; i < classIRIs.size(); i++)
			set.retainAll(getIndividuals(classIRIs.get(i)));
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
	 * Returns a set of individuals belonging to the specified class or any of its subclasses
	 * @param classRef The IRI of a class in the ontology
	 * @return a set of individuals belonging to the specified class or any of its subclasses
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public Set<String> getIndividuals(String classRef) {
    return this.individualQuery.getIndividualsOfClass(toIRI(classRef), Imports.INCLUDED).stream()
            .map(IRI::getShortForm)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));	
	}
	
	/** 
	 * Returns a list of strings representing the properties of the specified individual. This list includes the class it belongs to, the object properties and the data properties.
	 * @param individualIRI An individual in the ontology
	 * @param sep A separator to use between the property name and its value
	 * @return a list of strings representing the properties of the specified individual. This list includes the class it belongs to, the object properties and the data properties.
	 * Each string in the list is formatted as "PROPERTY_NAME" + sep + "PROPERTY_VALUE".
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public ArrayList<String> getIndividualProperties(String individualIRI, String sep) {
		final ArrayList<String> list = new ArrayList<>();
		for (String clazz : getIndividualClasses(individualIRI)) {
		    list.add("SUBCLASS_OF" + sep + clazz);
		}
		for (String[] objectProp : getIndividualObjectProperties(individualIRI)) {
		    list.add(objectProp[0] + sep + objectProp[1]);
		}
		for (String[] dataProp : getIndividualDataProperties(individualIRI)) {
		    list.add(dataProp[0] + sep + dataProp[1]);
		}
		return list;
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
	 * Returns a list of strings representing the (direct) classes of a specified individual.
	 * @param individualIRI An individual in the ontology
	 * @return a list of strings representing the classes of a specified individual.
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public Set<String> getIndividualClasses(String individualIRI) {
		return getIndividualClasses(individualIRI, false);
	}

	/** 
	 * Returns a list of strings representing the classes of a specified individual. 
	 * If includeSuperClasses is true, the list will include all superclasses as well.
	 * @param individualIRI An individual in the ontology
	 * @return a list of strings representing the classes of a specified individual.
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public Set<String> getIndividualClasses(String individualIRI, boolean includeSuperClasses) {
		final Set<String> result = new TreeSet<>();
		final Set<IRI> individualIRIs = getAssertedTypes(toIRI(individualIRI), includeSuperClasses, Imports.INCLUDED);
		for (IRI iri : individualIRIs) {
			result.add(iri.getShortForm());
		}
		return result;		
	}

	/** 
	 * Returns a list of pairs of strings representing the data property names and their values for the specified individual.
	 * @param individualIRI An individual in the ontology
	 * @return a list of pairs of strings representing the data property names and their values for the specified individual.
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public ArrayList<String[]> getIndividualDataProperties(String individualIRI) {
		final ArrayList<String[]> list = new ArrayList<>();
		Map<IRI, Set<OWLLiteral>> allDataProps = getAllDataPropertyValues(toIRI(individualIRI), Imports.INCLUDED);
		for (IRI key : allDataProps.keySet()) {
			final Set<OWLLiteral> values = allDataProps.get(key);
			for (OWLLiteral value : values) {
				list.add(new String[] {key.getShortForm(), value.getLiteral()});
			}
		}
		return list;
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
	 * Returns a list of pairs of strings representing the object property names and their values for the specified individual.
	 * @param individualIRI An individual in the ontology
	 * @return a list of pairs of strings representing the object property names and their values for the specified individual.
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public ArrayList<String[]> getIndividualObjectProperties(String individualIRI) {
		final ArrayList<String[]> list = new ArrayList<>();
		final Map<IRI, Set<IRI>> allObjProps = getAllObjectPropertyValues(toIRI(individualIRI), Imports.INCLUDED);
		for (IRI key : allObjProps.keySet()) {
			final Set<IRI> values = allObjProps.get(key);
			for (IRI value : values) {
				list.add(new String[] {key.getShortForm(), value.getShortForm()});
			}
		}
		return list;
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
	 * Returns a list of strings representing the values the specified dataProperty has for the specified individual
	 * @param individualIRI An individual in the ontology
	 * @param dataPropIRI A data property in the ontology
	 * @return a list of strings representing the values the specified dataProperty has for the specified individual
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public ArrayList<String> getDataPropertyValues(String individualIRI, String dataPropIRI) {
		final Set<OWLLiteral> literals = getDataPropertyValues(toIRI(individualIRI), toIRI(dataPropIRI), Imports.INCLUDED);

		Set<String> values = new LinkedHashSet<>();
		for (OWLLiteral lit : literals) {
			values.add(lit.getLiteral());
		}
		return new ArrayList<>(values);
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
	 * Returns a set of strings representing the names of the individuals referenced by the specified objectProperty of specified individual
	 * @param individualIRI An individual in the ontology
	 * @param objectPropIRI An object property in the ontology
	 * @return a set of strings representing the names of the individuals referenced by the specified objectProperty of specified individual
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public Set<String> getObjectPropertyValues(String individualIRI, String objectPropIRI) {
		final Set<String> result = new TreeSet<>();
		final Set<IRI> values = this.individualQuery.getObjectPropertyValues(toIRI(individualIRI), toIRI(objectPropIRI), Imports.INCLUDED);
		for (IRI iri : values) {
			result.add(iri.getShortForm());
		}		
		return result;
	}
	
	/**
	 * Returns a set containing solely those individuals from the passed collection that belongs to the specified class
	 * @param individuals A collection of individual names 
	 * @param classIRI The IRI of a class in the ontology
	 * @return a set containing solely those individuals from the passed collection that belongs to the specified class
	 */
	public Set<IRI> getIndividualsSubclassOf(Collection<IRI> individuals, IRI classIRI) {
		final Set<IRI> result = getIndividualsOfClass(classIRI, Imports.INCLUDED, InstanceCheckMode.ASSERTED); 
		result.retainAll(individuals);
		return result;
	}
	
	/**
	 * Returns a set containing solely those individuals from the passed collection that belongs to the specified class
	 * @param individuals A collection of individual names 
	 * @param classIRI The IRI of a class in the ontology
	 * @return a set containing solely those individuals from the passed collection that belongs to the specified class
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public Set<String> getIndividualsSubclassOf(Collection<String> individuals, String classIRI) {
		final Set<String> result = getIndividuals(classIRI); 
		result.retainAll(individuals);
		return result;
	}
	
	/**
	 * Removes all individuals of the specified class from the ontology
	 * @param classIRI The IRI of a class in the ontology
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public void removeIndividualsOfClass(String classIRI) {
		final OWLClass owlClass = factory.asOWLClass(classIRI, pm);
		final NodeSet<OWLNamedIndividual> individualsNodeSet = reasoner.getInstances(owlClass, false);
		final Set<OWLNamedIndividual> individuals = OWLAPIStreamUtils.asSet(individualsNodeSet.entities());
		final OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(ontology));
		for (OWLNamedIndividual individual : individuals) {
			individual.accept(remover);
		}
		manager.applyChanges(remover.getChanges());
		remover.reset();
	}
	
	/**
	 * Creates a subclass relationship between the specified class and its superclass
	 * @param classIRI The IRI of the class to be created
	 * @param superclassIRI The IRI of the superclass
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public void createClassSubClassOf(String classIRI, String superclassIRI) {
		final OWLClassExpression owlSuperClass = factory.asOWLClass(superclassIRI, pm);
		final OWLClass owlClass = factory.asOWLClass(classIRI, pm);
		OWLSubClassOfAxiom ax = factory.getOWLSubClassOfAxiom(owlClass, owlSuperClass);
		AddAxiom addAx = new AddAxiom(ontology, ax);
	    manager.applyChange(addAx);
	}
	
	/**
	 * Collects all individuals of the specified class and changes them to subclasses of the specified class. 
	 * Adds the prefix to the individual names to create the new class IRIs. 
	 * @param classIRI The IRI of the class whose individuals will be changed to subclasses
	 * @param prefix The prefix to be added to the individual names to create the new class IRIs
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public void changeInstanceToSubclass(String classIRI, String prefix) {
		final Set<String> individuals = getIndividuals(classIRI);
		removeIndividualsOfClass(classIRI);
		for (String ind : individuals) {
			createClassSubClassOf(prefix + ind, classIRI);
		}
	}

	/**
	 * Returns the label (rdfs:label) of an ontology element in a specific language, if available.
	 * @param elementIRI The IRI of the element in the ontology
	 * @param lang The language code (e.g., "es" or "en")
	 * @return The label in the given language, or null if none is found
	 */
	@Deprecated(since = "2026-01", forRemoval = true)
	public String getLabelForIRI(String elementIRI, String lang) {
		OWLEntity entity = ontology.entitiesInSignature(factory.getOWLNamedIndividual(elementIRI, pm).getIRI(), Imports.INCLUDED)
			.findFirst()
			.orElse(null);
		if (entity != null) {
			for (OWLOntology o : ontology.getImportsClosure()) {
				for (OWLAnnotation annotation :
						EntitySearcher.getAnnotations(entity, o).collect(Collectors.toList())) {
					if (annotation.getProperty().isLabel() && annotation.getValue().asLiteral().isPresent()) {
						OWLLiteral literal = annotation.getValue().asLiteral().get();
						if (literal.hasLang(lang)) {
							return literal.getLiteral();
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Checks if the ontology complies with the OWL 2 DL profile.
	 * @param ontology The ontology to be checked
	 * @throws OWLOntologyCreationException if the ontology does not comply with the OWL 2 DL profile
	 */
    public void checkProfile(OWLOntology ontology) throws OWLOntologyCreationException {
        final OWLProfile profile = new OWL2DLProfile(); // o el perfil que te interese
        final OWLProfileReport report = profile.checkOntology(ontology);

        if (!report.isInProfile()) {
            throw new OWLOntologyCreationException("Ontology does NOT comply with OWL 2 DL. Violations: " + report.getViolations().toString());
        } 
    }

    /**
     * Explain the ontology by providing insights into its structure and inconsistencies.
     * @author Adapted from HermiT's example
     */
    public void explain() {
        
        ReasonerFactory factory = new ReasonerFactory();
        // We don't want HermiT to thrown an exception for inconsistent ontologies because then we 
        // can't explain the inconsistency. This can be controlled via a configuration setting.  
        Configuration configuration = new Configuration();
        configuration.throwInconsistentOntologyException = false;
        OWLReasoner reasoner = factory.createReasoner(ontology, configuration);
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
    public Set<OWLAxiom> getInferredAxioms() {
        OWLReasonerFactory reasonerFactory = new ReasonerFactory(); // HermiT
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        // Inference generator
        InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner);

        // Temporal ontology to store inferred axioms
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        OWLOntology inferredOnt;
        try {
            inferredOnt = manager.createOntology();
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

    public void replaceMin0ByOnlyRecursive() {
        final OWLDataFactory factory = manager.getOWLDataFactory();
        Set<OWLAxiom> axiomsToRemove = new HashSet<>();
        Set<OWLAxiom> axiomsToAdd = new HashSet<>();

        for (OWLSubClassOfAxiom ax : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression subCls = ax.getSubClass();
            OWLClassExpression superCls = ax.getSuperClass();

            OWLClassExpression newSuperCls = replaceMin0InExpression(superCls, factory);
            if (!newSuperCls.equals(superCls)) {
                axiomsToRemove.add(ax);
                axiomsToAdd.add(factory.getOWLSubClassOfAxiom(subCls, newSuperCls));
            }
        }

        manager.removeAxioms(ontology, axiomsToRemove);
        manager.addAxioms(ontology, axiomsToAdd);
    }

    private OWLClassExpression replaceMin0InExpression(OWLClassExpression expr, OWLDataFactory factory) {
        // Si es una restricción de min cardinality
        if (expr instanceof OWLObjectMinCardinality) {
            OWLObjectMinCardinality minCard = (OWLObjectMinCardinality) expr;
            if (minCard.getCardinality() == 0) {
                return factory.getOWLObjectAllValuesFrom(minCard.getProperty().asOWLObjectProperty(), minCard.getFiller());
            }
        }
        else if (expr instanceof OWLDataMinCardinality) {
            OWLDataMinCardinality minCard = (OWLDataMinCardinality) expr;
            if (minCard.getCardinality() == 0) {
                return factory.getOWLDataAllValuesFrom(minCard.getProperty().asOWLDataProperty(), minCard.getFiller());
            }
        }

        // Si es una intersección, se procesa recursivamente cada operando
        if (expr instanceof OWLObjectIntersectionOf) {
            List<OWLClassExpression> newOperands = ((OWLObjectIntersectionOf) expr).getOperands().stream()
                    .map(e -> replaceMin0InExpression(e, factory))
                    .collect(Collectors.toList());
            return factory.getOWLObjectIntersectionOf(newOperands);
        }

        // Si es unión, complemento, etc., se puede extender de forma similar
        // Por ahora devolvemos el mismo si no se cumple nada
        return expr;
    }

	/**
	 * Prints a list of individuals, their classes, object properties and data properties.
	 * If full is true, it prints the individual name, class, object properties and data properties.
	 * If full is false, it prints only the individual name.
	 * @param full If true, prints the individual name, class, object properties and data properties; otherwise, prints only the individual name.
	 */
	public void printIndividuals(boolean full) {
		if (full) {
			for (String individual : individualsToString()) {
				final ArrayList<String> props = getIndividualProperties(individual, "\t");
				for (String prop : props)
					System.out.println(individual + "\t" + prop);
			}
		}
		else  {
			for (String individual : individualsToString())
				System.out.println(individual);
		}
	}

	/**
	 * Prints a list of individuals of the specified class, their properties and values.
	 * If full is true, it prints the individual name, class, object properties and data properties.
	 * If full is false, it prints only the individual name.
	 * @param classIRI The IRI of the class whose individuals will be printed
	 * @param full If true, prints the individual name, class, object properties and data properties; otherwise, prints only the individual name.
	 */
	public void printIndividuals(String classIRI, boolean full) {
		if (full) {
			for (String individual : getIndividuals(classIRI)) {
				final ArrayList<String> props = getIndividualProperties(individual, "\t");
				for (String prop : props)
					System.out.println(individual + "\t" + prop);
			}
		}
		else  {
			for (String individual : getIndividuals(classIRI))
				System.out.println(individual);
		}
	}
	
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar OWLOntologyWrapper.jar <ontology file/IRI> <mode>");
			System.out.println("Mode can be 1 to print individuals, 2 to print classes and properties to be used in an enum");
			return;
		}
		boolean isURI = true;
		OWLOntologyWrapper wrapper = null;
		try {
			new URI(args[0]);
		} catch (Exception e) {
			isURI = false;
		}
		
		try {
			if (isURI) {
				IRI iri = IRI.create(args[0]);
				wrapper = new OWLOntologyWrapper(iri);
			}
			else {
				final Path path = Paths.get(args[0]);
				if (!Files.exists(path) || !Files.isRegularFile(path)) {
					System.err.println("The specified ontology file does not exist or is not a regular file: " + args[0]);
					return;
				}
				wrapper = new OWLOntologyWrapper(args[0]);
			}
			int mode = Integer.parseInt(args[1]);
			switch (mode) {
				case 1:
					wrapper.getDebugPrinter().printTabulatedIndividuals(Imports.INCLUDED);
					break;
				case 2:
					System.out.println("---------------- CLASSES ----------------");
					wrapper.getDebugPrinter().printClassesAsEnum(Imports.EXCLUDED);
					System.out.println("---------------- DATA PROPS ----------------");
					wrapper.getDebugPrinter().printDataPropertiesAsEnum(Imports.EXCLUDED);
					System.out.println("---------------- OBJECT PROPS ----------------");
					wrapper.getDebugPrinter().printObjectPropertiesAsEnum(Imports.EXCLUDED);
					break;
				default:
					System.out.println("Invalid mode. Use 1 or 2.");
					break;
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
}
