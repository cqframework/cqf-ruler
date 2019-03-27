package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.PatientCareExperience;
import org.opencds.cqf.qdm.fivepoint4.repository.PatientCareExperienceRepository;
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
public class PatientCareExperienceController implements Serializable
{
    private final PatientCareExperienceRepository repository;

    @Autowired
    public PatientCareExperienceController(PatientCareExperienceRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/PatientCareExperience")
    public List<PatientCareExperience> getAll()
    {
        return repository.findAll();
    }

    @GetMapping("/PatientCareExperience/{id}")
    public @ResponseBody
    PatientCareExperience getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: PatientCareExperience/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/PatientCareExperience")
    public @ResponseBody PatientCareExperience create(@RequestBody @Valid PatientCareExperience patientCareExperience)
    {
        QdmValidator.validateResourceTypeAndName(patientCareExperience, patientCareExperience);
        return repository.save(patientCareExperience);
    }

    @PutMapping("/PatientCareExperience/{id}")
    public PatientCareExperience update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid PatientCareExperience patientCareExperience)
    {
        QdmValidator.validateResourceId(patientCareExperience.getId(), id);
        Optional<PatientCareExperience> update = repository.findById(id);
        if (update.isPresent())
        {
            PatientCareExperience updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(patientCareExperience, updateResource);
            updateResource.copy(patientCareExperience);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(patientCareExperience, patientCareExperience);
        return repository.save(patientCareExperience);
    }

    @DeleteMapping("/PatientCareExperience/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        PatientCareExperience par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: PatientCareExperience/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
