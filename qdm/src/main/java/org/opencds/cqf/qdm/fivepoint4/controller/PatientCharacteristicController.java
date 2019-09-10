package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristic;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicRepository;
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
public class PatientCharacteristicController implements Serializable
{
    private final PatientCharacteristicRepository repository;

    @Autowired
    public PatientCharacteristicController(PatientCharacteristicRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristic")
    public List<PatientCharacteristic> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristic/{id}")
    @ResponseBody
    public PatientCharacteristic getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(


                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristic/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristic")
    @ResponseBody
    public PatientCharacteristic create(@RequestBody @Valid PatientCharacteristic patientCharacteristic)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristic, patientCharacteristic);
        return repository.save(patientCharacteristic);
    }

    @PutMapping("/PatientCharacteristic/{id}")
    public PatientCharacteristic update(@PathVariable(value = "id") String id,
                                @RequestBody @Valid PatientCharacteristic patientCharacteristic)
    {
        QdmValidator.validateResourceId(patientCharacteristic.getId(), id);
        Optional<PatientCharacteristic> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristic updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristic, updateResource);
            updateResource.copy(patientCharacteristic);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristic, patientCharacteristic);
        return repository.save(patientCharacteristic);
    }

    @DeleteMapping("/PatientCharacteristic/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristic pcs =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristic/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pcs);

        return ResponseEntity.ok().build();
    }
}
