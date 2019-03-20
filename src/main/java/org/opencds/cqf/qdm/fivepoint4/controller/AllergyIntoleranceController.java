package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.AllergyIntolerance;
import org.opencds.cqf.qdm.fivepoint4.repository.AllergyIntoleranceRepository;
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
public class AllergyIntoleranceController implements Serializable
{
    private final AllergyIntoleranceRepository repository;

    @Autowired
    public AllergyIntoleranceController(AllergyIntoleranceRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/AllergyIntolerance")
    public List<AllergyIntolerance> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/AllergyIntolerance/{id}")
    public @ResponseBody
    AllergyIntolerance getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: AllergyIntolerance/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/AllergyIntolerance")
    public @ResponseBody AllergyIntolerance create(@RequestBody @Valid AllergyIntolerance allergyIntolerance)
    {
        QdmValidator.validateResourceTypeAndName(allergyIntolerance, allergyIntolerance);
        return repository.save(allergyIntolerance);
    }

    @PutMapping("/AllergyIntolerance/{id}")
    public AllergyIntolerance update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid AllergyIntolerance allergyIntolerance)
    {
        QdmValidator.validateResourceId(allergyIntolerance.getId(), id);
        Optional<AllergyIntolerance> update = repository.findById(id);
        if (update.isPresent())
        {
            AllergyIntolerance updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(allergyIntolerance, updateResource);
            updateResource.copy(allergyIntolerance);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(allergyIntolerance, allergyIntolerance);
        return repository.save(allergyIntolerance);
    }

    @DeleteMapping("/AllergyIntolerance/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        AllergyIntolerance par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: AllergyIntolerance/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
