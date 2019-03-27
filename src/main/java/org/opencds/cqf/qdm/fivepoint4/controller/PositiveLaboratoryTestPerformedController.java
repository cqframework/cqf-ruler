package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveLaboratoryTestPerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveLaboratoryTestPerformedRepository;
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
public class PositiveLaboratoryTestPerformedController implements Serializable
{
    private final PositiveLaboratoryTestPerformedRepository repository;

    @Autowired
    public PositiveLaboratoryTestPerformedController(PositiveLaboratoryTestPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveLaboratoryTestPerformed")
    public List<PositiveLaboratoryTestPerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveLaboratoryTestPerformed/{id}")
    public @ResponseBody PositiveLaboratoryTestPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveLaboratoryTestPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveLaboratoryTestPerformed")
    public PositiveLaboratoryTestPerformed create(@RequestBody @Valid PositiveLaboratoryTestPerformed positiveLaboratoryTestPerformed)
    {
        QdmValidator.validateResourceTypeAndName(positiveLaboratoryTestPerformed, positiveLaboratoryTestPerformed);
        return repository.save(positiveLaboratoryTestPerformed);
    }

    @PutMapping("/PositiveLaboratoryTestPerformed/{id}")
    public PositiveLaboratoryTestPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid PositiveLaboratoryTestPerformed positiveLaboratoryTestPerformed)
    {
        QdmValidator.validateResourceId(positiveLaboratoryTestPerformed.getId(), id);
        Optional<PositiveLaboratoryTestPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveLaboratoryTestPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveLaboratoryTestPerformed, updateResource);
            updateResource.copy(positiveLaboratoryTestPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveLaboratoryTestPerformed, positiveLaboratoryTestPerformed);
        return repository.save(positiveLaboratoryTestPerformed);
    }

    @DeleteMapping("/PositiveLaboratoryTestPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveLaboratoryTestPerformed pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveLaboratoryTestPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
