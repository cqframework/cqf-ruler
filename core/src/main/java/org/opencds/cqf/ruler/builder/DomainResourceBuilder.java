package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IDomainResource;

public abstract class DomainResourceBuilder<SELF, T extends IDomainResource>
		extends ResourceBuilder<SELF, T> {

	private List<Pair<String, CodeableConceptSettings>> myExtension;
	private List<Pair<String, CodeableConceptSettings>> myModifierExtension;

	protected DomainResourceBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	protected DomainResourceBuilder(Class<T> theResourceClass, String theId) {
		super(theResourceClass, theId);
	}

	protected List<Pair<String, CodeableConceptSettings>> getExtension() {
		if (myExtension == null) {
			return Collections.emptyList();
		}
		return myExtension;
	}

	protected List<Pair<String, CodeableConceptSettings>> getModifierExtension() {
		if (myModifierExtension == null) {
			return Collections.emptyList();
		}
		return myModifierExtension;
	}

	public SELF withExtension(Pair<String, CodeableConceptSettings> theExtension) {
		checkNotNull(theExtension);

		if (myExtension == null) {
			myExtension = new ArrayList<>();
		}
		myExtension.add(theExtension);

		return self();
	}

	public SELF withModifierExtension(Pair<String, CodeableConceptSettings> theModifierExtension) {
		checkNotNull(theModifierExtension);

		if (myModifierExtension == null) {
			myModifierExtension = new ArrayList<>();
		}
		myModifierExtension.add(theModifierExtension);

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

		getExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt codeableConcept = new ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt()
									.addCoding(new ca.uhn.fhir.model.dstu2.composite.CodingDt()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> extension = theResource.addModifierExtension();
							extension.setUrl(extensionSetting.getKey());
							extension.setValue(codeableConcept);
						}));

		getModifierExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt codeableConcept = new ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt()
									.addCoding(new ca.uhn.fhir.model.dstu2.composite.CodingDt()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
							modifierExtension.setUrl(extensionSetting.getKey());
							modifierExtension.setValue(codeableConcept);
						}));
	}

	@Override
	protected void initializeDstu2_1(T theResource) {
		super.initializeDstu2_1(theResource);

		getExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.dstu2016may.model.CodeableConcept codeableConcept = new org.hl7.fhir.dstu2016may.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.dstu2016may.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> extension = theResource.addExtension();
							extension.setUrl(extensionSetting.getKey());
							extension.setValue(codeableConcept);
						}));

		getModifierExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.dstu2016may.model.CodeableConcept codeableConcept = new org.hl7.fhir.dstu2016may.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.dstu2016may.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
							modifierExtension.setUrl(extensionSetting.getKey());
							modifierExtension.setValue(codeableConcept);
						}));

	}

	@Override
	protected void initializeDstu2_HL7Org(T theResource) {
		super.initializeDstu2_HL7Org(theResource);

		getExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.dstu2.model.CodeableConcept codeableConcept = new org.hl7.fhir.dstu2.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.dstu2.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> extension = theResource.addExtension();
							extension.setUrl(extensionSetting.getKey());
							extension.setValue(codeableConcept);
						}));

		getModifierExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.dstu2.model.CodeableConcept codeableConcept = new org.hl7.fhir.dstu2.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.dstu2.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
							modifierExtension.setUrl(extensionSetting.getKey());
							modifierExtension.setValue(codeableConcept);
						}));
	}

	@Override
	protected void initializeDstu3(T theResource) {
		super.initializeDstu3(theResource);

		getExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept = new org.hl7.fhir.dstu3.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.dstu3.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> extension = theResource.addExtension();
							extension.setUrl(extensionSetting.getKey());
							extension.setValue(codeableConcept);
						}));

		getModifierExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept = new org.hl7.fhir.dstu3.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.dstu3.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
							modifierExtension.setUrl(extensionSetting.getKey());
							modifierExtension.setValue(codeableConcept);
						}));
	}

	@Override
	protected void initializeR4(T theResource) {
		super.initializeR4(theResource);

		getExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.r4.model.CodeableConcept codeableConcept = new org.hl7.fhir.r4.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.r4.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> extension = theResource.addExtension();
							extension.setUrl(extensionSetting.getKey());
							extension.setValue(codeableConcept);
						}));

		getModifierExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.r4.model.CodeableConcept codeableConcept = new org.hl7.fhir.r4.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.r4.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
							modifierExtension.setUrl(extensionSetting.getKey());
							modifierExtension.setValue(codeableConcept);
						}));
	}

	@Override
	protected void initializeR5(T theResource) {
		super.initializeR5(theResource);

		getExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.r5.model.CodeableConcept codeableConcept = new org.hl7.fhir.r5.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.r5.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> extension = theResource.addExtension();
							extension.setUrl(extensionSetting.getKey());
							extension.setValue(codeableConcept);
						}));

		getModifierExtension()
				.forEach(extensionSetting -> extensionSetting.getValue().getCodingSettings()
						.forEach(coding -> {
							org.hl7.fhir.r5.model.CodeableConcept codeableConcept = new org.hl7.fhir.r5.model.CodeableConcept()
									.addCoding(new org.hl7.fhir.r5.model.Coding()
											.setSystem(coding.getSystem())
											.setCode(coding.getCode())
											.setDisplay(coding.getDisplay()));
							IBaseExtension<?, ?> modifierExtension = theResource.addModifierExtension();
							modifierExtension.setUrl(extensionSetting.getKey());
							modifierExtension.setValue(codeableConcept);
						}));
	}
}
