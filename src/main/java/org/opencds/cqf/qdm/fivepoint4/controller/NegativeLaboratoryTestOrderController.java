package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeLaboratoryTestOrderRepository;
import org.opencds.cqf.qdm.fivepoint4.validation.QdmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
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
public class NegativeLaboratoryTestOrderController implements Serializable
{
    private final NegativeLaboratoryTestOrderRepository repository;

    @Autowired
    public NegativeLaboratoryTestOrderController(NegativeLaboratoryTestOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeLaboratoryTestOrder")
    public List<NegativeLaboratoryTestOrder> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeLaboratoryTestOrder exampleType = new NegativeLaboratoryTestOrder();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeLaboratoryTestOrder> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeLaboratoryTestOrder/{id}")
    public @ResponseBody NegativeLaboratoryTestOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeLaboratoryTestOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeLaboratoryTestOrder")
    public @ResponseBody NegativeLaboratoryTestOrder create(@RequestBody @Valid NegativeLaboratoryTestOrder negativeLaboratoryTestOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeLaboratoryTestOrder, negativeLaboratoryTestOrder);
        return repository.save(negativeLaboratoryTestOrder);
    }

    @PutMapping("/NegativeLaboratoryTestOrder/{id}")
    public NegativeLaboratoryTestOrder update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeLaboratoryTestOrder negativeLaboratoryTestOrder)
    {
        QdmValidator.validateResourceId(negativeLaboratoryTestOrder.getId(), id);
        Optional<NegativeLaboratoryTestOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeLaboratoryTestOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeLaboratoryTestOrder, updateResource);
            updateResource.copy(negativeLaboratoryTestOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeLaboratoryTestOrder, negativeLaboratoryTestOrder);
        return repository.save(negativeLaboratoryTestOrder);
    }

    @DeleteMapping("/NegativeLaboratoryTestOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeLaboratoryTestOrder nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeLaboratoryTestOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
