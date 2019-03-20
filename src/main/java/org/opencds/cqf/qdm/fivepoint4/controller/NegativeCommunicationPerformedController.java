package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeCommunicationPerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeCommunicationPerformedRepository;
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
public class NegativeCommunicationPerformedController implements Serializable
{
    private final NegativeCommunicationPerformedRepository repository;

    @Autowired
    public NegativeCommunicationPerformedController(NegativeCommunicationPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeCommunicationPerformed")
    public List<NegativeCommunicationPerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeCommunicationPerformed/{id}")
    public @ResponseBody NegativeCommunicationPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeCommunicationPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeCommunicationPerformed")
    public @ResponseBody NegativeCommunicationPerformed create(@RequestBody @Valid NegativeCommunicationPerformed negativeCommunicationPerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativeCommunicationPerformed, negativeCommunicationPerformed);
        return repository.save(negativeCommunicationPerformed);
    }

    @PutMapping("/NegativeCommunicationPerformed/{id}")
    public NegativeCommunicationPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeCommunicationPerformed negativeCommunicationPerformed)
    {
        QdmValidator.validateResourceId(negativeCommunicationPerformed.getId(), id);
        Optional<NegativeCommunicationPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeCommunicationPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeCommunicationPerformed, updateResource);
            updateResource.copy(negativeCommunicationPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeCommunicationPerformed, negativeCommunicationPerformed);
        return repository.save(negativeCommunicationPerformed);
    }

    @DeleteMapping("/NegativeCommunicationPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeCommunicationPerformed nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeCommunicationPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
