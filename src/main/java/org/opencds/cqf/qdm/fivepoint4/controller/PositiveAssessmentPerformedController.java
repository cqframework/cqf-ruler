package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveAssessmentPerformedRepository;
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
public class PositiveAssessmentPerformedController implements Serializable
{
    private final PositiveAssessmentPerformedRepository repository;

    @Autowired
    public PositiveAssessmentPerformedController(PositiveAssessmentPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveAssessmentPerformed")
    public List<PositiveAssessmentPerformed> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveAssessmentPerformed exampleType = new PositiveAssessmentPerformed();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveAssessmentPerformed> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveAssessmentPerformed/{id}")
    public @ResponseBody
    PositiveAssessmentPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveAssessmentPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveAssessmentPerformed")
    public @ResponseBody PositiveAssessmentPerformed create(@RequestBody @Valid PositiveAssessmentPerformed positiveAssessmentPerformed)
    {
        QdmValidator.validateResourceTypeAndName(positiveAssessmentPerformed, positiveAssessmentPerformed);
        return repository.save(positiveAssessmentPerformed);
    }

    @PutMapping("/PositiveAssessmentPerformed/{id}")
    public PositiveAssessmentPerformed update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveAssessmentPerformed positiveAssessmentPerformed)
    {
        QdmValidator.validateResourceId(positiveAssessmentPerformed.getId(), id);
        Optional<PositiveAssessmentPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveAssessmentPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveAssessmentPerformed, updateResource);
            updateResource.copy(positiveAssessmentPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveAssessmentPerformed, positiveAssessmentPerformed);
        return repository.save(positiveAssessmentPerformed);
    }

    @DeleteMapping("/PositiveAssessmentPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveAssessmentPerformed par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveAssessmentPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
