package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeInterventionOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeInterventionOrderRepository;
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
public class NegativeInterventionOrderController implements Serializable
{
    private final NegativeInterventionOrderRepository repository;

    @Autowired
    public NegativeInterventionOrderController(NegativeInterventionOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeInterventionOrder")
    public List<NegativeInterventionOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeInterventionOrder/{id}")
    public @ResponseBody NegativeInterventionOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeInterventionOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeInterventionOrder")
    public @ResponseBody NegativeInterventionOrder create(@RequestBody @Valid NegativeInterventionOrder negativeInterventionOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeInterventionOrder, negativeInterventionOrder);
        return repository.save(negativeInterventionOrder);
    }

    @PutMapping("/NegativeInterventionOrder/{id}")
    public NegativeInterventionOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeInterventionOrder negativeInterventionOrder)
    {
        QdmValidator.validateResourceId(negativeInterventionOrder.getId(), id);
        Optional<NegativeInterventionOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeInterventionOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeInterventionOrder, updateResource);
            updateResource.copy(negativeInterventionOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeInterventionOrder, negativeInterventionOrder);
        return repository.save(negativeInterventionOrder);
    }

    @DeleteMapping("/NegativeInterventionOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeInterventionOrder nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeInterventionOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
