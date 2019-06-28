package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeEncounterPerformedRepository;
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
public class NegativeEncounterPerformedController implements Serializable
{
    private final NegativeEncounterPerformedRepository repository;

    @Autowired
    public NegativeEncounterPerformedController(NegativeEncounterPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeEncounterPerformed")
    public List<NegativeEncounterPerformed> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeEncounterPerformed exampleType = new NegativeEncounterPerformed();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeEncounterPerformed> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeEncounterPerformed/{id}")
    public @ResponseBody NegativeEncounterPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeEncounterPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeEncounterPerformed")
    public @ResponseBody NegativeEncounterPerformed create(@RequestBody @Valid NegativeEncounterPerformed negativeEncounterPerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativeEncounterPerformed, negativeEncounterPerformed);
        return repository.save(negativeEncounterPerformed);
    }

    @PutMapping("/NegativeEncounterPerformed/{id}")
    public NegativeEncounterPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeEncounterPerformed negativeEncounterPerformed)
    {
        QdmValidator.validateResourceId(negativeEncounterPerformed.getId(), id);
        Optional<NegativeEncounterPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeEncounterPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeEncounterPerformed, updateResource);
            updateResource.copy(negativeEncounterPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeEncounterPerformed, negativeEncounterPerformed);
        return repository.save(negativeEncounterPerformed);
    }

    @DeleteMapping("/NegativeEncounterPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeEncounterPerformed nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeEncounterPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
