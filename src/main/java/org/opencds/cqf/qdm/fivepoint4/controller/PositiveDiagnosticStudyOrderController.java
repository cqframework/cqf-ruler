package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveDiagnosticStudyOrder;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveDiagnosticStudyOrderRepository;
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
public class PositiveDiagnosticStudyOrderController implements Serializable
{
    private final PositiveDiagnosticStudyOrderRepository repository;

    @Autowired
    public PositiveDiagnosticStudyOrderController(PositiveDiagnosticStudyOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveDiagnosticStudyOrder")
    public List<PositiveDiagnosticStudyOrder> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveDiagnosticStudyOrder/{id}")
    public @ResponseBody
    PositiveDiagnosticStudyOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveDiagnosticStudyOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveDiagnosticStudyOrder")
    public @ResponseBody PositiveDiagnosticStudyOrder create(@RequestBody @Valid PositiveDiagnosticStudyOrder positiveDiagnosticStudyOrder)
    {
        QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyOrder, positiveDiagnosticStudyOrder);
        return repository.save(positiveDiagnosticStudyOrder);
    }

    @PutMapping("/PositiveDiagnosticStudyOrder/{id}")
    public PositiveDiagnosticStudyOrder update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveDiagnosticStudyOrder positiveDiagnosticStudyOrder)
    {
        QdmValidator.validateResourceId(positiveDiagnosticStudyOrder.getId(), id);
        Optional<PositiveDiagnosticStudyOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveDiagnosticStudyOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyOrder, updateResource);
            updateResource.copy(positiveDiagnosticStudyOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveDiagnosticStudyOrder, positiveDiagnosticStudyOrder);
        return repository.save(positiveDiagnosticStudyOrder);
    }

    @DeleteMapping("/PositiveDiagnosticStudyOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveDiagnosticStudyOrder par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveDiagnosticStudyOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
