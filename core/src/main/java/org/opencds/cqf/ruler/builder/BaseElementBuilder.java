package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

import ca.uhn.fhir.model.api.BaseElement;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;

public abstract class BaseElementBuilder<SELF, T extends IBaseResource>
		extends ResourceBuilder<SELF, T> {

	protected List<Pair<String, CodeableConceptSettings>> myExtension;
	protected List<Pair<String, CodeableConceptSettings>> myModifierExtension;

	protected BaseElementBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	protected BaseElementBuilder(Class<T> theResourceClass, String theId) {
		super(theResourceClass, theId);
	}

	public SELF withExtension(Pair<String, CodeableConceptSettings> theExtension) {
		List<Pair<String, CodeableConceptSettings>> extension = new ArrayList<>();
		extension.add(theExtension);
		withExtension(extension);

		return self();
	}

	public SELF withExtension(List<Pair<String, CodeableConceptSettings>> theExtension) {
		checkNotNull(theExtension);
		myExtension = theExtension;

		return self();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected SELF self() {
		return (SELF) this;
	}

	@Override
	protected void initializeDstu2(T theResource) {
		super.initializeDstu2(theResource);
		BaseElement baseElement = (BaseElement) theResource;

		myExtension
				.forEach(extension -> extension.getValue().getCodingSettings()
						.forEach(coding -> baseElement.addUndeclaredExtension(false, extension.getKey(),
								new CodeableConceptDt(coding.getSystem(), coding.getCode()))));
	}

	@Override
	protected void initializeDstu2_1(T theResource) {
		super.initializeDstu2_1(theResource);

	}

	@Override
	protected void initializeDstu2_HL7Org(T theResource) {
		super.initializeDstu2_HL7Org(theResource);

	}

	@Override
	protected void initializeDstu3(T theResource) {
		super.initializeDstu3(theResource);

	}

	@Override
	protected void initializeR4(T theResource) {
		super.initializeR4(theResource);
		BaseElement baseElement = (BaseElement) theResource;

		myExtension
				.forEach(extension -> extension.getValue().getCodingSettings()
						.forEach(coding -> baseElement.addUndeclaredExtension(false, extension.getKey(),
								new CodeableConcept()
										.addCoding(new Coding()
												.setSystem(coding.getSystem())
												.setCode(coding.getCode())
												.setDisplay(coding.getDisplay())))));
	}

	@Override
	protected void initializeR5(T theResource) {
		super.initializeR5(theResource);

	}
}
