package org.opencds.cqf.providers;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ContactDetail;
import org.hl7.fhir.dstu3.model.Contributor;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.MarkdownType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.ParameterDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.UsageContext;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;

public class CqfMeasure extends Measure {
    private static final long serialVersionUID = -1297192817969868337L;

    @Child(name = "parameter", type = {ParameterDefinition.class}, order=11, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Parameters defined by the library", formalDefinition="The parameter element defines parameters used by the library." )
    protected List<ParameterDefinition> parameter;

    @Child(name = "dataRequirement", type = {DataRequirement.class}, order=12, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="What data is referenced by this library", formalDefinition="Describes a set of data that must be provided in order to be able to successfully perform the computations defined by the library." )
    protected List<DataRequirement> dataRequirement;

    @Child(name = "content", type = {Attachment.class}, order=13, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Contents of the library, either embedded or referenced", formalDefinition="The content of the library as an Attachment. The content may be a reference to a url, or may be directly embedded as a base-64 string. Either way, the contentType of the attachment determines how to interpret the content." )
    protected List<Attachment> content;
    

    /**
     * @return {@link #parameter} (The parameter element defines parameters used by the library.)
    */
    public List<ParameterDefinition> getParameter() { 
        if (this.parameter == null)
            this.parameter = new ArrayList<ParameterDefinition>();
        return this.parameter;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setParameter(List<ParameterDefinition> theParameter) { 
        this.parameter = theParameter;
        return this;
    }

    public boolean hasParameter() { 
        if (this.parameter == null)
            return false;
        for (ParameterDefinition item : this.parameter)
            if (!item.isEmpty())
            return true;
        return false;
    }

    public ParameterDefinition addParameter() { //3
        ParameterDefinition t = new ParameterDefinition();
        if (this.parameter == null)
            this.parameter = new ArrayList<ParameterDefinition>();
        this.parameter.add(t);
        return t;
    }

    public CqfMeasure addParameter(ParameterDefinition t) { //3
        if (t == null)
            return this;
        if (this.parameter == null)
            this.parameter = new ArrayList<ParameterDefinition>();
        this.parameter.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #parameter}, creating it if it does not already exist
     */
    public ParameterDefinition getParameterFirstRep() { 
        if (getParameter().isEmpty()) {
            addParameter();
        }
        return getParameter().get(0);
    }

    /**
     * @return {@link #dataRequirement} (Describes a set of data that must be provided in order to be able to successfully perform the computations defined by the library.)
     */
    public List<DataRequirement> getDataRequirement() { 
        if (this.dataRequirement == null)
            this.dataRequirement = new ArrayList<DataRequirement>();
        return this.dataRequirement;
    }
  
    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setDataRequirement(List<DataRequirement> theDataRequirement) { 
        this.dataRequirement = theDataRequirement;
        return this;
    }
  
    public boolean hasDataRequirement() { 
        if (this.dataRequirement == null)
            return false;
        for (DataRequirement item : this.dataRequirement)
            if (!item.isEmpty())
            return true;
        return false;
    }
  
    public DataRequirement addDataRequirement() { //3
        DataRequirement t = new DataRequirement();
        if (this.dataRequirement == null)
            this.dataRequirement = new ArrayList<DataRequirement>();
        this.dataRequirement.add(t);
        return t;
    }
  
    public CqfMeasure addDataRequirement(DataRequirement t) { //3
        if (t == null)
            return this;
        if (this.dataRequirement == null)
            this.dataRequirement = new ArrayList<DataRequirement>();
        this.dataRequirement.add(t);
        return this;
    }
  
    /**
     * @return The first repetition of repeating field {@link #dataRequirement}, creating it if it does not already exist
     */
    public DataRequirement getDataRequirementFirstRep() { 
        if (getDataRequirement().isEmpty()) {
            addDataRequirement();
        }
        return getDataRequirement().get(0);
    }
  
    /**
     * @return {@link #content} (The content of the library as an Attachment. The content may be a reference to a url, or may be directly embedded as a base-64 string. Either way, the contentType of the attachment determines how to interpret the content.)
     */
    public List<Attachment> getContent() { 
        if (this.content == null)
            this.content = new ArrayList<Attachment>();
        return this.content;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setContent(List<Attachment> theContent) { 
        this.content = theContent;
        return this;
    }

    public boolean hasContent() { 
        if (this.content == null)
            return false;
        for (Attachment item : this.content)
            if (!item.isEmpty())
            return true;
        return false;
    }

    public Attachment addContent() { //3
        Attachment t = new Attachment();
        if (this.content == null)
            this.content = new ArrayList<Attachment>();
        this.content.add(t);
        return t;
    }

    public CqfMeasure addContent(Attachment t) { //3
        if (t == null)
            return this;
        if (this.content == null)
            this.content = new ArrayList<Attachment>();
        this.content.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #content}, creating it if it does not already exist
     */
    public Attachment getContentFirstRep() { 
        if (getContent().isEmpty()) {
            addContent();
        }
        return getContent().get(0);
    }

    public CqfMeasure(Measure measure) {
        super();
        id = measure.getIdElement() == null ? null : measure.getIdElement().copy();
        meta = measure.getMeta() == null ? null : measure.getMeta().copy();
        implicitRules = measure.getImplicitRulesElement() == null ? null : measure.getImplicitRulesElement().copy();
        language = measure.getLanguageElement() == null ? null : measure.getLanguageElement().copy();
        text = measure.getText() == null ? null : measure.getText().copy();
        if (measure.getContained() != null) {
            contained = new ArrayList<Resource>();
            for (Resource i : measure.getContained())
                contained.add(i.copy());
        };
        if (measure.getExtension() != null) {
            extension = new ArrayList<Extension>();
            for (Extension i : measure.getExtension())
                extension.add(i.copy());
        };
        if (measure.getModifierExtension() != null) {
            modifierExtension = new ArrayList<Extension>();
            for (Extension i : measure.getModifierExtension())
                modifierExtension.add(i.copy());
        };
        url = measure.getUrlElement() == null ? null : measure.getUrlElement().copy();
        if (measure.getIdentifier() != null) {
            identifier = new ArrayList<Identifier>();
            for (Identifier i : measure.getIdentifier())
                identifier.add(i.copy());
        };
        version = measure.getVersionElement() == null ? null : measure.getVersionElement().copy();
        name = measure.getNameElement() == null ? null : measure.getNameElement().copy();
        title = measure.getTitleElement() == null ? null : measure.getTitleElement().copy();
        status = measure.getStatusElement() == null ? null : measure.getStatusElement().copy();
        experimental = measure.getExperimentalElement() == null ? null : measure.getExperimentalElement().copy();
        date = measure.getDateElement() == null ? null : measure.getDateElement().copy();
        publisher = measure.getPublisherElement() == null ? null : measure.getPublisherElement().copy();
        description = measure.getDescriptionElement() == null ? null : measure.getDescriptionElement().copy();
        purpose = measure.getPurposeElement() == null ? null : measure.getPurposeElement().copy();
        usage = measure.getUsageElement() == null ? null : measure.getUsageElement().copy();
        approvalDate = measure.getApprovalDateElement() == null ? null : measure.getApprovalDateElement().copy();
        lastReviewDate = measure.getLastReviewDateElement() == null ? null : measure.getLastReviewDateElement().copy();
        effectivePeriod = measure.getEffectivePeriod() == null ? null : measure.getEffectivePeriod().copy();
        if (measure.getUseContext() != null) {
            useContext = new ArrayList<UsageContext>();
            for (UsageContext i : measure.getUseContext())
                useContext.add(i.copy());
        };
        if (measure.getJurisdiction() != null) {
            jurisdiction = new ArrayList<CodeableConcept>();
            for (CodeableConcept i : measure.getJurisdiction())
                jurisdiction.add(i.copy());
        };
        if (measure.getTopic() != null) {
            topic = new ArrayList<CodeableConcept>();
            for (CodeableConcept i : measure.getTopic())
                topic.add(i.copy());
        };
        if (measure.getContributor() != null) {
            contributor = new ArrayList<Contributor>();
            for (Contributor i : measure.getContributor())
                contributor.add(i.copy());
        };
        if (measure.getContact() != null) {
            contact = new ArrayList<ContactDetail>();
            for (ContactDetail i : measure.getContact())
                contact.add(i.copy());
        };
        copyright = measure.getCopyrightElement() == null ? null : measure.getCopyrightElement().copy();
        if (measure.getRelatedArtifact() != null) {
            relatedArtifact = new ArrayList<RelatedArtifact>();
            for (RelatedArtifact i : measure.getRelatedArtifact())
                relatedArtifact.add(i.copy());
        };
        if (measure.getLibrary() != null) {
            library = new ArrayList<Reference>();
            for (Reference i : measure.getLibrary())
                library.add(i.copy());
        };
        disclaimer = measure.getDisclaimerElement() == null ? null : measure.getDisclaimerElement().copy();
        scoring = measure.getScoring() == null ? null : measure.getScoring().copy();
        compositeScoring = measure.getCompositeScoring() == null ? null : measure.getCompositeScoring().copy();
        if (measure.getType() != null) {
            type = new ArrayList<CodeableConcept>();
            for (CodeableConcept i : measure.getType())
                type.add(i.copy());
        };
        riskAdjustment = measure.getRiskAdjustmentElement() == null ? null : measure.getRiskAdjustmentElement().copy();
        rateAggregation = measure.getRateAggregationElement() == null ? null : measure.getRateAggregationElement().copy();
        rationale = measure.getRationaleElement() == null ? null : measure.getRationaleElement().copy();
        clinicalRecommendationStatement = measure.getClinicalRecommendationStatementElement() == null ? null : measure.getClinicalRecommendationStatementElement().copy();
        improvementNotation = measure.getImprovementNotationElement() == null ? null : measure.getImprovementNotationElement().copy();
        if (measure.getDefinition() != null) {
            definition = new ArrayList<MarkdownType>();
            for (MarkdownType i : measure.getDefinition())
                definition.add(i.copy());
        };
        guidance = measure.getGuidanceElement() == null ? null : measure.getGuidanceElement().copy();
        set = measure.getSetElement() == null ? null : measure.getSetElement().copy();
        if (measure.getGroup() != null) {
            group = new ArrayList<MeasureGroupComponent>();
            for (MeasureGroupComponent i : measure.getGroup())
                group.add(i.copy());
        };
        if (measure.getSupplementalData() != null) {
            supplementalData = new ArrayList<MeasureSupplementalDataComponent>();
            for (MeasureSupplementalDataComponent i : measure.getSupplementalData())
                supplementalData.add(i.copy());
        };
    }
}
