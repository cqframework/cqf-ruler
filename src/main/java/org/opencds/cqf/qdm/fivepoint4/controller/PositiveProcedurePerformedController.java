package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveProcedurePerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveProcedurePerformedRepository;
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
public class PositiveProcedurePerformedController implements Serializable
{
    private final PositiveProcedurePerformedRepository repository;

    @Autowired
    public PositiveProcedurePerformedController(PositiveProcedurePerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveProcedurePerformed")
    public List<PositiveProcedurePerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveProcedurePerformed/{id}")
    public @ResponseBody PositiveProcedurePerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveProcedurePerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveProcedurePerformed")
    public PositiveProcedurePerformed create(@RequestBody @Valid PositiveProcedurePerformed positiveProcedurePerformed)
    {
        QdmValidator.validateResourceTypeAndName(positiveProcedurePerformed, positiveProcedurePerformed);
        return repository.save(positiveProcedurePerformed);
    }

    @PutMapping("/PositiveProcedurePerformed/{id}")
    public PositiveProcedurePerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveProcedurePerformed positiveProcedurePerformed)
    {
        QdmValidator.validateResourceId(positiveProcedurePerformed.getId(), id);
        Optional<PositiveProcedurePerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveProcedurePerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveProcedurePerformed, updateResource);
            updateResource.copy(positiveProcedurePerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveProcedurePerformed, positiveProcedurePerformed);
        return repository.save(positiveProcedurePerformed);
    }

    @DeleteMapping("/PositiveProcedurePerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveProcedurePerformed pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveProcedurePerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
