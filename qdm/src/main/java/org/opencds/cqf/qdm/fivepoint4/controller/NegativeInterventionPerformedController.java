package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeInterventionPerformedRepository;
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
public class NegativeInterventionPerformedController implements Serializable
{
    private final NegativeInterventionPerformedRepository repository;

    @Autowired
    public NegativeInterventionPerformedController(NegativeInterventionPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeInterventionPerformed")
    public List<NegativeInterventionPerformed> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeInterventionPerformed exampleType = new NegativeInterventionPerformed();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeInterventionPerformed> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeInterventionPerformed/{id}")
    public @ResponseBody NegativeInterventionPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeInterventionPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeInterventionPerformed")
    public @ResponseBody NegativeInterventionPerformed create(@RequestBody @Valid NegativeInterventionPerformed negativeInterventionPerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativeInterventionPerformed, negativeInterventionPerformed);
        return repository.save(negativeInterventionPerformed);
    }

    @PutMapping("/NegativeInterventionPerformed/{id}")
    public NegativeInterventionPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeInterventionPerformed negativeInterventionPerformed)
    {
        QdmValidator.validateResourceId(negativeInterventionPerformed.getId(), id);
        Optional<NegativeInterventionPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeInterventionPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeInterventionPerformed, updateResource);
            updateResource.copy(negativeInterventionPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeInterventionPerformed, negativeInterventionPerformed);
        return repository.save(negativeInterventionPerformed);
    }

    @DeleteMapping("/NegativeInterventionPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeInterventionPerformed nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeInterventionPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
