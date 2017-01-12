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
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.JpaFhirTerminologyProvider;
import org.opencds.cqf.helpers.LibraryHelper;

import java.util.*;

/**
 * Created by Chris Schuler on 12/11/2016.
 */
public class MeasureResourceProvider extends JpaResourceProviderDstu3<Measure> {

    private JpaFhirDataProvider provider;
    private LibraryManager libraryManager;
    private ModelManager modelManager;

    public MeasureResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaFhirDataProvider(providers).withEndpoint("http://localhost:8080/cql-measure-processor/baseDstu3");
        resolveManagers();
    }

    private void resolveManagers() {
        modelManager = new ModelManager();
        libraryManager = new LibraryManager(modelManager);
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

        Measure measure = ((MeasureResourceProvider) provider.resolveResourceProvider("Measure")).getDao().read(theId);
        org.hl7.fhir.dstu3.model.Library lib = ((LibraryResourceProvider) provider.resolveResourceProvider("Library"))
                .getDao().read(new IdType(measure.getLibraryFirstRep().getReference()));

        String contentType = lib.getContentFirstRep().getContentType();
        byte[] data = lib.getContentFirstRep().getData();
        LibraryHelper helper = new LibraryHelper(libraryManager, modelManager);
        org.cqframework.cql.elm.execution.Library library = helper.resolveLibrary(contentType, data);

        Patient patient = ((PatientResourceProvider) provider.resolveResourceProvider("Patient")).getDao().read(new IdType(patientId));

        if (patient == null) {
            throw new InternalErrorException("Patient is null");
        }

        Context context = new Context(library);
        context.setContextValue("Patient", patientId);

        if (startPeriod == null || endPeriod == null) {
            throw new InternalErrorException("The start and end dates of the measurement period must be specified in request.");
        }

        Date periodStart = resolveRequestDate(startPeriod, true);
        Date periodEnd = resolveRequestDate(endPeriod, false);

        JpaResourceProviderDstu3<ValueSet> vs = (ValueSetResourceProvider) provider.resolveResourceProvider("ValueSet");
        JpaResourceProviderDstu3<CodeSystem> cs = (CodeSystemResourceProvider) provider.resolveResourceProvider("CodeSystem");

        TerminologyProvider terminologyProvider;
        if (source == null) {
            terminologyProvider = new JpaFhirTerminologyProvider(vs, cs);
        }
        else {
            terminologyProvider = user == null || pass == null ? new FhirTerminologyProvider().withEndpoint(source)
                    : new FhirTerminologyProvider().withEndpoint(source).withBasicAuth("user", "pass");
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
    public static Date resolveRequestDate(String date, boolean start) {
        // split it up - support dashes or slashes
        String[] dissect = date.contains("-") ? date.split("-") : date.split("/");
        List<Integer> dateVals = new ArrayList<>();
        for (String dateElement : dissect) {
            dateVals.add(Integer.parseInt(dateElement));
        }

        if (dateVals.isEmpty())
            throw new IllegalArgumentException("Invalid date");

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
