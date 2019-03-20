package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveDeviceApplied;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveDeviceAppliedRepository;
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
public class PositiveDeviceAppliedController implements Serializable
{
    private final PositiveDeviceAppliedRepository repository;

    @Autowired
    public PositiveDeviceAppliedController(PositiveDeviceAppliedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveDeviceApplied")
    public List<PositiveDeviceApplied> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveDeviceApplied/{id}")
    public @ResponseBody
    PositiveDeviceApplied getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveDeviceApplied/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveDeviceApplied")
    public @ResponseBody PositiveDeviceApplied create(@RequestBody @Valid PositiveDeviceApplied positiveDeviceApplied)
    {
        QdmValidator.validateResourceTypeAndName(positiveDeviceApplied, positiveDeviceApplied);
        return repository.save(positiveDeviceApplied);
    }

    @PutMapping("/PositiveDeviceApplied/{id}")
    public PositiveDeviceApplied update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveDeviceApplied positiveDeviceApplied)
    {
        QdmValidator.validateResourceId(positiveDeviceApplied.getId(), id);
        Optional<PositiveDeviceApplied> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveDeviceApplied updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveDeviceApplied, updateResource);
            updateResource.copy(positiveDeviceApplied);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveDeviceApplied, positiveDeviceApplied);
        return repository.save(positiveDeviceApplied);
    }

    @DeleteMapping("/PositiveDeviceApplied/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveDeviceApplied par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveDeviceApplied/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
