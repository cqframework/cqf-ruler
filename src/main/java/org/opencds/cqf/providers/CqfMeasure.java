package org.opencds.cqf.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ContactDetail;
import org.hl7.fhir.dstu3.model.Contributor;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.MarkdownType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.ParameterDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UsageContext;
import org.opencds.cqf.providers.CqfMeasure.TerminologyRef.TerminologyRefType;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.Description;

public class CqfMeasure extends Measure {
    private static final long serialVersionUID = -1297192817969868337L;

    @Child(name = "parameter", type = {ParameterDefinition.class}, order=26, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Parameters defined by the library", formalDefinition="The parameter element defines parameters used by the library." )
    protected List<ParameterDefinition> parameter;

    @Child(name = "dataRequirement", type = {DataRequirement.class}, order=27, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="What data is referenced by this library", formalDefinition="Describes a set of data that must be provided in order to be able to successfully perform the computations defined by the library." )
    protected List<DataRequirement> dataRequirement;

    @Child(name = "content", type = {Attachment.class}, order=28, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Contents of the library, either embedded or referenced", formalDefinition="The content of the library as an Attachment. The content may be a reference to a url, or may be directly embedded as a base-64 string. Either way, the contentType of the attachment determines how to interpret the content." )
    protected List<Attachment> content;

    @Child(name = "populationStatements", type = {}, order=29, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Population Statements of the library", formalDefinition="The populations of the library as a MeasureGroupComponent." )
    protected List<MeasureGroupComponent> populationStatements;
    
    @Child(name = "definitionStatements", type = {}, order=30, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Defintion Statements of the library", formalDefinition="The definitions of the library as a MeasureGroupPopulationComponent." )
    protected List<MeasureGroupPopulationComponent> definitionStatements;

    @Child(name = "functionStatements", type = {}, order=31, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Function Statements of the library", formalDefinition="The functions of the library as a MeasureGroupPopulationComponent." )
    protected List<MeasureGroupPopulationComponent> functionStatements;

    @Child(name = "supplementalDataElements", type = {}, order=32, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Supplemental Data Elements of the library", formalDefinition="The supplemental data elements of the library as a MeasureGroupPopulationComponent." )
    protected List<MeasureGroupPopulationComponent> supplementalDataElements;

    @Child(name = "terminology", type = {CqfMeasure.TerminologyRef.class}, order=33, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Terminology of the library", formalDefinition="The terminology referenced in the library" )
    protected List<CqfMeasure.TerminologyRef> terminology;

    @Child(name = "dataCriteria", type = {StringType.class}, order=34, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Data Elements of the library", formalDefinition="The data elements referenced in the library" )
    protected List<StringType> dataCriteria;

    @Child(name = "libraries", type = {Library.class}, order=35, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Measure libraries", formalDefinition="All the libraries the measure depends on" )
    protected List<Library> libraries;

    @Child(name = "citations", type = {RelatedArtifact.class}, order=36, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Additional documentation, citations, etc", formalDefinition="Related artifacts such as additional documentation, justification, or bibliographic references." )
    protected List<RelatedArtifact> citations;

    @Child(name = "sharedPopulationCriteria", type = {}, order=37, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    @Description(shortDefinition="Shared critiera of the library", formalDefinition="The shared criteria of the measure." )
    protected Map<String, String> sharedPopulationCritieria;
    
    // @Child(name = "uniquePopulationGroup", type = {}, order=38, min=0, max=Child.MAX_UNLIMITED, modifier=false, summary=false)
    // @Description(shortDefinition="Population Statements of the library", formalDefinition="The populations of the library as a MeasureGroupComponent." )
    // protected List<MeasureGroupComponent> uniquePopulationGroup;
    
    /**
     * @return {@link #relatedArtifact} (Related artifacts such as additional documentation, justification, or bibliographic references.)
     */
    public List<RelatedArtifact> getCitations() { 
      if (this.citations == null)
        this.citations = new ArrayList<RelatedArtifact>();
      return this.citations;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public Measure setCitations(List<RelatedArtifact> theRelatedArtifact) { 
      this.citations = theRelatedArtifact;
      return this;
    }

    public boolean hasCitations() { 
      if (this.citations == null)
        return false;
      for (RelatedArtifact item : this.citations)
        if (!item.isEmpty())
          return true;
      return false;
    }

    public RelatedArtifact addCitations() { //3
      RelatedArtifact t = new RelatedArtifact();
      if (this.citations == null)
        this.citations = new ArrayList<RelatedArtifact>();
      this.citations.add(t);
      return t;
    }

    public CqfMeasure addCitations(RelatedArtifact t) { //3
      if (t == null)
        return this;
      if (this.citations == null)
        this.citations = new ArrayList<RelatedArtifact>();
      this.citations.add(t);
      return this;
    }

    /**
     * @return The first repetition of repeating field {@link #relatedArtifact}, creating it if it does not already exist
     */
    public RelatedArtifact getCitationsFirstRep() { 
      if (getCitations().isEmpty()) {
        addCitations();
      }
      return getCitations().get(0);
    }

    /**
     * @return {@link #library} (The library element defines libraries used by the library.)
    */
    public List<Library> getLibraries() { 
        if (this.libraries == null)
            this.libraries = new ArrayList<Library>();
        return this.libraries;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setLibraries(List<Library> theLibraries) { 
        this.libraries = theLibraries;
        return this;
    }

    public boolean hasLibraries() { 
        return this.libraries != null && this.libraries.size() > 0;
    }

    public Library addLibraries() { //3
        Library t = new Library();
        if (this.libraries == null)
            this.libraries = new ArrayList<Library>();
        this.libraries.add(t);
        return t;
    }

    public CqfMeasure addLibraries(Library t) { //3
        if (t == null)
            return this;
        if (this.libraries == null)
            this.libraries = new ArrayList<Library>();
        this.libraries.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #library}, creating it if it does not already exist
     */
    public Library getLibrariesFirstRep() { 
        if (getLibraries().isEmpty()) {
            addLibraries();
        }
        return getLibraries().get(0);
    }


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


    public Map<String, String> getSharedPopulationCritieria() { 
        if (this.sharedPopulationCritieria == null)
            this.sharedPopulationCritieria = new HashMap<String, String>();
        return this.sharedPopulationCritieria;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setSharedPopulationCritiera(Map<String, String> theSharedPopulationCriteria) { 
        this.sharedPopulationCritieria = theSharedPopulationCriteria;
        return this;
    }

    public boolean hasSharedPopulationCritiera() { 
        if (this.sharedPopulationCritieria == null)
            return false;
        return this.sharedPopulationCritieria.size() > 0;
    }

    // public MeasureGroupComponent addSharedPopulationCritiera() { //3
    //     MeasureGroupComponent t = new MeasureGroupComponent();
    //     if (this.combinedPopulationGroup == null)
    //         this.combinedPopulationGroup = new ArrayList<MeasureGroupComponent>();
    //     this.combinedPopulationGroup.add(t);
    //     return t;
    // }

    public CqfMeasure addSharedPopulationCritiera(String key, String value) { //3
        if (key == null || value == null)
            return this;
        if (this.sharedPopulationCritieria == null)
            this.sharedPopulationCritieria = new HashMap<String, String>();
        this.sharedPopulationCritieria.put(key, value);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #populationStatements}, creating it if it does not already exist
     */
    // public MeasureGroupComponent getCombinedPopulationGroupFirstRep() { 
    //     if (getCombinedPopulationGroup().isEmpty()) {
    //         addCombinedPopulationGroup();
    //     }
    //     return getCombinedPopulationGroup().get(0);
    // }

    /**
     * @return {@link #populationStatements} (The Population Statements of the library as an Attachment. The content may be a reference to a url, or may be directly embedded as a base-64 string. Either way, the contentType of the attachment determines how to interpret the content.)
     */
    // public List<MeasureGroupComponent> getUniquePopulationGroup() { 
    //     if (this.uniquePopulationGroup == null)
    //         this.uniquePopulationGroup = new ArrayList<MeasureGroupComponent>();
    //     return this.uniquePopulationGroup;
    // }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    // public CqfMeasure setUniquePopulationGroup(List<MeasureGroupComponent> thePopulationGroup) { 
    //     this.uniquePopulationGroup = thePopulationGroup;
    //     return this;
    // }

    // public boolean hasUniquePopulationGroup() { 
    //     if (this.uniquePopulationGroup == null)
    //         return false;
    //     for (MeasureGroupComponent item : this.uniquePopulationGroup)
    //         if (!item.isEmpty())
    //         return true;
    //     return false;
    // }

    // public MeasureGroupComponent addUniquePopulationGroup() { //3
    //     MeasureGroupComponent t = new MeasureGroupComponent();
    //     if (this.uniquePopulationGroup == null)
    //         this.uniquePopulationGroup = new ArrayList<MeasureGroupComponent>();
    //     this.uniquePopulationGroup.add(t);
    //     return t;
    // }

    // public CqfMeasure addUniquePopulationGroup(MeasureGroupComponent t) { //3
    //     if (t == null)
    //         return this;
    //     if (this.uniquePopulationGroup == null)
    //         this.uniquePopulationGroup = new ArrayList<MeasureGroupComponent>();
    //     this.uniquePopulationGroup.add(t);
    //     return this;
    // }

    /**
     * @return The first repetition of repeating field {@link #populationStatements}, creating it if it does not already exist
     */
    // public MeasureGroupComponent getUniquePopulationGroupFirstRep() { 
    //     if (getUniquePopulationGroup().isEmpty()) {
    //         addUniquePopulationGroup();
    //     }
    //     return getUniquePopulationGroup().get(0);
    // }

    /**
     * @return {@link #populationStatements} (The Population Statements of the library as an Attachment. The content may be a reference to a url, or may be directly embedded as a base-64 string. Either way, the contentType of the attachment determines how to interpret the content.)
     */
    public List<MeasureGroupComponent> getPopulationStatements() { 
        if (this.populationStatements == null)
            this.populationStatements = new ArrayList<MeasureGroupComponent>();
        return this.populationStatements;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setPopulationStatements(List<MeasureGroupComponent> thePopulationStatements) { 
        this.populationStatements = thePopulationStatements;
        return this;
    }

    public boolean hasPopulationStatements() { 
        if (this.populationStatements == null)
            return false;
        for (MeasureGroupComponent item : this.populationStatements)
            if (!item.isEmpty())
            return true;
        return false;
    }

    public MeasureGroupComponent addPopulationStatements() { //3
        MeasureGroupComponent t = new MeasureGroupComponent();
        if (this.populationStatements == null)
            this.populationStatements = new ArrayList<MeasureGroupComponent>();
        this.populationStatements.add(t);
        return t;
    }

    public CqfMeasure addPopulationStatements(MeasureGroupComponent t) { //3
        if (t == null)
            return this;
        if (this.populationStatements == null)
            this.populationStatements = new ArrayList<MeasureGroupComponent>();
        this.populationStatements.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #populationStatements}, creating it if it does not already exist
     */
    public MeasureGroupComponent getPopulationStatementsFirstRep() { 
        if (getPopulationStatements().isEmpty()) {
            addPopulationStatements();
        }
        return getPopulationStatements().get(0);
    }

    /**
     * @return {@link #definitionStatements} (The Definition Statements of the library as a MeasureGroupPopulationComponent.)
     */
    public List<MeasureGroupPopulationComponent> getDefinitionStatements() { 
        if (this.definitionStatements == null)
            this.definitionStatements = new ArrayList<MeasureGroupPopulationComponent>();
        return this.definitionStatements;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setDefinitionStatements(List<MeasureGroupPopulationComponent> theDefinitionStatements) { 
        this.definitionStatements = theDefinitionStatements;
        return this;
    }

    public boolean hasDefinitionStatements() { 
        if (this.definitionStatements == null)
            return false;
        for (MeasureGroupPopulationComponent item : this.definitionStatements)
            if (!item.isEmpty())
            return true;
        return false;
    }

    public MeasureGroupPopulationComponent addDefinitionStatements() { //3
        MeasureGroupPopulationComponent t = new MeasureGroupPopulationComponent();
        if (this.definitionStatements == null)
            this.definitionStatements = new ArrayList<MeasureGroupPopulationComponent>();
        this.definitionStatements.add(t);
        return t;
    }

    public CqfMeasure addDefinitionStatements(MeasureGroupPopulationComponent t) { //3
        if (t == null)
            return this;
        if (this.definitionStatements == null)
            this.definitionStatements = new ArrayList<MeasureGroupPopulationComponent>();
        this.definitionStatements.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #definitionStatements}, creating it if it does not already exist
     */
    public MeasureGroupPopulationComponent getDefinitionStatementsFirstRep() { 
        if (getDefinitionStatements().isEmpty()) {
            addDefinitionStatements();
        }
        return getDefinitionStatements().get(0);
    }

    /**
     * @return {@link #functionStatements} (The Function Statements of the library as a MeasureGroupPopulationComponent.)
     */
    public List<MeasureGroupPopulationComponent> getFunctionStatements() { 
        if (this.functionStatements == null)
            this.functionStatements = new ArrayList<MeasureGroupPopulationComponent>();
        return this.functionStatements;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setFunctionStatements(List<MeasureGroupPopulationComponent> theFunctionStatements) { 
        this.functionStatements = theFunctionStatements;
        return this;
    }

    public boolean hasFunctionStatements() { 
        if (this.functionStatements == null)
            return false;
        for (MeasureGroupPopulationComponent item : this.functionStatements)
            if (!item.isEmpty())
            return true;
        return false;
    }

    public MeasureGroupPopulationComponent addFunctionStatements() { //3
        MeasureGroupPopulationComponent t = new MeasureGroupPopulationComponent();
        if (this.functionStatements == null)
            this.functionStatements = new ArrayList<MeasureGroupPopulationComponent>();
        this.functionStatements.add(t);
        return t;
    }

    public CqfMeasure addFunctionStatements(MeasureGroupPopulationComponent t) { //3
        if (t == null)
            return this;
        if (this.functionStatements == null)
            this.functionStatements = new ArrayList<MeasureGroupPopulationComponent>();
        this.functionStatements.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #functionStatements}, creating it if it does not already exist
     */
    public MeasureGroupPopulationComponent getFunctionStatementsFirstRep() { 
        if (getFunctionStatements().isEmpty()) {
            addFunctionStatements();
        }
        return getFunctionStatements().get(0);
    }

    /**
     * @return {@link #supplementalSupplementalDataElements} (The supplemenetal data elements referenced in the library.)
     */
    public List<MeasureGroupPopulationComponent> getSupplementalDataElements() { 
        if (this.supplementalDataElements == null)
            this.supplementalDataElements = new ArrayList<MeasureGroupPopulationComponent>();
        return this.supplementalDataElements;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setSupplementalDataElements(List<MeasureGroupPopulationComponent> theSupplementalDataElements) { 
        this.supplementalDataElements = theSupplementalDataElements;
        return this;
    }

    public boolean hasSupplementalDataElements() { 
        if (this.supplementalDataElements == null)
            return false;
        for (MeasureGroupPopulationComponent item : this.supplementalDataElements)
            if (!item.isEmpty())
            return true;
        return false;
    }

    public MeasureGroupPopulationComponent addSupplementalDataElements() { //3
        MeasureGroupPopulationComponent t = new MeasureGroupPopulationComponent();
        if (this.supplementalDataElements == null)
            this.supplementalDataElements = new ArrayList<MeasureGroupPopulationComponent>();
        this.supplementalDataElements.add(t);
        return t;
    }

    public CqfMeasure addSupplementalDataElements(MeasureGroupPopulationComponent t) { //3
        if (t == null)
            return this;
        if (this.supplementalDataElements == null)
            this.supplementalDataElements = new ArrayList<MeasureGroupPopulationComponent>();
        this.supplementalDataElements.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #supplementalSupplementalDataElements}, creating it if it does not already exist
     */
    public MeasureGroupPopulationComponent getSupplementalDataElementsFirstRep() { 
        if (getSupplementalDataElements().isEmpty()) {
            addSupplementalDataElements();
        }
        return getSupplementalDataElements().get(0);
    }

    /**
     * @return {@link #terminology} (The terminology referenced in the library.)
     */
    public List<CqfMeasure.TerminologyRef> getTerminology() { 
        if (this.terminology == null)
            this.terminology = new ArrayList<CqfMeasure.TerminologyRef>();
        return this.terminology;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setTerminology(List<CqfMeasure.TerminologyRef> theTerminology) { 
        this.terminology = theTerminology;
        return this;
    }

    public boolean hasTerminology() { 
        return (this.terminology != null && this.terminology.size() > 0);
    }

    // TODO: Need to rethink this. Do we want all the termionologies to be the same type?
    // This was originally a string so I think the refactor is probably not right or incomplete.
    public CqfMeasure.TerminologyRef addTerminology() { //3
        CqfMeasure.TerminologyRef t = new CqfMeasure.VersionedTerminologyRef(TerminologyRefType.VALUESET, null, null);
        if (this.terminology == null)
            this.terminology = new ArrayList<CqfMeasure.TerminologyRef>();
        this.terminology.add(t);
        return t;
    }

    public CqfMeasure addTerminology(CqfMeasure.TerminologyRef  t) { //3
        if (t == null)
            return this;
        if (this.terminology == null)
            this.terminology = new ArrayList<CqfMeasure.TerminologyRef >();
        this.terminology.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #terminology}, creating it if it does not already exist
     */
    public CqfMeasure.TerminologyRef getTerminologyFirstRep() { 
        if (getTerminology().isEmpty()) {
            addTerminology();
        }
        return getTerminology().get(0);
    }

    /**
     * @return {@link #dataCriteria} (The data elements referenced in the library.)
     */
    public List<StringType> getDataCriteria() { 
        if (this.dataCriteria == null)
            this.dataCriteria = new ArrayList<StringType>();
        return this.dataCriteria;
    }

    /**
     * @return Returns a reference to <code>this</code> for easy method chaining
     */
    public CqfMeasure setDataCriteria(List<StringType> theDataCriteria) { 
        this.dataCriteria = theDataCriteria;
        return this;
    }

    public boolean hasDataCriteria() { 
        if (this.dataCriteria == null)
            return false;
        for (StringType item : this.dataCriteria)
            if (!item.isEmpty())
            return true;
        return false;
    }

    public StringType addDataCriteria() { //3
        StringType t = new StringType();
        if (this.dataCriteria == null)
            this.dataCriteria = new ArrayList<StringType>();
        this.dataCriteria.add(t);
        return t;
    }

    public CqfMeasure addDataCriteria(StringType t) { //3
        if (t == null)
            return this;
        if (this.dataCriteria == null)
            this.dataCriteria = new ArrayList<StringType>();
        this.dataCriteria.add(t);
        return this;
    }

    /**
     * @return The first repetition of repeating field {@link #dataCriteria}, creating it if it does not already exist
     */
    public StringType getDataCriteriaFirstRep() { 
        if (getDataCriteria().isEmpty()) {
            addDataCriteria();
        }
        return getDataCriteria().get(0);
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

    public static class CodeTerminologyRef extends TerminologyRef   {

        public CodeTerminologyRef(String name, String id, String codeSystemName, String codeSystemId, String displayName) {
            this.type = TerminologyRefType.CODE;
            this.name = name;
            this.id = id;
            this.codeSystemName = codeSystemName;
            this.codeSystemId = codeSystemId;
            this.displayName = displayName;

        }

        protected String codeSystemName;
        public String getcodeSystemName() {
            return codeSystemName;
        }

        public void setcodeSystemName(String codeSystemName) {
            this.codeSystemName = codeSystemName;
        }

        protected String codeSystemId;
        public String getcodeSystemId() {
            return codeSystemId;
        }

        public void setcodeSystemId(String codeSystemId) {
            this.codeSystemId = codeSystemId;
        }

        protected String displayName;
        public String getdisplayName() {
            return displayName;
        }

        public void setdisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDefinition() {
            String definition = "code \"" + name + "\" : '" + id + "' from \"" + codeSystemName + "\"";
            if (this.displayName != null) {
                definition += ( " display '" + this.displayName + "'");
            }

            return definition;
        }

    }

    public static class VersionedTerminologyRef extends TerminologyRef {

        public VersionedTerminologyRef(TerminologyRefType type, String name, String id) {
            this.type = type;
            this.name = name;
            this.id = id;
        }

        public VersionedTerminologyRef(TerminologyRefType type, String name, String id, String version) {
            this(type, name, id);
            this.version = version;
        }

        protected String version;
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public String getDefinition() {
            String definition = "";
            switch(type) {
                case CODESYSTEM:
                    definition += "codesystem";
                    break;
                case VALUESET:
                    definition += "valueset";
                    break;
                default:
                    break;
            }

            if (name != null && id != null) {
                definition += (" \"" + name + "\" : '" + id + "'");
            }

            if (version != null) {
                definition += (" version \"" + version + "\"");
            }

            return definition;
        }
    }

    public abstract static class TerminologyRef implements ca.uhn.fhir.model.api.IElement {

        public static enum TerminologyRefType {
            VALUESET,
            CODE,
            CODESYSTEM
        }

        protected String name;
        protected String id;
        protected TerminologyRefType type;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TerminologyRefType getType() {
            return type;
        }

        public void setType(TerminologyRefType type) {
            this.type = type;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean hasFormatComment() {
            return false;
        }

        @Override
        public List<String> getFormatCommentsPre() {
            return null;
        }

        @Override
        public List<String> getFormatCommentsPost() {
            return null;
        }

        public abstract String getDefinition();
    }
}
