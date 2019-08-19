package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.QdmContext;
import org.opencds.cqf.qdm.fivepoint4.exception.InvalidResourceType;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.*;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@CrossOrigin(origins = "*")
@RestController
public class BaseController implements Serializable
{
    @GetMapping("{id}/$delete-measure-data")
    public ResponseEntity<?> deleteMeasureData(@PathVariable(value = "id") String id)
    {
        PatientRepository patientRepository = QdmContext.getBean(PatientRepository.class);
        Patient examplePatient = new Patient();
        examplePatient.setSystemId(id + "-");
        ExampleMatcher patientMatcher = ExampleMatcher.matchingAny().withMatcher("systemId", ExampleMatcher.GenericPropertyMatchers.startsWith());
        Example<Patient> example = Example.of(examplePatient, patientMatcher);
        List<Patient> patients = patientRepository.findAll(example);

        Id patientId = new Id();
        patientId.setValue(id + "-");
        ExampleMatcher resourceMatcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.startsWith());

        for (Types resourceType : Types.values()) {
			try {
                BaseType exampleType = getNewType(resourceType.name());
                exampleType.setPatientId(patientId);
                Example<BaseType> resourceExample = Example.of((BaseType) exampleType, resourceMatcher);
                BaseRepository<BaseType> resourceRepository = (BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + resourceType.name() + "Repository"));
                List<BaseType> resources = resourceRepository.findAll(resourceExample);
                resourceRepository.deleteAll(resources);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }

        for (PatientCharacteristicTypes resourceType : PatientCharacteristicTypes.values()) {
			try {
                BaseType exampleType = getNewType(resourceType.name());
                exampleType.setPatientId(patientId);
                Example<BaseType> resourceExample = Example.of((BaseType) exampleType, resourceMatcher);
                BaseRepository<BaseType> resourceRepository = (BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + resourceType.name() + "Repository"));
                List<BaseType> resources = resourceRepository.findAll(resourceExample);
                resourceRepository.deleteAll(resources);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }

        patientRepository.deleteAll(patients);

