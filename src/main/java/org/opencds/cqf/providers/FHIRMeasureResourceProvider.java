package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.dao.SearchParameterMap;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.data.fhir.FhirMeasureEvaluator;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;
import org.opencds.cqf.helpers.DateHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
    IN	periodStart	1..1	date
    The start of the measurement period. In keeping with the semantics of the date parameter used in the FHIR search operation, the period will start at the beginning of the period implied by the supplied timestamp. E.g. a value of 2014 would set the period start to be 2014-01-01T00:00:00 inclusive

    IN	periodEnd	1..1	date
    The end of the measurement period. The period will end at the end of the period implied by the supplied timestamp. E.g. a value of 2014 would set the period end to be 2014-12-31T23:59:59 inclusive

    IN	measure	0..1	Reference
    The measure to evaluate. This parameter is only required when the operation is invoked on the resource type, it is not used when invoking the operation on a Measure instance

    IN	reportType	0..1	code
    The type of measure report, patient, patient-list, or population. If not specified, a default value of patient will be used if the patient parameter is supplied, otherwise, population will be used

    IN	patient	0..1	Reference
    Patient to evaluate against. If not specified, the measure will be evaluated for all patients that meet the requirements of the measure. If specified, only the referenced patient will be evaluated

    IN	practitioner	0..1	Reference
    Practitioner to evaluate. If specified, the measure will be evaluated only for patients whose primary practitioner is the identified practitioner

    IN	lastReceivedOn	0..1	dateTime
    The date the results of this measure were last received. This parameter is only valid for patient-level reports and is used to indicate when the last time a result for this patient was received. This information can be used to limit the set of resources returned for a patient-level report

    OUT	return	1..1	MeasureReport
    The results of the measure calculation. See the MeasureReport resource for a complete description of the output of this operation
*/

public class FHIRMeasureResourceProvider extends JpaResourceProviderDstu3<Measure> {

    private JpaDataProvider provider;
    private TerminologyProvider terminologyProvider;

    private Context context;
    private Interval measurementPeriod;
    private MeasureReport report = new MeasureReport();
    private FhirMeasureEvaluator evaluator = new FhirMeasureEvaluator();

    public FHIRMeasureResourceProvider(Collection<IResourceProvider> providers) {
        this.provider = new JpaDataProvider(providers);
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)provider.resolveResourceProvider("Library");
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

    /*
        This is not "pure" FHIR.
        The "source", "user", "pass", and "primaryLibraryName" parameters were added to simplify the operation.
    */
    @Operation(name = "$evaluate", idempotent = true)
    public MeasureReport evaluateMeasure(
            @IdParam IdType theId,
            @OptionalParam(name="reportType") String reportType,
            @OptionalParam(name="patient") String patientId,
            @OptionalParam(name="practitioner") String practitioner,
            @OptionalParam(name="lastReceivedOn") String lastReceivedOn,
            @RequiredParam(name="startPeriod") String startPeriod,
            @RequiredParam(name="endPeriod") String endPeriod,
            @OptionalParam(name="source") String source,
            @OptionalParam(name="user") String user,
            @OptionalParam(name="pass") String pass,
            @OptionalParam(name="primaryLibraryName") String primaryLibraryName) throws InternalErrorException, FHIRException
    {
        Measure measure = this.getDao().read(theId);

        // load libraries referenced in measure
        // TODO: need better way to determine primary library
        //   - for now using a name convention: <measure ID>-library or primaryLibraryName param
        Library primary = null;
        for (Reference ref : measure.getLibrary()) {
            VersionedIdentifier vid = new VersionedIdentifier().withId(ref.getReference());
            Library temp = getLibraryLoader().load(vid);

            if (vid.getId().equals(measure.getIdElement().getIdPart() + "-logic")
                    || vid.getId().equals("Library/" + measure.getIdElement().getIdPart() + "-logic")
                    || (primaryLibraryName != null && temp.getIdentifier().getId().equals(primaryLibraryName)))
            {
                primary = temp;
                context = new Context(primary);
            }
        }
        if (primary == null) {
            throw new IllegalArgumentException(
                    "Primary library not found.\nFollow the naming conventions <measureID>-library or specify primary library in request."
            );
        }
        if (((STU3LibraryLoader)getLibraryLoader()).getLibraries().isEmpty()) {
            throw new IllegalArgumentException(String.format("Could not load library source for libraries referenced in %s measure.", measure.getId()));
        }

        // Prep - defining the context, measurementPeriod, terminology provider, and data provider
        context.registerLibraryLoader(getLibraryLoader());
        measurementPeriod =
                new Interval(
                        DateHelper.resolveRequestDate(startPeriod, true), true,
                        DateHelper.resolveRequestDate(endPeriod, false), true
                );

        if (source == null) {
            JpaResourceProviderDstu3<ValueSet> vs = (ValueSetResourceProvider) provider.resolveResourceProvider("ValueSet");
            JpaResourceProviderDstu3<CodeSystem> cs = (CodeSystemResourceProvider) provider.resolveResourceProvider("CodeSystem");
            terminologyProvider = new JpaTerminologyProvider(vs, cs);
        }
        else {
            terminologyProvider = user == null || pass == null ? new FhirTerminologyProvider().withEndpoint(source)
                    : new FhirTerminologyProvider().withBasicAuth(user, pass).withEndpoint(source);
        }
        provider.setTerminologyProvider(terminologyProvider);
        provider.setExpandValueSets(true);
        context.registerDataProvider("http://hl7.org/fhir", provider);

        // determine the report type (patient, patient-list, or population (summary))
        if (reportType != null) {
            switch (reportType) {
                case "patient": return evaluatePatientMeasure(measure, patientId);
                case "patient-list": return evaluatePatientListMeasure(measure, practitioner);
                case "population": return evaluatePopulationMeasure(measure);
                case "summary": return evaluatePopulationMeasure(measure);
                default:
                    throw new IllegalArgumentException("Invalid report type " + reportType);
            }
        }

        // default behavior
        else {
            if (patientId != null) return evaluatePatientMeasure(measure, patientId);
            if (practitioner != null) return evaluatePatientListMeasure(measure, practitioner);
            return evaluatePopulationMeasure(measure);
        }
    }

