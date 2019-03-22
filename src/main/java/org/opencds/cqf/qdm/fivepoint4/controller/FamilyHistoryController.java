package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.FamilyHistory;
import org.opencds.cqf.qdm.fivepoint4.repository.FamilyHistoryRepository;
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
public class FamilyHistoryController implements Serializable
{
    private final FamilyHistoryRepository repository;

    @Autowired
    public FamilyHistoryController(FamilyHistoryRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/FamilyHistory")
    public List<FamilyHistory> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/FamilyHistory/{id}")
    public @ResponseBody
    FamilyHistory getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: FamilyHistory/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/FamilyHistory")
    public @ResponseBody FamilyHistory create(@RequestBody @Valid FamilyHistory familyHistory)
    {
        QdmValidator.validateResourceTypeAndName(familyHistory, familyHistory);
        return repository.save(familyHistory);
    }

    @PutMapping("/FamilyHistory/{id}")
    public FamilyHistory update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid FamilyHistory familyHistory)
    {
        QdmValidator.validateResourceId(familyHistory.getId(), id);
        Optional<FamilyHistory> update = repository.findById(id);
        if (update.isPresent())
        {
            FamilyHistory updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(familyHistory, updateResource);
            updateResource.copy(familyHistory);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(familyHistory, familyHistory);
        return repository.save(familyHistory);
    }

    @DeleteMapping("/FamilyHistory/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        FamilyHistory par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: FamilyHistory/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
