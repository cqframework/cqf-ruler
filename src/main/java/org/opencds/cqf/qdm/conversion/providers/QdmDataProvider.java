package org.opencds.cqf.qdm.conversion.providers;

import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.runtime.*;
import org.opencds.cqf.providers.JpaDataProvider;
import org.opencds.cqf.qdm.conversion.model.*;
import org.opencds.cqf.qdm.conversion.model.Patient;
import org.opencds.cqf.qdm.conversion.model.Ratio;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QdmDataProvider extends JpaDataProvider {

    private QdmAdapter adapter;

    public QdmDataProvider(Collection<IResourceProvider> providers) {
        super(providers);
        setPackageName("org.opencds.cqf.qdm.conversion.model");
        adapter = new QdmAdapter(this);
        fhirContext.registerCustomType(Component.class);
        fhirContext.registerCustomType(Diagnosis.class);
        fhirContext.registerCustomType(PositiveDiagnosticStudyPerformed.class);
        fhirContext.registerCustomType(NegativeDiagnosticStudyPerformed.class);
        fhirContext.registerCustomType(PositiveEncounterPerformed.class);
        fhirContext.registerCustomType(NegativeEncounterPerformed.class);
        fhirContext.registerCustomType(FacilityLocation.class);
        fhirContext.registerCustomType(Id.class);
        fhirContext.registerCustomType(PositiveInterventionOrder.class);
        fhirContext.registerCustomType(NegativeInterventionOrder.class);
        fhirContext.registerCustomType(PositiveInterventionPerformed.class);
        fhirContext.registerCustomType(NegativeInterventionPerformed.class);
        fhirContext.registerCustomType(PositiveLaboratoryTestPerformed.class);
        fhirContext.registerCustomType(NegativeLaboratoryTestPerformed.class);
        fhirContext.registerCustomType(Patient.class);
        fhirContext.registerCustomType(PatientCharacteristicBirthdate.class);
        fhirContext.registerCustomType(PatientCharacteristicEthnicity.class);
        fhirContext.registerCustomType(PatientCharacteristicPayer.class);
        fhirContext.registerCustomType(PatientCharacteristicRace.class);
        fhirContext.registerCustomType(PatientCharacteristicSex.class);
        fhirContext.registerCustomType(PositiveProcedurePerformed.class);
        fhirContext.registerCustomType(NegativeProcedurePerformed.class);
        fhirContext.registerCustomType(Ratio.class);
        fhirContext.registerCustomType(ResultComponent.class);
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    }

    @Override
    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        if (codePath != null) {
            codePath = QdmAdapter.mapCodePath(dataType, codePath);
        }
        // TODO - add dateProperty mapping
        datePath = null;
        dateLowPath = null;
        dateHighPath = null;
        dateRange = null;

        Iterable<Object> fhirResources = super.retrieve(context, contextValue, adapter.mapToFhirType(dataType), templateId, codePath, codes, valueSet, datePath, dateLowPath, dateHighPath, dateRange);
        List<Object> qdmResources = new ArrayList<>();

        for (Object fhirResource : fhirResources) {
            qdmResources.add(adapter.createQdmResource(dataType, fhirResource));
        }

        return qdmResources;
    }

    // NOTE: this is used to represent a Null Interval - only used for the intervals that are resolved using extensions (opioid test data)
    public Interval nullInterval = new Interval(
            new DateTime("0001-01-01T00:00:00.000", ZoneOffset.UTC), true,
            new DateTime("0001-01-01T00:00:00.000", ZoneOffset.UTC), true
    );

    @Override
    protected Object toJavaPrimitive(Object result, Object source) {
        if (result instanceof Period) {
            if (QdmAdapter.isPeriodNull((Period) result)) {
                return nullInterval;
            }
            return new Interval(
                    ((Period) result).hasStart() ? DateTime.fromJavaDate(((Period) result).getStart()) : null, true,
                    ((Period) result).hasEnd() ? DateTime.fromJavaDate(((Period) result).getEnd()) : null, true
            );
        }

        if (result instanceof Coding) {
            Coding coding = (Coding) result;
            return new Code()
                    .withCode(coding.getCode())
                    .withSystem(coding.getSystem())
                    .withVersion(coding.getVersion())
                    .withDisplay(coding.getDisplay());
        }

        if (result instanceof Range) {
            Range range = (Range) result;
            return new Interval(
                    new org.opencds.cqf.cql.runtime.Quantity().withValue(range.getLow().getValue()).withUnit(range.getLow().getUnit()), true,
                    new org.opencds.cqf.cql.runtime.Quantity().withValue(range.getHigh().getValue()).withUnit(range.getHigh().getUnit()), true
            );
        }

        if (result instanceof Quantity) {
            return new org.opencds.cqf.cql.runtime.Quantity()
                    .withValue(((Quantity) result).getValue())
                    .withUnit(((Quantity) result).getUnit());
        }

        if (result instanceof DateTimeType) {
            return DateTime.fromJavaDate(((DateTimeType) result).getValue());
        }

        if (result instanceof IPrimitiveType) {
            return ((IPrimitiveType) result).getValue();
        }

        return super.toJavaPrimitive(result, source);
    }

    @Override
    public Object resolvePath(Object target, String path) {
        return toJavaPrimitive(super.resolvePath(target, path), target);
    }

}
