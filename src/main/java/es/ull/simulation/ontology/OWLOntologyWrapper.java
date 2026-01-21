/**
 * 
 */
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
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
 * A wrapper for an ontology in OWL. Creates convenient methods that shorten the use of OWLApi.
 * @author Iván Castilla Rodríguez
 *
 */
public class OWLOntologyWrapper {
	/** 
	 * The OWL ontology manager 
	 */
	@Nonnull
	private final OWLOntologyManager manager;
	/** 
	 * The OWL ontology 
	 */
	@Nonnull
	private OWLOntology ontology;
	/** 
	 * The prefix manager for the ontology 
	 */
	@Nonnull
	private final PrefixManager pm;
	/**
	 * The OWL data factory
	 */
	@Nonnull
    private final OWLDataFactory factory;
	/**
	 * The OWL reasoner factory
	 */
	@Nonnull
	private final OWLReasonerFactory reasonerFactory;
	/**
	 * The OWL reasoner
	 */
    private OWLReasoner reasoner;

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
		ontology = Objects.requireNonNull(ontology, "OWL Ontology should never be null");
		this.manager = Objects.requireNonNull(ontology.getOWLOntologyManager(), "OWLOntologyManager should never be null");
        this.reasonerFactory = new StructuralReasonerFactory();
        this.factory = Objects.requireNonNull(manager.getOWLDataFactory(), "OWL Data Factory should never be null");
		setOntology(ontology);
		this.pm = new DefaultPrefixManager();
		final OWLDocumentFormat format = manager.getOntologyFormat(ontology);

