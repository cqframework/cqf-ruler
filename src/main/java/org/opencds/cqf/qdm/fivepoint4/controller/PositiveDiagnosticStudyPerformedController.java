package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveDiagnosticStudyPerformedRepository;
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
public class PositiveDiagnosticStudyPerformedController implements Serializable
{
    private final PositiveDiagnosticStudyPerformedRepository repository;

    @Autowired
    public PositiveDiagnosticStudyPerformedController(PositiveDiagnosticStudyPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveDiagnosticStudyPerformed")
    public List<PositiveDiagnosticStudyPerformed> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveDiagnosticStudyPerformed exampleType = new PositiveDiagnosticStudyPerformed();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveDiagnosticStudyPerformed> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveDiagnosticStudyPerformed/{id}")
    public @ResponseBody
    PositiveDiagnosticStudyPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveDiagnosticStudyPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveDiagnosticStudyPerformed")
    public @ResponseBody PositiveDiagnosticStudyPerformed create(@RequestBody @Valid PositiveDiagnosticStudyPerformed positiveDiagnosticStudyPerformed)
    {
        QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyPerformed, positiveDiagnosticStudyPerformed);
        return repository.save(positiveDiagnosticStudyPerformed);
    }

    @PutMapping("/PositiveDiagnosticStudyPerformed/{id}")
    public PositiveDiagnosticStudyPerformed update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveDiagnosticStudyPerformed positiveDiagnosticStudyPerformed)
    {
        QdmValidator.validateResourceId(positiveDiagnosticStudyPerformed.getId(), id);
        Optional<PositiveDiagnosticStudyPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveDiagnosticStudyPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyPerformed, updateResource);
            updateResource.copy(positiveDiagnosticStudyPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyPerformed, positiveDiagnosticStudyPerformed);
        return repository.save(positiveDiagnosticStudyPerformed);
    }

    @DeleteMapping("/PositiveDiagnosticStudyPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveDiagnosticStudyPerformed par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveDiagnosticStudyPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
