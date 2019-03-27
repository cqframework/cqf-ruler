package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeLaboratoryTestPerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeLaboratoryTestPerformedRepository;
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
public class NegativeLaboratoryTestPerformedController implements Serializable
{
    private final NegativeLaboratoryTestPerformedRepository repository;

    @Autowired
    public NegativeLaboratoryTestPerformedController(NegativeLaboratoryTestPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeLaboratoryTestPerformed")
    public List<NegativeLaboratoryTestPerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeLaboratoryTestPerformed/{id}")
    public @ResponseBody NegativeLaboratoryTestPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeLaboratoryTestPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeLaboratoryTestPerformed")
    public @ResponseBody NegativeLaboratoryTestPerformed create(@RequestBody @Valid NegativeLaboratoryTestPerformed negativeLaboratoryTestPerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativeLaboratoryTestPerformed, negativeLaboratoryTestPerformed);
        return repository.save(negativeLaboratoryTestPerformed);
    }

    @PutMapping("/NegativeLaboratoryTestPerformed/{id}")
    public NegativeLaboratoryTestPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeLaboratoryTestPerformed negativeLaboratoryTestPerformed)
    {
        QdmValidator.validateResourceId(negativeLaboratoryTestPerformed.getId(), id);
        Optional<NegativeLaboratoryTestPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeLaboratoryTestPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeLaboratoryTestPerformed, updateResource);
            updateResource.copy(negativeLaboratoryTestPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeLaboratoryTestPerformed, negativeLaboratoryTestPerformed);
        return repository.save(negativeLaboratoryTestPerformed);
    }

    @DeleteMapping("/NegativeLaboratoryTestPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeLaboratoryTestPerformed nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeLaboratoryTestPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
