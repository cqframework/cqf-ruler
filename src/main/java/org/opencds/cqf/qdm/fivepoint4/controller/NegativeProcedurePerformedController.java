package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeProcedurePerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeProcedurePerformedRepository;
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
public class NegativeProcedurePerformedController implements Serializable
{
    private final NegativeProcedurePerformedRepository repository;

    @Autowired
    public NegativeProcedurePerformedController(NegativeProcedurePerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeProcedurePerformed")
    public List<NegativeProcedurePerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeProcedurePerformed/{id}")
    public @ResponseBody NegativeProcedurePerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeProcedurePerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeProcedurePerformed")
    public @ResponseBody NegativeProcedurePerformed create(@RequestBody @Valid NegativeProcedurePerformed negativeProcedurePerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativeProcedurePerformed, negativeProcedurePerformed);
        return repository.save(negativeProcedurePerformed);
    }

    @PutMapping("/NegativeProcedurePerformed/{id}")
    public NegativeProcedurePerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeProcedurePerformed negativeProcedurePerformed)
    {
        QdmValidator.validateResourceId(negativeProcedurePerformed.getId(), id);
        Optional<NegativeProcedurePerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeProcedurePerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeProcedurePerformed, updateResource);
            updateResource.copy(negativeProcedurePerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeProcedurePerformed, negativeProcedurePerformed);
        return repository.save(negativeProcedurePerformed);
    }

    @DeleteMapping("/NegativeProcedurePerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeProcedurePerformed nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeProcedurePerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
