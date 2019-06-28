package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.DiagnosisRepository;
import org.opencds.cqf.qdm.fivepoint4.validation.QdmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
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
public class DiagnosisController implements Serializable
{
    private final DiagnosisRepository repository;

    @Autowired
    public DiagnosisController(DiagnosisRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/Diagnosis")
    public List<Diagnosis> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            Diagnosis exampleType = new Diagnosis();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<Diagnosis> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/Diagnosis/{id}")
    public @ResponseBody
    Diagnosis getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: Diagnosis/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/Diagnosis")
    public @ResponseBody Diagnosis create(@RequestBody @Valid Diagnosis diagnosis)
    {
        QdmValidator.validateResourceTypeAndName(diagnosis, diagnosis);
        return repository.save(diagnosis);
    }

    @PutMapping("/Diagnosis/{id}")
    public Diagnosis update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid Diagnosis diagnosis)
    {
        QdmValidator.validateResourceId(diagnosis.getId(), id);
        Optional<Diagnosis> update = repository.findById(id);
        if (update.isPresent())
        {
            Diagnosis updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(diagnosis, updateResource);
            updateResource.copy(diagnosis);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(diagnosis, diagnosis);
        return repository.save(diagnosis);
    }

    @DeleteMapping("/Diagnosis/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        Diagnosis par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: Diagnosis/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
