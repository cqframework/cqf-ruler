package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeDiagnosticStudyRecommended;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeDiagnosticStudyRecommendedRepository;
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
public class NegativeDiagnosticStudyRecommendedController implements Serializable
{
    private final NegativeDiagnosticStudyRecommendedRepository repository;

    @Autowired
    public NegativeDiagnosticStudyRecommendedController(NegativeDiagnosticStudyRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeDiagnosticStudyRecommended")
    public List<NegativeDiagnosticStudyRecommended> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeDiagnosticStudyRecommended/{id}")
    public @ResponseBody
    NegativeDiagnosticStudyRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeDiagnosticStudyRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeDiagnosticStudyRecommended")
    public @ResponseBody NegativeDiagnosticStudyRecommended create(@RequestBody @Valid NegativeDiagnosticStudyRecommended negativeDiagnosticStudyRecommended)
    {
        QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyRecommended, negativeDiagnosticStudyRecommended);
        return repository.save(negativeDiagnosticStudyRecommended);
    }

    @PutMapping("/NegativeDiagnosticStudyRecommended/{id}")
    public NegativeDiagnosticStudyRecommended update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeDiagnosticStudyRecommended negativeDiagnosticStudyRecommended)
    {
        QdmValidator.validateResourceId(negativeDiagnosticStudyRecommended.getId(), id);
        Optional<NegativeDiagnosticStudyRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeDiagnosticStudyRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyRecommended, updateResource);
            updateResource.copy(negativeDiagnosticStudyRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyRecommended, negativeDiagnosticStudyRecommended);
        return repository.save(negativeDiagnosticStudyRecommended);
    }

    @DeleteMapping("/NegativeDiagnosticStudyRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeDiagnosticStudyRecommended par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeDiagnosticStudyRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
