package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveSubstanceAdministered;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveSubstanceAdministeredRepository;
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
public class PositiveSubstanceAdministeredController implements Serializable
{
    private final PositiveSubstanceAdministeredRepository repository;

    @Autowired
    public PositiveSubstanceAdministeredController(PositiveSubstanceAdministeredRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveSubstanceAdministered")
    public List<PositiveSubstanceAdministered> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveSubstanceAdministered/{id}")
    public @ResponseBody PositiveSubstanceAdministered getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveSubstanceAdministered/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveSubstanceAdministered")
    public PositiveSubstanceAdministered create(@RequestBody @Valid PositiveSubstanceAdministered positiveSubstanceAdministered)
    {
        QdmValidator.validateResourceTypeAndName(positiveSubstanceAdministered, positiveSubstanceAdministered);
        return repository.save(positiveSubstanceAdministered);
    }

    @PutMapping("/PositiveSubstanceAdministered/{id}")
    public PositiveSubstanceAdministered update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveSubstanceAdministered positiveSubstanceAdministered)
    {
        QdmValidator.validateResourceId(positiveSubstanceAdministered.getId(), id);
        Optional<PositiveSubstanceAdministered> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveSubstanceAdministered updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveSubstanceAdministered, updateResource);
            updateResource.copy(positiveSubstanceAdministered);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveSubstanceAdministered, positiveSubstanceAdministered);
        return repository.save(positiveSubstanceAdministered);
    }

    @DeleteMapping("/PositiveSubstanceAdministered/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveSubstanceAdministered pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveSubstanceAdministered/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
