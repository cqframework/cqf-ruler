package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveDiagnosticStudyRecommendedRepository;
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
public class PositiveDiagnosticStudyRecommendedController implements Serializable
{
    private final PositiveDiagnosticStudyRecommendedRepository repository;

    @Autowired
    public PositiveDiagnosticStudyRecommendedController(PositiveDiagnosticStudyRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveDiagnosticStudyRecommended")
    public List<PositiveDiagnosticStudyRecommended> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveDiagnosticStudyRecommended exampleType = new PositiveDiagnosticStudyRecommended();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveDiagnosticStudyRecommended> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveDiagnosticStudyRecommended/{id}")
    public @ResponseBody
    PositiveDiagnosticStudyRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveDiagnosticStudyRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveDiagnosticStudyRecommended")
    public @ResponseBody PositiveDiagnosticStudyRecommended create(@RequestBody @Valid PositiveDiagnosticStudyRecommended positiveDiagnosticStudyRecommended)
    {
        QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyRecommended, positiveDiagnosticStudyRecommended);
        return repository.save(positiveDiagnosticStudyRecommended);
    }

    @PutMapping("/PositiveDiagnosticStudyRecommended/{id}")
    public PositiveDiagnosticStudyRecommended update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveDiagnosticStudyRecommended positiveDiagnosticStudyRecommended)
    {
        QdmValidator.validateResourceId(positiveDiagnosticStudyRecommended.getId(), id);
        Optional<PositiveDiagnosticStudyRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveDiagnosticStudyRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyRecommended, updateResource);
            updateResource.copy(positiveDiagnosticStudyRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyRecommended, positiveDiagnosticStudyRecommended);
        return repository.save(positiveDiagnosticStudyRecommended);
    }

    @DeleteMapping("/PositiveDiagnosticStudyRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveDiagnosticStudyRecommended par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveDiagnosticStudyRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
