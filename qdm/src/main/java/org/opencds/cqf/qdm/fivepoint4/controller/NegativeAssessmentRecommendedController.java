package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeAssessmentRecommendedRepository;
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
public class NegativeAssessmentRecommendedController implements Serializable
{
    private final NegativeAssessmentRecommendedRepository repository;

    @Autowired
    public NegativeAssessmentRecommendedController(NegativeAssessmentRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeAssessmentRecommended")
    public List<NegativeAssessmentRecommended> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeAssessmentRecommended exampleType = new NegativeAssessmentRecommended();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeAssessmentRecommended> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeAssessmentRecommended/{id}")
    public @ResponseBody
    NegativeAssessmentRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeAssessmentRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeAssessmentRecommended")
    public @ResponseBody NegativeAssessmentRecommended create(@RequestBody @Valid NegativeAssessmentRecommended negativeAssessmentRecommended)
    {
        QdmValidator.validateResourceTypeAndName(negativeAssessmentRecommended, negativeAssessmentRecommended);
        return repository.save(negativeAssessmentRecommended);
    }

    @PutMapping("/NegativeAssessmentRecommended/{id}")
    public NegativeAssessmentRecommended update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeAssessmentRecommended negativeAssessmentRecommended)
    {
        QdmValidator.validateResourceId(negativeAssessmentRecommended.getId(), id);
        Optional<NegativeAssessmentRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeAssessmentRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeAssessmentRecommended, updateResource);
            updateResource.copy(negativeAssessmentRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeAssessmentRecommended, negativeAssessmentRecommended);
        return repository.save(negativeAssessmentRecommended);
    }

    @DeleteMapping("/NegativeAssessmentRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeAssessmentRecommended par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeAssessmentRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
