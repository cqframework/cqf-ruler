package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveProcedureRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveProcedureRecommendedRepository;
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
public class PositiveProcedureRecommendedController implements Serializable
{
    private final PositiveProcedureRecommendedRepository repository;

    @Autowired
    public PositiveProcedureRecommendedController(PositiveProcedureRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveProcedureRecommended")
    public List<PositiveProcedureRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveProcedureRecommended/{id}")
    public @ResponseBody PositiveProcedureRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveProcedureRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveProcedureRecommended")
    public PositiveProcedureRecommended create(@RequestBody @Valid PositiveProcedureRecommended positiveProcedureRecommended)
    {
        QdmValidator.validateResourceTypeAndName(positiveProcedureRecommended, positiveProcedureRecommended);
        return repository.save(positiveProcedureRecommended);
    }

    @PutMapping("/PositiveProcedureRecommended/{id}")
    public PositiveProcedureRecommended update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveProcedureRecommended positiveProcedureRecommended)
    {
        QdmValidator.validateResourceId(positiveProcedureRecommended.getId(), id);
        Optional<PositiveProcedureRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveProcedureRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveProcedureRecommended, updateResource);
            updateResource.copy(positiveProcedureRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveProcedureRecommended, positiveProcedureRecommended);
        return repository.save(positiveProcedureRecommended);
    }

    @DeleteMapping("/PositiveProcedureRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveProcedureRecommended pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveProcedureRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
