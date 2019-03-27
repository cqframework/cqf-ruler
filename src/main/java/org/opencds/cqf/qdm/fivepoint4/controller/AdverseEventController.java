package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.AdverseEvent;
import org.opencds.cqf.qdm.fivepoint4.repository.AdverseEventRepository;
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
public class AdverseEventController implements Serializable
{
    private final AdverseEventRepository repository;

    @Autowired
    public AdverseEventController(AdverseEventRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/AdverseEvent")
    public List<AdverseEvent> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/AdverseEvent/{id}")
    public @ResponseBody
    AdverseEvent getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: AdverseEvent/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/AdverseEvent")
    public @ResponseBody AdverseEvent create(@RequestBody @Valid AdverseEvent adverseEvent)
    {
        QdmValidator.validateResourceTypeAndName(adverseEvent, adverseEvent);
        return repository.save(adverseEvent);
    }

    @PutMapping("/AdverseEvent/{id}")
    public AdverseEvent update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid AdverseEvent adverseEvent)
    {
        QdmValidator.validateResourceId(adverseEvent.getId(), id);
        Optional<AdverseEvent> update = repository.findById(id);
        if (update.isPresent())
        {
            AdverseEvent updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(adverseEvent, updateResource);
            updateResource.copy(adverseEvent);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(adverseEvent, adverseEvent);
        return repository.save(adverseEvent);
    }

    @DeleteMapping("/AdverseEvent/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        AdverseEvent par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: AdverseEvent/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
