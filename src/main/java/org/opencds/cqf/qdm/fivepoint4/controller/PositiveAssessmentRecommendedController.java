package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveAssessmentRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveAssessmentRecommendedRepository;
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
public class PositiveAssessmentRecommendedController implements Serializable
{
    private final PositiveAssessmentRecommendedRepository repository;

    @Autowired
    public PositiveAssessmentRecommendedController(PositiveAssessmentRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveAssessmentRecommended")
    public List<PositiveAssessmentRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveAssessmentRecommended/{id}")
    public @ResponseBody
    PositiveAssessmentRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveAssessmentRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveAssessmentRecommended")
    public @ResponseBody PositiveAssessmentRecommended create(@RequestBody @Valid PositiveAssessmentRecommended positiveAssessmentRecommended)
    {
        QdmValidator.validateResourceTypeAndName(positiveAssessmentRecommended, positiveAssessmentRecommended);
        return repository.save(positiveAssessmentRecommended);
    }

    @PutMapping("/PositiveAssessmentRecommended/{id}")
    public PositiveAssessmentRecommended update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveAssessmentRecommended positiveAssessmentRecommended)
    {
        QdmValidator.validateResourceId(positiveAssessmentRecommended.getId(), id);
        Optional<PositiveAssessmentRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveAssessmentRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveAssessmentRecommended, updateResource);
            updateResource.copy(positiveAssessmentRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveAssessmentRecommended, positiveAssessmentRecommended);
        return repository.save(positiveAssessmentRecommended);
    }

    @DeleteMapping("/PositiveAssessmentRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveAssessmentRecommended par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveAssessmentRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
