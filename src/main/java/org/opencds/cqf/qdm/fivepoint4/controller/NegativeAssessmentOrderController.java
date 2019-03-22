package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeAssessmentOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeAssessmentOrderRepository;
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
public class NegativeAssessmentOrderController implements Serializable
{
    private final NegativeAssessmentOrderRepository repository;

    @Autowired
    public NegativeAssessmentOrderController(NegativeAssessmentOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeAssessmentOrder")
    public List<NegativeAssessmentOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeAssessmentOrder/{id}")
    public @ResponseBody
    NegativeAssessmentOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeAssessmentOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeAssessmentOrder")
    public @ResponseBody NegativeAssessmentOrder create(@RequestBody @Valid NegativeAssessmentOrder negativeAssessmentOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeAssessmentOrder, negativeAssessmentOrder);
        return repository.save(negativeAssessmentOrder);
    }

    @PutMapping("/NegativeAssessmentOrder/{id}")
    public NegativeAssessmentOrder update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeAssessmentOrder negativeAssessmentOrder)
    {
        QdmValidator.validateResourceId(negativeAssessmentOrder.getId(), id);
        Optional<NegativeAssessmentOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeAssessmentOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeAssessmentOrder, updateResource);
            updateResource.copy(negativeAssessmentOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeAssessmentOrder, negativeAssessmentOrder);
        return repository.save(negativeAssessmentOrder);
    }

    @DeleteMapping("/NegativeAssessmentOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeAssessmentOrder par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeAssessmentOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
