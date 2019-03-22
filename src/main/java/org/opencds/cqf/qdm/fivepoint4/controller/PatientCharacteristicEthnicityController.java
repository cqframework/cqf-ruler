package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristic;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicEthnicity;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicEthnicityRepository;
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
public class PatientCharacteristicEthnicityController implements Serializable
{
    private final PatientCharacteristicEthnicityRepository repository;

    @Autowired
    public PatientCharacteristicEthnicityController(PatientCharacteristicEthnicityRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristicEthnicity")
    public List<PatientCharacteristicEthnicity> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristicEthnicity/{id}")
    @ResponseBody
    public PatientCharacteristicEthnicity getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(


                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristicEthnicity/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristicEthnicity")
    @ResponseBody
    public PatientCharacteristicEthnicity create(@RequestBody @Valid PatientCharacteristicEthnicity patientCharacteristicEthnicity)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristicEthnicity, patientCharacteristicEthnicity);
        return repository.save(patientCharacteristicEthnicity);
    }

    @PutMapping("/PatientCharacteristicEthnicity/{id}")
    public PatientCharacteristicEthnicity update(@PathVariable(value = "id") String id,
                                @RequestBody @Valid PatientCharacteristicEthnicity patientCharacteristicEthnicity)
    {
        QdmValidator.validateResourceId(patientCharacteristicEthnicity.getId(), id);
        Optional<PatientCharacteristicEthnicity> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristicEthnicity updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristicEthnicity, updateResource);
            updateResource.copy(patientCharacteristicEthnicity);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristicEthnicity, patientCharacteristicEthnicity);
        return repository.save(patientCharacteristicEthnicity);
    }

    @DeleteMapping("/PatientCharacteristicEthnicity/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristicEthnicity pcs =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristicEthnicity/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pcs);

        return ResponseEntity.ok().build();
    }
}
