package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicExpired;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCharacteristicExpiredRepository;
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
public class PatientCharacteristicExpiredController implements Serializable
{
    private final PatientCharacteristicExpiredRepository repository;

    @Autowired
    public PatientCharacteristicExpiredController(PatientCharacteristicExpiredRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCharacteristicExpired")
    public List<PatientCharacteristicExpired> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCharacteristicExpired/{id}")
    public @ResponseBody
    PatientCharacteristicExpired getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCharacteristicExpired/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCharacteristicExpired")
    public @ResponseBody PatientCharacteristicExpired create(@RequestBody @Valid PatientCharacteristicExpired patientCharacteristicExpired)
    {
        QdmValidator.validateResourceTypeAndName(patientCharacteristicExpired, patientCharacteristicExpired);
        return repository.save(patientCharacteristicExpired);
    }

    @PutMapping("/PatientCharacteristicExpired/{id}")
    public PatientCharacteristicExpired update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PatientCharacteristicExpired patientCharacteristicExpired)
    {
        QdmValidator.validateResourceId(patientCharacteristicExpired.getId(), id);
        Optional<PatientCharacteristicExpired> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCharacteristicExpired updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCharacteristicExpired, updateResource);
            updateResource.copy(patientCharacteristicExpired);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCharacteristicExpired, patientCharacteristicExpired);
        return repository.save(patientCharacteristicExpired);
    }

    @DeleteMapping("/PatientCharacteristicExpired/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCharacteristicExpired par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCharacteristicExpired/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
