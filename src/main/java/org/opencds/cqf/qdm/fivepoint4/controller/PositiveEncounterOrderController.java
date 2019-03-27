package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveEncounterOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveEncounterOrderRepository;
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
public class PositiveEncounterOrderController implements Serializable
{
    private final PositiveEncounterOrderRepository repository;

    @Autowired
    public PositiveEncounterOrderController(PositiveEncounterOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveEncounterOrder")
    public List<PositiveEncounterOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveEncounterOrder/{id}")
    public @ResponseBody PositiveEncounterOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveEncounterOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveEncounterOrder")
    public PositiveEncounterOrder create(@RequestBody @Valid PositiveEncounterOrder positiveEncounterOrder)
    {
        QdmValidator.validateResourceTypeAndName(positiveEncounterOrder, positiveEncounterOrder);
        return repository.save(positiveEncounterOrder);
    }

    @PutMapping("/PositiveEncounterOrder/{id}")
    public PositiveEncounterOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveEncounterOrder positiveEncounterOrder)
    {
        QdmValidator.validateResourceId(positiveEncounterOrder.getId(), id);
        Optional<PositiveEncounterOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveEncounterOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveEncounterOrder, updateResource);
            updateResource.copy(positiveEncounterOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveEncounterOrder, positiveEncounterOrder);
        return repository.save(positiveEncounterOrder);
    }

    @DeleteMapping("/PositiveEncounterOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveEncounterOrder pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveEncounterOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
