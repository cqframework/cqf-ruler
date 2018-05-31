package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.IncludeDef;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.evaluation.MeasureEvaluation;
import org.opencds.cqf.helpers.DateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FHIRMeasureResourceProvider extends JpaResourceProviderDstu3<Measure> {

    private JpaDataProvider provider;
    private STU3LibraryLoader libraryLoader;

    private Interval measurementPeriod;

    private static final Logger logger = LoggerFactory.getLogger(FHIRMeasureResourceProvider.class);

    public FHIRMeasureResourceProvider(JpaDataProvider dataProvider) {
        this.provider = dataProvider;
        this.libraryLoader =
                new STU3LibraryLoader(
                        (LibraryResourceProvider) provider.resolveResourceProvider("Library"),
                        new LibraryManager(new ModelManager()), new ModelManager()
                );
    }

    /*
    *
    * NOTE that the source, user, and pass parameters are not standard parameters for the FHIR $evaluate-measure operation
    *
    * */
    @Operation(name = "$evaluate-measure", idempotent = true)
    public MeasureReport evaluateMeasure(
            @IdParam IdType theId,
            @RequiredParam(name="periodStart") String periodStart,
            @RequiredParam(name="periodEnd") String periodEnd,
            @OptionalParam(name="measure") String measureRef,
            @OptionalParam(name="reportType") String reportType,
            @OptionalParam(name="patient") String patientRef,
            @OptionalParam(name="practitioner") String practitionerRef,
            @OptionalParam(name="lastReceivedOn") String lastReceivedOn,
            @OptionalParam(name="source") String source,
            @OptionalParam(name="user") String user,
            @OptionalParam(name="pass") String pass) throws InternalErrorException, FHIRException
    {
        Pair<Measure, Context> measureSetup = setup(measureRef == null ? "" : measureRef, theId, periodStart, periodEnd, source, user, pass);
        measureSetup.getRight().registerDataProvider("http://hl7.org/fhir", provider);


        // resolve report type
        MeasureEvaluation evaluator = new MeasureEvaluation(provider, measurementPeriod);
        if (reportType != null) {
            switch (reportType) {
                case "patient": return evaluator.evaluatePatientMeasure(measureSetup.getLeft(), measureSetup.getRight(), patientRef);
                case "patient-list": return  evaluator.evaluatePatientListMeasure(measureSetup.getLeft(), measureSetup.getRight(), practitionerRef);
                case "population": return evaluator.evaluatePopulationMeasure(measureSetup.getLeft(), measureSetup.getRight());
                default: throw new IllegalArgumentException("Invalid report type: " + reportType);
            }
        }

        // default report type is patient
        return evaluator.evaluatePatientMeasure(measureSetup.getLeft(), measureSetup.getRight(), patientRef);
    }

    @Operation(name = "$evaluate-measure-with-source", idempotent = true)
    public MeasureReport evaluateMeasure(
            @IdParam IdType theId,
            @OperationParam(name="sourceData", min = 1, max = 1, type = Bundle.class) Bundle sourceData,
            @OperationParam(name="periodStart", min = 1, max = 1) String periodStart,
            @OperationParam(name="periodEnd", min = 1, max = 1) String periodEnd)
    {
        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("periodStart and periodEnd are required for measure evaluation");
        }
        Pair<Measure, Context> measureSetup = setup("", theId, periodStart, periodEnd, null, null, null);
        BundleDataProviderStu3 bundleProvider = new BundleDataProviderStu3(sourceData);
        bundleProvider.setTerminologyProvider(provider.getTerminologyProvider());
        measureSetup.getRight().registerDataProvider("http://hl7.org/fhir", bundleProvider);
        MeasureEvaluation evaluator = new MeasureEvaluation(bundleProvider, measurementPeriod);
        return evaluator.evaluatePatientMeasure(measureSetup.getLeft(), measureSetup.getRight(), "");
    }

    @Operation(name = "$care-gaps", idempotent = true)
    public Bundle careGapsReport(
            @IdParam IdType theId,
            @RequiredParam(name="periodStart") String periodStart,
            @RequiredParam(name="periodEnd") String periodEnd,
            @RequiredParam(name="topic") String topic,
            @RequiredParam(name="patient") String patientRef
    ) {
        List<IBaseResource> measures = getDao().search(new SearchParameterMap().add("topic", new TokenParam().setModifier(TokenParamModifier.TEXT).setValue(topic))).getResources(0, 1000);
        Bundle careGapReport = new Bundle();
        careGapReport.setType(Bundle.BundleType.DOCUMENT);

        Composition composition = new Composition();
        // TODO - this is a placeholder code for now ... replace with preferred code once identified
        CodeableConcept typeCode = new CodeableConcept().addCoding(new Coding().setSystem("http://loinc.org").setCode("57024-2"));
        composition.setStatus(Composition.CompositionStatus.FINAL)
                .setType(typeCode)
                .setSubject(new Reference(patientRef.startsWith("Patient/") ? patientRef : "Patient/" + patientRef))
                .setTitle(topic + " Care Gap Report");

        List<MeasureReport> reports = new ArrayList<>();
        MeasureReport report = new MeasureReport();
        for (IBaseResource resource : measures) {
            Composition.SectionComponent section = new Composition.SectionComponent();

            Measure measure = (Measure) resource;
            section.addEntry(new Reference(measure.getIdElement().getResourceType() + "/" + measure.getIdElement().getIdPart()));
            if (measure.hasTitle()) {
                section.setTitle(measure.getTitle());
            }
            String improvementNotation = "increase"; // defaulting to "increase"
            if (measure.hasImprovementNotation()) {
                improvementNotation = measure.getImprovementNotation();
                section.setText(
                        new Narrative()
                                .setStatus(Narrative.NarrativeStatus.GENERATED)
                                .setDiv(new XhtmlNode().setValue(improvementNotation))
                );
            }

            Pair<Measure, Context> measureSetup = setup(measure, theId, periodStart, periodEnd, null, null, null);
            MeasureEvaluation evaluator = new MeasureEvaluation(provider, measurementPeriod);
            // TODO - this is configured for patient-level evaluation only
            report = evaluator.evaluatePatientMeasure(measureSetup.getLeft(), measureSetup.getRight(), patientRef);

            if (report.hasGroup() && measure.hasScoring()) {
                int numerator = 0;
                int denominator = 0;
                for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
                    if (group.hasPopulation()) {
                        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
                            // TODO - currently configured for measures with only 1 numerator and 1 denominator
                            if (population.hasCode()) {
                                if (population.getCode().hasCoding()) {
                                    for (Coding coding : population.getCode().getCoding()) {
                                        if (coding.hasCode()) {
                                            if (coding.getCode().equals("numerator") && population.hasCount()) {
                                                numerator = population.getCount();
                                            }
                                            else if (coding.getCode().equals("denominator") && population.hasCount()) {
                                                denominator = population.getCount();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                double proportion = 0.0;
                if (measure.getScoring().hasCoding()) {
                    for (Coding coding : measure.getScoring().getCoding()) {
                        if (coding.hasCode() && coding.getCode().equals("proportion")) {
                            proportion = numerator / denominator;
                        }
                    }
                }

                // TODO - this is super hacky ... change once improvementNotation is specified as a code
                if (improvementNotation.toLowerCase().contains("increase")) {
                    if (proportion < 1.0) {
                        composition.addSection(section);
                        reports.add(report);
                    }
                }
                else if (improvementNotation.toLowerCase().contains("decrease")) {
                    if (proportion > 0.0) {
                        composition.addSection(section);
                        reports.add(report);
                    }
                }

                // TODO - add other types of improvement notation cases
            }
        }

        careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(composition));

        for (MeasureReport rep : reports) {
            careGapReport.addEntry(new Bundle.BundleEntryComponent().setResource(rep));
        }

        return careGapReport;
    }

    private Pair<Measure, Context> setup(String measureRef, IdType theId, String periodStart,
                                         String periodEnd, String source, String user, String pass)
    {
        // fetch the measure
        Measure measure = this.getDao().read(measureRef == null || measureRef.isEmpty() ? theId : new IdType(measureRef));
        if (measure == null) {
            throw new IllegalArgumentException("Could not find Measure/" + theId);
        }

        return setup(measure, theId, periodStart, periodEnd, source, user, pass);
    }

    private Pair<Measure, Context> setup(Measure measure, IdType theId, String periodStart,
                                         String periodEnd, String source, String user, String pass)
    {
        logger.info("Evaluating Measure/" + measure.getIdElement().getIdPart());

        // load libraries
        for (Reference ref : measure.getLibrary()) {
            // if library is contained in measure, load it into server
            if (ref.getReferenceElement().getIdPart().startsWith("#")) {
                for (Resource resource : measure.getContained()) {
                    if (resource instanceof org.hl7.fhir.dstu3.model.Library
                            && resource.getIdElement().getIdPart().equals(ref.getReferenceElement().getIdPart().substring(1)))
                    {
                        LibraryResourceProvider libraryResourceProvider = (LibraryResourceProvider) provider.resolveResourceProvider("Library");
                        libraryResourceProvider.getDao().update((org.hl7.fhir.dstu3.model.Library) resource);
                    }
                }
            }
            libraryLoader.load(
                    new VersionedIdentifier()
                            .withVersion(ref.getReferenceElement().getVersionIdPart())
                            .withId(ref.getReferenceElement().getIdPart())
            );
        }

        if (libraryLoader.getLibraries().isEmpty()) {
            throw new IllegalArgumentException(String.format("Could not load library source for libraries referenced in Measure/%s.", measure.getId()));
        }

        // resolve primary library
        Library library;
        if (libraryLoader.getLibraries().size() == 1) {
            library = libraryLoader.getLibraries().values().iterator().next();
        }
        else {
            library = resolvePrimaryLibrary(measure);
        }

        logger.info("Resolved primary library as Library/" + library.getLocalId());

        // resolve execution context
        Context context = new Context(library);
        context.registerLibraryLoader(libraryLoader);

        // resolve remote term svc if provided
        if (source != null) {
            logger.info("Remote terminology service provided");
            FhirTerminologyProvider terminologyProvider = user == null || pass == null
                    ? new FhirTerminologyProvider().setEndpoint(source, true)
                    : new FhirTerminologyProvider().withBasicAuth(user, pass).setEndpoint(source, true);
            provider.setTerminologyProvider(terminologyProvider);
        }

        context.registerDataProvider("http://hl7.org/fhir", provider);

        // resolve the measurement period
        measurementPeriod =
                new Interval(
                        DateHelper.resolveRequestDate(periodStart, true), true,
                        DateHelper.resolveRequestDate(periodEnd, false), true
                );

        logger.info("Measurement period defined as [" + measurementPeriod.getStart().toString() + ", " + measurementPeriod.getEnd().toString() + "]");

        context.setParameter(
                null, "Measurement Period",
                new Interval(
                        DateTime.fromJavaDate((Date) measurementPeriod.getStart()), true,
                        DateTime.fromJavaDate((Date) measurementPeriod.getEnd()), true
                )
        );

        return new ImmutablePair<>(measure, context);
    }

    private Library resolvePrimaryLibrary(Measure measure) {
        // default is the first library reference
        Library library = libraryLoader.getLibraries().get(measure.getLibraryFirstRep().getReferenceElement().getIdPart());

        // gather all the population criteria expressions
        List<String> criteriaExpressions = new ArrayList<>();
        for (Measure.MeasureGroupComponent grouping : measure.getGroup()) {
            for (Measure.MeasureGroupPopulationComponent population : grouping.getPopulation()) {
                criteriaExpressions.add(population.getCriteria());
            }
        }

        // check each library to see if it includes the expression namespace - return if true
        for (Library candidate : libraryLoader.getLibraries().values()) {
            for (String expression : criteriaExpressions) {
                String namespace = expression.split("\\.")[0];
                if (!namespace.equals(expression)) {
                    for (IncludeDef include : candidate.getIncludes().getDef()) {
                        if (include.getLocalIdentifier().equals(namespace)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return library;
    }

    // TODO - this needs a lot of work
    @Operation(name = "$data-requirements", idempotent = true)
    public org.hl7.fhir.dstu3.model.Library dataRequirements(
            @IdParam IdType theId,
            @RequiredParam(name="startPeriod") String startPeriod,
            @RequiredParam(name="endPeriod") String endPeriod)
            throws InternalErrorException, FHIRException
    {
        Measure measure = this.getDao().read(theId);

        // NOTE: This assumes there is only one library and it is the primary library for the measure.
        org.hl7.fhir.dstu3.model.Library libraryResource =
                (org.hl7.fhir.dstu3.model.Library) provider.resolveResourceProvider("Library")
                        .getDao()
                        .read(new IdType(measure.getLibraryFirstRep().getReference()));

        List<RelatedArtifact> dependencies = new ArrayList<>();
        for (RelatedArtifact dependency : libraryResource.getRelatedArtifact()) {
            if (dependency.getType().toCode().equals("depends-on")) {
                dependencies.add(dependency);
            }
        }

        List<Coding> typeCoding = new ArrayList<>();
        typeCoding.add(new Coding().setCode("module-definition"));
        org.hl7.fhir.dstu3.model.Library library =
                new org.hl7.fhir.dstu3.model.Library().setType(new CodeableConcept().setCoding(typeCoding));

        if (!dependencies.isEmpty()) {
            library.setRelatedArtifact(dependencies);
        }

        return library
                .setDataRequirement(libraryResource.getDataRequirement())
                .setParameter(libraryResource.getParameter());
    }

    @Search(allowUnknownParams=true)
    public IBundleProvider search(
            javax.servlet.http.HttpServletRequest theServletRequest,

            RequestDetails theRequestDetails,

            @Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT)
                    StringAndListParam theFtContent,

            @Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_TEXT)
                    StringAndListParam theFtText,

            @Description(shortDefinition="Search for resources which have the given tag")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_TAG)
                    TokenAndListParam theSearchForTag,

            @Description(shortDefinition="Search for resources which have the given security labels")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY)
                    TokenAndListParam theSearchForSecurity,

            @Description(shortDefinition="Search for resources which have the given profile")
            @OptionalParam(name=ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE)
                    UriAndListParam theSearchForProfile,

            @Description(shortDefinition="Return resources linked to by the given target")
            @OptionalParam(name="_has")
                    HasAndListParam theHas,

            @Description(shortDefinition="The ID of the resource")
            @OptionalParam(name="_id")
                    TokenAndListParam the_id,

            @Description(shortDefinition="The language of the resource")
            @OptionalParam(name="_language")
                    StringAndListParam the_language,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="composed-of")
                    ReferenceAndListParam theComposed_of,

            @Description(shortDefinition="The measure publication date")
            @OptionalParam(name="date")
                    DateRangeParam theDate,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="depends-on")
                    ReferenceAndListParam theDepends_on,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="derived-from")
                    ReferenceAndListParam theDerived_from,

            @Description(shortDefinition="The description of the measure")
            @OptionalParam(name="description")
                    StringAndListParam theDescription,

            @Description(shortDefinition="The time during which the measure is intended to be in use")
            @OptionalParam(name="effective")
                    DateRangeParam theEffective,

            @Description(shortDefinition="External identifier for the measure")
            @OptionalParam(name="identifier")
                    TokenAndListParam theIdentifier,

            @Description(shortDefinition="Intended jurisdiction for the measure")
            @OptionalParam(name="jurisdiction")
                    TokenAndListParam theJurisdiction,

            @Description(shortDefinition="Computationally friendly name of the measure")
            @OptionalParam(name="name")
                    StringAndListParam theName,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="predecessor")
                    ReferenceAndListParam thePredecessor,

            @Description(shortDefinition="Name of the publisher of the measure")
            @OptionalParam(name="publisher")
                    StringAndListParam thePublisher,

            @Description(shortDefinition="The current status of the measure")
            @OptionalParam(name="status")
                    TokenAndListParam theStatus,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="successor")
                    ReferenceAndListParam theSuccessor,

            @Description(shortDefinition="The human-friendly name of the measure")
            @OptionalParam(name="title")
                    StringAndListParam theTitle,

            @Description(shortDefinition="Topics associated with the module")
            @OptionalParam(name="topic")
                    TokenAndListParam theTopic,

            @Description(shortDefinition="The uri that identifies the measure")
            @OptionalParam(name="url")
                    UriAndListParam theUrl,

            @Description(shortDefinition="The business version of the measure")
            @OptionalParam(name="version")
                    TokenAndListParam theVersion,

            @RawParam
                    Map<String, List<String>> theAdditionalRawParams,

            @IncludeParam(reverse=true)
                    Set<Include> theRevIncludes,
            @Description(shortDefinition="Only return resources which were last updated as specified by the given range")
            @OptionalParam(name="_lastUpdated")
                    DateRangeParam theLastUpdated,

            @IncludeParam(allow= {
                    "Measure:composed-of",
                    "Measure:depends-on",
                    "Measure:derived-from",
                    "Measure:predecessor",
                    "Measure:successor",
                    "Measure:composed-of",
                    "Measure:depends-on",
                    "Measure:derived-from",
                    "Measure:predecessor",
                    "Measure:successor",
                    "Measure:composed-of",
                    "Measure:depends-on",
                    "Measure:derived-from",
                    "Measure:predecessor",
                    "Measure:successor",
                    "Measure:composed-of",
                    "Measure:depends-on",
                    "Measure:derived-from",
                    "Measure:predecessor",
                    "Measure:successor",
                    "Measure:composed-of",
                    "Measure:depends-on",
                    "Measure:derived-from",
                    "Measure:predecessor",
                    "Measure:successor",
                    "*"
            })
                    Set<Include> theIncludes,

            @Sort
                    SortSpec theSort,

            @ca.uhn.fhir.rest.annotation.Count
                    Integer theCount
    ) {
        startRequest(theServletRequest);
        try {
            SearchParameterMap paramMap = new SearchParameterMap();
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT, theFtContent);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TEXT, theFtText);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TAG, theSearchForTag);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY, theSearchForSecurity);
            paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE, theSearchForProfile);
            paramMap.add("_has", theHas);
            paramMap.add("_id", the_id);
            paramMap.add("_language", the_language);
            paramMap.add("composed-of", theComposed_of);
            paramMap.add("date", theDate);
            paramMap.add("depends-on", theDepends_on);
            paramMap.add("derived-from", theDerived_from);
            paramMap.add("description", theDescription);
            paramMap.add("effective", theEffective);
            paramMap.add("identifier", theIdentifier);
            paramMap.add("jurisdiction", theJurisdiction);
            paramMap.add("name", theName);
            paramMap.add("predecessor", thePredecessor);
            paramMap.add("publisher", thePublisher);
            paramMap.add("status", theStatus);
            paramMap.add("successor", theSuccessor);
            paramMap.add("title", theTitle);
            paramMap.add("topic", theTopic);
            paramMap.add("url", theUrl);
            paramMap.add("version", theVersion);
            paramMap.setRevIncludes(theRevIncludes);
            paramMap.setLastUpdated(theLastUpdated);
            paramMap.setIncludes(theIncludes);
            paramMap.setSort(theSort);
            paramMap.setCount(theCount);
//            paramMap.setRequestDetails(theRequestDetails);

            getDao().translateRawParameters(theAdditionalRawParams, paramMap);

            return getDao().search(paramMap, theRequestDetails);
        } finally {
            endRequest(theServletRequest);
        }
    }
}
