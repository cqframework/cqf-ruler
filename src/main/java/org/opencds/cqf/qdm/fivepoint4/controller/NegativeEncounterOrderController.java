package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeEncounterOrderRepository;
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
public class NegativeEncounterOrderController implements Serializable
{
    private final NegativeEncounterOrderRepository repository;

    @Autowired
    public NegativeEncounterOrderController(NegativeEncounterOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeEncounterOrder")
    public List<NegativeEncounterOrder> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeEncounterOrder exampleType = new NegativeEncounterOrder();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeEncounterOrder> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeEncounterOrder/{id}")
    public @ResponseBody NegativeEncounterOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeEncounterOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeEncounterOrder")
    public @ResponseBody NegativeEncounterOrder create(@RequestBody @Valid NegativeEncounterOrder negativeEncounterOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeEncounterOrder, negativeEncounterOrder);
        return repository.save(negativeEncounterOrder);
    }

    @PutMapping("/NegativeEncounterOrder/{id}")
    public NegativeEncounterOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeEncounterOrder negativeEncounterOrder)
    {
        QdmValidator.validateResourceId(negativeEncounterOrder.getId(), id);
        Optional<NegativeEncounterOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeEncounterOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeEncounterOrder, updateResource);
            updateResource.copy(negativeEncounterOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeEncounterOrder, negativeEncounterOrder);
        return repository.save(negativeEncounterOrder);
    }

    @DeleteMapping("/NegativeEncounterOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeEncounterOrder nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeEncounterOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
