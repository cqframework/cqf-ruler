package org.opencds.cqf.qdm.fivepoint4.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.opencds.cqf.qdm.fivepoint4.QdmContext;
import org.opencds.cqf.qdm.fivepoint4.exception.InvalidResourceType;
import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.*;
import org.opencds.cqf.qdm.fivepoint4.validation.QdmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
public class PatientController implements Serializable
{
    private final PatientRepository repository;

    @Autowired
    public PatientController(PatientRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/Patient")
    public List<Patient> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/Patient/{id}")
    public @ResponseBody Patient getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: Patient/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    // Returns a List with the Patient and all the Characteristics associated with that patient
    @GetMapping("Patient/{id}/$characteristics")
    public @ResponseBody List<Object> getPatientCharacteristics(@PathVariable(value = "id") String id)
    {
        return getPatientCharacteristics(getById(id));
    }

    private List<Object> getPatientCharacteristics(Patient patient)
    {
        List<Object> result = new ArrayList<>();
        result.add(patient);

        String patientId = patient.getSystemId();

        QdmContext.getBean(PatientCharacteristicBirthdateRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);
        QdmContext.getBean(PatientCharacteristicClinicalTrialParticipantRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);
        QdmContext.getBean(PatientCharacteristicEthnicityRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);
        QdmContext.getBean(PatientCharacteristicExpiredRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);
        QdmContext.getBean(PatientCharacteristicPayerRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);
        QdmContext.getBean(PatientCharacteristicRaceRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);
        QdmContext.getBean(PatientCharacteristicRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);
        QdmContext.getBean(PatientCharacteristicSexRepository.class).findByPatientIdValue(patientId).ifPresent(result::addAll);

        return result;
    }

    @GetMapping("Patient/$characteristics")
    public @ResponseBody List<List<Object>> getAllPatientCharacteristics()
    {
        List<List<Object>> result = new ArrayList<>();
        for (Patient patient : getAll())
        {
            result.add(getPatientCharacteristics(patient));
        }

        return result;
    }

    @PostMapping("Patient/$characteristics")
    public @ResponseBody List<Object> postPatientCharacteristics(@RequestBody List<Object> patientCharacteristics) throws IOException
    {
        List<Object> results = new ArrayList<>();
        patientCharacteristics = parseResources(patientCharacteristics);
        String patientId = null;

        // Find patient first - need patient id for validation
        for (Object characteristic : patientCharacteristics)
        {
            if (characteristic instanceof Patient)
            {
                results.add(
                        ((Patient) characteristic).getId() != null
                                ? update(((Patient) characteristic).getId().getValue(), (Patient) characteristic)
                                : create((Patient) characteristic)
                );
                patientId = ((Patient) results.get(0)).getSystemId();
                break;
            }
        }

        // Now operate on characteristics
        for (Object characteristic : patientCharacteristics)
        {
            if (characteristic instanceof PatientCharacteristic)
            {
                PatientCharacteristicController controller = QdmContext.getBean(PatientCharacteristicController.class);
                PatientCharacteristic patientCharacteristic = (PatientCharacteristic) characteristic;

                validatePatientId(patientCharacteristic, patientId);

                results.add(
                        patientCharacteristic.getId() != null
                                ? controller.update(patientCharacteristic.getId().getValue(), patientCharacteristic)
                                : controller.create(patientCharacteristic)
                );
            }
            else if (characteristic instanceof PatientCharacteristicBirthdate)
            {
                PatientCharacteristicBirthdateController controller = QdmContext.getBean(PatientCharacteristicBirthdateController.class);
                PatientCharacteristicBirthdate patientCharacteristicBirthdate = (PatientCharacteristicBirthdate) characteristic;

                validatePatientId(patientCharacteristicBirthdate, patientId);

                results.add(
                        patientCharacteristicBirthdate.getId() != null
                                ? controller.update(patientCharacteristicBirthdate.getId().getValue(), patientCharacteristicBirthdate)
                                : controller.create(patientCharacteristicBirthdate)
                );
            }
            else if (characteristic instanceof PatientCharacteristicClinicalTrialParticipant)
            {
                PatientCharacteristicClinicalTrialParticipantController controller = QdmContext.getBean(PatientCharacteristicClinicalTrialParticipantController.class);
                PatientCharacteristicClinicalTrialParticipant patientCharacteristicClinicalTrialParticipant = (PatientCharacteristicClinicalTrialParticipant) characteristic;

                validatePatientId(patientCharacteristicClinicalTrialParticipant, patientId);

                results.add(
                        patientCharacteristicClinicalTrialParticipant.getId() != null
                                ? controller.update(patientCharacteristicClinicalTrialParticipant.getId().getValue(), patientCharacteristicClinicalTrialParticipant)
                                : controller.create(patientCharacteristicClinicalTrialParticipant)
                );
            }
            else if (characteristic instanceof PatientCharacteristicEthnicity)
            {
                PatientCharacteristicEthnicityController controller = QdmContext.getBean(PatientCharacteristicEthnicityController.class);
                PatientCharacteristicEthnicity patientCharacteristicEthnicity = (PatientCharacteristicEthnicity) characteristic;

                validatePatientId(patientCharacteristicEthnicity, patientId);

                results.add(
                        patientCharacteristicEthnicity.getId() != null
                                ? controller.update(patientCharacteristicEthnicity.getId().getValue(), patientCharacteristicEthnicity)
                                : controller.create(patientCharacteristicEthnicity)
                );
            }
            else if (characteristic instanceof PatientCharacteristicExpired)
            {
                PatientCharacteristicExpiredController controller = QdmContext.getBean(PatientCharacteristicExpiredController.class);
                PatientCharacteristicExpired patientCharacteristicExpired = (PatientCharacteristicExpired) characteristic;

                validatePatientId(patientCharacteristicExpired, patientId);

                results.add(
                        ((PatientCharacteristicExpired) characteristic).getId() != null
                                ? controller.update(patientCharacteristicExpired.getId().getValue(), patientCharacteristicExpired)
                                : controller.create(patientCharacteristicExpired)
                );
            }
            else if (characteristic instanceof PatientCharacteristicPayer)
            {
                PatientCharacteristicPayerController controller = QdmContext.getBean(PatientCharacteristicPayerController.class);
                PatientCharacteristicPayer patientCharacteristicPayer = (PatientCharacteristicPayer) characteristic;

                validatePatientId(patientCharacteristicPayer, patientId);

                results.add(
                        patientCharacteristicPayer.getId() != null
                                ? controller.update(patientCharacteristicPayer.getId().getValue(), patientCharacteristicPayer)
                                : controller.create(patientCharacteristicPayer)
                );
            }
            else if (characteristic instanceof PatientCharacteristicRace)
            {
                PatientCharacteristicRaceController controller = QdmContext.getBean(PatientCharacteristicRaceController.class);
                PatientCharacteristicRace patientCharacteristicRace = (PatientCharacteristicRace) characteristic;

                validatePatientId(patientCharacteristicRace, patientId);

                results.add(
                        patientCharacteristicRace.getId() != null
                                ? controller.update(patientCharacteristicRace.getId().getValue(), patientCharacteristicRace)
                                : controller.create(patientCharacteristicRace)
                );
            }
            else if (characteristic instanceof PatientCharacteristicSex)
            {
                PatientCharacteristicSexController controller = QdmContext.getBean(PatientCharacteristicSexController.class);
                PatientCharacteristicSex patientCharacteristicSex = (PatientCharacteristicSex) characteristic;

                validatePatientId(patientCharacteristicSex, patientId);

                results.add(
                        patientCharacteristicSex.getId() != null
                                ? controller.update(patientCharacteristicSex.getId().getValue(), patientCharacteristicSex)
                                : controller.create(patientCharacteristicSex)
                );
            }
        }

        // Make sure characteristics reference Patient

        return results;
    }

    @PostMapping("/Patient")
    public @ResponseBody Patient create(@RequestBody @Valid Patient patient)
    {
        QdmValidator.validatePatientTypeAndName(patient, patient);
        return repository.save(patient);
    }

    @PutMapping("/Patient/{id}")
    public Patient update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid Patient patient)
    {
        QdmValidator.validateResourceId(patient.getId(), id);
        Optional<Patient> update = repository.findById(id);
        if (update.isPresent())
        {
            Patient updateResource = update.get();
            QdmValidator.validatePatientTypeAndName(patient, updateResource);
            updateResource.copy(patient);
            return repository.save(updateResource);
        }

        QdmValidator.validatePatientTypeAndName(patient, patient);
        return repository.save(patient);
    }

