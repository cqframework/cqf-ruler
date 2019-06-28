package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveMedicationDischargeRepository;
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
public class PositiveMedicationDischargeController implements Serializable
{
    private final PositiveMedicationDischargeRepository repository;

    @Autowired
    public PositiveMedicationDischargeController(PositiveMedicationDischargeRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveMedicationDischarge")
    public List<PositiveMedicationDischarge> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveMedicationDischarge exampleType = new PositiveMedicationDischarge();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveMedicationDischarge> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveMedicationDischarge/{id}")
    public @ResponseBody PositiveMedicationDischarge getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveMedicationDischarge/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveMedicationDischarge")
    public PositiveMedicationDischarge create(@RequestBody @Valid PositiveMedicationDischarge positiveMedicationDischarge)
    {
        QdmValidator.validateResourceTypeAndName(positiveMedicationDischarge, positiveMedicationDischarge);
        return repository.save(positiveMedicationDischarge);
    }

    @PutMapping("/PositiveMedicationDischarge/{id}")
    public PositiveMedicationDischarge update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveMedicationDischarge positiveMedicationDischarge)
    {
        QdmValidator.validateResourceId(positiveMedicationDischarge.getId(), id);
        Optional<PositiveMedicationDischarge> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveMedicationDischarge updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveMedicationDischarge, updateResource);
            updateResource.copy(positiveMedicationDischarge);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveMedicationDischarge, positiveMedicationDischarge);
        return repository.save(positiveMedicationDischarge);
    }

    @DeleteMapping("/PositiveMedicationDischarge/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveMedicationDischarge pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveMedicationDischarge/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
