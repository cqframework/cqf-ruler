package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeAssessmentPerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeAssessmentPerformedRepository;
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
public class NegativeAssessmentPerformedController implements Serializable
{
    private final NegativeAssessmentPerformedRepository repository;

    @Autowired
    public NegativeAssessmentPerformedController(NegativeAssessmentPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeAssessmentPerformed")
    public List<NegativeAssessmentPerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeAssessmentPerformed/{id}")
    public @ResponseBody
    NegativeAssessmentPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeAssessmentPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeAssessmentPerformed")
    public @ResponseBody NegativeAssessmentPerformed create(@RequestBody @Valid NegativeAssessmentPerformed negativeAssessmentPerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativeAssessmentPerformed, negativeAssessmentPerformed);
        return repository.save(negativeAssessmentPerformed);
    }

    @PutMapping("/NegativeAssessmentPerformed/{id}")
    public NegativeAssessmentPerformed update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeAssessmentPerformed negativeAssessmentPerformed)
    {
        QdmValidator.validateResourceId(negativeAssessmentPerformed.getId(), id);
        Optional<NegativeAssessmentPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeAssessmentPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeAssessmentPerformed, updateResource);
            updateResource.copy(negativeAssessmentPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeAssessmentPerformed, negativeAssessmentPerformed);
        return repository.save(negativeAssessmentPerformed);
    }

    @DeleteMapping("/NegativeAssessmentPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeAssessmentPerformed par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeAssessmentPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
