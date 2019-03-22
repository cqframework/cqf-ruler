package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveImmunizationAdministered;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveImmunizationAdministeredRepository;
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
public class PositiveImmunizationAdministeredController implements Serializable
{
    private final PositiveImmunizationAdministeredRepository repository;

    @Autowired
    public PositiveImmunizationAdministeredController(PositiveImmunizationAdministeredRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveImmunizationAdministered")
    public List<PositiveImmunizationAdministered> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveImmunizationAdministered/{id}")
    public @ResponseBody PositiveImmunizationAdministered getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveImmunizationAdministered/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveImmunizationAdministered")
    public PositiveImmunizationAdministered create(@RequestBody @Valid PositiveImmunizationAdministered positiveImmunizationAdministered)
    {
        QdmValidator.validateResourceTypeAndName(positiveImmunizationAdministered, positiveImmunizationAdministered);
        return repository.save(positiveImmunizationAdministered);
    }

    @PutMapping("/PositiveImmunizationAdministered/{id}")
    public PositiveImmunizationAdministered update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveImmunizationAdministered positiveImmunizationAdministered)
    {
        QdmValidator.validateResourceId(positiveImmunizationAdministered.getId(), id);
        Optional<PositiveImmunizationAdministered> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveImmunizationAdministered updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveImmunizationAdministered, updateResource);
            updateResource.copy(positiveImmunizationAdministered);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveImmunizationAdministered, positiveImmunizationAdministered);
        return repository.save(positiveImmunizationAdministered);
    }

    @DeleteMapping("/PositiveImmunizationAdministered/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveImmunizationAdministered pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveImmunizationAdministered/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
