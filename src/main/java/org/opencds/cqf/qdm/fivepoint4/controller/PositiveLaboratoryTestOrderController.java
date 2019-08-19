package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveLaboratoryTestOrderRepository;
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
public class PositiveLaboratoryTestOrderController implements Serializable
{
    private final PositiveLaboratoryTestOrderRepository repository;

    @Autowired
    public PositiveLaboratoryTestOrderController(PositiveLaboratoryTestOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveLaboratoryTestOrder")
    public List<PositiveLaboratoryTestOrder> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            PositiveLaboratoryTestOrder exampleType = new PositiveLaboratoryTestOrder();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<PositiveLaboratoryTestOrder> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/PositiveLaboratoryTestOrder/{id}")
    public @ResponseBody PositiveLaboratoryTestOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveLaboratoryTestOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveLaboratoryTestOrder")
    public PositiveLaboratoryTestOrder create(@RequestBody @Valid PositiveLaboratoryTestOrder positiveLaboratoryTestOrder)
    {
        QdmValidator.validateResourceTypeAndName(positiveLaboratoryTestOrder, positiveLaboratoryTestOrder);
        return repository.save(positiveLaboratoryTestOrder);
    }

    @PutMapping("/PositiveLaboratoryTestOrder/{id}")
    public PositiveLaboratoryTestOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveLaboratoryTestOrder positiveLaboratoryTestOrder)
    {
        QdmValidator.validateResourceId(positiveLaboratoryTestOrder.getId(), id);
        Optional<PositiveLaboratoryTestOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveLaboratoryTestOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveLaboratoryTestOrder, updateResource);
            updateResource.copy(positiveLaboratoryTestOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveLaboratoryTestOrder, positiveLaboratoryTestOrder);
        return repository.save(positiveLaboratoryTestOrder);
    }

    @DeleteMapping("/PositiveLaboratoryTestOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveLaboratoryTestOrder pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveLaboratoryTestOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
