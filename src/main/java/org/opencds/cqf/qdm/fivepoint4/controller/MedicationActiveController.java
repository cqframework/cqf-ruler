package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.MedicationActiveRepository;
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
public class MedicationActiveController implements Serializable
{
    private final MedicationActiveRepository repository;

    @Autowired
    public MedicationActiveController(MedicationActiveRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/MedicationActive")
    public List<MedicationActive> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            MedicationActive exampleType = new MedicationActive();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<MedicationActive> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/MedicationActive/{id}")
    public @ResponseBody
    MedicationActive getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: MedicationActive/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/MedicationActive")
    public @ResponseBody MedicationActive create(@RequestBody @Valid MedicationActive medicationActive)
    {
        QdmValidator.validateResourceTypeAndName(medicationActive, medicationActive);
        return repository.save(medicationActive);
    }

    @PutMapping("/MedicationActive/{id}")
    public MedicationActive update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid MedicationActive medicationActive)
    {
        QdmValidator.validateResourceId(medicationActive.getId(), id);
        Optional<MedicationActive> update = repository.findById(id);
        if (update.isPresent())
        {
            MedicationActive updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(medicationActive, updateResource);
            updateResource.copy(medicationActive);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(medicationActive, medicationActive);
        return repository.save(medicationActive);
    }

    @DeleteMapping("/MedicationActive/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        MedicationActive par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: MedicationActive/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
