package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeSubstanceAdministered;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeSubstanceAdministeredRepository;
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
public class NegativeSubstanceAdministeredController implements Serializable
{
    private final NegativeSubstanceAdministeredRepository repository;

    @Autowired
    public NegativeSubstanceAdministeredController(NegativeSubstanceAdministeredRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeSubstanceAdministered")
    public List<NegativeSubstanceAdministered> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeSubstanceAdministered/{id}")
    public @ResponseBody NegativeSubstanceAdministered getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeSubstanceAdministered/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeSubstanceAdministered")
    public @ResponseBody NegativeSubstanceAdministered create(@RequestBody @Valid NegativeSubstanceAdministered negativeSubstanceAdministered)
    {
        QdmValidator.validateResourceTypeAndName(negativeSubstanceAdministered, negativeSubstanceAdministered);
        return repository.save(negativeSubstanceAdministered);
    }

    @PutMapping("/NegativeSubstanceAdministered/{id}")
    public NegativeSubstanceAdministered update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeSubstanceAdministered negativeSubstanceAdministered)
    {
        QdmValidator.validateResourceId(negativeSubstanceAdministered.getId(), id);
        Optional<NegativeSubstanceAdministered> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeSubstanceAdministered updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeSubstanceAdministered, updateResource);
            updateResource.copy(negativeSubstanceAdministered);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeSubstanceAdministered, negativeSubstanceAdministered);
        return repository.save(negativeSubstanceAdministered);
    }

    @DeleteMapping("/NegativeSubstanceAdministered/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeSubstanceAdministered nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeSubstanceAdministered/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
