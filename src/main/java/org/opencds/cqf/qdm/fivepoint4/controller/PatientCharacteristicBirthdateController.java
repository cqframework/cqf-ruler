package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicBirthdate;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicBirthdateRepository;
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

@RestController
public class PatientCharacteristicBirthdateController implements Serializable
{
    private final PatientCharacteristicBirthdateRepository repository;

    @Autowired
    public PatientCharacteristicBirthdateController(PatientCharacteristicBirthdateRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristicBirthdate")
    public List<PatientCharacteristicBirthdate> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristicBirthdate/{id}")
    public @ResponseBody
    PatientCharacteristicBirthdate getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristicBirthdate/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristicBirthdate")
    public @ResponseBody PatientCharacteristicBirthdate create(@RequestBody @Valid PatientCharacteristicBirthdate patientCharacteristicBirthdate)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristicBirthdate, patientCharacteristicBirthdate);
        return repository.save(patientCharacteristicBirthdate);
    }

    @PutMapping("/PatientCharacteristicBirthdate/{id}")
    public PatientCharacteristicBirthdate update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PatientCharacteristicBirthdate patientCharacteristicBirthdate)
    {
        QdmValidator.validateResourceId(patientCharacteristicBirthdate.getId(), id);
        Optional<PatientCharacteristicBirthdate> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristicBirthdate updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristicBirthdate, updateResource);
            updateResource.copy(patientCharacteristicBirthdate);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristicBirthdate, patientCharacteristicBirthdate);
        return repository.save(patientCharacteristicBirthdate);
    }

    @DeleteMapping("/PatientCharacteristicBirthdate/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristicBirthdate par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristicBirthdate/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
