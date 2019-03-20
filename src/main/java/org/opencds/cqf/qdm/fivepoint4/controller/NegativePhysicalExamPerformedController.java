package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativePhysicalExamPerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativePhysicalExamPerformedRepository;
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
public class NegativePhysicalExamPerformedController implements Serializable
{
    private final NegativePhysicalExamPerformedRepository repository;

    @Autowired
    public NegativePhysicalExamPerformedController(NegativePhysicalExamPerformedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativePhysicalExamPerformed")
    public List<NegativePhysicalExamPerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativePhysicalExamPerformed/{id}")
    public @ResponseBody NegativePhysicalExamPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativePhysicalExamPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativePhysicalExamPerformed")
    public @ResponseBody NegativePhysicalExamPerformed create(@RequestBody @Valid NegativePhysicalExamPerformed negativePhysicalExamPerformed)
    {
        QdmValidator.validateResourceTypeAndName(negativePhysicalExamPerformed, negativePhysicalExamPerformed);
        return repository.save(negativePhysicalExamPerformed);
    }

    @PutMapping("/NegativePhysicalExamPerformed/{id}")
    public NegativePhysicalExamPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativePhysicalExamPerformed negativePhysicalExamPerformed)
    {
        QdmValidator.validateResourceId(negativePhysicalExamPerformed.getId(), id);
        Optional<NegativePhysicalExamPerformed> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativePhysicalExamPerformed updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativePhysicalExamPerformed, updateResource);
            updateResource.copy(negativePhysicalExamPerformed);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativePhysicalExamPerformed, negativePhysicalExamPerformed);
        return repository.save(negativePhysicalExamPerformed);
    }

    @DeleteMapping("/NegativePhysicalExamPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativePhysicalExamPerformed nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativePhysicalExamPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
