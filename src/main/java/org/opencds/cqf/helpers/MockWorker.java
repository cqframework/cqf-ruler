package org.opencds.cqf.helpers;

import org.hl7.fhir.dstu3.context.IWorkerContext;
import org.hl7.fhir.dstu3.formats.IParser;
import org.hl7.fhir.dstu3.formats.ParserType;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpander;
import org.hl7.fhir.dstu3.utils.INarrativeGenerator;
import org.hl7.fhir.dstu3.utils.IResourceValidator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.TerminologyServiceException;
import org.opencds.cqf.exceptions.NotImplementedException;
import org.opencds.cqf.providers.JpaDataProvider;

import java.util.*;

public class MockWorker implements IWorkerContext {

    public MockWorker(JpaDataProvider provider) {

    }

    @Override
    public String getVersion() {
        throw new NotImplementedException();
    }

    @Override
    public IParser getParser(ParserType parserType) {
        throw new NotImplementedException();
    }

    @Override
    public IParser getParser(String s) {
        throw new NotImplementedException();
    }

    @Override
    public IParser newJsonParser() {
        throw new NotImplementedException();
    }

    @Override
    public IParser newXmlParser() {
        throw new NotImplementedException();
    }

    @Override
    public StructureDefinition fetchTypeDefinition(String theCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INarrativeGenerator getNarrativeGenerator(String s, String s1) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends Resource> T fetchResource(Class<T> aClass, String s) {
        throw new NotImplementedException();
    }

    @Override
    public <T extends Resource> T fetchResourceWithException(Class<T> aClass, String s) throws FHIRException {
        throw new NotImplementedException();
    }

    @Override
    public <T extends Resource> boolean hasResource(Class<T> aClass, String s) {
        return false;
    }

    @Override
    public List<String> getResourceNames() {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getTypeNames() {
        throw new NotImplementedException();
    }

    @Override
    public List<StructureDefinition> allStructures() {
        throw new NotImplementedException();
    }

    @Override
    public List<MetadataResource> allConformanceResources() {
        throw new NotImplementedException();
    }

    @Override
    public ExpansionProfile getExpansionProfile() {
        throw new NotImplementedException();
    }

    @Override
    public void setExpansionProfile(ExpansionProfile expansionProfile) {

    }

    @Override
    public CodeSystem fetchCodeSystem(String s) {
        throw new NotImplementedException();
    }

    @Override
    public boolean supportsSystem(String s) throws TerminologyServiceException {
        return false;
    }

    @Override
    public List<ConceptMap> findMapsForSource(String s) {
        throw new NotImplementedException();
    }

    @Override
    public ValueSetExpander.ValueSetExpansionOutcome expandVS(ValueSet valueSet, boolean b, boolean b1) {
        throw new NotImplementedException();
    }

    @Override
    public ValueSet.ValueSetExpansionComponent expandVS(ValueSet.ConceptSetComponent conceptSetComponent, boolean b) throws TerminologyServiceException {
        throw new NotImplementedException();
    }

    @Override
    public ValidationResult validateCode(String s, String s1, String s2) {
        throw new NotImplementedException();
    }

    @Override
    public ValidationResult validateCode(String s, String s1, String s2, ValueSet valueSet) {
        throw new NotImplementedException();
    }

    @Override
    public ValidationResult validateCode(Coding coding, ValueSet valueSet) {
        throw new NotImplementedException();
    }

    @Override
    public ValidationResult validateCode(CodeableConcept codeableConcept, ValueSet valueSet) {
        throw new NotImplementedException();
    }

    @Override
    public ValidationResult validateCode(String s, String s1, String s2, ValueSet.ConceptSetComponent conceptSetComponent) {
        throw new NotImplementedException();
    }

    @Override
    public String getAbbreviation(String s) {
        throw new NotImplementedException();
    }

    @Override
    public Set<String> typeTails() {
        throw new NotImplementedException();
    }

    @Override
    public String oid2Uri(String s) {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasCache() {
        return false;
    }

    @Override
    public void setLogger(ILoggingService iLoggingService) {

    }

    @Override
    public boolean isNoTerminologyServer() {
        return false;
    }
}