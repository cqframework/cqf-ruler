package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.Patient;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientRepository;
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
}
