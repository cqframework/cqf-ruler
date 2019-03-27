package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeDeviceApplied;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeDeviceAppliedRepository;
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
public class NegativeDeviceAppliedController implements Serializable
{
    private final NegativeDeviceAppliedRepository repository;

    @Autowired
    public NegativeDeviceAppliedController(NegativeDeviceAppliedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeDeviceApplied")
    public List<NegativeDeviceApplied> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeDeviceApplied/{id}")
    public @ResponseBody
    NegativeDeviceApplied getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeDeviceApplied/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeDeviceApplied")
    public @ResponseBody NegativeDeviceApplied create(@RequestBody @Valid NegativeDeviceApplied negativeDeviceApplied)
    {
        QdmValidator.validateResourceTypeAndName(negativeDeviceApplied, negativeDeviceApplied);
        return repository.save(negativeDeviceApplied);
    }

    @PutMapping("/NegativeDeviceApplied/{id}")
    public NegativeDeviceApplied update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeDeviceApplied negativeDeviceApplied)
    {
        QdmValidator.validateResourceId(negativeDeviceApplied.getId(), id);
        Optional<NegativeDeviceApplied> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeDeviceApplied updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeDeviceApplied, updateResource);
            updateResource.copy(negativeDeviceApplied);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeDeviceApplied, negativeDeviceApplied);
        return repository.save(negativeDeviceApplied);
    }

    @DeleteMapping("/NegativeDeviceApplied/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeDeviceApplied par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeDeviceApplied/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
