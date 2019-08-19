package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositivePhysicalExamRecommendedRepository;
import org.opencds.cqf.qdm.fivepoint4.validation.QdmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
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
public class PositivePhysicalExamRecommendedController implements Serializable
{
    private final PositivePhysicalExamRecommendedRepository repository;

    @Autowired
    public PositivePhysicalExamRecommendedController(PositivePhysicalExamRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositivePhysicalExamRecommended")
    public List<PositivePhysicalExamRecommended> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositivePhysicalExamRecommended exampleType = new PositivePhysicalExamRecommended();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositivePhysicalExamRecommended> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositivePhysicalExamRecommended/{id}")
    public @ResponseBody PositivePhysicalExamRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositivePhysicalExamRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositivePhysicalExamRecommended")
    public PositivePhysicalExamRecommended create(@RequestBody @Valid PositivePhysicalExamRecommended positivePhysicalExamRecommended)
    {
        QdmValidator.validateResourceTypeAndName(positivePhysicalExamRecommended, positivePhysicalExamRecommended);
        return repository.save(positivePhysicalExamRecommended);
    }

    @PutMapping("/PositivePhysicalExamRecommended/{id}")
    public PositivePhysicalExamRecommended update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositivePhysicalExamRecommended positivePhysicalExamRecommended)
    {
        QdmValidator.validateResourceId(positivePhysicalExamRecommended.getId(), id);
        Optional<PositivePhysicalExamRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            PositivePhysicalExamRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positivePhysicalExamRecommended, updateResource);
            updateResource.copy(positivePhysicalExamRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positivePhysicalExamRecommended, positivePhysicalExamRecommended);
        return repository.save(positivePhysicalExamRecommended);
    }

    @DeleteMapping("/PositivePhysicalExamRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositivePhysicalExamRecommended pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositivePhysicalExamRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
