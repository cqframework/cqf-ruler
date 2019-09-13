package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveCommunicationPerformedRepository;
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
public class PositiveCommunicationPerformedController implements Serializable
{
    private final PositiveCommunicationPerformedRepository repository;

    @Autowired
    public PositiveCommunicationPerformedController(PositiveCommunicationPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveCommunicationPerformed")
    public List<PositiveCommunicationPerformed> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveCommunicationPerformed exampleType = new PositiveCommunicationPerformed();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveCommunicationPerformed> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveCommunicationPerformed/{id}")
    public @ResponseBody PositiveCommunicationPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveCommunicationPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveCommunicationPerformed")
    public PositiveCommunicationPerformed create(@RequestBody @Valid PositiveCommunicationPerformed positiveCommunicationPerformed)
    {
        QdmValidator.validateResourceTypeAndName(positiveCommunicationPerformed, positiveCommunicationPerformed);
        return repository.save(positiveCommunicationPerformed);
    }

    @PutMapping("/PositiveCommunicationPerformed/{id}")
    public PositiveCommunicationPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveCommunicationPerformed positiveCommunicationPerformed)
    {
        QdmValidator.validateResourceId(positiveCommunicationPerformed.getId(), id);
        Optional<PositiveCommunicationPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveCommunicationPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveCommunicationPerformed, updateResource);
            updateResource.copy(positiveCommunicationPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveCommunicationPerformed, positiveCommunicationPerformed);
        return repository.save(positiveCommunicationPerformed);
    }

    @DeleteMapping("/PositiveCommunicationPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveCommunicationPerformed pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveCommunicationPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
