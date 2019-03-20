package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeDiagnosticStudyPerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeDiagnosticStudyPerformedRepository;
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
public class NegativeDiagnosticStudyPerformedController implements Serializable
{
    private final NegativeDiagnosticStudyPerformedRepository repository;

    @Autowired
    public NegativeDiagnosticStudyPerformedController(NegativeDiagnosticStudyPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeDiagnosticStudyPerformed")
    public List<NegativeDiagnosticStudyPerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeDiagnosticStudyPerformed/{id}")
    public @ResponseBody
    NegativeDiagnosticStudyPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeDiagnosticStudyPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeDiagnosticStudyPerformed")
    public @ResponseBody NegativeDiagnosticStudyPerformed create(@RequestBody @Valid NegativeDiagnosticStudyPerformed negativeDiagnosticStudyPerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyPerformed, negativeDiagnosticStudyPerformed);
        return repository.save(negativeDiagnosticStudyPerformed);
    }

    @PutMapping("/NegativeDiagnosticStudyPerformed/{id}")
    public NegativeDiagnosticStudyPerformed update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeDiagnosticStudyPerformed negativeDiagnosticStudyPerformed)
    {
        QdmValidator.validateResourceId(negativeDiagnosticStudyPerformed.getId(), id);
        Optional<NegativeDiagnosticStudyPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeDiagnosticStudyPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyPerformed, updateResource);
            updateResource.copy(negativeDiagnosticStudyPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyPerformed, negativeDiagnosticStudyPerformed);
        return repository.save(negativeDiagnosticStudyPerformed);
    }

    @DeleteMapping("/NegativeDiagnosticStudyPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeDiagnosticStudyPerformed par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeDiagnosticStudyPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
