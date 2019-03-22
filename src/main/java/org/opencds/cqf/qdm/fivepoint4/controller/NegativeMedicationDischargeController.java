package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.NegativeMedicationDischarge;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeMedicationDischargeRepository;
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
public class NegativeMedicationDischargeController implements Serializable
{
    private final NegativeMedicationDischargeRepository repository;

    @Autowired
    public NegativeMedicationDischargeController(NegativeMedicationDischargeRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeMedicationDischarge")
    public List<NegativeMedicationDischarge> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/NegativeMedicationDischarge/{id}")
    public @ResponseBody NegativeMedicationDischarge getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeMedicationDischarge/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeMedicationDischarge")
    public @ResponseBody NegativeMedicationDischarge create(@RequestBody @Valid NegativeMedicationDischarge negativeMedicationDischarge)
    {
        QdmValidator.validateResourceTypeAndName(negativeMedicationDischarge, negativeMedicationDischarge);
        return repository.save(negativeMedicationDischarge);
    }

    @PutMapping("/NegativeMedicationDischarge/{id}")
    public NegativeMedicationDischarge update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeMedicationDischarge negativeMedicationDischarge)
    {
        QdmValidator.validateResourceId(negativeMedicationDischarge.getId(), id);
        Optional<NegativeMedicationDischarge> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeMedicationDischarge updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeMedicationDischarge, updateResource);
            updateResource.copy(negativeMedicationDischarge);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeMedicationDischarge, negativeMedicationDischarge);
        return repository.save(negativeMedicationDischarge);
    }

    @DeleteMapping("/NegativeMedicationDischarge/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeMedicationDischarge nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeMedicationDischarge/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
