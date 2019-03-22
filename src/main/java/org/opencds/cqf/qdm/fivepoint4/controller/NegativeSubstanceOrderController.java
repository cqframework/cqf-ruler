package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeSubstanceOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeSubstanceOrderRepository;
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
public class NegativeSubstanceOrderController implements Serializable
{
    private final NegativeSubstanceOrderRepository repository;

    @Autowired
    public NegativeSubstanceOrderController(NegativeSubstanceOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeSubstanceOrder")
    public List<NegativeSubstanceOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeSubstanceOrder/{id}")
    public @ResponseBody NegativeSubstanceOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeSubstanceOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeSubstanceOrder")
    public @ResponseBody NegativeSubstanceOrder create(@RequestBody @Valid NegativeSubstanceOrder negativeSubstanceOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeSubstanceOrder, negativeSubstanceOrder);
        return repository.save(negativeSubstanceOrder);
    }

    @PutMapping("/NegativeSubstanceOrder/{id}")
    public NegativeSubstanceOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeSubstanceOrder negativeSubstanceOrder)
    {
        QdmValidator.validateResourceId(negativeSubstanceOrder.getId(), id);
        Optional<NegativeSubstanceOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeSubstanceOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeSubstanceOrder, updateResource);
            updateResource.copy(negativeSubstanceOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeSubstanceOrder, negativeSubstanceOrder);
        return repository.save(negativeSubstanceOrder);
    }

    @DeleteMapping("/NegativeSubstanceOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeSubstanceOrder nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeSubstanceOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
