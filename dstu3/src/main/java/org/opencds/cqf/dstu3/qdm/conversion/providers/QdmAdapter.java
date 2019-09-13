package org.opencds.cqf.dstu3.qdm.conversion.providers;

import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Ratio;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.hl7.fhir.dstu3.model.Quantity;
import org.opencds.cqf.exceptions.NotImplementedException;
import org.opencds.cqf.dstu3.qdm.conversion.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QdmAdapter {

    private QdmDataProvider dataProvider;

    public QdmAdapter(QdmDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public String mapToFhirType(String dataType) {
        switch (dataType) {
            case "Diagnosis":
                return "Condition";
            case "NegativeEncounterPerformed":
                return "Encounter";
            case "PositiveEncounterPerformed":
                return "Encounter";
            case "NegativeDiagnosticStudyPerformed":
                return "DiagnosticReport";
            case "PositiveDiagnosticStudyPerformed.json":
                return "DiagnosticReport";
            case "NegativeInterventionOrder":
                return "ProcedureRequest";
            case "PositiveInterventionOrder":
                return "ProcedureRequest";
            case "NegativeInterventionPerformed":
                return "Procedure";
            case "PositiveInterventionPerformed":
                return "Procedure";
            case "NegativeLaboratoryTestPerformed":
                return "Observation";
            case "PositiveLaboratoryTestPerformed":
                return "Observation";
            case "NegativeProcedurePerformed":
                return "Procedure";
            case "PositiveProcedurePerformed":
                return "Procedure";
            case "PatientCharacteristicBirthdate":
                return "Patient";
            case "PatientCharacteristicSex":
                return "Patient";
            default:
                throw new NotImplementedException("Mapping not implemented for QDM type: " + dataType);
        }
    }

    public QdmBaseType createQdmResource(String qdmResourceType, Object fhirResource) {
        switch (qdmResourceType) {
            case "Diagnosis":
                return createDiagnosis(fhirResource);
            case "PositiveEncounterPerformed":
                return createEncounterPerformed(fhirResource, true);
            case "NegativeEncounterPerformed":
                return createEncounterPerformed(fhirResource, false);
            case "PositiveDiagnosticStudyPerformed.json":
                return createDiagnosticStudyPerformed(fhirResource, true);
            case "NegativeDiagnosticStudyPerformed":
                return createDiagnosticStudyPerformed(fhirResource, false);
            case "PositiveInterventionOrder":
                return createInterventionOrder(fhirResource, true);
            case "NegativeInterventionOrder":
                return createInterventionOrder(fhirResource, false);
            case "PositiveInterventionPerformed":
                return createInterventionPerformed(fhirResource, true);
            case "NegativeInterventionPerformed":
                return createInterventionPerformed(fhirResource, false);
            case "PositiveLaboratoryTestPerformed":
                return createLaboratoryTestPerformed(fhirResource, true);
            case "NegativeLaboratoryTestPerformed":
                return createLaboratoryTestPerformed(fhirResource, false);
            case "PositiveProcedurePerformed":
                return createProcedurePerformed(fhirResource, true);
            case "NegativeProcedurePerformed":
                return createProcedurePerformed(fhirResource, false);
            case "PatientCharacteristicBirthdate":
                return createPatientCharacteristicBirthdate(fhirResource);
            case "PatientCharacteristicSex":
                return createPatientCharacteristicSex(fhirResource);
            default:
                throw new NotImplementedException("Resource creation is not implemented for QDM type: " + qdmResourceType);
        }
    }

    public static String mapCodePath(String dataType, String qdmPath) {
        switch (dataType) {
            case "PositiveEncounterPerformed": case "NegativeEncounterPerformed": {
                switch (qdmPath) {
                    case "code":
                        return "class";
                }
            }
            case "PatientCharacteristicSex": return "gender";
        }

        return qdmPath;
    }

    public static Code createCode(Coding fhirCode) {
        if (fhirCode == null) {
            return null;
        }
        return new Code()
                .withSystem(fhirCode.getSystem())
                .withCode(fhirCode.getCode())
                .withDisplay(fhirCode.getDisplay())
                .withVersion(fhirCode.getVersion());
    }

    public static List<Code> createCodeList(List<Coding> codings) {
        if (codings == null) {
            return null;
        }

        List<Code> codes = new ArrayList<>();
        for (Coding diagnosis : codings) {
            codes.add(QdmAdapter.createCode(diagnosis));
        }
        return codes;
    }

    public static Interval createInterval(Period period) {
        if (period == null) {
            return null;
        }
        if (!period.hasStart() && !period.hasEnd()) {
            return null;
        }
        return new Interval(
                period.hasStart() ? DateTime.fromJavaDate(period.getStart()) : null, true,
                period.hasEnd() ? DateTime.fromJavaDate(period.getEnd()) : null, true
        );
    }

    public static Interval createInterval(Date date) {
        if (date == null) {
            return null;
        }
        return new Interval(
                DateTime.fromJavaDate(date), true,
                DateTime.fromJavaDate(date), true
        );
    }

    public static org.opencds.cqf.cql.runtime.Quantity createQuantity(Quantity quantity) {
        if (quantity == null) {
            return null;
        }
        return new org.opencds.cqf.cql.runtime.Quantity()
                .withValue(quantity.getValue())
                .withUnit(quantity.getUnit());
    }

    public static boolean isPeriodNull(Period period) {
        return period == null || !period.hasStart() && !period.hasEnd();
    }

    public EncounterPerformed createEncounterPerformed(Object fhirResource, boolean isPositive) {
        if (!(fhirResource instanceof Encounter)) {
            throw new IllegalArgumentException("Expecting FHIR Encounter resource, found " + fhirResource.getClass().getSimpleName());
        }

        Encounter encounter = (Encounter) fhirResource;

        EncounterPerformed encounterPerformed = isPositive ? new PositiveEncounterPerformed() : new NegativeEncounterPerformed();

        // TODO - populate once Provenance support is added
        encounterPerformed.setAuthorDatetime(null);

        // id
        if (encounter.hasId()) {
            encounterPerformed.setId(new Id().setValue(new StringType(encounter.getId())));
        }

        // Code
        if (encounter.hasClass_()) {
            encounterPerformed.setCode(encounter.getClass_());
        }

        // Admission Source
        if (encounter.hasHospitalization()
                && encounter.getHospitalization().hasAdmitSource()
                && encounter.getHospitalization().getAdmitSource().hasCoding()) {
            encounterPerformed.setAdmissionSource(encounter.getHospitalization().getAdmitSource().getCodingFirstRep());
        }

        // Relevant Period
        if (encounter.hasPeriod()) {
            encounterPerformed.setRelevantPeriod(encounter.getPeriod());
        }

        // Discharge Disposition
        if (encounter.hasHospitalization()
                && encounter.getHospitalization().hasDischargeDisposition()
                && encounter.getHospitalization().getDischargeDisposition().hasCoding()) {
            encounterPerformed.setDischargeDisposition(encounter.getHospitalization().getDischargeDisposition().getCodingFirstRep());
        }

        // Diagnosis
        if (encounter.hasDiagnosis() && !encounter.getDiagnosis().isEmpty()) {
            List<Coding> diagnosisCodes = new ArrayList<>();
            for (Encounter.DiagnosisComponent component : encounter.getDiagnosis()) {
                boolean principleDaignosis = component.hasRank() && component.getRank() == 1;
                JpaResourceProviderDstu3<? extends IAnyResource> resourceProvider = dataProvider.resolveResourceProvider(component.getCondition().getReference().split("/")[0]);
                IAnyResource resource = resourceProvider.getDao().read(new IdType(component.getCondition().getReference()));
                if (resource instanceof Condition) {
                    Condition condition = (Condition) resource;
                    if (condition.hasCode()) {
                        for (Coding coding : condition.getCode().getCoding()) {
                            diagnosisCodes.add(coding);
                            if (principleDaignosis) {
                                encounterPerformed.setPrincipalDiagnosis(coding);
                            }
                        }
                    }
                } else if (resource instanceof Procedure) {
                    Procedure procedure = (Procedure) resource;
                    if (procedure.hasCode()) {
                        for (Coding coding : procedure.getCode().getCoding()) {
                            diagnosisCodes.add(coding);
                            if (principleDaignosis) {
                                encounterPerformed.setPrincipalDiagnosis(coding);
                            }
                        }
                    }
                }
            }
            encounterPerformed.setDiagnoses(diagnosisCodes);
        }

        // FacilityLocations
        if (encounter.hasLocation()) {
            List<FacilityLocation> facilityLocations = new ArrayList<>();
            for (Encounter.EncounterLocationComponent location : encounter.getLocation()) {
                FacilityLocation facilityLocation = new FacilityLocation();
                facilityLocation.setLocationPeriod(isPeriodNull(location.getPeriod()) ? new Period() : location.getPeriod());
                JpaResourceProviderDstu3<? extends IAnyResource> resourceProvider = dataProvider.resolveResourceProvider("Location");
                IAnyResource resource = resourceProvider.getDao().read(new IdType(location.getLocation().getReference()));
                Location fhirLocation = (Location) resource;
                if (fhirLocation.hasType() && fhirLocation.getType().hasCoding()) {
                    facilityLocation.setCode(fhirLocation.getType().getCodingFirstRep());
                }
                facilityLocations.add(facilityLocation);
            }
            encounterPerformed.setFacilityLocations(facilityLocations);
        }

        // Principal Diagnosis
        if (encounterPerformed.getPrincipalDiagnosis() == null
                && encounterPerformed.getDiagnoses() != null
                && !encounterPerformed.getDiagnoses().isEmpty()) {
            encounterPerformed.setPrincipalDiagnosis(encounterPerformed.getDiagnoses().get(0));
        }

        // Negation Rationale
        if (encounter.hasExtension()) {
            for (Extension extension : encounter.getExtension()) {
                if (extension.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-reasonCancelled")) {
                    if (extension.hasValue()) {
                        encounterPerformed.setNegationRationale(((CodeableConcept) extension.getValue()).getCodingFirstRep());
                        break;
                    }
                }
            }
        }

        // Length of Stay
        if (encounter.hasLength()) {
            Duration duration = encounter.getLength();
            encounterPerformed.setLengthOfStay(new Quantity().setValue(duration.getValue()).setUnit(duration.getUnit()));
        }

        return encounterPerformed;
    }

    public Diagnosis createDiagnosis(Object fhirResource) {
        if (!(fhirResource instanceof Condition)) {
            throw new IllegalArgumentException("Expecting FHIR Condition resource, found " + fhirResource.getClass().getSimpleName());
        }

        Condition condition = (Condition) fhirResource;
        Diagnosis diagnosis = new Diagnosis();

        // TODO - populate once Provenance support is added
        diagnosis.setAuthorDatetime(null);

        // Prevalence Period - start
        Date prevalenceStart = null;
        if (condition.hasOnset() || condition.hasAssertedDate()) {
            if (condition.hasOnset()) {
                Type onset = condition.getOnset();
                if (onset instanceof Period) {
                    prevalenceStart = ((Period) onset).getStart();
                } else if (onset instanceof DateTimeType) {
                    prevalenceStart = ((DateTimeType) onset).getValue();
                }
            }
            if (condition.hasAssertedDate()) {
                Date contender = condition.getAssertedDate();
                if (prevalenceStart != null) {
                    if (prevalenceStart.compareTo(contender) > 0) {
                        prevalenceStart = contender;
                    }
                } else {
                    prevalenceStart = contender;
                }
            }
        }

        // Prevalence Period - end
        Date prevalenceEnd = null;
        if (condition.hasAbatement()) {
            if (condition.getAbatement() instanceof Period) {
                prevalenceEnd = ((Period) condition.getAbatement()).getEnd();
            }
            if (condition.getAbatement() instanceof DateTimeType) {
                prevalenceEnd = ((DateTimeType) condition.getAbatement()).getValue();
            }
        }

        // Prevalence Period
        diagnosis.setPrevalencePeriod(new Period().setStart(prevalenceStart).setEnd(prevalenceEnd));

        // Anatomical Location Site
        if (condition.hasBodySite() && condition.getBodySiteFirstRep().hasCoding()) {
            diagnosis.setAnatomicalLocationSite(condition.getBodySiteFirstRep().getCodingFirstRep());
        }

        // Severity
        if (condition.hasSeverity() && condition.getSeverity().hasCoding()) {
            diagnosis.setSeverity(condition.getSeverity().getCodingFirstRep());
        }

        // Code
        if (condition.hasCode() && condition.getCode().hasCoding()) {
            diagnosis.setCode(condition.getCode().getCodingFirstRep());
        }

        // id
        if (condition.hasId()) {
            diagnosis.setId(condition.getId());
        }

        if (condition.hasAsserter()) {
            // TODO - Source? It is not in the schema or modelinfo, but it is in the qdm-to-qicore mappings
        }

        return diagnosis;
    }

    public DiagnosticStudyPerformed createDiagnosticStudyPerformed(Object fhirResource, boolean isPositive) {
        if (!(fhirResource instanceof DiagnosticReport)) {
            throw new IllegalArgumentException("Expecting FHIR DiagnosticReport resource, found " + fhirResource.getClass().getSimpleName());
        }

        // TODO - DiagnosticStudyPerformed can map to both DiagnosticReport(imaging) and Observation(non-imaging) - determine how to identify when to use which
        DiagnosticReport diagnosticReport = (DiagnosticReport) fhirResource;

        DiagnosticStudyPerformed diagnosticStudyPerformed = isPositive ? new PositiveDiagnosticStudyPerformed() : new NegativeDiagnosticStudyPerformed();

        // TODO - populate once Provenance support is added
        diagnosticStudyPerformed.setAuthorDatetime(null);

        // FacilityLocation
        if (diagnosticReport.hasExtension()) {
            for (Extension extension : diagnosticReport.getExtension()) {
                if (extension.getUrl().equals("http://hl7.org/fhir/StructureDefinition/diagnosticReport-locationPerformedhttp://hl7.org/fhir/StructureDefinition/diagnosticReport-locationPerformed")) {
                    if (extension.hasValue()) {
                        JpaResourceProviderDstu3<? extends IAnyResource> resourceProvider = dataProvider.resolveResourceProvider("Location");
                        Reference reference = (Reference) extension.getValue();
                        Location location = (Location) resourceProvider.getDao().read(new IdType(reference.getReference()));

                        if (location.hasType() && location.getType().hasCoding()) {
                            diagnosticStudyPerformed.setFacilityLocation(location.getType().getCodingFirstRep());
                        }
                    }
                }
            }
        }

        // Method
        if (diagnosticReport.hasImagingStudy()) {
            JpaResourceProviderDstu3<? extends IAnyResource> resourceProvider = dataProvider.resolveResourceProvider("ImagingStudy");
            ImagingStudy imagingStudy = (ImagingStudy) resourceProvider.getDao().read(new IdType(diagnosticReport.getImagingStudyFirstRep().getReference()));

            if (imagingStudy.hasModalityList()) {
                diagnosticStudyPerformed.setMethod(imagingStudy.getModalityListFirstRep());
            }
        }

        // Result and Components
        if (diagnosticReport.hasResult()) {
            JpaResourceProviderDstu3<? extends IAnyResource> resourceProvider = dataProvider.resolveResourceProvider("Observation");
            Observation observation = (Observation) resourceProvider.getDao().read(new IdType(diagnosticReport.getResultFirstRep().getReference()));

            // Result
            if (observation.hasValue()) {
                if (observation.getValue() instanceof Quantity
                        || observation.getValue() instanceof Ratio
                        || observation.getValue() instanceof CodeableConcept) {
                    diagnosticStudyPerformed.setResult(observation.getValue());
                }
            }

            // Components
            if (observation.hasComponent()) {
                List<Component> components = new ArrayList<>();
                for (Observation.ObservationComponentComponent component : observation.getComponent()) {
                    Component newComponent = new Component();
                    if (component.hasCode() && component.getCode().hasCoding()) {
                        newComponent.setCode(component.getCode().getCodingFirstRep());
                    }
                    if (component.hasValue()) {
                        if (component.getValue() instanceof Quantity
                                || component.getValue() instanceof Ratio
                                || component.getValue() instanceof CodeableConcept) {
                            newComponent.setResult(component.getValue());
                        }
                    }
                    components.add(newComponent);
                }
                diagnosticStudyPerformed.setComponents(components);
            }
        }

        // Result dateTime
        if (diagnosticReport.hasIssued()) {
            diagnosticStudyPerformed.setResultDatetime(new DateTimeType(diagnosticReport.getIssued()));
        }

        // Relevant Period
        if (diagnosticReport.hasEffective()) {
            if (diagnosticReport.getEffective() instanceof Period) {
                diagnosticStudyPerformed.setRelevantPeriod((Period) diagnosticReport.getEffective());
            } else {
                diagnosticStudyPerformed.setRelevantPeriod(
                        new Period()
                                .setStart(((DateTimeType) diagnosticReport.getEffective()).getValue())
                                .setEnd(((DateTimeType) diagnosticReport.getEffective()).getValue())
                );
            }
        }

        // Status
        if (diagnosticReport.hasStatus()) {
            diagnosticStudyPerformed.setStatus(
                    new Coding().setCode(diagnosticReport.getStatus().toCode())
                            .setSystem(diagnosticReport.getStatus().getSystem())
                            .setDisplay(diagnosticReport.getStatus().getDisplay())
            );
        }

        // Code
        if (diagnosticReport.hasCode() && diagnosticReport.getCode().hasCoding()) {
            diagnosticStudyPerformed.setCode(diagnosticReport.getCode().getCodingFirstRep());
        }

        // id
        if (diagnosticReport.hasId()) {
            diagnosticStudyPerformed.setId(new Id().setValue(new StringType(diagnosticReport.getId())));
        }

        if (diagnosticReport.hasPerformer()) {
            // TODO - Source? It is not in the schema or modelinfo, but it is in the qdm-to-qicore mappings
        }

        return diagnosticStudyPerformed;
    }

    public InterventionOrder createInterventionOrder(Object fhirResource, boolean isPositive) {
        if (!(fhirResource instanceof ProcedureRequest)) {
            throw new IllegalArgumentException("Expecting FHIR ProcedureRequest resource, found " + fhirResource.getClass().getSimpleName());
        }

        ProcedureRequest procedureRequest = (ProcedureRequest) fhirResource;

        InterventionOrder interventionOrder = isPositive ? new PositiveInterventionOrder() : new NegativeInterventionOrder();

        // Negation Rationale
        if (procedureRequest.hasExtension()) {
            for (Extension extension : procedureRequest.getExtension()) {
                if (extension.getUrl().equals("http://hl7.org/fhir/StructureDefinition/procedurerequest-reasonRefused")) {
                    if (extension.hasValue()) {
                        interventionOrder.setNegationRationale(((CodeableConcept) extension.getValue()).getCodingFirstRep());
                        break;
                    }
                }
            }
        }

        // Reason
        if (procedureRequest.hasReasonCode() && procedureRequest.getReasonCodeFirstRep().hasCoding()) {
            interventionOrder.setReason(procedureRequest.getReasonCodeFirstRep().getCodingFirstRep());
        }

        // Author dateTime
        if (procedureRequest.hasAuthoredOn()) {
            interventionOrder.setAuthorDatetime(procedureRequest.getAuthoredOnElement());
        }

        // Code
        if (procedureRequest.hasCode() && procedureRequest.getCode().hasCoding()) {
            interventionOrder.setCode(procedureRequest.getCode().getCodingFirstRep());
        }

        // id
        if (procedureRequest.hasId()) {
            interventionOrder.setId(new Id().setValue(new StringType(procedureRequest.getId())));
        }

        // Source
        if (procedureRequest.hasRequester()) {
            // TODO - Source? It is not in the schema or modelinfo, but it is in the qdm-to-qicore mappings
        }

        return interventionOrder;
    }

    public InterventionPerformed createInterventionPerformed(Object fhirResource, boolean isPositive) {
        if (!(fhirResource instanceof Procedure)) {
            throw new IllegalArgumentException("Expecting FHIR Procedure resource, found " + fhirResource.getClass().getSimpleName());
        }

        Procedure procedure = (Procedure) fhirResource;

        InterventionPerformed interventionPerformed = isPositive ? new PositiveInterventionPerformed() : new NegativeInterventionPerformed();

        // Relevant Period
        if (procedure.hasPerformed()) {
            if (procedure.getPerformed() instanceof Period) {
                interventionPerformed.setRelevantPeriod((Period) procedure.getPerformed());
            }
            else if (procedure.getPerformed() instanceof DateTimeType) {
                interventionPerformed.setRelevantPeriod(
                        new Period()
                                .setStart(((DateTimeType) procedure.getPerformed()).getValue())
                                .setEnd(((DateTimeType) procedure.getPerformed()).getValue())
                );
            }
        }

        // Negation Rationale
        if (procedure.hasNotDoneReason() && procedure.getNotDoneReason().hasCoding()) {
            interventionPerformed.setNegationRationale(procedure.getNotDoneReason().getCodingFirstRep());
        }

        // Reason
        if (procedure.hasReasonCode() && procedure.getReasonCodeFirstRep().hasCoding()) {
            interventionPerformed.setReason(procedure.getReasonCodeFirstRep().getCodingFirstRep());
        }
        else if (procedure.hasReasonCode() && procedure.getReasonCodeFirstRep().hasText()) {
            interventionPerformed.setReason(new Coding().setDisplay(procedure.getReasonCodeFirstRep().getText()));
        }

        // Result
        if (procedure.hasOutcome() && procedure.getOutcome().hasCoding()) {
            interventionPerformed.setResult(procedure.getOutcome().getCodingFirstRep());
        }

        // Status
        if (procedure.hasStatus()) {
            interventionPerformed.setStatus(
                    new Coding()
                            .setCode(procedure.getStatus().toCode())
                            .setSystem(procedure.getStatus().getSystem())
                            .setDisplay(procedure.getStatus().getDisplay())
            );
        }

        // Code
        if (procedure.hasCode() && procedure.getCode().hasCoding()) {
            interventionPerformed.setCode(procedure.getCode().getCodingFirstRep());
        }

        // id
        if (procedure.hasId()) {
            interventionPerformed.setId(new Id().setValue(new StringType(procedure.getId())));
        }

        // Source
        if (procedure.hasPerformer()) {
            // TODO - Source? It is not in the schema or modelinfo, but it is in the qdm-to-qicore mappings
        }

        return interventionPerformed;
    }

    public LaboratoryTestPerformed createLaboratoryTestPerformed(Object fhirResource, boolean isPositive) {
        if (!(fhirResource instanceof Observation)) {
            throw new IllegalArgumentException("Expecting FHIR Observation resource, found " + fhirResource.getClass().getSimpleName());
        }

        Observation observation = (Observation) fhirResource;

        LaboratoryTestPerformed laboratoryTestPerformed = isPositive ? new PositiveLaboratoryTestPerformed() : new NegativeLaboratoryTestPerformed();

        // TODO - populate once Provenance support is added
        laboratoryTestPerformed.setAuthorDatetime(null);

        // Method
        if (observation.hasMethod() && observation.getMethod().hasCoding()) {
            laboratoryTestPerformed.setMethod(observation.getMethod().getCodingFirstRep());
        }

        // Negation Rationale
        if (observation.hasDataAbsentReason() && observation.getDataAbsentReason().hasCoding()) {
            laboratoryTestPerformed.setNegationRationale(observation.getDataAbsentReason().getCodingFirstRep());
        }

        // Reason
        if (observation.hasBasedOn()) {
            // TODO - validate that this is correct
            laboratoryTestPerformed.setReason(new Coding().setCode(observation.getBasedOnFirstRep().getReference()));
        }

        // Reference Range
        if (observation.hasReferenceRange()) {
            Range referenceRange = new Range();
            if (observation.getReferenceRangeFirstRep().hasLow()) {
                referenceRange.setLow(observation.getReferenceRangeFirstRep().getLow());
            }
            if (observation.getReferenceRangeFirstRep().hasHigh()) {
                referenceRange.setHigh(observation.getReferenceRangeFirstRep().getHigh());
            }
            laboratoryTestPerformed.setReferenceRange(referenceRange);
        }

        // Result
        if (observation.hasValue()) {
            if (observation.getValue() instanceof Quantity
                    || observation.getValue() instanceof CodeableConcept
                    || observation.getValue() instanceof Ratio)
            {
                laboratoryTestPerformed.setResult(observation.getValue());
            }
        }

        // Result dateTime
        if (observation.hasIssued()) {
            laboratoryTestPerformed.setResultDatetime(new DateTimeType(observation.getIssued()));
        }

        // Relevant Period
        if (observation.hasEffective()) {
            if (observation.getEffective() instanceof Period) {
                laboratoryTestPerformed.setRelevantPeriod((Period) observation.getEffective());
            }
            else if (observation.getEffective() instanceof DateTimeType) {
                laboratoryTestPerformed.setRelevantPeriod(
                        new Period()
                                .setStart(((DateTimeType) observation.getEffective()).getValue())
                                .setEnd(((DateTimeType) observation.getEffective()).getValue())
                );
            }
        }

        // Status
        if (observation.hasStatus()) {
            laboratoryTestPerformed.setStatus(
                    new Coding()
                            .setCode(observation.getStatus().toCode())
                            .setSystem(observation.getStatus().getSystem())
                            .setDisplay(observation.getStatus().getDisplay())
            );
        }

        // Code
        if (observation.hasCode() && observation.getCode().hasCoding()) {
            laboratoryTestPerformed.setCode(observation.getCode().getCodingFirstRep());
        }

        // Components
        if (observation.hasComponent()) {
            List<ResultComponent> resultComponents = new ArrayList<>();
            for (Observation.ObservationComponentComponent component : observation.getComponent()) {
                ResultComponent resultComponent = new ResultComponent();
                if (component.hasCode() && component.getCode().hasCoding()) {
                    resultComponent.setCode(component.getCode().getCodingFirstRep());
                }
                if (component.hasValue()) {
                    if (component.getValue() instanceof Quantity
                            || component.getValue() instanceof CodeableConcept
                            || component.getValue() instanceof Ratio)
                    {
                        resultComponent.setResult(component.getValue());
                    }
                }
                if (component.hasReferenceRange()) {
                    Range referenceRange = new Range();
                    if (component.getReferenceRangeFirstRep().hasLow()) {
                        referenceRange.setLow(observation.getReferenceRangeFirstRep().getLow());
                    }
                    if (component.getReferenceRangeFirstRep().hasHigh()) {
                        referenceRange.setHigh(observation.getReferenceRangeFirstRep().getHigh());
                    }
                    resultComponent.setReferenceRange(referenceRange);
                }
                resultComponents.add(resultComponent);
            }
            laboratoryTestPerformed.setComponents(resultComponents);
        }

        // id
        if (observation.hasId()) {
            laboratoryTestPerformed.setId(new Id().setValue(new StringType(observation.getId())));
        }

        // Source
        if (observation.hasPerformer()) {
            // TODO - Source? It is not in the schema or modelinfo, but it is in the qdm-to-qicore mappings
        }

        return laboratoryTestPerformed;
    }

    public ProcedurePerformed createProcedurePerformed(Object fhirResource, boolean isPositive) {
        if (!(fhirResource instanceof Procedure)) {
            throw new IllegalArgumentException("Expecting FHIR Procedure resource, found " + fhirResource.getClass().getSimpleName());
        }

        Procedure procedure = (Procedure) fhirResource;

        ProcedurePerformed procedurePerformed = isPositive ? new PositiveProcedurePerformed() : new NegativeProcedurePerformed();

        // Relevant Period
        if (procedure.hasPerformed()) {
            if (procedure.getPerformed() instanceof Period) {
                procedurePerformed.setRelevantPeriod((Period) procedure.getPerformed());
            }
            else if (procedure.getPerformed() instanceof DateTimeType) {
                procedurePerformed.setRelevantPeriod(
                        new Period()
                                .setStart(((DateTimeType) procedure.getPerformed()).getValue())
                                .setEnd(((DateTimeType) procedure.getPerformed()).getValue())
                );
            }
        }

        // Negation Rationale
        if (procedure.hasNotDoneReason() && procedure.getNotDoneReason().hasCoding()) {
            procedurePerformed.setNegationRationale(procedure.getNotDoneReason().getCodingFirstRep());
        }

        // Reason
        if (procedure.hasReasonCode() && procedure.getReasonCodeFirstRep().hasCoding()) {
            procedurePerformed.setReason(procedure.getReasonCodeFirstRep().getCodingFirstRep());
        }
        else if (procedure.hasReasonCode() && procedure.getReasonCodeFirstRep().hasText()) {
            procedurePerformed.setReason(new Coding().setDisplay(procedure.getReasonCodeFirstRep().getText()));
        }

        // Result
        if (procedure.hasOutcome() && procedure.getOutcome().hasCoding()) {
            procedurePerformed.setResult(procedure.getOutcome().getCodingFirstRep());
        }

        // AnatomicalLocation
        if (procedure.hasBodySite() && procedure.getBodySiteFirstRep().hasCoding()) {
            procedurePerformed.setAnatomicalLocationSite(procedure.getBodySiteFirstRep().getCodingFirstRep());
        }

        // IncisionDateTime
        if (procedure.hasExtension()) {
            for (Extension extension : procedure.getExtension()) {
                if (extension.getUrl().equals("http://hl7.org/fhir/StructureDefinition/procedure-incisionDateTime")) {
                    if (extension.hasValue()) {
                        procedurePerformed.setIncisionDatetime((DateTimeType) extension.getValue());
                        break;
                    }
                }
            }
        }

        // Status
        if (procedure.hasStatus()) {
            procedurePerformed.setStatus(
                    new Coding()
                            .setCode(procedure.getStatus().toCode())
                            .setSystem(procedure.getStatus().getSystem())
                            .setDisplay(procedure.getStatus().getDisplay())
            );
        }

        // Code
        if (procedure.hasCode() && procedure.getCode().hasCoding()) {
            procedurePerformed.setCode(procedure.getCode().getCodingFirstRep());
        }

        // id
        if (procedure.hasId()) {
            procedurePerformed.setId(new Id().setValue(new StringType(procedure.getId())));
        }

        // Source
        if (procedure.hasPerformer()) {
            // TODO - Source? It is not in the schema or modelinfo, but it is in the qdm-to-qicore mappings
        }

        return procedurePerformed;
    }

    public PatientCharacteristicBirthdate createPatientCharacteristicBirthdate(Object fhirResource) {
        if (!(fhirResource instanceof Patient)) {
            throw new IllegalArgumentException("Expecting FHIR Patient resource, found " + fhirResource.getClass().getSimpleName());
        }

        Patient patient = (Patient) fhirResource;

        PatientCharacteristicBirthdate patientCharacteristicBirthdate = new PatientCharacteristicBirthdate();

        if (patient.hasExtension()) {
            for (Extension extension : patient.getExtension()) {
                if (extension.getUrl().equals("http://hl7.org/fhir/StructureDefinition/patient-birthTime")) {
                    if (extension.hasValue()) {
                        patientCharacteristicBirthdate.setBirthDatetime((DateTimeType) extension.getValue());
                        break;
                    }
                }
            }
        }
        if (patient.hasBirthDate() && patientCharacteristicBirthdate.getBirthDatetime() == null) {
            patientCharacteristicBirthdate.setBirthDatetime(new DateTimeType(patient.getBirthDate()));
        }

        if (patient.hasId()) {
            patientCharacteristicBirthdate.setId(new Id().setValue(new StringType(patient.getId())));
        }

        return patientCharacteristicBirthdate;
    }

    public PatientCharacteristicSex createPatientCharacteristicSex(Object fhirResource) {
        if (!(fhirResource instanceof Patient)) {
            throw new IllegalArgumentException("Expecting FHIR Patient resource, found " + fhirResource.getClass().getSimpleName());
        }

        Patient patient = (Patient) fhirResource;

        PatientCharacteristicSex patientCharacteristicSex = new PatientCharacteristicSex();

        if (patient.hasGender()) {
            patientCharacteristicSex.setCode(
                    new Coding()
                            .setCode(patient.getGender().toCode())
                            .setSystem(patient.getGender().getSystem())
                            .setDisplay(patient.getGender().getDisplay())
            );
        }

        if (patient.hasId()) {
            patientCharacteristicSex.setId(new Id().setValue(new StringType(patient.getId())));
        }

        return patientCharacteristicSex;
    }
}