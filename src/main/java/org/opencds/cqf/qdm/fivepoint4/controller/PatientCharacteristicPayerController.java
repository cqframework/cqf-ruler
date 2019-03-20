package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicPayer;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicPayerRepository;
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
public class PatientCharacteristicPayerController implements Serializable
{
    private final PatientCharacteristicPayerRepository repository;

    @Autowired
    public PatientCharacteristicPayerController(PatientCharacteristicPayerRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristicPayer")
    public List<PatientCharacteristicPayer> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristicPayer/{id}")
    public @ResponseBody
    PatientCharacteristicPayer getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristicPayer/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristicPayer")
    public @ResponseBody PatientCharacteristicPayer create(@RequestBody @Valid PatientCharacteristicPayer patientCharacteristicPayer)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristicPayer, patientCharacteristicPayer);
        return repository.save(patientCharacteristicPayer);
    }

    @PutMapping("/PatientCharacteristicPayer/{id}")
    public PatientCharacteristicPayer update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PatientCharacteristicPayer patientCharacteristicPayer)
    {
        QdmValidator.validateResourceId(patientCharacteristicPayer.getId(), id);
        Optional<PatientCharacteristicPayer> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristicPayer updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristicPayer, updateResource);
            updateResource.copy(patientCharacteristicPayer);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristicPayer, patientCharacteristicPayer);
        return repository.save(patientCharacteristicPayer);
    }

    @DeleteMapping("/PatientCharacteristicPayer/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristicPayer par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristicPayer/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