        return ResponseEntity.ok().build();
    }

    @GetMapping("$delete-test-data")
    public ResponseEntity<?> deleteTestData()
    {
        for (Types resourceType : Types.values()) {
			try {
                BaseRepository<BaseType> resourceRepository = (BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + resourceType.name() + "Repository"));
                resourceRepository.deleteAll();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }

        for (PatientCharacteristicTypes resourceType : PatientCharacteristicTypes.values()) {
			try {
                BaseRepository<BaseType> resourceRepository = (BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + resourceType.name() + "Repository"));
                resourceRepository.deleteAll();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }

        PatientRepository patientRepository = QdmContext.getBean(PatientRepository.class);
        patientRepository.deleteAll();

        return ResponseEntity.ok().build();
    }

    @PostMapping("")
    public @ResponseBody List<String> postBundle(@RequestBody List<Object> resources) throws IOException
    {
        ParseResponse response = parseResources(resources);
        List<String> errorList = response.getErrorList();
        try {
            resources = response.getParsedResources();
            List<Patient> patients = new ArrayList<>();
            for (Object resource : resources)
            {
                if (resource instanceof Patient) {
                    patients.add((Patient) resource);
                }
            }
            PatientRepository patientRepository = QdmContext.getBean(PatientRepository.class);
            patientRepository.saveAll(patients);
        } catch (Exception e) {
            errorList.add(e.getMessage());
        }
    
        for (Object resource : resources) {
            if (!(resource instanceof Patient)) {
                String resourceType = ((BaseType) resource).getResourceType();
                try {
                    BaseRepository<BaseType> resourceRepository = (BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + resourceType + "Repository"));
                    resourceRepository.save((BaseType) resource);
                } catch (Exception e) {
                    errorList.add("Failed to save resource (" + ((BaseType) resource).getSystemId() + "):" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return errorList;
    }

    private final class ParseResponse
    {
        private final List<Object> parsedResources;
        private final List<String> errorList;

        public ParseResponse(List<Object> parsedResources, List<String> errorList)
        {
            this.parsedResources = parsedResources;
            this.errorList = errorList;
        }

        public List<Object> getParsedResources()
        {
            return this.parsedResources;
        }

        public List<String> getErrorList()
        {
            return this.errorList;
        }

    }
    
    private ParseResponse parseResources(List<Object> raw) throws IOException
    {
        List<Object> parsedResources = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        Gson gson = new Gson();

        for (Object rawObj : raw)
        {
            if (rawObj instanceof LinkedHashMap)
            {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) rawObj;
                String type;
                if (map.containsKey("resourceType"))
                {
                    type = (String) map.get("resourceType");
                }
                else {
                    throw new InvalidResourceType();
                }
                String id;
                if (map.containsKey("id")) {
                    LinkedHashMap<String, Object> idMap = (LinkedHashMap<String, Object>) map.get("id");
                    if (idMap.containsKey("value")) {
                        id = (String) idMap.get("value");
                    }
                    else {
                        throw new InvalidResourceType();
                    }
                }
                else {
                    throw new InvalidResourceType();
                }

                try {
                    switch(type)
                    {
                        case "Patient":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), Patient.class));
                            break;
                        case "PatientCharacteristicBirthdate":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristicBirthdate.class));
                            break;
                        case "PatientCharacteristicClinicalTrialParticipant":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristicClinicalTrialParticipant.class));
                            break;
                        case "PatientCharacteristicEthnicity":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristicEthnicity.class));
                            break;
                        case "PatientCharacteristicExpired":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristicExpired.class));
                            break;
                        case "PatientCharacteristicPayer":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristicPayer.class));
                            break;
                        case "PatientCharacteristicRace":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristicRace.class));
                            break;
                        case "PatientCharacteristic":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristic.class));
                            break;
                        case "PatientCharacteristicSex":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCharacteristicSex.class));
                            break;
                        case "ProviderCharacteristic":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), ProviderCharacteristic.class));
                            break;
                        case "AdverseEvent":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), AdverseEvent.class));
                            break;
                        case "AllergyIntolerance":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), AllergyIntolerance.class));
                            break;
                        case "PositiveAssessmentOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveAssessmentOrder.class));
                            break;
                        case "NegativeAssessmentOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeAssessmentOrder.class));
                            break;
                        case "PositiveAssessmentPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveAssessmentPerformed.class));
                            break;
                        case "NegativeAssessmentPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeAssessmentPerformed.class));
                            break;
                        case "PositiveAssessmentRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveAssessmentRecommended.class));
                            break;
                        case "NegativeAssessmentRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeAssessmentRecommended.class));
                            break;
                        case "CareGoal":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), CareGoal.class));
                            break;
                        case "PositiveCommunicationPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveCommunicationPerformed.class));
                            break;
                        case "NegativeCommunicationPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeCommunicationPerformed.class));
                            break;
                        case "PositiveDeviceApplied":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveDeviceApplied.class));
                            break;
                        case "NegativeDeviceApplied":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeDeviceApplied.class));
                            break;
                        case "PositiveDeviceOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveDeviceOrder.class));
                            break;
                        case "NegativeDeviceOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeDeviceOrder.class));
                            break;
                        case "PositiveDeviceRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveDeviceRecommended.class));
                            break;
                        case "NegativeDeviceRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeDeviceRecommended.class));
                            break;
                        case "Diagnosis":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), Diagnosis.class));
                            break;
                        case "PositiveDiagnosticStudyOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveDiagnosticStudyOrder.class));
                            break;
                        case "NegativeDiagnosticStudyOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeDiagnosticStudyOrder.class));
                            break;
                        case "PositiveDiagnosticStudyPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveDiagnosticStudyPerformed.class));
                            break;
                        case "NegativeDiagnosticStudyPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeDiagnosticStudyPerformed.class));
                            break;
                        case "PositiveDiagnosticStudyRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveDiagnosticStudyRecommended.class));
                            break;
                        case "NegativeDiagnosticStudyRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeDiagnosticStudyRecommended.class));
                            break;
                        case "PositiveEncounterOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveEncounterOrder.class));
                            break;
                        case "NegativeEncounterOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeEncounterOrder.class));
                            break;
                        case "PositiveEncounterPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveEncounterPerformed.class));
                            break;
                        case "NegativeEncounterPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeEncounterPerformed.class));
                            break;
                        case "PositiveEncounterRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveEncounterRecommended.class));
                            break;
                        case "NegativeEncounterRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeEncounterRecommended.class));
                            break;
                        case "FamilyHistory":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), FamilyHistory.class));
                            break;
                        case "PositiveImmunizationAdministered":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveImmunizationAdministered.class));
                            break;
                        case "NegativeImmunizationAdministered":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeImmunizationAdministered.class));
                            break;
                        case "PositiveImmunizationOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveImmunizationOrder.class));
                            break;
                        case "NegativeImmunizationOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeImmunizationOrder.class));
                            break;
                        case "PositiveInterventionOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveInterventionOrder.class));
                            break;
                        case "NegativeInterventionOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeInterventionOrder.class));
                            break;
                        case "PositiveInterventionPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveInterventionPerformed.class));
                            break;
                        case "NegativeInterventionPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeInterventionPerformed.class));
                            break;
                        case "PositiveInterventionRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveInterventionRecommended.class));
                            break;
                        case "NegativeInterventionRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeInterventionRecommended.class));
                            break;
                        case "PositiveLaboratoryTestOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveLaboratoryTestOrder.class));
                            break;
                        case "NegativeLaboratoryTestOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeLaboratoryTestOrder.class));
                            break;
                        case "PositiveLaboratoryTestPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveLaboratoryTestPerformed.class));
                            break;
                        case "NegativeLaboratoryTestPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeLaboratoryTestPerformed.class));
                            break;
                        case "PositiveLaboratoryTestRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveLaboratoryTestRecommended.class));
                            break;
                        case "NegativeLaboratoryTestRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeLaboratoryTestRecommended.class));
                            break;
                        case "MedicationActive":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), MedicationActive.class));
                            break;
                        case "PositiveMedicationAdministered":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveMedicationAdministered.class));
                            break;
                        case "NegativeMedicationAdministered":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeMedicationAdministered.class));
                            break;
                        case "PositiveMedicationDischarge":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveMedicationDischarge.class));
                            break;
                        case "NegativeMedicationDischarge":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeMedicationDischarge.class));
                            break;
                        case "PositiveMedicationDispensed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveMedicationDispensed.class));
                            break;
                        case "NegativeMedicationDispensed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeMedicationDispensed.class));
                            break;
                        case "PositiveMedicationOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveMedicationOrder.class));
                            break;
                        case "NegativeMedicationOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeMedicationOrder.class));
                            break;
                        case "Participation":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), Participation.class));
                            break;
                        case "PatientCareExperience":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PatientCareExperience.class));
                            break;
                        case "PositivePhysicalExamOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositivePhysicalExamOrder.class));
                            break;
                        case "NegativePhysicalExamOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativePhysicalExamOrder.class));
                            break;
                        case "PositivePhysicalExamPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositivePhysicalExamPerformed.class));
                            break;
                        case "NegativePhysicalExamPerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativePhysicalExamPerformed.class));
                            break;
                        case "PositivePhysicalExamRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositivePhysicalExamRecommended.class));
                            break;
                        case "NegativePhysicalExamRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativePhysicalExamRecommended.class));
                            break;
                        case "PositiveProcedureOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveProcedureOrder.class));
                            break;
                        case "NegativeProcedureOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeProcedureOrder.class));
                            break;
                        case "PositiveProcedurePerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveProcedurePerformed.class));
                            break;
                        case "NegativeProcedurePerformed":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeProcedurePerformed.class));
                            break;
                        case "PositiveProcedureRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveProcedureRecommended.class));
                            break;
                        case "NegativeProcedureRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeProcedureRecommended.class));
                            break;
                        case "ProviderCareExperience":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), ProviderCareExperience.class));
                            break;
                        case "PositiveSubstanceAdministered":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveSubstanceAdministered.class));
                            break;
                        case "NegativeSubstanceAdministered":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeSubstanceAdministered.class));
                            break;
                        case "PositiveSubstanceOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveSubstanceOrder.class));
                            break;
                        case "NegativeSubstanceOrder":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeSubstanceOrder.class));
                            break;
                        case "PositiveSubstanceRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), PositiveSubstanceRecommended.class));
                            break;
                        case "NegativeSubstanceRecommended":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), NegativeSubstanceRecommended.class));
                            break;
                        case "Symptom":
                            parsedResources.add(mapper.readValue(gson.toJson(map, LinkedHashMap.class), Symptom.class));
                            break;
                    }
                } catch (Exception e) {
                    errorList.add("Failed to parse resource (" + id + "):" + e.getMessage());
                }
           }
        }

        return new ParseResponse(parsedResources, errorList);
    }

    private BaseType getNewType(String type)
    {
        switch (type) {
            case "PatientCharacteristicBirthdate":
                return (BaseType) new PatientCharacteristicBirthdate();
            case "PatientCharacteristicClinicalTrialParticipant":
                return (BaseType) new PatientCharacteristicClinicalTrialParticipant();
            case "PatientCharacteristicEthnicity":
                return (BaseType) new PatientCharacteristicEthnicity();
            case "PatientCharacteristicExpired":
                return (BaseType) new PatientCharacteristicExpired();
            case "PatientCharacteristicPayer":
                return (BaseType) new PatientCharacteristicPayer();
            case "PatientCharacteristicRace":
                return (BaseType) new PatientCharacteristicRace();
            case "PatientCharacteristic":
                return (BaseType) new PatientCharacteristic();
            case "PatientCharacteristicSex":
                return (BaseType) new PatientCharacteristicSex();
            case "ProviderCharacteristic":
                return (BaseType) new ProviderCharacteristic();
            case "AdverseEvent":
                return (BaseType) new AdverseEvent();
            case "AllergyIntolerance":
                return (BaseType) new AllergyIntolerance();
            case "PositiveAssessmentOrder":
                return (BaseType) new PositiveAssessmentOrder();
            case "NegativeAssessmentOrder":
                return (BaseType) new NegativeAssessmentOrder();
            case "PositiveAssessmentPerformed":
                return (BaseType) new PositiveAssessmentPerformed();
            case "NegativeAssessmentPerformed":
                return (BaseType) new NegativeAssessmentPerformed();
            case "PositiveAssessmentRecommended":
                return (BaseType) new PositiveAssessmentRecommended();
            case "NegativeAssessmentRecommended":
                return (BaseType) new NegativeAssessmentRecommended();
            case "CareGoal":
                return (BaseType) new CareGoal();
            case "PositiveCommunicationPerformed":
                return (BaseType) new PositiveCommunicationPerformed();
            case "NegativeCommunicationPerformed":
                return (BaseType) new NegativeCommunicationPerformed();
            case "PositiveDeviceApplied":
                return (BaseType) new PositiveDeviceApplied();
            case "NegativeDeviceApplied":
                return (BaseType) new NegativeDeviceApplied();
            case "PositiveDeviceOrder":
                return (BaseType) new PositiveDeviceOrder();
            case "NegativeDeviceOrder":
                return (BaseType) new NegativeDeviceOrder();
            case "PositiveDeviceRecommended":
                return (BaseType) new PositiveDeviceRecommended();
            case "NegativeDeviceRecommended":
                return (BaseType) new NegativeDeviceRecommended();
            case "Diagnosis":
                return (BaseType) new Diagnosis();
            case "PositiveDiagnosticStudyOrder":
                return (BaseType) new PositiveDiagnosticStudyOrder();
            case "NegativeDiagnosticStudyOrder":
                return (BaseType) new NegativeDiagnosticStudyOrder();
            case "PositiveDiagnosticStudyPerformed":
                return (BaseType) new PositiveDiagnosticStudyPerformed();
            case "NegativeDiagnosticStudyPerformed":
                return (BaseType) new NegativeDiagnosticStudyPerformed();
            case "PositiveDiagnosticStudyRecommended":
                return (BaseType) new PositiveDiagnosticStudyRecommended();
            case "NegativeDiagnosticStudyRecommended":
                return (BaseType) new NegativeDiagnosticStudyRecommended();
            case "PositiveEncounterOrder":
                return (BaseType) new PositiveEncounterOrder();
            case "NegativeEncounterOrder":
                return (BaseType) new NegativeEncounterOrder();
            case "PositiveEncounterPerformed":
                return (BaseType) new PositiveEncounterPerformed();
            case "NegativeEncounterPerformed":
                return (BaseType) new NegativeEncounterPerformed();
            case "PositiveEncounterRecommended":
                return (BaseType) new PositiveEncounterRecommended();
            case "NegativeEncounterRecommended":
                return (BaseType) new NegativeEncounterRecommended();
            case "FamilyHistory":
                return (BaseType) new FamilyHistory();
            case "PositiveImmunizationAdministered":
                return (BaseType) new PositiveImmunizationAdministered();
            case "NegativeImmunizationAdministered":
                return (BaseType) new NegativeImmunizationAdministered();
            case "PositiveImmunizationOrder":
                return (BaseType) new PositiveImmunizationOrder();
            case "NegativeImmunizationOrder":
                return (BaseType) new NegativeImmunizationOrder();
            case "PositiveInterventionOrder":
                return (BaseType) new PositiveInterventionOrder();
            case "NegativeInterventionOrder":
                return (BaseType) new NegativeInterventionOrder();
            case "PositiveInterventionPerformed":
                return (BaseType) new PositiveInterventionPerformed();
            case "NegativeInterventionPerformed":
                return (BaseType) new NegativeInterventionPerformed();
            case "PositiveInterventionRecommended":
                return (BaseType) new PositiveInterventionRecommended();
            case "NegativeInterventionRecommended":
                return (BaseType) new NegativeInterventionRecommended();
            case "PositiveLaboratoryTestOrder":
                return (BaseType) new PositiveLaboratoryTestOrder();
            case "NegativeLaboratoryTestOrder":
                return (BaseType) new NegativeLaboratoryTestOrder();
            case "PositiveLaboratoryTestPerformed":
                return (BaseType) new PositiveLaboratoryTestPerformed();
            case "NegativeLaboratoryTestPerformed":
                return (BaseType) new NegativeLaboratoryTestPerformed();
            case "PositiveLaboratoryTestRecommended":
                return (BaseType) new PositiveLaboratoryTestRecommended();
            case "NegativeLaboratoryTestRecommended":
                return (BaseType) new NegativeLaboratoryTestRecommended();
            case "MedicationActive":
                return (BaseType) new MedicationActive();
            case "PositiveMedicationAdministered":
                return (BaseType) new PositiveMedicationAdministered();
            case "NegativeMedicationAdministered":
                return (BaseType) new NegativeMedicationAdministered();
            case "PositiveMedicationDischarge":
                return (BaseType) new PositiveMedicationDischarge();
            case "NegativeMedicationDischarge":
                return (BaseType) new NegativeMedicationDischarge();
            case "PositiveMedicationDispensed":
                return (BaseType) new PositiveMedicationDispensed();
            case "NegativeMedicationDispensed":
                return (BaseType) new NegativeMedicationDispensed();
            case "PositiveMedicationOrder":
                return (BaseType) new PositiveMedicationOrder();
            case "NegativeMedicationOrder":
                return (BaseType) new NegativeMedicationOrder();
            case "Participation":
                return (BaseType) new Participation();
            case "PatientCareExperience":
                return (BaseType) new PatientCareExperience();
            case "PositivePhysicalExamOrder":
                return (BaseType) new PositivePhysicalExamOrder();
            case "NegativePhysicalExamOrder":
                return (BaseType) new NegativePhysicalExamOrder();
            case "PositivePhysicalExamPerformed":
                return (BaseType) new PositivePhysicalExamPerformed();
            case "NegativePhysicalExamPerformed":
                return (BaseType) new NegativePhysicalExamPerformed();
            case "PositivePhysicalExamRecommended":
                return (BaseType) new PositivePhysicalExamRecommended();
            case "NegativePhysicalExamRecommended":
                return (BaseType) new NegativePhysicalExamRecommended();
            case "PositiveProcedureOrder":
                return (BaseType) new PositiveProcedureOrder();
            case "NegativeProcedureOrder":
                return (BaseType) new NegativeProcedureOrder();
            case "PositiveProcedurePerformed":
                return (BaseType) new PositiveProcedurePerformed();
            case "NegativeProcedurePerformed":
                return (BaseType) new NegativeProcedurePerformed();
            case "PositiveProcedureRecommended":
                return (BaseType) new PositiveProcedureRecommended();
            case "NegativeProcedureRecommended":
                return (BaseType) new NegativeProcedureRecommended();
            case "ProviderCareExperience":
                return (BaseType) new ProviderCareExperience();
            case "PositiveSubstanceAdministered":
                return (BaseType) new PositiveSubstanceAdministered();
            case "NegativeSubstanceAdministered":
                return (BaseType) new NegativeSubstanceAdministered();
            case "PositiveSubstanceOrder":
                return (BaseType) new PositiveSubstanceOrder();
            case "NegativeSubstanceOrder":
                return (BaseType) new NegativeSubstanceOrder();
            case "PositiveSubstanceRecommended":
                return (BaseType) new PositiveSubstanceRecommended();
            case "NegativeSubstanceRecommended":
                return (BaseType) new NegativeSubstanceRecommended();
            case "Symptom":
                return (BaseType) new Symptom();

            default:
                throw new InvalidResourceType();
        }
    }
}