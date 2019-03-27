package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveSubstanceRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveSubstanceRecommendedRepository;
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
public class PositiveSubstanceRecommendedController implements Serializable
{
    private final PositiveSubstanceRecommendedRepository repository;

    @Autowired
    public PositiveSubstanceRecommendedController(PositiveSubstanceRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveSubstanceRecommended")
    public List<PositiveSubstanceRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveSubstanceRecommended/{id}")
    public @ResponseBody PositiveSubstanceRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveSubstanceRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveSubstanceRecommended")
    public PositiveSubstanceRecommended create(@RequestBody @Valid PositiveSubstanceRecommended positiveSubstanceRecommended)
    {
        QdmValidator.validateResourceTypeAndName(positiveSubstanceRecommended, positiveSubstanceRecommended);
        return repository.save(positiveSubstanceRecommended);
    }

    @PutMapping("/PositiveSubstanceRecommended/{id}")
    public PositiveSubstanceRecommended update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveSubstanceRecommended positiveSubstanceRecommended)
    {
        QdmValidator.validateResourceId(positiveSubstanceRecommended.getId(), id);
        Optional<PositiveSubstanceRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveSubstanceRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveSubstanceRecommended, updateResource);
            updateResource.copy(positiveSubstanceRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveSubstanceRecommended, positiveSubstanceRecommended);
        return repository.save(positiveSubstanceRecommended);
    }

    @DeleteMapping("/PositiveSubstanceRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveSubstanceRecommended pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveSubstanceRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
