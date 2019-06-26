package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeDeviceOrderRepository;
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
public class NegativeDeviceOrderController implements Serializable
{
    private final NegativeDeviceOrderRepository repository;

    @Autowired
    public NegativeDeviceOrderController(NegativeDeviceOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeDeviceOrder")
    public List<NegativeDeviceOrder> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeDeviceOrder exampleType = new NegativeDeviceOrder();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeDeviceOrder> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeDeviceOrder/{id}")
    public @ResponseBody
    NegativeDeviceOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeDeviceOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeDeviceOrder")
    public @ResponseBody NegativeDeviceOrder create(@RequestBody @Valid NegativeDeviceOrder negativeDeviceOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeDeviceOrder, negativeDeviceOrder);
        return repository.save(negativeDeviceOrder);
    }

    @PutMapping("/NegativeDeviceOrder/{id}")
    public NegativeDeviceOrder update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeDeviceOrder negativeDeviceOrder)
    {
        QdmValidator.validateResourceId(negativeDeviceOrder.getId(), id);
        Optional<NegativeDeviceOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeDeviceOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeDeviceOrder, updateResource);
            updateResource.copy(negativeDeviceOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeDeviceOrder, negativeDeviceOrder);
        return repository.save(negativeDeviceOrder);
    }

    @DeleteMapping("/NegativeDeviceOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeDeviceOrder par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeDeviceOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
