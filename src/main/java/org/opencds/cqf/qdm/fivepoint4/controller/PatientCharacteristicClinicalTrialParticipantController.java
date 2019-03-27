package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicClinicalTrialParticipant;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicClinicalTrialParticipantRepository;
import org.opencds.cqf.qdm.fivepoint4.validation.QdmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
public class PatientCharacteristicClinicalTrialParticipantController implements Serializable
{
    private final PatientCharacteristicClinicalTrialParticipantRepository repository;

    @Autowired
    public PatientCharacteristicClinicalTrialParticipantController(PatientCharacteristicClinicalTrialParticipantRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristicClinicalTrialParticipant")
    public List<PatientCharacteristicClinicalTrialParticipant> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristicClinicalTrialParticipant/{id}")
    public @ResponseBody
    PatientCharacteristicClinicalTrialParticipant getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristicClinicalTrialParticipant/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristicClinicalTrialParticipant")
    public @ResponseBody PatientCharacteristicClinicalTrialParticipant create(@RequestBody @Valid PatientCharacteristicClinicalTrialParticipant patientCharacteristicClinicalTrialParticipant)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristicClinicalTrialParticipant, patientCharacteristicClinicalTrialParticipant);
        return repository.save(patientCharacteristicClinicalTrialParticipant);
    }

    @PutMapping("/PatientCharacteristicClinicalTrialParticipant/{id}")
    public PatientCharacteristicClinicalTrialParticipant update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PatientCharacteristicClinicalTrialParticipant patientCharacteristicClinicalTrialParticipant)
    {
        QdmValidator.validateResourceId(patientCharacteristicClinicalTrialParticipant.getId(), id);
        Optional<PatientCharacteristicClinicalTrialParticipant> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristicClinicalTrialParticipant updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristicClinicalTrialParticipant, updateResource);
            updateResource.copy(patientCharacteristicClinicalTrialParticipant);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristicClinicalTrialParticipant, patientCharacteristicClinicalTrialParticipant);
        return repository.save(patientCharacteristicClinicalTrialParticipant);
    }

    @DeleteMapping("/PatientCharacteristicClinicalTrialParticipant/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristicClinicalTrialParticipant par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristicClinicalTrialParticipant/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
