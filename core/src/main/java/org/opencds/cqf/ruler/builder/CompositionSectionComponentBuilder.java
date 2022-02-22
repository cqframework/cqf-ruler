package org.opencds.cqf.ruler.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.dstu2016may.model.Narrative;
import org.hl7.fhir.dstu2016may.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

import ca.uhn.fhir.model.dstu2.composite.NarrativeDt;
import ca.uhn.fhir.model.dstu2.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.primitive.XhtmlDt;

public class CompositionSectionComponentBuilder<T extends IBaseBackboneElement>
		extends BackboneElementBuilder<CompositionSectionComponentBuilder<T>, T> {

	private String myTitle;
	private String myFocus;
	private List<String> myEntry;
	private NarrativeSettings myText;

	public CompositionSectionComponentBuilder(Class<T> theResourceClass) {
		super(theResourceClass);
	}

	public CompositionSectionComponentBuilder(Class<T> theResourceClass, String theId) {
		super(theResourceClass, theId);
	}

	public CompositionSectionComponentBuilder(Class<T> theResourceClass, String theId, String theFocus,
			String theEntry) {
		this(theResourceClass, theId);
		checkNotNull(theFocus, theEntry);

		myFocus = theFocus;
		addEntry(theEntry);
	}

	private void addEntry(String entry) {
		if (myEntry == null) {
			myEntry = new ArrayList<>();
		}

		myEntry.add(entry);
	}

	private List<String> getEntries() {
		if (myEntry == null) {
			return Collections.emptyList();
		}

		return myEntry;
	}

	public CompositionSectionComponentBuilder<T> withTitle(String theTitle) {
		checkNotNull(theTitle);

		myTitle = theTitle;

		return this;
	}

	public CompositionSectionComponentBuilder<T> withFocus(String theFocus) {
		checkNotNull(theFocus);

		myFocus = theFocus;

		return this;
	}

	public CompositionSectionComponentBuilder<T> withEntry(String theEntry) {
		checkNotNull(theEntry);

		addEntry(theEntry);

		return this;
	}

	public CompositionSectionComponentBuilder<T> withText(NarrativeSettings theText) {
		checkNotNull(theText);

		myText = theText;

		return this;
	}

	@Override
	public T build() {
		checkNotNull(myFocus, myEntry);
		checkArgument(!myEntry.isEmpty());

		return super.build();
	}

	@Override
	protected void initializeDstu2(T theResource) {
		super.initializeDstu2(theResource);
		ca.uhn.fhir.model.dstu2.resource.Composition.Section section = (ca.uhn.fhir.model.dstu2.resource.Composition.Section) theResource;

		section.setTitle(myTitle).setElementSpecificId(getId());
		getEntries().forEach(entry -> section.addEntry().setReference(entry));
		if (myText != null) {
			section.setText(
					new NarrativeDt(new XhtmlDt(myText.getText()), NarrativeStatusEnum.valueOf(myText.getStatus())));
		}
		// no focus
	}

	@Override
	protected void initializeDstu2_1(T theResource) {
		super.initializeDstu2_1(theResource);
		org.hl7.fhir.dstu2016may.model.Composition.SectionComponent section = (org.hl7.fhir.dstu2016may.model.Composition.SectionComponent) theResource;

		section.setTitle(myTitle).setId(getId());
		getEntries().forEach(entry -> section.addEntry(new Reference(entry)));
		if (myText != null) {
			Narrative narrative = new Narrative();
			narrative.setStatusAsString(myText.getStatus());
			narrative.setDivAsString(myText.getText());
			section.setText(narrative);
		}
		// no focus
	}

	@Override
	protected void initializeDstu2_HL7Org(T theResource) {
		super.initializeDstu2_HL7Org(theResource);
		org.hl7.fhir.dstu2.model.Composition.SectionComponent section = (org.hl7.fhir.dstu2.model.Composition.SectionComponent) theResource;

		section.setTitle(myTitle).setId(getId());
		getEntries().forEach(entry -> section.addEntry(new org.hl7.fhir.dstu2.model.Reference(entry)));
		if (myText != null) {
			org.hl7.fhir.dstu2.model.Narrative narrative = new org.hl7.fhir.dstu2.model.Narrative();
			narrative.setStatusAsString(myText.getStatus());
			narrative.setDivAsString(myText.getText());
			section.setText(narrative);
		}
		// no focus
	}

	@Override
	protected void initializeDstu3(T theResource) {
		super.initializeDstu3(theResource);

		org.hl7.fhir.dstu3.model.Composition.SectionComponent section = (org.hl7.fhir.dstu3.model.Composition.SectionComponent) theResource;

		section.setTitle(myTitle).setId(getId());
		getEntries().forEach(entry -> section.addEntry(new org.hl7.fhir.dstu3.model.Reference(entry)));
		if (myText != null) {
			org.hl7.fhir.dstu3.model.Narrative narrative = new org.hl7.fhir.dstu3.model.Narrative();
			narrative.setStatusAsString(myText.getStatus());
			narrative.setDivAsString(myText.getText());
			section.setText(narrative);
		}
		// no focus
	}

	@Override
	protected void initializeR4(T theResource) {
		super.initializeR4(theResource);
		org.hl7.fhir.r4.model.Composition.SectionComponent section = (org.hl7.fhir.r4.model.Composition.SectionComponent) theResource;

		section.setFocus(new org.hl7.fhir.r4.model.Reference(myFocus)).setTitle(myTitle).setId(getId());
		getEntries().forEach(entry -> section.addEntry(new org.hl7.fhir.r4.model.Reference(entry)));
		if (myText != null) {
			org.hl7.fhir.r4.model.Narrative narrative = new org.hl7.fhir.r4.model.Narrative();
			narrative.setStatusAsString(myText.getStatus());
			narrative.setDivAsString(myText.getText());
			section.setText(narrative);
		}
	}

	@Override
	protected void initializeR5(T theResource) {
		super.initializeR5(theResource);
		org.hl7.fhir.r5.model.Composition.SectionComponent section = (org.hl7.fhir.r5.model.Composition.SectionComponent) theResource;

		section.setFocus(new org.hl7.fhir.r5.model.Reference(myFocus)).setTitle(myTitle).setId(getId());
		getEntries().forEach(entry -> section.addEntry(new org.hl7.fhir.r5.model.Reference(entry)));
		if (myText != null) {
			org.hl7.fhir.r5.model.Narrative narrative = new org.hl7.fhir.r5.model.Narrative();
			narrative.setStatusAsString(myText.getStatus());
			narrative.setDivAsString(myText.getText());
			section.setText(narrative);
		}
	}
}
