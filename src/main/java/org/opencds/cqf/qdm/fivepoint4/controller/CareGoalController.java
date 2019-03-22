package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.CareGoal;
import org.opencds.cqf.qdm.fivepoint4.repository.CareGoalRepository;
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
public class CareGoalController implements Serializable
{
    private final CareGoalRepository repository;

    @Autowired
    public CareGoalController(CareGoalRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/CareGoal")
    public List<CareGoal> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/CareGoal/{id}")
    public @ResponseBody
    CareGoal getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: CareGoal/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/CareGoal")
    public @ResponseBody CareGoal create(@RequestBody @Valid CareGoal careGoal)
    {
        QdmValidator.validateResourceTypeAndName(careGoal, careGoal);
        return repository.save(careGoal);
    }

    @PutMapping("/CareGoal/{id}")
    public CareGoal update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid CareGoal careGoal)
    {
        QdmValidator.validateResourceId(careGoal.getId(), id);
        Optional<CareGoal> update = repository.findById(id);
        if (update.isPresent())
        {
            CareGoal updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(careGoal, updateResource);
            updateResource.copy(careGoal);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(careGoal, careGoal);
        return repository.save(careGoal);
    }

    @DeleteMapping("/CareGoal/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        CareGoal par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: CareGoal/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
