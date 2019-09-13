package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveMedicationAdministeredRepository;
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
public class PositiveMedicationAdministeredController implements Serializable
{
    private final PositiveMedicationAdministeredRepository repository;

    @Autowired
    public PositiveMedicationAdministeredController(PositiveMedicationAdministeredRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveMedicationAdministered")
    public List<PositiveMedicationAdministered> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveMedicationAdministered exampleType = new PositiveMedicationAdministered();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveMedicationAdministered> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveMedicationAdministered/{id}")
    public @ResponseBody PositiveMedicationAdministered getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveMedicationAdministered/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveMedicationAdministered")
    public PositiveMedicationAdministered create(@RequestBody @Valid PositiveMedicationAdministered positiveMedicationAdministered)
    {
        QdmValidator.validateResourceTypeAndName(positiveMedicationAdministered, positiveMedicationAdministered);
        return repository.save(positiveMedicationAdministered);
    }

    @PutMapping("/PositiveMedicationAdministered/{id}")
    public PositiveMedicationAdministered update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveMedicationAdministered positiveMedicationAdministered)
    {
        QdmValidator.validateResourceId(positiveMedicationAdministered.getId(), id);
        Optional<PositiveMedicationAdministered> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveMedicationAdministered updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveMedicationAdministered, updateResource);
            updateResource.copy(positiveMedicationAdministered);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveMedicationAdministered, positiveMedicationAdministered);
        return repository.save(positiveMedicationAdministered);
    }

    @DeleteMapping("/PositiveMedicationAdministered/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveMedicationAdministered pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveMedicationAdministered/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
