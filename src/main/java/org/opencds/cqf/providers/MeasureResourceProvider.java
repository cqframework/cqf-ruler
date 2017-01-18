package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.data.fhir.FhirMeasureEvaluator;
import org.opencds.cqf.cql.data.fhir.JpaFhirDataProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.JpaFhirTerminologyProvider;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.opencds.cqf.helpers.LibraryHelper.readLibrary;
import static org.opencds.cqf.helpers.LibraryHelper.translateLibrary;

/**
 * Created by Chris Schuler on 12/11/2016.
 */
public class MeasureResourceProvider extends JpaResourceProviderDstu3<Measure> {

    private JpaFhirDataProvider provider;

    public MeasureResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaFhirDataProvider(providers);
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
            libraryLoader = new MeasureLibraryLoader(getLibraryResourceProvider(), getModelManager(), getLibraryManager());
        }
        return libraryLoader;
    }

    private MeasureLibrarySourceProvider librarySourceProvider;
    private MeasureLibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new MeasureLibrarySourceProvider(getLibraryResourceProvider());
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
            throws InternalErrorException, FHIRException {
        MeasureReport report;
        Measure measure = this.getDao().read(theId);

        // NOTE: This assumes there is only one library and it is the primary library for the measure.
        Library libraryResource = getLibraryResourceProvider().getDao().read(new IdType(measure.getLibraryFirstRep().getReference()));
        org.cqframework.cql.elm.execution.Library library = null;
        for (Attachment content : libraryResource.getContent()) {
            switch (content.getContentType()) {
                case "text/cql":
                    library = translateLibrary(new ByteArrayInputStream(content.getData()), getModelManager(), getLibraryManager());
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
            terminologyProvider = new JpaFhirTerminologyProvider(vs, cs);
        }
        else {
            terminologyProvider = user == null || pass == null ? new FhirTerminologyProvider().withEndpoint(source)
                    : new FhirTerminologyProvider().withEndpoint(source).withBasicAuth(user, pass);
        }
        provider.setTerminologyProvider(terminologyProvider);
        provider.setExpandValueSets(true);
        context.registerDataProvider("http://hl7.org/fhir", provider);

        FhirMeasureEvaluator evaluator = new FhirMeasureEvaluator();
        report = evaluator.evaluate(provider.getFhirClient(), context, measure, patient, periodStart, periodEnd);

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

    public Class getResourceType() {
        return Measure.class;
    }

}
