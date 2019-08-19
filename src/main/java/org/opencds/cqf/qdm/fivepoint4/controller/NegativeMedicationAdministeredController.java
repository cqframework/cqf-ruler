package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeMedicationAdministeredRepository;
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
public class NegativeMedicationAdministeredController implements Serializable
{
    private final NegativeMedicationAdministeredRepository repository;

    @Autowired
    public NegativeMedicationAdministeredController(NegativeMedicationAdministeredRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeMedicationAdministered")
    public List<NegativeMedicationAdministered> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeMedicationAdministered exampleType = new NegativeMedicationAdministered();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeMedicationAdministered> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeMedicationAdministered/{id}")
    public @ResponseBody NegativeMedicationAdministered getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeMedicationAdministered/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeMedicationAdministered")
    public @ResponseBody NegativeMedicationAdministered create(@RequestBody @Valid NegativeMedicationAdministered negativeMedicationAdministered)
    {
        QdmValidator.validateResourceTypeAndName(negativeMedicationAdministered, negativeMedicationAdministered);
        return repository.save(negativeMedicationAdministered);
    }

    @PutMapping("/NegativeMedicationAdministered/{id}")
    public NegativeMedicationAdministered update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeMedicationAdministered negativeMedicationAdministered)
    {
        QdmValidator.validateResourceId(negativeMedicationAdministered.getId(), id);
        Optional<NegativeMedicationAdministered> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeMedicationAdministered updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeMedicationAdministered, updateResource);
            updateResource.copy(negativeMedicationAdministered);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeMedicationAdministered, negativeMedicationAdministered);
        return repository.save(negativeMedicationAdministered);
    }

    @DeleteMapping("/NegativeMedicationAdministered/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeMedicationAdministered nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeMedicationAdministered/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
