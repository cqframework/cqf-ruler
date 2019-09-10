package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativePhysicalExamOrderRepository;
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
public class NegativePhysicalExamOrderController implements Serializable
{
    private final NegativePhysicalExamOrderRepository repository;

    @Autowired
    public NegativePhysicalExamOrderController(NegativePhysicalExamOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativePhysicalExamOrder")
    public List<NegativePhysicalExamOrder> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativePhysicalExamOrder exampleType = new NegativePhysicalExamOrder();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativePhysicalExamOrder> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativePhysicalExamOrder/{id}")
    public @ResponseBody NegativePhysicalExamOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativePhysicalExamOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativePhysicalExamOrder")
    public @ResponseBody NegativePhysicalExamOrder create(@RequestBody @Valid NegativePhysicalExamOrder negativePhysicalExamOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativePhysicalExamOrder, negativePhysicalExamOrder);
        return repository.save(negativePhysicalExamOrder);
    }

    @PutMapping("/NegativePhysicalExamOrder/{id}")
    public NegativePhysicalExamOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativePhysicalExamOrder negativePhysicalExamOrder)
    {
        QdmValidator.validateResourceId(negativePhysicalExamOrder.getId(), id);
        Optional<NegativePhysicalExamOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativePhysicalExamOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativePhysicalExamOrder, updateResource);
            updateResource.copy(negativePhysicalExamOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativePhysicalExamOrder, negativePhysicalExamOrder);
        return repository.save(negativePhysicalExamOrder);
    }

    @DeleteMapping("/NegativePhysicalExamOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativePhysicalExamOrder nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativePhysicalExamOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
