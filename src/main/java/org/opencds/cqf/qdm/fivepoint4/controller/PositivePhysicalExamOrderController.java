package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositivePhysicalExamOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.PositivePhysicalExamOrderRepository;
import org.opencds.cqf.qdm.fivepoint4.validation.QdmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@RestController
public class PositivePhysicalExamOrderController implements Serializable
{
    private final PositivePhysicalExamOrderRepository repository;

    @Autowired
    public PositivePhysicalExamOrderController(PositivePhysicalExamOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositivePhysicalExamOrder")
    public List<PositivePhysicalExamOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositivePhysicalExamOrder/{id}")
    public @ResponseBody PositivePhysicalExamOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositivePhysicalExamOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositivePhysicalExamOrder")
    public PositivePhysicalExamOrder create(@RequestBody @Valid PositivePhysicalExamOrder positivePhysicalExamOrder)
    {
        QdmValidator.validateResourceTypeAndName(positivePhysicalExamOrder, positivePhysicalExamOrder);
        return repository.save(positivePhysicalExamOrder);
    }

    @PutMapping("/PositivePhysicalExamOrder/{id}")
    public PositivePhysicalExamOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositivePhysicalExamOrder positivePhysicalExamOrder)
    {
        QdmValidator.validateResourceId(positivePhysicalExamOrder.getId(), id);
        Optional<PositivePhysicalExamOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositivePhysicalExamOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positivePhysicalExamOrder, updateResource);
            updateResource.copy(positivePhysicalExamOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positivePhysicalExamOrder, positivePhysicalExamOrder);
        return repository.save(positivePhysicalExamOrder);
    }

    @DeleteMapping("/PositivePhysicalExamOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositivePhysicalExamOrder pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositivePhysicalExamOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
