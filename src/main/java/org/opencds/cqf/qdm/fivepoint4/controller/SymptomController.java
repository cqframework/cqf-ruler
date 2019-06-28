package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.SymptomRepository;
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
public class SymptomController implements Serializable
{
    private final SymptomRepository repository;

    @Autowired
    public SymptomController(SymptomRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/Symptom")
    public List<Symptom> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            Symptom exampleType = new Symptom();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<Symptom> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/Symptom/{id}")
    public @ResponseBody
    Symptom getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: Symptom/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/Symptom")
    public @ResponseBody Symptom create(@RequestBody @Valid Symptom symptom)
    {
        QdmValidator.validateResourceTypeAndName(symptom, symptom);
        return repository.save(symptom);
    }

    @PutMapping("/Symptom/{id}")
    public Symptom update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid Symptom symptom)
    {
        QdmValidator.validateResourceId(symptom.getId(), id);
        Optional<Symptom> update = repository.findById(id);
        if (update.isPresent())
        {
            Symptom updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(symptom, updateResource);
            updateResource.copy(symptom);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(symptom, symptom);
        return repository.save(symptom);
    }

    @DeleteMapping("/Symptom/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        Symptom par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: Symptom/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
