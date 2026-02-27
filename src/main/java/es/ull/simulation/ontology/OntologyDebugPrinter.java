package es.ull.simulation.ontology;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * Human-friendly debug/inspection utilities.
 *
 * Design:
 * - Read-only: never modifies ontology.
 * - Uses OntologyResolution for rendering.
 * - Keeps "printing" concerns out of OWLOntologyWrapper and other modules.
 */
public final class OntologyDebugPrinter {

    private final OntologyContext ctx;
    private final OntologyResolution resolution;
    private final IndividualQuery individualQuery;
    private final PrintStream out;

    public OntologyDebugPrinter(final OntologyContext ctx, final OntologyResolution resolution, final IndividualQuery individualQuery) {
        this(ctx, resolution, individualQuery, System.out);
    }

    public OntologyDebugPrinter(final OntologyContext ctx,
                                final OntologyResolution resolution,
                                final IndividualQuery individualQuery,
                                final PrintStream out) {
        this.ctx = Objects.requireNonNull(ctx, "ctx must not be null");
        this.resolution = Objects.requireNonNull(resolution, "resolution must not be null");
        this.individualQuery = Objects.requireNonNull(individualQuery, "individualQuery must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    /* -----------------------------
     * Prefixes
     * ----------------------------- */

    public void printPrefixes() {
        final OWLDocumentFormat fmt = ctx.getManager().getOntologyFormat(ctx.getOntology());
        if (!(fmt instanceof PrefixDocumentFormat)) {
            out.println("No prefix format available for ontology.");
            return;
        }

        final PrefixDocumentFormat pf = (PrefixDocumentFormat) fmt;
        final Map<String, String> prefixes = new LinkedHashMap<>(pf.getPrefixName2PrefixMap());

        if (prefixes.isEmpty()) {
            out.println("No prefixes declared.");
            return;
        }

        out.println("Prefixes:");
        prefixes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> out.printf("  %-10s %s%n", e.getKey(), e.getValue()));
    }

    /* -----------------------------
     * Signature listings (IRI sets)
     * ----------------------------- */

    public void printIndividuals(final Imports imports) {
        printIRISet("Individuals (" + imports + ")", individualQuery.getIndividualsInSignature(imports));
    }

    public void printClasses(final Imports imports) {
        printIRISet("Classes (" + imports + ")", individualQuery.getClassesInSignature(imports));
    }

    public void printObjectProperties(final Imports imports) {
        printIRISet("Object properties (" + imports + ")", individualQuery.getObjectPropertiesInSignature(imports));
    }

    public void printDataProperties(final Imports imports) {
        printIRISet("Data properties (" + imports + ")", individualQuery.getDataPropertiesInSignature(imports));
    }

    private void printIRISet(final String title, final Set<IRI> iris) {
        out.println(title + " [" + iris.size() + "]");
        iris.stream()
                .sorted(Comparator.comparing(IRI::toString))
                .forEach(iri -> out.println("  " + resolution.toPrefixedName(iri)));
    }

    /* -----------------------------
     * Enum-like printers
     * ----------------------------- */

    public void printClassesAsEnum(final Imports imports) {
        printEntityEnum(individualQuery.getClassesInSignature(imports));
    }

    public void printObjectPropertiesAsEnum(final Imports imports) {
        printEntityEnum(individualQuery.getObjectPropertiesInSignature(imports));
    }

    public void printDataPropertiesAsEnum(final Imports imports) {
        printEntityEnum(individualQuery.getDataPropertiesInSignature(imports));
    }

    /**
     * Prints all the items in an enum style, ready for manual copy/paste into code.
     */
    private void printEntityEnum(final Set<IRI> entityIris) {
        Objects.requireNonNull(entityIris, "entityIris must not be null");
        for (IRI iri : entityIris) {
            out.println("\t" + toEnumConstantName(iri.getShortForm()) + "(\"" + iri.getShortForm() + "\"),");
        }
    }

    /**
     * Conservative conversion of a local name into an ALL_CAPS enum constant:
     * - splits camelCase boundaries
     * - replaces non-alphanumeric with '_'
     * - collapses multiple underscores
     * - uppercases
     */
    public static String toEnumConstantName(final String localName) {
        Objects.requireNonNull(localName, "localName must not be null");
        if (localName.isBlank()) {
            return "_";
        }

        // Insert underscores between camelCase boundaries: "hasModelItem" -> "has_Model_Item"
        String s = localName.replaceAll("([a-z0-9])([A-Z])", "$1_$2");

        // Replace non-alphanumeric with underscores
        s = s.replaceAll("[^A-Za-z0-9_]", "_");

        // Collapse repeated underscores
        s = s.replaceAll("_+", "_");

        // Trim underscores
        s = s.replaceAll("^_+", "").replaceAll("_+$", "");

        if (s.isBlank()) {
            s = "_";
        }

        return s.toUpperCase();
    }

    /**
     * Returns a tabulated list of individuals, their classes, object properties, data properties and annotations.
     * Each row in the returned list corresponds to an individual and contains the following columns:
     * 0: Individual IRI (short form, prefixed with '#')
     * 1: Classes (short forms, separated by '; ')
     * 2: Object Properties and their values (formatted as '#ObjectProp: #ObjectValue; ', separated by '; ')
     * 3: Data Properties and their values (formatted as '#DataProp: DataValue; ', separated by '; ')
     * 4: Annotations and their values (formatted as '#AnnotationProp: AnnotationValue; ', separated by '; ')
     * 
     * @param imports The import settings to use when querying individuals
     * @return A list of string arrays, each representing a row in the tabulated output
     */
    public List<String[]> getTabulatedIndividuals(Imports imports) {
        final List<String[]> table = new java.util.ArrayList<>();
		for (IRI individual : individualQuery.getIndividualsInSignature(imports)) {
            String[] row = new String[5];
            table.add(row);
            final Set<IRI> types = individualQuery.getAssertedTypes(individual, true, imports);
            row[0] = individual.getShortForm();
            row[1] = types.stream().map(IRI::getShortForm).collect(Collectors.joining("; "));
            Map<IRI, Set<IRI>> objProps = individualQuery.getAllObjectPropertyValues(individual, imports);
            StringBuilder objPropsStr = new StringBuilder();
            for (Map.Entry<IRI, Set<IRI>> entry : objProps.entrySet())
                for (IRI value : entry.getValue())
                    objPropsStr.append(entry.getKey().getShortForm() + ":" + value.getShortForm() + "; ");
            row[2] = objPropsStr.toString();
            StringBuilder dataPropsStr = new StringBuilder();
            Map<IRI, Set<OWLLiteral>> dataProps = individualQuery.getAllDataPropertyValues(individual, imports);
            for (Map.Entry<IRI, Set<OWLLiteral>> entry : dataProps.entrySet())
                for (OWLLiteral value : entry.getValue())
                    dataPropsStr.append(entry.getKey().getShortForm() + ":\"" + value.getLiteral() + "\"; ");
            row[3] = dataPropsStr.toString();
            StringBuilder annotationsStr = new StringBuilder();
            Map<IRI, Set<OWLAnnotationValue>> annotations = individualQuery.getAllAnnotationValues(individual, imports);
            for (Map.Entry<IRI, Set<OWLAnnotationValue>> entry : annotations.entrySet())
                for (OWLAnnotationValue value : entry.getValue())
                    annotationsStr.append(entry.getKey().getShortForm() + ":\"" + value.toString() + "\"; ");
            row[4] = annotationsStr.toString();
		}
        return table;
    }
        
	/**
	 * Prints a tabulated list of individuals, their classes, object properties and data properties.
	 * The output is formatted as:
	 * Individual    Class    ObjectProperties    DataProperties
	 * Individual1  Class1  ObjectProp1: ObjectValue1; ObjectProp2: ObjectValue2;    DataProp1: DataValue1; DataProp2: DataValue2;
	 */
	public void printTabulatedIndividuals(Imports imports) {
        final List<String[]> table = getTabulatedIndividuals(imports);
		out.print("Individual\tClass\tObjectProperties\tDataProperties\tAnnotations\n");

        for (String[] row : table) {
            out.println(row[0] + "\t" + row[1] + "\t" + row[2] + "\t" + row[3] + "\t" + row[4]);
        }
	}

	/** 
	 * Prints the list of properties of the specified individual. This list includes the class it belongs to, the object properties and the data properties.
	 * @param individualIRI An individual in the ontology
	 * @param sep A separator to use between the property name and its value
	 * @return a list of strings representing the properties of the specified individual. This list includes the class it belongs to, the object properties and the data properties.
	 * Each string in the list is formatted as "PROPERTY_NAME" + sep + "PROPERTY_VALUE".
	 */
	public void prettyPrintIndividualProperties(IRI individualIRI, String sep) {
		for (IRI clazz : individualQuery.getAssertedTypes(individualIRI, false, Imports.INCLUDED)) {
		    out.println(individualIRI.getShortForm() + sep + "SUBCLASS_OF" + sep + clazz.getShortForm());
		}
        Map<IRI, Set<IRI>> objProps = individualQuery.getAllObjectPropertyValues(individualIRI, Imports.INCLUDED);
        for (Map.Entry<IRI, Set<IRI>> entry : objProps.entrySet())
            for (IRI value : entry.getValue())
                out.println(individualIRI.getShortForm() + sep + entry.getKey().getShortForm() + sep + value.getShortForm());
        Map<IRI, Set<OWLLiteral>> dataProps = individualQuery.getAllDataPropertyValues(individualIRI, Imports.INCLUDED);
        for (Map.Entry<IRI, Set<OWLLiteral>> entry : dataProps.entrySet())
            for (OWLLiteral value : entry.getValue())
                out.println(individualIRI.getShortForm() + sep + entry.getKey().getShortForm() + sep + value.getLiteral());
	}
	
	/**
	 * Prints a list of individuals, their classes, object properties and data properties.
	 */
	public void prettyPrintIndividuals() {
        for (IRI individual : individualQuery.getIndividualsInSignature(Imports.EXCLUDED)) {
            prettyPrintIndividualProperties(individual,  "\t");
        }
	}

	/**
	 * Prints a list of individuals of the specified class, their properties and values.
	 * @param classIRI The IRI of the class whose individuals will be printed
	 */
	public void prettyPrintIndividuals(String classIRI) {
        Objects.requireNonNull(classIRI, "classIRI must not be null");
        final Set<IRI> individualIris = individualQuery.getIndividualsOfClass(IRI.create(classIRI), true, Imports.INCLUDED);
        for (IRI individual : individualIris) {
            prettyPrintIndividualProperties(individual,  "\t");
        }
	}
}