		if (format != null && format.isPrefixOWLDocumentFormat()) {
			PrefixDocumentFormat prefixFormat = format.asPrefixOWLDocumentFormat();
			// Copies the prefixes to the local prefix manager
			prefixFormat.getPrefixName2PrefixMap().forEach((name, prefIRI) -> {
				pm.setPrefix(Objects.requireNonNull(name, "Prefix name should never be null"), Objects.requireNonNull(prefIRI, "Prefix IRI should never be null"));
			});
		}		
	}

	/**
	 * Returns the ontology
	 * @return The ontology
	 */
	public OWLOntology getOntology() {
		return ontology;
	}

	/**
	 * Returns the reasoner used for this ontology
	 * @return The reasoner used for this ontology
	 */
	public OWLReasoner getReasoner() {
		return reasoner;
	}

	/**
	 * Returns the data factory
	 * @return The data factory
	 */
	public OWLDataFactory getDataFactory() {
		return factory;
	}

	/**
	 * Returns the ontology manager
	 * @return The ontology manager
	 */
	public OWLOntologyManager getManager() {
		return manager;
	}

	/**
	 * Sets the main ontology and checks that it complies with the OWL2 DL profile
	 * @param ontology The ontology to set
	 * @throws OWLOntologyCreationException If the ontology does not comply with the OWL2 DL profile
	 */
	public void setOntology(OWLOntology ontology) throws OWLOntologyCreationException {
		this.ontology = Objects.requireNonNull(ontology, "OWL Ontology should never be null");
		checkProfile();
		this.reasoner = Objects.requireNonNull(reasonerFactory.createReasoner(ontology), "OWL Reasoner should never be null");
		// Ask the reasoner to do all the necessary work now
		reasoner.precomputeInferences();
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
	 * @param documentIRI The IRI where the ontology will be saved
	 * @throws OWLOntologyStorageException If the ontology cannot be saved
	 */
    public void saveAs(IRI documentIRI) throws OWLOntologyStorageException {
		manager.setOntologyDocumentIRI(ontology, Objects.requireNonNull(documentIRI));
        manager.saveOntology(ontology);
    }

	/**
	 * Adds a local path mapping for an IRI
	 * @param iri The IRI of the ontology to map
	 * @param path The path to the local file
	 */
	public void addLocalIRIMapper(String iri, String path) {
		IRI schemaIRI = IRI.create(Objects.requireNonNull(iri, "IRI should never be null"));
		File schemaFile = Objects.requireNonNull(new File(path), "File should never be null");
		manager.getIRIMappers().add(new SimpleIRIMapper(schemaIRI, IRI.create(schemaFile)));		
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
	 * @param classIRI The IRI of the class
	 * @param individualIRI The IRI of the new individual
	 * @return True if the individual was created; false otherwise
	 */
	public boolean addIndividual(String classIRI, String individualIRI) {
		final OWLNamedIndividual owlIndividual = factory.getOWLNamedIndividual(individualIRI, pm);
		final boolean ok = !ontology.containsIndividualInSignature(owlIndividual.getIRI(), Imports.INCLUDED);

		if (ok) {
			final OWLClass owlClass = factory.getOWLClass(classIRI, pm);
			final OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(owlClass, owlIndividual);
			manager.addAxiom(ontology, classAssertion);
		}
		return ok;
	}
	
	/**
	 * Adds an object property value to the specified individual, linking it to another individual.
	 * @param srcIndividualIRI The IRI of the source individual
	 * @param objectProperty The IRI of the object property
	 * @param destIndividualIRI The IRI of the destination individual
	 */
	public boolean addObjectPropertyValue(String srcIndividualIRI, String objectProperty, String destIndividualIRI) {
		final OWLNamedIndividual owlSrcIndividual = factory.getOWLNamedIndividual(srcIndividualIRI, pm);
		final OWLNamedIndividual owlDestIndividual = factory.getOWLNamedIndividual(destIndividualIRI, pm);
		final OWLObjectProperty owlObjectProperty = factory.getOWLObjectProperty(objectProperty, pm);

		boolean srcExists = ontology.containsIndividualInSignature(owlSrcIndividual.getIRI(), Imports.INCLUDED);
		boolean destExists = ontology.containsIndividualInSignature(owlDestIndividual.getIRI(), Imports.INCLUDED);
		boolean propExists = ontology.containsObjectPropertyInSignature(owlObjectProperty.getIRI(), Imports.INCLUDED);

		if (srcExists && destExists && propExists) {
			final OWLObjectPropertyAssertionAxiom objectPropertyAssertion =
				factory.getOWLObjectPropertyAssertionAxiom(owlObjectProperty, owlSrcIndividual, owlDestIndividual);
			manager.addAxiom(ontology, objectPropertyAssertion);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Adds a string data property value to the specified individual.
	 * @param individualIRI The IRI of the individual to which the data property will be added
	 * @param dataProperty The IRI of the data property
	 * @param value The value to be assigned to the data property
	 * @return true if the data property value was added successfully; false if the individual or the data property does not exist
	 */
	public boolean addDataPropertyValue(String individualIRI, String dataProperty, String value) {
		return addDataPropertyValue(individualIRI, dataProperty, value, OWL2Datatype.XSD_STRING);
	}

	/**
	 * Adds a data property value to the specified individual with a specific OWL2 datatype.
	 * @param individualIRI The IRI of the individual to which the data property will be added
	 * @param dataProperty The IRI of the data property
	 * @param value The value to be assigned to the data property
	 * @param dataType The OWL2 datatype of the value
	 * @return true if the data property value was added successfully; false if the individual or the data propertydoes not exist
	 */
	public boolean addDataPropertyValue(String individualIRI, String dataProperty, String value, OWL2Datatype dataType) {
        final OWLNamedIndividual owlIndividual = factory.getOWLNamedIndividual(individualIRI, pm);
        final OWLDataProperty owlDataProperty = factory.getOWLDataProperty(dataProperty, pm);

		boolean srcExists = ontology.containsIndividualInSignature(owlIndividual.getIRI(), Imports.INCLUDED);
		boolean propExists = ontology.containsDataPropertyInSignature(owlDataProperty.getIRI(), Imports.INCLUDED);
		if (srcExists && propExists) {
			final OWLLiteral literal = factory.getOWLLiteral(value, factory.getOWLDatatype(dataType));
			final OWLAxiom ax = factory.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlIndividual, literal);
			manager.addAxiom(ontology, ax);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the OWLClass object for the specified class IRI, independently of whether it exists or not
	 * @param classIRI The IRI of the class
	 * @return The OWLClass object for the specified class IRI
	 */
	public OWLClass getOWLClass(String classIRI) {
		return factory.getOWLClass(Objects.requireNonNull(classIRI), pm);
	}
	
	/**
	 * Returns the OWLObjectProperty object for the specified object property IRI, independently of whether it exists or not
	 * @param objectPropIRI The IRI of the object property
	 * @return The OWLObjectProperty object for the specified object property IRI
	 */
	public OWLObjectProperty getOWLObjectProperty(String objectPropIRI) {
		return factory.getOWLObjectProperty(Objects.requireNonNull(objectPropIRI), pm);
	}
	
	/**
	 * Returns the OWLDataProperty object for the specified data property IRI, independently of whether it exists or not
	 * @param dataPropIRI The IRI of the data property
	 * @return The OWLDataProperty object for the specified data property IRI
	 */
	public OWLDataProperty getOWLDataProperty(String dataPropIRI) {
		return factory.getOWLDataProperty(Objects.requireNonNull(dataPropIRI), pm);
	}
	
	/**
	 * Returns the OWLIndividual object for the specified individual IRI, independently of whether it exists or not
	 * @param individualIRI The IRI of the individual
	 * @return The OWLIndividual object for the specified individual IRI
	 */
	public OWLIndividual getOWLIndividual(String individualIRI) {
		return factory.getOWLNamedIndividual(Objects.requireNonNull(individualIRI), pm);
	}

    /**
	 * Returns the OWLClass object for the specified class IRI, only if it is already defined
	 * @param classIRI The IRI of the class
	 * @return The OWLClass object for the specified class IRI, null if it is not defined
     */
    public OWLClass getOWLClassIfExists(String classIRI) {
        final OWLClass cls = factory.getOWLClass(Objects.requireNonNull(classIRI), pm);
        if (cls != null && ontology.isDeclared(cls, Imports.INCLUDED)) {
            return cls;
        }
        return null;
    }

	/**
	 * Returns the OWLObjectProperty object for the specified object property IRI, only if it is already defined
	 * @param objectPropIRI The IRI of the object property
	 * @return The OWLObjectProperty object for the specified object property IRI, null if it is not defined
	 */
    public OWLObjectProperty getOWLObjectPropertyIfExists(String objectPropIRI) {
        final OWLObjectProperty prop = factory.getOWLObjectProperty(Objects.requireNonNull(objectPropIRI), pm);
        if (prop != null && ontology.isDeclared(prop, Imports.INCLUDED)) {
            return prop;
        }
        return null;
    }

	/**
	 * Returns the OWLDataProperty object for the specified data property IRI, only if it is already defined
	 * @param dataPropIRI The IRI of the data property
	 * @return The OWLDataProperty object for the specified data property IRI, null if it is not defined
	 */
    public OWLDataProperty getOWLDataPropertyIfExists(String dataPropIRI) {
        final OWLDataProperty prop = factory.getOWLDataProperty(Objects.requireNonNull(dataPropIRI), pm);
        if (prop != null && ontology.isDeclared(prop, Imports.INCLUDED)) {
            return prop;
        }
        return null;
    }

	/**
	 * Returns the OWLIndividual object for the specified individual IRI, only if it is already defined
	 * @param individualIRI The IRI of the individual
	 * @return The OWLIndividual object for the specified individual IRI, null if it is not defined
	 */
    public OWLNamedIndividual getOWLIndividualIfExists(String individualIRI) {
        final OWLNamedIndividual ind = factory.getOWLNamedIndividual(Objects.requireNonNull(individualIRI), pm);
        if (ind != null && ontology.isDeclared(ind, Imports.INCLUDED)) {
            return ind;
        }
        return null;
    }

	/**
	 * Returns a set of strings representing the names of the individuals in the ontology
	 * @return a set of strings representing the names of the individuals in the ontology
	 */
	public Set<String> individualsToString() {
		final TreeSet<String> set = new TreeSet<>();
		for (OWLNamedIndividual individual : ontology.getIndividualsInSignature(Imports.INCLUDED)) {
			set.add(individual.getIRI().getShortForm());
		}
		return set;
	}
	
	/**
	 * Returns a set of strings representing the names of the classes in the ontology
	 * @return a set of strings representing the names of the classes in the ontology
	 */
	public Set<String> classesToString() {
		final TreeSet<String> set = new TreeSet<>();
		for (OWLClass clazz : ontology.getClassesInSignature(Imports.INCLUDED)) {
			set.add(clazz.getIRI().getShortForm());
		}
		return set;
	}
	
	/**
	 * Returns a set of strings representing the names of the data properties in the ontology
	 * @return a set of strings representing the names of the data properties in the ontology
	 */
	public Set<String> dataPropertiesToString() {
		final TreeSet<String> set = new TreeSet<>();
		for (OWLDataProperty dataProp : ontology.getDataPropertiesInSignature(Imports.INCLUDED)) {
			set.add(dataProp.getIRI().getShortForm());
		}
		return set;
	}
	
	/**
	 * Returns a set of strings representing the names of the object properties in the ontology
	 * @return a set of strings representing the names of the object properties in the ontology
	 */
	public Set<String> objectPropertiesToString() {
		final TreeSet<String> set = new TreeSet<>();
		for (OWLObjectProperty objectProp : ontology.getObjectPropertiesInSignature(Imports.INCLUDED)) {
			set.add(objectProp.getIRI().getShortForm());
		}
		return set;
	}
	
	/**
	 * Returns true if the specified individual is an instance of the specified class (or any of its subclasses)
	 * @param individualIRI The IRI of an individual in the ontology
	 * @param classIRI The IRI of a class in the ontology
	 * @return true if the specified individual is instance of the specified class (or any of its subclasses).
	 */
	public boolean isInstanceOf(String individualIRI, String classIRI) {
		return getIndividuals(classIRI).contains(individualIRI);
	}
	
	/**
	 * Returns true if the ontology contains the specified individual
	 * @param individualIRI The IRI of an individual in the ontology
	 * @return true if the ontology contains the specified individual
	 */
	public boolean containsIndividual(String individualIRI) {
		return ontology.containsIndividualInSignature(factory.getOWLNamedIndividual(individualIRI, pm).getIRI(), Imports.INCLUDED);
	}

	/**
	 * Returns a set of individuals belonging at the same time to ALL the specified classes or any of its subclasses
	 * @param classIRIs A collection of IRIs for classes in the ontology
	 * @return a set of individuals belonging at the same time to ALL the specified classes or any of its subclasses
	 */
	public Set<String> getIndividuals(ArrayList<String> classIRIs) {
		if (classIRIs.size() == 0)
			return new TreeSet<>();
		final Set<String> set = getIndividuals(classIRIs.get(0));
		for (int i = 1; i < classIRIs.size(); i++)
			set.retainAll(getIndividuals(classIRIs.get(i)));
		return set;
	}
	
	/**
	 * Returns a set of individuals belonging to the specified class or any of its subclasses
	 * @param classIRI The IRI of a class in the ontology
	 * @return a set of individuals belonging to the specified class or any of its subclasses
	 */
	public Set<String> getIndividuals(String classIRI) {
		final TreeSet<String> set = new TreeSet<>();
		final OWLClass owlClass = factory.getOWLClass(classIRI, pm);
		final NodeSet<OWLNamedIndividual> individualsNodeSet = reasoner.getInstances(owlClass, false);
		final Set<OWLNamedIndividual> individuals = OWLAPIStreamUtils.asSet(individualsNodeSet.entities());
		for (OWLNamedIndividual individual : individuals) {
			set.add(individual.getIRI().getShortForm());
		}
		return set;
	}
	
	/** 
	 * Returns a list of strings representing the properties of the specified individual. This list includes the class it belongs to, the object properties and the data properties.
	 * @param individualIRI An individual in the ontology
	 * @param sep A separator to use between the property name and its value
	 * @return a list of strings representing the properties of the specified individual. This list includes the class it belongs to, the object properties and the data properties.
	 * Each string in the list is formatted as "PROPERTY_NAME" + sep + "PROPERTY_VALUE".
	 */
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
	 * Returns a list of strings representing the (direct) classes of a specified individual.
	 * @param individualIRI An individual in the ontology
	 * @return a list of strings representing the classes of a specified individual.
	 */
	public Set<String> getIndividualClasses(String individualIRI) {
		return getIndividualClasses(individualIRI, false);
	}

	/** 
	 * Returns a list of strings representing the classes of a specified individual. 
	 * If includeSuperClasses is true, the list will include all superclasses as well.
	 * @param individualIRI An individual in the ontology
	 * @return a list of strings representing the classes of a specified individual.
	 */
	public Set<String> getIndividualClasses(String individualIRI, boolean includeSuperClasses) {
		final Set<String> result = new TreeSet<>();
		OWLNamedIndividual ind = factory.getOWLNamedIndividual(individualIRI, pm);
		if (includeSuperClasses) {
			final Set<OWLClass> types = reasoner.types(ind).collect(Collectors.toSet());
			for (OWLClass clazz : types)
			if (!clazz.isAnonymous())
				result.add(clazz.getIRI().getShortForm());
		}
		else {
			for (OWLClassAssertionAxiom axiom : ontology.getAxioms(AxiomType.CLASS_ASSERTION, Imports.INCLUDED)) {
				if (axiom.getIndividual().equals(ind)) {
					OWLClassImpl classExpression = (OWLClassImpl) axiom.getClassExpression();
					if (!classExpression.isAnonymous()) {
						result.add(((OWLClassImpl)classExpression.asOWLClass()).getIRI().getShortForm());
					}
				}
			}
		}
		return result;
	}

	/** 
	 * Returns a list of pairs of strings representing the data property names and their values for the specified individual.
	 * @param individualIRI An individual in the ontology
	 * @return a list of pairs of strings representing the data property names and their values for the specified individual.
	 */
	public ArrayList<String[]> getIndividualDataProperties(String individualIRI) {
		final ArrayList<String[]> list = new ArrayList<>();
		for (OWLDataPropertyAssertionAxiom axiom : ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION, Imports.INCLUDED)) {
		    if (axiom.getSubject().equals(factory.getOWLNamedIndividual(individualIRI, pm))) {
		        OWLDataPropertyImpl property = (OWLDataPropertyImpl) axiom.getProperty();
		        list.add(new String[] {property.getIRI().getShortForm(), axiom.getObject().getLiteral()});
		    }
		}		
		return list;
	}
	
	/** 
	 * Returns a list of pairs of strings representing the object property names and their values for the specified individual.
	 * @param individualIRI An individual in the ontology
	 * @return a list of pairs of strings representing the object property names and their values for the specified individual.
	 */
	public ArrayList<String[]> getIndividualObjectProperties(String individualIRI) {
		final ArrayList<String[]> list = new ArrayList<>();
		for (OWLObjectPropertyAssertionAxiom axiom : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION, Imports.INCLUDED)) {
		    if (axiom.getSubject().equals(factory.getOWLNamedIndividual(individualIRI, pm))) {
		        OWLObjectPropertyImpl property = (OWLObjectPropertyImpl) axiom.getProperty();
		        list.add(new String[] {property.getIRI().getShortForm(), ((OWLNamedIndividualImpl)axiom.getObject()).getIRI().getShortForm()});
		    }
		}
		return list;
	}
	
	/**
	 * Returns a list of strings representing the values the specified dataProperty has for the specified individual
	 * @param individualIRI An individual in the ontology
	 * @param dataPropIRI A data property in the ontology
	 * @return a list of strings representing the values the specified dataProperty has for the specified individual
	 */
	public ArrayList<String> getDataPropertyValues(String individualIRI, String dataPropIRI) {
		Set<String> values = new LinkedHashSet<>();
		OWLNamedIndividual ind = factory.getOWLNamedIndividual(individualIRI, pm);
		OWLDataProperty prop = factory.getOWLDataProperty(dataPropIRI, pm);

		ontology.getImportsClosure().forEach(o -> {
			EntitySearcher.getDataPropertyValues(ind, prop, o)
				.forEach(lit -> values.add(lit.getLiteral()));
		});

		return new ArrayList<>(values);
	}

	
	/**
	 * Returns a set of strings representing the names of the individuals referenced by the specified objectProperty of specified individual
	 * @param individualIRI An individual in the ontology
	 * @param objectPropIRI An object property in the ontology
	 * @return a set of strings representing the names of the individuals referenced by the specified objectProperty of specified individual
	 */
	public Set<String> getObjectPropertyValues(String individualIRI, String objectPropIRI) {
		final Set<String> result = new TreeSet<>();
		final OWLNamedIndividual subject = factory.getOWLNamedIndividual(individualIRI, pm);
		final OWLObjectProperty property = factory.getOWLObjectProperty(objectPropIRI, pm);

		ontology.getImportsClosure().forEach(o -> {
			EntitySearcher.getObjectPropertyValues(subject, property, o)
				.forEach(obj -> {
					if (obj.isNamed()) {
						String shortName = obj.asOWLNamedIndividual().getIRI().getShortForm();
						result.add(shortName);
					} else {
						result.add("[Anonymous Individual]");
					}
				});
		});
		
		return result;
	}
	
	/**
	 * Returns a set containing solely those individuals from the passed collection that belongs to the specified class
	 * @param individuals A collection of individual names 
	 * @param classIRI The IRI of a class in the ontology
	 * @return a set containing solely those individuals from the passed collection that belongs to the specified class
	 */
	public Set<String> getIndividualsSubclassOf(Collection<String> individuals, String classIRI) {
		final Set<String> result = getIndividuals(classIRI); 
		result.retainAll(individuals);
		return result;
	}
	
	/**
	 * Removes all individuals of the specified class from the ontology
	 * @param classIRI The IRI of a class in the ontology
	 */
	public void removeIndividualsOfClass(String classIRI) {
		final OWLClass owlClass = factory.getOWLClass(classIRI, pm);
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
	public void createClassSubClassOf(String classIRI, String superclassIRI) {
		final OWLClassExpression owlSuperClass = factory.getOWLClass(superclassIRI, pm);
		final OWLClass owlClass = factory.getOWLClass(classIRI, pm);
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


    public void checkProfile() throws OWLOntologyCreationException {
        final OWLProfile profile = new OWL2DLProfile(); // o el perfil que te interese
        final OWLProfileReport report = profile.checkOntology(ontology);

        if (!report.isInProfile()) {
            throw new OWLOntologyCreationException("Ontology does NOT comply with OWL 2 DL. Violations: " + report.getViolations().toString());
        } 
    }

    /**
     * Explain the ontology by providing insights into its structure and inconsistencies.
     * @param ontology
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
	 * Converts a camel case string to SNAKE_CASE.
	 * Adapted from https://www.geeksforgeeks.org/convert-camel-case-string-to-snake-case-in-java/
	 * @param name The camel case string to be converted
	 * @return The converted string in SNAKE_CASE
	 */
	public static String camel2SNAKE(String name) {
        // Regular Expression
        final String regex = "([a-z])([A-Z]+)";
 
        // Replacement string
        final String replacement = "$1_$2";
 
        // Replace the given regex
        // with replacement string
        // and convert it to upper case.
        return name.replaceAll(regex, replacement).toUpperCase();
	}

	/**
	 * Prints a tabulated list of individuals, their classes, object properties and data properties.
	 * The output is formatted as:
	 * Individual    Class    ObjectProperties    DataProperties
	 * #Individual1  Class1  #ObjectProp1: #ObjectValue1; #ObjectProp2: #ObjectValue2;    #DataProp1: DataValue1; #DataProp2: DataValue2;
	 */
	public void printTabulatedIndividuals() {
		System.out.print("Individual\tClass\tObjectProperties\tDataProperties\n");

		for (String individual : individualsToString()) {

			System.out.print("#" + individual + "\t" + getIndividualClasses(individual).stream().collect(Collectors.joining(", ")) + "\t");
			ArrayList<String[]> objectProps = getIndividualObjectProperties(individual);
			ArrayList<String[]> dataProps = getIndividualDataProperties(individual);
			for (String[] objectProp : objectProps)
				System.out.print("#" + objectProp[0] + ": #" + objectProp[1] + "; ");
			System.out.print("\t");
			for (String[] dataProp : dataProps)
				System.out.print("#" + dataProp[0] + ": " + dataProp[1] + "; ");
			System.out.println();
		}
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
	
	/**
	 * Print the names of the classes in the ontology, one per line.
	 * If the class names are to be printed as an enum, use printClassesAsEnum() instead.
	 */
	public void printClasses() {
		for (String clazz: classesToString())
			System.out.println(clazz);
	}
	
	/**
	 * A convenient method to populate enums in java with a collection of classes.
	 * The class names are converted to SNAKE_CASE format.
	 * If the class names are to be printed as a list, use printClasses() instead.
	 */
	public void printClassesAsEnum() {
		for (String name : classesToString()) {
			System.out.println(camel2SNAKE(name) + "(\"" + name + "\"),");
		}
	}

	/**
	 * Print the names of the data properties in the ontology, one per line.
	 * If the data property names are to be printed as an enum, use printDataPropertiesAsEnum() instead.
	 */
	public void printDataProperties() {
		for (String dataProp: dataPropertiesToString())
			System.out.println(dataProp);
	}
	
	/**
	 * A convenient method to populate enums in java with a collection of data properties.
	 * The data property names are converted to SNAKE_CASE format.
	 * If the data property names are to be printed as a list, use printDataProperties() instead.
	 */
	public void printDataPropertiesAsEnum() {
		for (String name : dataPropertiesToString()) {
			System.out.println(camel2SNAKE(name) + "(\"" + name + "\"),");
		}
	}

	/**
	 * Print the names of the object properties in the ontology, one per line.
	 * If the object property names are to be printed as an enum, use printObjectPropertiesAsEnum() instead.
	 */
	public void printObjectProperties() {
		for (String objectProp: objectPropertiesToString())
			System.out.println(objectProp);
	}
	
	/**
	 * A convenient method to populate enums in java with a collection of object properties.
	 * The object property names are converted to SNAKE_CASE format.
	 * If the object property names are to be printed as a list, use printObjectProperties() instead.
	 */
	public void printObjectPropertiesAsEnum() {
		for (String name : objectPropertiesToString()) {
			System.out.println(camel2SNAKE(name) + "(\"" + name + "\"),");
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
					wrapper.printTabulatedIndividuals();
					break;
				case 2:
					System.out.println("---------------- CLASSES ----------------");
					wrapper.printClassesAsEnum();
					System.out.println("---------------- DATA PROPS ----------------");
					wrapper.printDataPropertiesAsEnum();
					System.out.println("---------------- OBJECT PROPS ----------------");
					wrapper.printObjectPropertiesAsEnum();
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
