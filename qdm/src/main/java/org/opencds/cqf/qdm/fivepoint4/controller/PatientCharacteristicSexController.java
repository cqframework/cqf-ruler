package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicSex;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicSexRepository;
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
public class PatientCharacteristicSexController implements Serializable
{
    private final PatientCharacteristicSexRepository repository;

    @Autowired
    public PatientCharacteristicSexController(PatientCharacteristicSexRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristicSex")
    public List<PatientCharacteristicSex> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristicSex/{id}")
    public @ResponseBody
    PatientCharacteristicSex getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(


                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristicSex/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristicSex")
    public @ResponseBody PatientCharacteristicSex create(@RequestBody @Valid PatientCharacteristicSex patientCharacteristicSex)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristicSex, patientCharacteristicSex);
        return repository.save(patientCharacteristicSex);
    }

    @PutMapping("/PatientCharacteristicSex/{id}")
    public PatientCharacteristicSex update(@PathVariable(value = "id") String id,
                                @RequestBody @Valid PatientCharacteristicSex patientCharacteristicSex)
    {
        QdmValidator.validateResourceId(patientCharacteristicSex.getId(), id);
        Optional<PatientCharacteristicSex> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristicSex updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristicSex, updateResource);
            updateResource.copy(patientCharacteristicSex);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristicSex, patientCharacteristicSex);
        return repository.save(patientCharacteristicSex);
    }

    @DeleteMapping("/PatientCharacteristicSex/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristicSex pcs =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristicSex/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pcs);

        return ResponseEntity.ok().build();
    }
}
