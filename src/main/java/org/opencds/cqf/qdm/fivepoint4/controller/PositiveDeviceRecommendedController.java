package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveDeviceRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveDeviceRecommendedRepository;
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
public class PositiveDeviceRecommendedController implements Serializable
{
    private final PositiveDeviceRecommendedRepository repository;

    @Autowired
    public PositiveDeviceRecommendedController(PositiveDeviceRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PositiveDeviceRecommended")
    public List<PositiveDeviceRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveDeviceRecommended/{id}")
    public @ResponseBody
    PositiveDeviceRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveDeviceRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveDeviceRecommended")
    public @ResponseBody PositiveDeviceRecommended create(@RequestBody @Valid PositiveDeviceRecommended positiveDeviceRecommended)
    {
        QdmValidator.validateResourceTypeAndName(positiveDeviceRecommended, positiveDeviceRecommended);
        return repository.save(positiveDeviceRecommended);
    }

    @PutMapping("/PositiveDeviceRecommended/{id}")
    public PositiveDeviceRecommended update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PositiveDeviceRecommended positiveDeviceRecommended)
    {
        QdmValidator.validateResourceId(positiveDeviceRecommended.getId(), id);
        Optional<PositiveDeviceRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            PositiveDeviceRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(positiveDeviceRecommended, updateResource);
            updateResource.copy(positiveDeviceRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(positiveDeviceRecommended, positiveDeviceRecommended);
        return repository.save(positiveDeviceRecommended);
    }

    @DeleteMapping("/PositiveDeviceRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveDeviceRecommended par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveDeviceRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
