package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.ruler.utility.FhirVersions;
import org.opencds.cqf.ruler.utility.Resources;

public abstract class BackboneElementBuilder<SELF, T extends IBaseBackboneElement> {

	private final Class<T> myResourceClass;

	private String myId = UUID.randomUUID().toString();

	private List<Pair<String, CodeableConceptSettings>> myExtension;
	private List<Pair<String, CodeableConceptSettings>> myModifierExtension;

	protected BackboneElementBuilder(Class<T> theResourceClass) {
		checkNotNull(theResourceClass);
		myResourceClass = theResourceClass;
	}

	protected BackboneElementBuilder(Class<T> theResourceClass, String theId) {
		this(theResourceClass);
		checkNotNull(theId);

		myId = theId;
	}

	public T build() {
		T backboneElement = Resources
				.newBackboneElement(myResourceClass);

		switch (FhirVersions.forClass(myResourceClass)) {
			case DSTU2:
				initializeDstu2(backboneElement);
				break;
			case DSTU2_1:
				initializeDstu2_1(backboneElement);
				break;
			case DSTU2_HL7ORG:
				initializeDstu2_HL7Org(backboneElement);
				break;
			case DSTU3:
				initializeDstu3(backboneElement);
				break;
			case R4:
				initializeR4(backboneElement);
				break;
			case R5:
				initializeR5(backboneElement);
				break;
			default:
				throw new IllegalArgumentException(
						String.format("ResourceBuilder.initializeResource does not support FHIR version %s",
								FhirVersions.forClass(myResourceClass).getFhirVersionString()));
		}

		return backboneElement;
	}

	private void addExtension(Pair<String, CodeableConceptSettings> theExtension) {
		if (myExtension == null) {
			myExtension = new ArrayList<>();
		}
		myExtension.add(theExtension);
	}

	private List<Pair<String, CodeableConceptSettings>> getExtensions() {
		if (myExtension == null) {
			return Collections.emptyList();
		}
		return myExtension;
	}

	protected String getId() {
		return myId;
	}

	private void addModifierExtension(Pair<String, CodeableConceptSettings> theModifierExtension) {
		if (myModifierExtension == null) {
			myModifierExtension = new ArrayList<>();
		}
		myModifierExtension.add(theModifierExtension);
	}

	private List<Pair<String, CodeableConceptSettings>> getModifierExtensions() {
		if (myModifierExtension == null) {
			return Collections.emptyList();
		}
		return myModifierExtension;
	}

	public SELF withId(String theId) {
		checkNotNull(theId);

		myId = theId;

		return self();
	}

	public SELF withExtension(Pair<String, CodeableConceptSettings> theExtension) {
		checkNotNull(theExtension);

		addExtension(theExtension);

		return self();
	}

	public SELF withModifierExtension(Pair<String, CodeableConceptSettings> theModifierExtension) {
		checkNotNull(theModifierExtension);

		addModifierExtension(theModifierExtension);

		return self();
	}

	@SuppressWarnings("unchecked")
	protected SELF self() {
		return (SELF) this;
	}

	// TODO: these are duplicated in DomainResourceBuilder. Refactor and reuse.
	protected void initializeDstu2(T theResource) {
		getExtensions()
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

		getModifierExtensions()
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

	protected void initializeDstu2_1(T theResource) {
		getExtensions()
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

		getModifierExtensions()
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

	protected void initializeDstu2_HL7Org(T theResource) {
		getExtensions()
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

		getModifierExtensions()
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

	protected void initializeDstu3(T theResource) {
		getExtensions()
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

		getModifierExtensions()
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

	protected void initializeR4(T theResource) {
		getExtensions()
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

		getModifierExtensions()
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

	protected void initializeR5(T theResource) {
		getExtensions()
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

		getModifierExtensions()
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
