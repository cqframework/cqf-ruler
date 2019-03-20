package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeSubstanceRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeSubstanceRecommendedRepository;
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
public class NegativeSubstanceRecommendedController implements Serializable
{
    private final NegativeSubstanceRecommendedRepository repository;

    @Autowired
    public NegativeSubstanceRecommendedController(NegativeSubstanceRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeSubstanceRecommended")
    public List<NegativeSubstanceRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeSubstanceRecommended/{id}")
    public @ResponseBody NegativeSubstanceRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeSubstanceRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeSubstanceRecommended")
    public @ResponseBody NegativeSubstanceRecommended create(@RequestBody @Valid NegativeSubstanceRecommended negativeSubstanceRecommended)
    {
        QdmValidator.validateResourceTypeAndName(negativeSubstanceRecommended, negativeSubstanceRecommended);
        return repository.save(negativeSubstanceRecommended);
    }

    @PutMapping("/NegativeSubstanceRecommended/{id}")
    public NegativeSubstanceRecommended update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeSubstanceRecommended negativeSubstanceRecommended)
    {
        QdmValidator.validateResourceId(negativeSubstanceRecommended.getId(), id);
        Optional<NegativeSubstanceRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeSubstanceRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeSubstanceRecommended, updateResource);
            updateResource.copy(negativeSubstanceRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeSubstanceRecommended, negativeSubstanceRecommended);
        return repository.save(negativeSubstanceRecommended);
    }

    @DeleteMapping("/NegativeSubstanceRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeSubstanceRecommended nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeSubstanceRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
