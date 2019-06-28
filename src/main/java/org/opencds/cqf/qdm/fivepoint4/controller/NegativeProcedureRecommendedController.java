package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeProcedureRecommendedRepository;
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
public class NegativeProcedureRecommendedController implements Serializable
{
    private final NegativeProcedureRecommendedRepository repository;

    @Autowired
    public NegativeProcedureRecommendedController(NegativeProcedureRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeProcedureRecommended")
    public List<NegativeProcedureRecommended> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeProcedureRecommended exampleType = new NegativeProcedureRecommended();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeProcedureRecommended> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeProcedureRecommended/{id}")
    public @ResponseBody NegativeProcedureRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeProcedureRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeProcedureRecommended")
    public @ResponseBody NegativeProcedureRecommended create(@RequestBody @Valid NegativeProcedureRecommended negativeProcedureRecommended)
    {
        QdmValidator.validateResourceTypeAndName(negativeProcedureRecommended, negativeProcedureRecommended);
        return repository.save(negativeProcedureRecommended);
    }

    @PutMapping("/NegativeProcedureRecommended/{id}")
    public NegativeProcedureRecommended update(@PathVariable(value = "id") String id,
                                             @RequestBody @Valid NegativeProcedureRecommended negativeProcedureRecommended)
    {
        QdmValidator.validateResourceId(negativeProcedureRecommended.getId(), id);
        Optional<NegativeProcedureRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeProcedureRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeProcedureRecommended, updateResource);
            updateResource.copy(negativeProcedureRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeProcedureRecommended, negativeProcedureRecommended);
        return repository.save(negativeProcedureRecommended);
    }

    @DeleteMapping("/NegativeProcedureRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeProcedureRecommended nep =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeProcedureRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(nep);

        return ResponseEntity.ok().build();
    }
}
