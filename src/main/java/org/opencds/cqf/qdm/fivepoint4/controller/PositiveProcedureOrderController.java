package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveProcedureOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveProcedureOrderRepository;
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
public class PositiveProcedureOrderController implements Serializable
{
    private final PositiveProcedureOrderRepository repository;

    @Autowired
    public PositiveProcedureOrderController(PositiveProcedureOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveProcedureOrder")
    public List<PositiveProcedureOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveProcedureOrder/{id}")
    public @ResponseBody PositiveProcedureOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveProcedureOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveProcedureOrder")
    public PositiveProcedureOrder create(@RequestBody @Valid PositiveProcedureOrder positiveProcedureOrder)
    {
        QdmValidator.validateResourceTypeAndName(positiveProcedureOrder, positiveProcedureOrder);
        return repository.save(positiveProcedureOrder);
    }

    @PutMapping("/PositiveProcedureOrder/{id}")
    public PositiveProcedureOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveProcedureOrder positiveProcedureOrder)
    {
        QdmValidator.validateResourceId(positiveProcedureOrder.getId(), id);
        Optional<PositiveProcedureOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveProcedureOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveProcedureOrder, updateResource);
            updateResource.copy(positiveProcedureOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveProcedureOrder, positiveProcedureOrder);
        return repository.save(positiveProcedureOrder);
    }

    @DeleteMapping("/PositiveProcedureOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveProcedureOrder pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveProcedureOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
