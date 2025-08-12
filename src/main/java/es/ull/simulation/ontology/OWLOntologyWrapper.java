/**
 * 
 */
package es.ull.simulation.ontology;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

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
	protected final OWLOntologyManager manager;
	protected OWLOntology ontology;
	protected final PrefixManager pm;
    protected final OWLDataFactory factory;
    protected final OWLReasoner reasoner;

    /**
	 * Creates a wrapper for the ontology in the file
	 * @param file The file with the ontology 
	 * @param prefix The prefix to use for the ontology
	 * @param localMappings Optional local mappings for IRIs, in the form "http://example.org/ontology#=path/to/local/file.owl"
	 * @throws OWLOntologyCreationException If the ontology cannot be opened
	 */
	public OWLOntologyWrapper(File file, String prefix, String... localMappings) throws OWLOntologyCreationException {
		manager = OWLManager.createOWLOntologyManager();
		if (localMappings.length > 0) {
			for (String mapping : localMappings) {
				String[] parts = mapping.split("=");
				if (parts.length == 2) {
					addLocalIRIMapper(parts[0], parts[1]);
				}
			}
		}
		ontology = manager.loadOntologyFromOntologyDocument(file);
        pm = new DefaultPrefixManager(prefix);
        factory = manager.getOWLDataFactory();
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        reasoner = reasonerFactory.createReasoner(ontology);
        // Ask the reasoner to do all the necessary work now
        reasoner.precomputeInferences();
    }

	/**
	 * Creates a wrapper for the ontology in the file with the specified path
	 * @param path Path to the file with the ontology
	 * @param prefix The prefix to use for the ontology
	 * @param localMappings Optional local mappings for IRIs, in the form "http://example.org/ontology#=path/to/local/file.owl"
	 * @throws OWLOntologyCreationException If the ontology cannot be opened
	 */
	public OWLOntologyWrapper(String path, String prefix, String... localMappings) throws OWLOntologyCreationException {
		this(new File(path), prefix, localMappings);
	}

	/**
	 * Returns the ontology
	 * @return The ontology
	 */
	public OWLOntology getOntology() {
		return ontology;
	}

	/**
	 * Saves the ontology to the original file
	 * @throws OWLOntologyStorageException If the ontology cannot be saved
	 */
	public void save() throws OWLOntologyStorageException {
		manager.saveOntology(ontology);
	}

	/**
	 * Adds a local path mapping for an IRI
	 * @param iri The IRI of the ontology to map
	 * @param path The path to the local file
	 */
	public void addLocalIRIMapper(String iri, String path) {
		IRI schemaIRI = IRI.create(iri);
		File schemaFile = new File(path);
		manager.getIRIMappers().add(new SimpleIRIMapper(schemaIRI, IRI.create(schemaFile)));		
	}

	/**
	 * Merges another ontology with a previously loaded one
	 * @param file The file containing the ontology to merge
	 * @throws OWLOntologyCreationException If the ontology cannot be merged
	 */
	public void mergeOtherOntology(File file) throws OWLOntologyCreationException {
		final OWLOntology otherOntology = manager.loadOntologyFromOntologyDocument(file);
		ontology = new OWLOntologyMerger(manager).createMergedOntology(manager, IRI.create(otherOntology.getOntologyID().getOntologyIRI().get() + "-merged"));
	}

	/**
	 * Merges another ontology with a previously loaded one
	 * @param path The path to the file containing the ontology to merge
	 * @throws OWLOntologyCreationException If the ontology cannot be merged
	 */
	public void mergeOtherOntology(String path) throws OWLOntologyCreationException {
		this.mergeOtherOntology(new File(path));
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
		return factory.getOWLClass(classIRI, pm);
	}
	
	/**
	 * Returns the OWLObjectProperty object for the specified object property IRI, independently of whether it exists or not
	 * @param objectPropIRI The IRI of the object property
	 * @return The OWLObjectProperty object for the specified object property IRI
	 */
	public OWLObjectProperty getOWLObjectProperty(String objectPropIRI) {
		return factory.getOWLObjectProperty(objectPropIRI, pm);
	}
	
	/**
	 * Returns the OWLDataProperty object for the specified data property IRI, independently of whether it exists or not
	 * @param dataPropIRI The IRI of the data property
	 * @return The OWLDataProperty object for the specified data property IRI
	 */
	public OWLDataProperty getOWLDataProperty(String dataPropIRI) {
		return factory.getOWLDataProperty(dataPropIRI, pm);
	}
	
	/**
	 * Returns the OWLIndividual object for the specified individual IRI, independently of whether it exists or not
	 * @param individualIRI The IRI of the individual
	 * @return The OWLIndividual object for the specified individual IRI
	 */
	public OWLIndividual getOWLIndividual(String individualIRI) {
		return factory.getOWLNamedIndividual(individualIRI, pm);
	}

    /**
	 * Returns the OWLClass object for the specified class IRI, only if it is already defined
	 * @param classIRI The IRI of the class
	 * @return The OWLClass object for the specified class IRI, null if it is not defined
     */
    public OWLClass getOWLClassIfExists(String classIRI) {
        OWLClass cls = factory.getOWLClass(classIRI, pm);
        if (ontology.containsClassInSignature(cls.getIRI(), Imports.INCLUDED)) {
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
        OWLObjectProperty prop = factory.getOWLObjectProperty(objectPropIRI, pm);
        if (ontology.containsObjectPropertyInSignature(prop.getIRI(), Imports.INCLUDED)) {
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
        OWLDataProperty prop = factory.getOWLDataProperty(dataPropIRI, pm);
        if (ontology.containsDataPropertyInSignature(prop.getIRI(), Imports.INCLUDED)) {
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
        OWLNamedIndividual ind = factory.getOWLNamedIndividual(individualIRI, pm);
        if (ontology.containsIndividualInSignature(ind.getIRI(), Imports.INCLUDED)) {
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
		for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
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
		for (OWLClass clazz : ontology.getClassesInSignature()) {
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
		for (OWLDataProperty dataProp : ontology.getDataPropertiesInSignature()) {
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
		for (OWLObjectProperty objectProp : ontology.getObjectPropertiesInSignature()) {
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
		return ontology.containsIndividualInSignature(factory.getOWLNamedIndividual(individualIRI, pm).getIRI());
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
		if (includeSuperClasses) {
			final Set<OWLClass> types = reasoner.types(factory.getOWLNamedIndividual(individualIRI, pm)).collect(Collectors.toSet());
			for (OWLClass clazz : types)
			if (!clazz.isAnonymous())
				result.add(clazz.getIRI().getShortForm());
		}
		else {
			for (OWLClassAssertionAxiom axiom : ontology.getAxioms(AxiomType.CLASS_ASSERTION)) {
				if (axiom.getIndividual().equals(factory.getOWLNamedIndividual(individualIRI, pm))) {
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
		for (OWLDataPropertyAssertionAxiom axiom : ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION)) {
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
		for (OWLObjectPropertyAssertionAxiom axiom : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
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
						String shortName = simplifyIRI(obj.asOWLNamedIndividual().getIRI().getShortForm());
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
	 * A convenient method to get just the name from IRIs. It depends on the ontology prefix, so it must be overriden by subclasses.
	 * @param IRI Original complex IRI
	 * @return The simplified IRI 
	 */
	public String simplifyIRI(String IRI) {
		return IRI;
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
			System.out.println("Usage: java -jar OWLOntologyWrapper.jar <ontology file> <prefix>");
			return;
		}
		try {
			OWLOntologyWrapper wrapper = new OWLOntologyWrapper(args[0], args[1]);
			wrapper.printTabulatedIndividuals();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
}