    private void validateReport() {
        if (report == null) {
            throw new InternalErrorException("MeasureReport is null");
        }

        if (report.getEvaluatedResources() == null) {
            throw new InternalErrorException("EvaluatedResources is null");
        }
    }

    private MeasureReport evaluatePatientMeasure(Measure measure, String patientId) {
        if (patientId == null) {
            throw new IllegalArgumentException("Patient id must be provided for patient type measure evaluation");
        }

        Patient patient = ((PatientResourceProvider) provider.resolveResourceProvider("Patient")).getDao().read(new IdType(patientId));
        if (patient == null) {
            throw new InternalErrorException("Patient is null");
        }

        context.setContextValue("Patient", patientId);

        report = evaluator.evaluate(context, measure, patient, measurementPeriod);
        validateReport();
        return report;
    }

    private MeasureReport evaluatePatientListMeasure(Measure measure, String practitioner) {
        SearchParameterMap map = new SearchParameterMap();
        map.add("general-practitioner", new ReferenceParam(practitioner));
        IBundleProvider patientProvider = ((PatientResourceProvider) provider.resolveResourceProvider("Patient")).getDao().search(map);
        List<IBaseResource> patientList = patientProvider.getResources(0, patientProvider.size());

        if (patientList.isEmpty()) {
            throw new IllegalArgumentException("No patients were found with practitioner reference " + practitioner);
        }

        List<Patient> patients = new ArrayList<>();
        patientList.forEach(x -> patients.add((Patient) x));

//        context.setContextValue("Population", patients);

        report = evaluator.evaluate(context, measure, patients, measurementPeriod, MeasureReport.MeasureReportType.PATIENTLIST);
        validateReport();
        return report;
    }

    private MeasureReport evaluatePopulationMeasure(Measure measure) {
        IBundleProvider patientProvider = ((PatientResourceProvider) provider.resolveResourceProvider("Patient")).getDao().search(new SearchParameterMap());
        List<IBaseResource> population = patientProvider.getResources(0, patientProvider.size());

        if (population.isEmpty()) {
            throw new IllegalArgumentException("No patients were found in the data provider at endpoint " + provider.getEndpoint());
        }

        List<Patient> patients = new ArrayList<>();
        population.forEach(x -> patients.add((Patient) x));

        report = evaluator.evaluate(context, measure, patients, measurementPeriod, MeasureReport.MeasureReportType.SUMMARY);
        validateReport();
        return report;
    }
}
