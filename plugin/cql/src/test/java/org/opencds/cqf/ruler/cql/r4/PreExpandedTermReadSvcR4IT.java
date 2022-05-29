package org.opencds.cqf.ruler.cql.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.opencds.cqf.ruler.cql.JpaFhirRetrieveProviderIT;
import org.opencds.cqf.ruler.test.DaoIntegrationTest;
import org.opencds.cqf.ruler.utility.Searches;
import org.opencds.cqf.ruler.utility.TypedBundleProvider;
import org.springframework.boot.test.context.SpringBootTest;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;

@SpringBootTest(classes = { JpaFhirRetrieveProviderIT.class }, properties = { "hapi.fhir.fhir_version=r4" })
public class PreExpandedTermReadSvcR4IT extends DaoIntegrationTest {

	@Test
	public void testSearchObservationByComposedValueSet() {
		this.update(composedValueSet());
		this.update(patient());
		this.update(observation());

		TypedBundleProvider<Observation> obs = this.search(Observation.class, Searches.byParam("code",
				new TokenParam("http://test.com/ValueSet/123").setModifier(TokenParamModifier.IN)));

		assertNotNull(obs.single());
	}

	@Test
	public void testSearchObservationByExpandedValueSet() {
		this.update(expandedValueSet());
		this.update(patient());
		this.update(observation());

		TypedBundleProvider<Observation> obs = this.search(Observation.class, Searches.byParam("code",
				new TokenParam("http://test.com/ValueSet/123").setModifier(TokenParamModifier.IN)));

		assertNotNull(obs.single());
	}

	private Patient patient() {
		return newResource(Patient.class, "ABC");
	}

	private Observation observation() {
		return newResource(Observation.class, "DEF")
				.setSubject(new Reference(patient()))
				.setCode(code("test.com", "ABC", "test code"));
	}

	private ValueSet composedValueSet() {
		return newResource(ValueSet.class, "GHI")
				.setUrl("http://test.com/ValueSet/123")
				.setCompose(compose().addInclude(include(code("test.com", "ABC", "test code"))));
	}

	private ValueSet expandedValueSet() {
		return newResource(ValueSet.class, "GHI")
				.setUrl("http://test.com/ValueSet/123")
				.setExpansion(expansion().addContains(contains(code("test.com", "ABC", "test code"))));
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
