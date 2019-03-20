package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeImmunizationOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeImmunizationOrderRepository;
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
public class NegativeImmunizationOrderController implements Serializable
{
    private final NegativeImmunizationOrderRepository repository;

    @Autowired
    public NegativeImmunizationOrderController(NegativeImmunizationOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeImmunizationOrder")
    public List<NegativeImmunizationOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeImmunizationOrder/{id}")
    public @ResponseBody NegativeImmunizationOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeImmunizationOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeImmunizationOrder")
    public @ResponseBody NegativeImmunizationOrder create(@RequestBody @Valid NegativeImmunizationOrder negativeImmunizationOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeImmunizationOrder, negativeImmunizationOrder);
        return repository.save(negativeImmunizationOrder);
    }

    @PutMapping("/NegativeImmunizationOrder/{id}")
    public NegativeImmunizationOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeImmunizationOrder negativeImmunizationOrder)
    {
        QdmValidator.validateResourceId(negativeImmunizationOrder.getId(), id);
        Optional<NegativeImmunizationOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeImmunizationOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeImmunizationOrder, updateResource);
            updateResource.copy(negativeImmunizationOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeImmunizationOrder, negativeImmunizationOrder);
        return repository.save(negativeImmunizationOrder);
    }

    @DeleteMapping("/NegativeImmunizationOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeImmunizationOrder nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeImmunizationOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
