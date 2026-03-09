package es.ull.simulation.ontology;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;

import java.util.Set;

public class ReasonedQueryTest {
    private OWLOntologyWrapper wrap;

    @BeforeEach
    public void setUp() {
        wrap = OntologyTestResources.createDefaultOwlOntologyWrapper();
    }


    @Test
    public void testGetTypesInferredWithNullIri() {
        assertThrows(NullPointerException.class, () -> wrap.getReasonedQuery().getTypesInferred(null, false));
    }

    @Test
    public void testIsInstanceOfInferredWithNullIndividualIri() {
        assertThrows(NullPointerException.class, () -> wrap.getReasonedQuery().isInstanceOfInferred(null, IRI.create("http://test.org/Class"), false));
    }

    @Test
    public void testIsInstanceOfInferredWithNullClassIri() {
        assertThrows(NullPointerException.class, () -> wrap.getReasonedQuery().isInstanceOfInferred(IRI.create("http://test.org/Individual"), null, false));
    }

    @Test
    public void testGetIndividualsOfClassInferredWithNullClassIri() {
        assertThrows(NullPointerException.class, () -> wrap.getReasonedQuery().getIndividualsOfClassInferred(null, false));
    }

    @Test
    public void testGetSuperClassesInferredWithNullClassIri() {
        assertThrows(NullPointerException.class, () -> wrap.getReasonedQuery().getSuperClassesInferred(null, false));
    }

    @Test
    public void testGetSubClassesInferredWithNullClassIri() {
        assertThrows(NullPointerException.class, () -> wrap.getReasonedQuery().getSubClassesInferred(null, false));
    }

    @Test
    public void testGetSubClassesInferred() {
        OWLOntologyWrapper wrap = OntologyTestResources.createDefaultOwlOntologyWrapper();
        final IRI classModelElementIRI = wrap.toIRI(OntologyTestResources.TEST_MODEL_ELEMENT_CLASS);
        final IRI classDiseaseIRI = wrap.toIRI(OntologyTestResources.TEST_DISEASE_CLASS);
        final IRI classRareDiseaseIRI = wrap.toIRI(OntologyTestResources.TEST_RARE_DISEASE_CLASS);

        Set<IRI> result = wrap.getReasonedQuery().getSubClassesInferred(classModelElementIRI, false);
        assertNotNull(result);
        assertTrue(result.size() == 2);
        assertTrue(result.contains(classDiseaseIRI));
        assertTrue(result.contains(classRareDiseaseIRI));
        result = wrap.getReasonedQuery().getSubClassesInferred(classModelElementIRI, true);
        assertNotNull(result);
        assertTrue(result.size() == 1);
        assertTrue(result.contains(classDiseaseIRI));
        result = wrap.getReasonedQuery().getSubClassesInferred(classDiseaseIRI, false);
        assertNotNull(result);
        assertTrue(result.size() == 1);
        assertTrue(result.contains(classRareDiseaseIRI));
        result = wrap.getReasonedQuery().getSubClassesInferred(classRareDiseaseIRI, false);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSuperClassesInferred() {
        OWLOntologyWrapper wrap = OntologyTestResources.createDefaultOwlOntologyWrapper();
        final IRI classModelElementIRI = wrap.toIRI(OntologyTestResources.TEST_MODEL_ELEMENT_CLASS);
        final IRI classDiseaseIRI = wrap.toIRI(OntologyTestResources.TEST_DISEASE_CLASS);
        final IRI classRareDiseaseIRI = wrap.toIRI(OntologyTestResources.TEST_RARE_DISEASE_CLASS);

        Set<IRI> result = wrap.getReasonedQuery().getSuperClassesInferred(classRareDiseaseIRI, false);
        assertNotNull(result);
        assertTrue(result.size() == 2);
        assertTrue(result.contains(classDiseaseIRI));
        assertTrue(result.contains(classModelElementIRI));
        result = wrap.getReasonedQuery().getSuperClassesInferred(classRareDiseaseIRI, true);
        assertTrue(result.size() == 1);
        assertTrue(result.contains(classDiseaseIRI));
        assertNotNull(result);
        result = wrap.getReasonedQuery().getSuperClassesInferred(classDiseaseIRI, false);
        assertTrue(result.size() == 1);
        assertNotNull(result);
        assertTrue(result.contains(classModelElementIRI));
        result = wrap.getReasonedQuery().getSuperClassesInferred(classModelElementIRI, false);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
