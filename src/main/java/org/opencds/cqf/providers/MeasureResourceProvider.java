package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.data.fhir.FhirMeasureEvaluator;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.opencds.cqf.helpers.LibraryHelper.readLibrary;
import static org.opencds.cqf.helpers.LibraryHelper.translateLibrary;

/**
 * Created by Chris Schuler on 12/11/2016.
 */
public class MeasureResourceProvider extends JpaResourceProviderDstu3<Measure> {

    private JpaDataProvider provider;

    public MeasureResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaDataProvider(providers);
    }

    private ModelManager modelManager;
    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
        }
        return modelManager;
    }

    private LibraryManager libraryManager;
    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
        }
        return libraryManager;
    }

    private LibraryLoader libraryLoader;
    private LibraryLoader getLibraryLoader() {
        if (libraryLoader == null) {
            libraryLoader = new STU3LibraryLoader(getLibraryResourceProvider(), getLibraryManager(), getModelManager());
        }
        return libraryLoader;
    }

    private STU3LibrarySourceProvider librarySourceProvider;
    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)provider.resolveResourceProvider("Library");
    }

    @Operation(name = "$evaluate", idempotent = true)
    public MeasureReport evaluateMeasure(@IdParam IdType theId, @RequiredParam(name="patient") String patientId,
                                         @RequiredParam(name="startPeriod") String startPeriod,
                                         @RequiredParam(name="endPeriod") String endPeriod,
                                         @OptionalParam(name="source") String source,
                                         @OptionalParam(name="user") String user,
                                         @OptionalParam(name="pass") String pass)
            throws InternalErrorException, FHIRException
    {
        MeasureReport report;
        Measure measure = this.getDao().read(theId);

        // NOTE: This assumes there is only one library and it is the primary library for the measure.
        Library libraryResource = getLibraryResourceProvider().getDao().read(new IdType(measure.getLibraryFirstRep().getReference()));
        org.cqframework.cql.elm.execution.Library library = null;
        for (Attachment content : libraryResource.getContent()) {
            switch (content.getContentType()) {
                case "text/cql":
                    library = translateLibrary(new ByteArrayInputStream(content.getData()), getLibraryManager(), getModelManager());
                    break;

                case "application/elm+xml":
                    library = readLibrary(new ByteArrayInputStream(content.getData()));
                    break;

            }
        }

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not load library source for library %s.", libraryResource.getId()));
        }

        Patient patient = ((PatientResourceProvider) provider.resolveResourceProvider("Patient")).getDao().read(new IdType(patientId));

        if (patient == null) {
            throw new InternalErrorException("Patient is null");
        }

        Context context = new Context(library);
        context.setContextValue("Patient", patientId);
        context.registerLibraryLoader(getLibraryLoader());

        if (startPeriod == null || endPeriod == null) {
            throw new InternalErrorException("The start and end dates of the measurement period must be specified in request.");
        }

        Date periodStart = resolveRequestDate(startPeriod, true);
        Date periodEnd = resolveRequestDate(endPeriod, false);

        TerminologyProvider terminologyProvider;
        if (source == null) {
            JpaResourceProviderDstu3<ValueSet> vs = (ValueSetResourceProvider) provider.resolveResourceProvider("ValueSet");
            JpaResourceProviderDstu3<CodeSystem> cs = (CodeSystemResourceProvider) provider.resolveResourceProvider("CodeSystem");
            terminologyProvider = new JpaTerminologyProvider(vs, cs);
        }
        else {
            terminologyProvider = user == null || pass == null ? new FhirTerminologyProvider().withEndpoint(source)
                    : new FhirTerminologyProvider().withEndpoint(source).withBasicAuth(user, pass);
        }
        provider.setTerminologyProvider(terminologyProvider);
        provider.setExpandValueSets(true);
        context.registerDataProvider("http://hl7.org/fhir", provider);

        FhirMeasureEvaluator evaluator = new FhirMeasureEvaluator();
        report = evaluator.evaluate(context, measure, patient, periodStart, periodEnd);

        if (report == null) {
            throw new InternalErrorException("MeasureReport is null");
        }

        if (report.getEvaluatedResources() == null) {
            throw new InternalErrorException("EvaluatedResources is null");
        }

        return report;
    }

    // Helper class to resolve period dates
    public Date resolveRequestDate(String date, boolean start) {
        // split it up - support dashes or slashes
        String[] dissect = date.contains("-") ? date.split("-") : date.split("/");
        List<Integer> dateVals = new ArrayList<>();
        for (String dateElement : dissect) {
            dateVals.add(Integer.parseInt(dateElement));
        }

        if (dateVals.isEmpty()) {
            throw new IllegalArgumentException("Invalid date");
        }

        // for now support dates up to day precision
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, dateVals.get(0));
        if (dateVals.size() > 1) {
            // java.util.Date months are zero based, hence the negative 1 -- 2014-01 == February 2014
            calendar.set(Calendar.MONTH, dateVals.get(1) - 1);
        }
        if (dateVals.size() > 2)
            calendar.set(Calendar.DAY_OF_MONTH, dateVals.get(2));
        else {
            if (start) {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            }
            else {
                // get last day of month for end period
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);
            }
        }
        return calendar.getTime();
    }

    @Operation(name = "$data-requirements", idempotent = true)
    public Library dataRequirements(@IdParam IdType theId, @RequiredParam(name="startPeriod") String startPeriod,
                                    @RequiredParam(name="endPeriod") String endPeriod) throws InternalErrorException, FHIRException
    {
        Measure measure = this.getDao().read(theId);

        // NOTE: This assumes there is only one library and it is the primary library for the measure.
        Library libraryResource = getLibraryResourceProvider().getDao().read(new IdType(measure.getLibraryFirstRep().getReference()));

        // TODO: what are the period params for? Library.effectivePeriod?

        List<RelatedArtifact> dependencies = new ArrayList<>();
        for (RelatedArtifact dependency : libraryResource.getRelatedArtifact()) {
            if (dependency.getType().toCode().equals("depends-on")) {
                dependencies.add(dependency);
            }
        }

        List<Coding> typeCoding = new ArrayList<>();
        typeCoding.add(new Coding().setCode("module-definition"));
        Library library = new Library().setType(new CodeableConcept().setCoding(typeCoding));

        if (!dependencies.isEmpty()) {
            library.setRelatedArtifact(dependencies);
        }

        return library.setDataRequirement(libraryResource.getDataRequirement()).setParameter(libraryResource.getParameter());
    }

    public Class getResourceType() {
        return Measure.class;
    }

    // Thought I got this for free ...
    @Search(allowUnknownParams=true)
    public ca.uhn.fhir.rest.server.IBundleProvider search(
            javax.servlet.http.HttpServletRequest theServletRequest,

            ca.uhn.fhir.rest.method.RequestDetails theRequestDetails,

            @Description(shortDefinition="Search the contents of the resource's data using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT)
                    StringAndListParam theFtContent,

            @Description(shortDefinition="Search the contents of the resource's narrative using a fulltext search")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TEXT)
                    StringAndListParam theFtText,

            @Description(shortDefinition="Search for resources which have the given tag")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_TAG)
                    TokenAndListParam theSearchForTag,

            @Description(shortDefinition="Search for resources which have the given security labels")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY)
                    TokenAndListParam theSearchForSecurity,

            @Description(shortDefinition="Search for resources which have the given profile")
            @OptionalParam(name=ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE)
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
            @OptionalParam(name="composed-of", targetTypes={  } )
                    ReferenceAndListParam theComposed_of,

            @Description(shortDefinition="The measure publication date")
            @OptionalParam(name="date")
                    DateRangeParam theDate,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="depends-on", targetTypes={  } )
                    ReferenceAndListParam theDepends_on,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="derived-from", targetTypes={  } )
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
            @OptionalParam(name="predecessor", targetTypes={  } )
                    ReferenceAndListParam thePredecessor,

            @Description(shortDefinition="Name of the publisher of the measure")
            @OptionalParam(name="publisher")
                    StringAndListParam thePublisher,

            @Description(shortDefinition="The current status of the measure")
            @OptionalParam(name="status")
                    TokenAndListParam theStatus,

            @Description(shortDefinition="What resource is being referenced")
            @OptionalParam(name="successor", targetTypes={  } )
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
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_CONTENT, theFtContent);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TEXT, theFtText);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_TAG, theSearchForTag);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_SECURITY, theSearchForSecurity);
            paramMap.add(ca.uhn.fhir.rest.server.Constants.PARAM_PROFILE, theSearchForProfile);
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
            paramMap.setRequestDetails(theRequestDetails);

            getDao().translateRawParameters(theAdditionalRawParams, paramMap);

            return getDao().search(paramMap);
        } finally {
            endRequest(theServletRequest);
        }
    }
}
