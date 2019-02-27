package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.InconsistentId;
import org.opencds.cqf.qdm.fivepoint4.exception.MissingId;
import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PositiveEncounterPerformed;
import org.opencds.cqf.qdm.fivepoint4.repository.PositiveEncounterPerformedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@RestController
public class PositiveEncounterPerformedController implements Serializable
{
    private final PositiveEncounterPerformedRepository repository;

    @Autowired
    public PositiveEncounterPerformedController(PositiveEncounterPerformedRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/PositiveEncounterPerformed")
    public List<PositiveEncounterPerformed> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PositiveEncounterPerformed/{id}")
    public PositiveEncounterPerformed getById(@PathVariable(value = "id") String id)
    {
        return repository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PositiveEncounterPerformed/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PositiveEncounterPerformed")
    public PositiveEncounterPerformed create(@RequestBody PositiveEncounterPerformed positiveEncounterPerformed)
    {
        return repository.save(positiveEncounterPerformed);
    }

    @PutMapping("/PositiveEncounterPerformed/{id}")
    public PositiveEncounterPerformed update(@PathVariable(value = "id") String id,
                                             @RequestBody PositiveEncounterPerformed positiveEncounterPerformed)
    {
        if (positiveEncounterPerformed.getId() == null
                || positiveEncounterPerformed.getId().getValue() == null)
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Update failed: payload resource is missing id value",
                    new MissingId()
            );
        }
        if (!positiveEncounterPerformed.getId().getValue().equals(id))
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Update failed: resource id (%s) doesn't match request id (%s)", positiveEncounterPerformed.getId().getValue(), id),
                    new InconsistentId()
            );
        }

        Optional<PositiveEncounterPerformed> update = repository.findById(id);
        if (update.isPresent()) {
            PositiveEncounterPerformed updateResource = update.get();
            updateResource.setCode(positiveEncounterPerformed.getCode());
            updateResource.setPatientId(positiveEncounterPerformed.getPatientId());
            updateResource.setReporter(positiveEncounterPerformed.getReporter());
            updateResource.setRecorder(positiveEncounterPerformed.getRecorder());
            updateResource.setAuthorDateTime(positiveEncounterPerformed.getAuthorDateTime());
            updateResource.setAdmissionSource(positiveEncounterPerformed.getAdmissionSource());
            updateResource.setRelevantPeriod(positiveEncounterPerformed.getRelevantPeriod());
            updateResource.setDischargeDisposition(positiveEncounterPerformed.getDischargeDisposition());
            updateResource.setDiagnosis(positiveEncounterPerformed.getDiagnosis());
            updateResource.setFacilityLocation(positiveEncounterPerformed.getFacilityLocation());
            updateResource.setPrincipalDiagnosis(positiveEncounterPerformed.getPrincipalDiagnosis());
            updateResource.setNegationRationale(positiveEncounterPerformed.getNegationRationale());
            updateResource.setLengthOfStay(positiveEncounterPerformed.getLengthOfStay());
            return repository.save(updateResource);
        }

        return repository.save(positiveEncounterPerformed);
    }

    @DeleteMapping("/PositiveEncounterPerformed/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PositiveEncounterPerformed pep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PositiveEncounterPerformed/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(pep);

        return ResponseEntity.ok().build();
    }
}