    @DeleteMapping("/Patient/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        //Delete patient resources
        List<String> patientResourceTypes = getPatientResourceTypeList(id);
        for (String resourceType : patientResourceTypes) {
			try {
                BaseRepository<BaseType> resourceRepository = (BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + resourceType + "Repository"));
                resourceRepository.findByPatientIdValue(id).ifPresent(resourceRepository::deleteAll);
                // Optional<List<BaseType>> patientResources = resourceRepository.findByPatientIdValue(id);
                // if (patientResources.isPresent()) {
                //     resourceRepository.deleteAll(patientResources.get());
                // }
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        //Delete patient characteristics
        PatientCharacteristicBirthdateRepository patientCharacteristicBirthdateRepository = QdmContext.getBean(PatientCharacteristicBirthdateRepository.class);
        patientCharacteristicBirthdateRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicBirthdateRepository::deleteAll);
        PatientCharacteristicClinicalTrialParticipantRepository patientCharacteristicClinicalTrialParticipantRepository = QdmContext.getBean(PatientCharacteristicClinicalTrialParticipantRepository.class);
        patientCharacteristicClinicalTrialParticipantRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicClinicalTrialParticipantRepository::deleteAll);
        PatientCharacteristicEthnicityRepository patientCharacteristicEthnicityRepository = QdmContext.getBean(PatientCharacteristicEthnicityRepository.class);
        patientCharacteristicEthnicityRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicEthnicityRepository::deleteAll);
        PatientCharacteristicExpiredRepository patientCharacteristicExpiredRepository = QdmContext.getBean(PatientCharacteristicExpiredRepository.class);
        patientCharacteristicExpiredRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicExpiredRepository::deleteAll);
        PatientCharacteristicPayerRepository patientCharacteristicPayerRepository = QdmContext.getBean(PatientCharacteristicPayerRepository.class);
        patientCharacteristicPayerRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicPayerRepository::deleteAll);
        PatientCharacteristicRaceRepository patientCharacteristicRaceRepository = QdmContext.getBean(PatientCharacteristicRaceRepository.class);
        patientCharacteristicRaceRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicRaceRepository::deleteAll);
        PatientCharacteristicRepository patientCharacteristicRepository = QdmContext.getBean(PatientCharacteristicRepository.class);
        patientCharacteristicRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicRepository::deleteAll);
        PatientCharacteristicSexRepository patientCharacteristicSexRepository = QdmContext.getBean(PatientCharacteristicSexRepository.class);
        patientCharacteristicSexRepository.findByPatientIdValue(id).ifPresent(patientCharacteristicSexRepository::deleteAll);

        Patient pat =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: Patient/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pat);

        return ResponseEntity.ok().build();
    }

    @GetMapping("Patient/{id}/$get-resource-type-list")
    public @ResponseBody List<String> getPatientResourceTypeList(@PathVariable(value = "id") String id)
    {
        return getPatientResourceTypeList(getById(id));
    }

    private List<String> getPatientResourceTypeList(Patient patient)
    {
        List<String> result = new ArrayList<String>();

        String patientId = patient.getSystemId();

        for (Types type : Types.values()) {
			try {
                if (((BaseRepository<BaseType>) QdmContext.getBean(Class.forName("org.opencds.cqf.qdm.fivepoint4.repository." + type.name() + "Repository"))).findByPatientIdValue(patientId).isPresent()) {
                    result.add(type.name());
                }
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        return result;
    }

    @GetMapping("Patient/{id}/$get-patients-for-measure")
    public @ResponseBody List<Patient> getPatientsForMeasure(@PathVariable(value = "id") String id)
    {
        Patient examplePatient = new Patient();
        examplePatient.setSystemId(id + "-");
        ExampleMatcher patientMatcher = ExampleMatcher.matchingAny().withMatcher("systemId", ExampleMatcher.GenericPropertyMatchers.startsWith());
        Example<Patient> example = Example.of(examplePatient, patientMatcher);
        
        return repository.findAll(example);

        // List<Patient> result = new ArrayList<>();
        // for (Patient patient : getAll())
        // {
        //     if (patient.getSystemId().startsWith(id)) {
        //         result.add(patient);
        //     }
        // }

        // return result;
    }

    private List<Object> parseResources(List<Object> raw) throws IOException
    {
        List<Object> parsedResources = new ArrayList<>();
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
                }
            }
        }

        return parsedResources;
    }

    private void validatePatientId(BaseType characteristic, String patientId)
    {
        // validate patientId
        if (characteristic.getPatientId() == null)
        {
            characteristic.setPatientId(new Id(patientId, null));
        }
        else if (!characteristic.getPatientId().getValue().equals(patientId)) {
            characteristic.getPatientId().setValue(patientId);
        }
    }
}
