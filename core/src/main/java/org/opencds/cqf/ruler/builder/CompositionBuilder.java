package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu2016may.model.CodeableConcept;
import org.hl7.fhir.dstu2016may.model.Coding;
import org.hl7.fhir.dstu2016may.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Enumerations.CompositionStatus;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.valueset.CompositionStatusEnum;

public class CompositionBuilder extends ResourceBuilder {

	protected <T extends IBaseResource> CompositionBuilder(Class<T> theCompositionClass,
			CompositionSettings theCompositionSettings) {
		super(theCompositionClass, theCompositionSettings);
	}

	/**
	 * Creates the appropriate IBaseComposition for a given bundle class
	 * 
	 * @param theCompositionClass    the FHIR version to generate a Composition for
	 * @param theCompositionSettings the settings for the Composition
	 * @return the Composition
	 */
	public static <T extends IBaseResource> T build(Class<T> theCompositionClass,
			CompositionSettings theCompositionSettings) {
		checkNotNull(theCompositionClass, theCompositionSettings);

		CompositionBuilder myBuilder = new CompositionBuilder(theCompositionClass,
				theCompositionSettings);

		@SuppressWarnings("unchecked")
		T myComposition = (T) myBuilder.getResource();
		return myComposition;
	}

	protected IBaseResource getResource() {
		return myResource;
	}

	protected CompositionSettings getSettings() {
		return (CompositionSettings) myResourceSettings;
	}

	private CodingSettings codingSettings;

	private CodingSettings getTypeSettings() {
		if (codingSettings == null) {
			codingSettings = getSettings().type().getCodingSettings().toArray(new CodingSettings[0])[0];
		}
		return codingSettings;
	}

	@Override
	protected void initializeDstu2() {
		super.initializeDstu2();
		ca.uhn.fhir.model.dstu2.resource.Composition myComposition = (ca.uhn.fhir.model.dstu2.resource.Composition) getResource();

		List<ResourceReferenceDt> myAuthor = new ArrayList<>();
		myAuthor.add(new ResourceReferenceDt(getSettings().author()));

		myComposition.setStatus(CompositionStatusEnum.forCode(getSettings().status()))
				.setSubject(new ResourceReferenceDt(getSettings().subject()))
				.setTitle(getSettings().title())
				.setType(new CodeableConceptDt()
						.addCoding(new CodingDt()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.setAuthor(myAuthor)
				.setCustodian(new ResourceReferenceDt(getSettings().custodian()));
	}

	@Override
	protected void initializeDstu2_1() {
		super.initializeDstu2_1();
		org.hl7.fhir.dstu2016may.model.Composition myComposition = (org.hl7.fhir.dstu2016may.model.Composition) getResource();

		myComposition
				.setStatus(org.hl7.fhir.dstu2016may.model.Composition.CompositionStatus.fromCode(getSettings().status()))
				.setSubject(new Reference(getSettings().subject()))
				.setTitle(getSettings().title())
				.setType(new CodeableConcept()
						.addCoding(new Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new Reference(getSettings().author()))
				.setCustodian(new Reference(getSettings().custodian()));
	}

	@Override
	protected void initializeDstu2_HL7Org() {
		super.initializeDstu2_HL7Org();
		org.hl7.fhir.dstu2.model.Composition myComposition = (org.hl7.fhir.dstu2.model.Composition) getResource();

		myComposition
				.setStatus(org.hl7.fhir.dstu2.model.Composition.CompositionStatus.fromCode(getSettings().status()))
				.setSubject(new org.hl7.fhir.dstu2.model.Reference(getSettings().subject()))
				.setTitle(getSettings().title())
				.setType(new org.hl7.fhir.dstu2.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.dstu2.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.dstu2.model.Reference(getSettings().author()))
				.setCustodian(new org.hl7.fhir.dstu2.model.Reference(getSettings().custodian()));
	}

	@Override
	protected void initializeDstu3() {
		super.initializeDstu3();
		org.hl7.fhir.dstu3.model.Composition myComposition = (org.hl7.fhir.dstu3.model.Composition) getResource();

		myComposition
				.setStatus(org.hl7.fhir.dstu3.model.Composition.CompositionStatus.fromCode(getSettings().status()))
				.setSubject(new org.hl7.fhir.dstu3.model.Reference(getSettings().subject()))
				.setTitle(getSettings().title())
				.setType(new org.hl7.fhir.dstu3.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.dstu3.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.dstu3.model.Reference(getSettings().author()))
				.setCustodian(new org.hl7.fhir.dstu3.model.Reference(getSettings().custodian()));
	}

	@Override
	protected void initializeR4() {
		super.initializeR4();
		org.hl7.fhir.r4.model.Composition myComposition = (org.hl7.fhir.r4.model.Composition) getResource();

		myComposition
				.setStatus(org.hl7.fhir.r4.model.Composition.CompositionStatus.fromCode(getSettings().status()))
				.setSubject(new org.hl7.fhir.r4.model.Reference(getSettings().subject()))
				.setTitle(getSettings().title())
				.setType(new org.hl7.fhir.r4.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.r4.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.r4.model.Reference(getSettings().author()))
				.setCustodian(new org.hl7.fhir.r4.model.Reference(getSettings().custodian()));

	}

	@Override
	protected void initializeR5() {
		super.initializeR5();
		org.hl7.fhir.r5.model.Composition myComposition = (org.hl7.fhir.r5.model.Composition) getResource();

		myComposition
				.setStatus(CompositionStatus.fromCode(getSettings().status()))
				.setSubject(new org.hl7.fhir.r5.model.Reference(getSettings().subject()))
				.setTitle(getSettings().title())
				.setType(new org.hl7.fhir.r5.model.CodeableConcept()
						.addCoding(new org.hl7.fhir.r5.model.Coding()
								.setSystem(getTypeSettings().getSystem())
								.setCode(getTypeSettings().getCode())
								.setDisplay(getTypeSettings().getDisplay())))
				.addAuthor(new org.hl7.fhir.r5.model.Reference(getSettings().author()))
				.setCustodian(new org.hl7.fhir.r5.model.Reference(getSettings().custodian()));
	}
}
