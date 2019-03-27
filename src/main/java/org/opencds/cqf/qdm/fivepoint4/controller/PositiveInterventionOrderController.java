package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveInterventionOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveInterventionOrderRepository;
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

@CrossOrigin(origins = "*")
@RestController
public class PositiveInterventionOrderController implements Serializable
{
    private final PositiveInterventionOrderRepository repository;

    @Autowired
    public PositiveInterventionOrderController(PositiveInterventionOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveInterventionOrder")
    public List<PositiveInterventionOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveInterventionOrder/{id}")
    public @ResponseBody PositiveInterventionOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveInterventionOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveInterventionOrder")
    public PositiveInterventionOrder create(@RequestBody @Valid PositiveInterventionOrder positiveInterventionOrder)
    {
        QdmValidator.validateResourceTypeAndName(positiveInterventionOrder, positiveInterventionOrder);
        return repository.save(positiveInterventionOrder);
    }

    @PutMapping("/PositiveInterventionOrder/{id}")
    public PositiveInterventionOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveInterventionOrder positiveInterventionOrder)
    {
        QdmValidator.validateResourceId(positiveInterventionOrder.getId(), id);
        Optional<PositiveInterventionOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveInterventionOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveInterventionOrder, updateResource);
            updateResource.copy(positiveInterventionOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveInterventionOrder, positiveInterventionOrder);
        return repository.save(positiveInterventionOrder);
    }

    @DeleteMapping("/PositiveInterventionOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveInterventionOrder pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveInterventionOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
