package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveInterventionRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveInterventionRecommendedRepository;
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
public class PositiveInterventionRecommendedController implements Serializable
{
    private final PositiveInterventionRecommendedRepository repository;

    @Autowired
    public PositiveInterventionRecommendedController(PositiveInterventionRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveInterventionRecommended")
    public List<PositiveInterventionRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveInterventionRecommended/{id}")
    public @ResponseBody PositiveInterventionRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveInterventionRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveInterventionRecommended")
    public PositiveInterventionRecommended create(@RequestBody @Valid PositiveInterventionRecommended positiveInterventionRecommended)
    {
        QdmValidator.validateResourceTypeAndName(positiveInterventionRecommended, positiveInterventionRecommended);
        return repository.save(positiveInterventionRecommended);
    }

    @PutMapping("/PositiveInterventionRecommended/{id}")
    public PositiveInterventionRecommended update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveInterventionRecommended positiveInterventionRecommended)
    {
        QdmValidator.validateResourceId(positiveInterventionRecommended.getId(), id);
        Optional<PositiveInterventionRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveInterventionRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveInterventionRecommended, updateResource);
            updateResource.copy(positiveInterventionRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveInterventionRecommended, positiveInterventionRecommended);
        return repository.save(positiveInterventionRecommended);
    }

    @DeleteMapping("/PositiveInterventionRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveInterventionRecommended pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveInterventionRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
