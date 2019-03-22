package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveDeviceOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveDeviceOrderRepository;
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
public class PositiveDeviceOrderController implements Serializable
{
    private final PositiveDeviceOrderRepository repository;

    @Autowired
    public PositiveDeviceOrderController(PositiveDeviceOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveDeviceOrder")
    public List<PositiveDeviceOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveDeviceOrder/{id}")
    public @ResponseBody
    PositiveDeviceOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveDeviceOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveDeviceOrder")
    public @ResponseBody PositiveDeviceOrder create(@RequestBody @Valid PositiveDeviceOrder positiveDeviceOrder)
    {
        QdmValidator.validateResourceTypeAndName(positiveDeviceOrder, positiveDeviceOrder);
        return repository.save(positiveDeviceOrder);
    }

    @PutMapping("/PositiveDeviceOrder/{id}")
    public PositiveDeviceOrder update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveDeviceOrder positiveDeviceOrder)
    {
        QdmValidator.validateResourceId(positiveDeviceOrder.getId(), id);
        Optional<PositiveDeviceOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveDeviceOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveDeviceOrder, updateResource);
            updateResource.copy(positiveDeviceOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveDeviceOrder, positiveDeviceOrder);
        return repository.save(positiveDeviceOrder);
    }

    @DeleteMapping("/PositiveDeviceOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveDeviceOrder par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveDeviceOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
