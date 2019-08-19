package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveMedicationDispensedRepository;
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
public class PositiveMedicationDispensedController implements Serializable
{
    private final PositiveMedicationDispensedRepository repository;

    @Autowired
    public PositiveMedicationDispensedController(PositiveMedicationDispensedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveMedicationDispensed")
    public List<PositiveMedicationDispensed> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveMedicationDispensed exampleType = new PositiveMedicationDispensed();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveMedicationDispensed> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveMedicationDispensed/{id}")
    public @ResponseBody PositiveMedicationDispensed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveMedicationDispensed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveMedicationDispensed")
    public PositiveMedicationDispensed create(@RequestBody @Valid PositiveMedicationDispensed positiveMedicationDispensed)
    {
        QdmValidator.validateResourceTypeAndName(positiveMedicationDispensed, positiveMedicationDispensed);
        return repository.save(positiveMedicationDispensed);
    }

    @PutMapping("/PositiveMedicationDispensed/{id}")
    public PositiveMedicationDispensed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveMedicationDispensed positiveMedicationDispensed)
    {
        QdmValidator.validateResourceId(positiveMedicationDispensed.getId(), id);
        Optional<PositiveMedicationDispensed> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveMedicationDispensed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveMedicationDispensed, updateResource);
            updateResource.copy(positiveMedicationDispensed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveMedicationDispensed, positiveMedicationDispensed);
        return repository.save(positiveMedicationDispensed);
    }

    @DeleteMapping("/PositiveMedicationDispensed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveMedicationDispensed pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveMedicationDispensed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
