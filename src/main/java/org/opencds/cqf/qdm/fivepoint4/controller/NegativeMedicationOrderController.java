package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeMedicationOrderRepository;
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
public class NegativeMedicationOrderController implements Serializable
{
    private final NegativeMedicationOrderRepository repository;

    @Autowired
    public NegativeMedicationOrderController(NegativeMedicationOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeMedicationOrder")
    public List<NegativeMedicationOrder> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeMedicationOrder exampleType = new NegativeMedicationOrder();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeMedicationOrder> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeMedicationOrder/{id}")
    public @ResponseBody NegativeMedicationOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeMedicationOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeMedicationOrder")
    public @ResponseBody NegativeMedicationOrder create(@RequestBody @Valid NegativeMedicationOrder negativeMedicationOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeMedicationOrder, negativeMedicationOrder);
        return repository.save(negativeMedicationOrder);
    }

    @PutMapping("/NegativeMedicationOrder/{id}")
    public NegativeMedicationOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeMedicationOrder negativeMedicationOrder)
    {
        QdmValidator.validateResourceId(negativeMedicationOrder.getId(), id);
        Optional<NegativeMedicationOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeMedicationOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeMedicationOrder, updateResource);
            updateResource.copy(negativeMedicationOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeMedicationOrder, negativeMedicationOrder);
        return repository.save(negativeMedicationOrder);
    }

    @DeleteMapping("/NegativeMedicationOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeMedicationOrder nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeMedicationOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
