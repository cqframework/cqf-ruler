package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeEncounterRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeEncounterRecommendedRepository;
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
public class NegativeEncounterRecommendedController implements Serializable
{
    private final NegativeEncounterRecommendedRepository repository;

    @Autowired
    public NegativeEncounterRecommendedController(NegativeEncounterRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeEncounterRecommended")
    public List<NegativeEncounterRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeEncounterRecommended/{id}")
    public @ResponseBody NegativeEncounterRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeEncounterRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeEncounterRecommended")
    public @ResponseBody NegativeEncounterRecommended create(@RequestBody @Valid NegativeEncounterRecommended negativeEncounterRecommended)
    {
        QdmValidator.validateResourceTypeAndName(negativeEncounterRecommended, negativeEncounterRecommended);
        return repository.save(negativeEncounterRecommended);
    }

    @PutMapping("/NegativeEncounterRecommended/{id}")
    public NegativeEncounterRecommended update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeEncounterRecommended negativeEncounterRecommended)
    {
        QdmValidator.validateResourceId(negativeEncounterRecommended.getId(), id);
        Optional<NegativeEncounterRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeEncounterRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeEncounterRecommended, updateResource);
            updateResource.copy(negativeEncounterRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeEncounterRecommended, negativeEncounterRecommended);
        return repository.save(negativeEncounterRecommended);
    }

    @DeleteMapping("/NegativeEncounterRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeEncounterRecommended nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeEncounterRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
