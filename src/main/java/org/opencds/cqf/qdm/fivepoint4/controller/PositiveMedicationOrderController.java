package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveMedicationOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveMedicationOrderRepository;
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
public class PositiveMedicationOrderController implements Serializable
{
    private final PositiveMedicationOrderRepository repository;

    @Autowired
    public PositiveMedicationOrderController(PositiveMedicationOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveMedicationOrder")
    public List<PositiveMedicationOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveMedicationOrder/{id}")
    public @ResponseBody PositiveMedicationOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveMedicationOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveMedicationOrder")
    public PositiveMedicationOrder create(@RequestBody @Valid PositiveMedicationOrder positiveMedicationOrder)
    {
        QdmValidator.validateResourceTypeAndName(positiveMedicationOrder, positiveMedicationOrder);
        return repository.save(positiveMedicationOrder);
    }

    @PutMapping("/PositiveMedicationOrder/{id}")
    public PositiveMedicationOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveMedicationOrder positiveMedicationOrder)
    {
        QdmValidator.validateResourceId(positiveMedicationOrder.getId(), id);
        Optional<PositiveMedicationOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveMedicationOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveMedicationOrder, updateResource);
            updateResource.copy(positiveMedicationOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveMedicationOrder, positiveMedicationOrder);
        return repository.save(positiveMedicationOrder);
    }

    @DeleteMapping("/PositiveMedicationOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveMedicationOrder pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveMedicationOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
