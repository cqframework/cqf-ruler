package org.opencds.cqf.ruler.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.opencds.cqf.ruler.utility.Searches;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.provider.ValueSetOperationProvider;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;

@SpringBootTest(classes = { JpaFhirRetrieveProviderIT.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class JpaFhirRetrieveProviderIT extends DaoIntegrationTest {

	@Autowired
	DaoRegistry daoRegistry;

	@Autowired
	SearchParameterResolver searchParameterResolver;

	@Autowired
	ModelResolver modelResolver;

	@Autowired
	ValueSetOperationProvider valueSetOperationProvider;

	private JpaFhirRetrieveProvider createProvider() {
		JpaFhirRetrieveProvider jfrp = new JpaFhirRetrieveProvider(daoRegistry, searchParameterResolver,
				new SystemRequestDetails());
		jfrp.setModelResolver(modelResolver);

		return jfrp;
	}

	@Test
	public void testReadPatient() {
		this.update(patient());

		JpaFhirRetrieveProvider jfrp = this.createProvider();

		Iterator<Object> result = jfrp
				.retrieve("Patient", "id", "ABC", "Patient", null, null, null, null, null, null, null, null).iterator();

		assertTrue(result.hasNext());
		Patient patient = (Patient) result.next();

		assertEquals(patient().getIdElement().getIdPart(), patient.getIdElement().getIdPart());
	}

	@Test
	public void testReadObservation() {
		this.update(patient());
		this.update(observation());

		JpaFhirRetrieveProvider jfrp = this.createProvider();

		Iterator<Object> result = jfrp
				.retrieve("Patient", "subject", "ABC", "Observation", null, null, null, null, null, null, null, null)
				.iterator();

		assertTrue(result.hasNext());
		Observation observation = (Observation) result.next();

		assertEquals(observation().getIdElement().getIdPart(), observation.getIdElement().getIdPart());
	}

	@Test
	public void testReadObservationByValueSet() {
		this.update(patient());
		this.update(observation());
		this.update(valueSet());

		JpaFhirRetrieveProvider jfrp = this.createProvider();

		Iterator<Object> result = jfrp
				.retrieve("Patient", "subject", "ABC", "Observation", null, "code", null, "http://test.com/ValueSet/123",
						null, null, null, null)
				.iterator();

		assertTrue(result.hasNext());
		Observation observation = (Observation) result.next();

		assertEquals(observation().getIdElement().getIdPart(), observation.getIdElement().getIdPart());
	}

	@Test
	public void testSearchObservationByValueSet() {
		this.update(valueSet());
		this.update(observation());

		this.search(Observation.class, Searches.byParam("code",
				new TokenParam("http://test.com/ValueSet/123").setModifier(TokenParamModifier.IN))).single();
	}

	private Patient patient() {
		return newResource(Patient.class, "ABC");
	}

	private Observation observation() {
		return newResource(Observation.class, "DEF")
				.setSubject(new Reference(patient()))
				.setCode(code("test.com", "ABC", "test code"));
	}

	private ValueSet valueSet() {
		return newResource(ValueSet.class, "GHI")
				.setUrl("http://test.com/ValueSet/123")
				.setCompose(compose().addInclude(include(code("test.com", "ABC", "test code"))));
	}

	private CodeableConcept code(String system, String code, String display) {
		return new CodeableConcept()
				.addCoding(new Coding(system, code, display));
	}

	private ValueSetExpansionComponent expansion() {
		return new ValueSetExpansionComponent();
	}

	private ValueSetExpansionContainsComponent contains(CodeableConcept codeableConcept) {
		ValueSetExpansionContainsComponent vsecc = new ValueSetExpansionContainsComponent();
		vsecc.setCode(codeableConcept.getCodingFirstRep().getCode());
		vsecc.setSystem(codeableConcept.getCodingFirstRep().getSystem());
		vsecc.setDisplay(codeableConcept.getCodingFirstRep().getDisplay());

		return vsecc;
	}

	private ValueSetComposeComponent compose() {
		return new ValueSetComposeComponent();
	}

	private ConceptSetComponent include(CodeableConcept codeableConcept) {
		ConceptSetComponent csc = new ConceptSetComponent();
		ConceptReferenceComponent cfc = csc.addConcept();
		csc.setSystem(codeableConcept.getCodingFirstRep().getSystem());
		cfc.setCode(codeableConcept.getCodingFirstRep().getCode());
		cfc.setDisplay(codeableConcept.getCodingFirstRep().getDisplay());

		return csc;
	}

}
