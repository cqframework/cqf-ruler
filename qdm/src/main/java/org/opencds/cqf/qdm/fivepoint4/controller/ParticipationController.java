package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.ParticipationRepository;
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
public class ParticipationController implements Serializable
{
    private final ParticipationRepository repository;

    @Autowired
    public ParticipationController(ParticipationRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/Participation")
    public List<Participation> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            Participation exampleType = new Participation();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<Participation> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/Participation/{id}")
    public @ResponseBody
    Participation getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: Participation/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/Participation")
    public @ResponseBody Participation create(@RequestBody @Valid Participation participation)
    {
        QdmValidator.validateResourceTypeAndName(participation, participation);
        return repository.save(participation);
    }

    @PutMapping("/Participation/{id}")
    public Participation update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid Participation participation)
    {
        QdmValidator.validateResourceId(participation.getId(), id);
        Optional<Participation> update = repository.findById(id);
        if (update.isPresent())
        {
            Participation updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(participation, updateResource);
            updateResource.copy(participation);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(participation, participation);
        return repository.save(participation);
    }

    @DeleteMapping("/Participation/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        Participation par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: Participation/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
