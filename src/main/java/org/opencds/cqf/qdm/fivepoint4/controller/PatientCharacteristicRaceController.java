package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicRace;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicRaceRepository;
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
public class PatientCharacteristicRaceController implements Serializable
{
    private final PatientCharacteristicRaceRepository repository;

    @Autowired
    public PatientCharacteristicRaceController(PatientCharacteristicRaceRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristicRace")
    public List<PatientCharacteristicRace> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristicRace/{id}")
    @ResponseBody
    public PatientCharacteristicRace getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(


                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristicRace/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristicRace")
    @ResponseBody
    public PatientCharacteristicRace create(@RequestBody @Valid PatientCharacteristicRace patientCharacteristicRace)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristicRace, patientCharacteristicRace);
        return repository.save(patientCharacteristicRace);
    }

    @PutMapping("/PatientCharacteristicRace/{id}")
    public PatientCharacteristicRace update(@PathVariable(value = "id") String id,
                                @RequestBody @Valid PatientCharacteristicRace patientCharacteristicRace)
    {
        QdmValidator.validateResourceId(patientCharacteristicRace.getId(), id);
        Optional<PatientCharacteristicRace> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristicRace updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristicRace, updateResource);
            updateResource.copy(patientCharacteristicRace);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristicRace, patientCharacteristicRace);
        return repository.save(patientCharacteristicRace);
    }

    @DeleteMapping("/PatientCharacteristicRace/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristicRace pcs =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristicRace/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pcs);

        return ResponseEntity.ok().build();
    }
}
