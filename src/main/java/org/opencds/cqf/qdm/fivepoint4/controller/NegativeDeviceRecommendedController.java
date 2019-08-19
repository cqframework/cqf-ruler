package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeDeviceRecommendedRepository;
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
public class NegativeDeviceRecommendedController implements Serializable
{
    private final NegativeDeviceRecommendedRepository repository;

    @Autowired
    public NegativeDeviceRecommendedController(NegativeDeviceRecommendedRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeDeviceRecommended")
    public List<NegativeDeviceRecommended> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeDeviceRecommended exampleType = new NegativeDeviceRecommended();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeDeviceRecommended> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeDeviceRecommended/{id}")
    public @ResponseBody
    NegativeDeviceRecommended getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeDeviceRecommended/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeDeviceRecommended")
    public @ResponseBody NegativeDeviceRecommended create(@RequestBody @Valid NegativeDeviceRecommended negativeDeviceRecommended)
    {
        QdmValidator.validateResourceTypeAndName(negativeDeviceRecommended, negativeDeviceRecommended);
        return repository.save(negativeDeviceRecommended);
    }

    @PutMapping("/NegativeDeviceRecommended/{id}")
    public NegativeDeviceRecommended update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeDeviceRecommended negativeDeviceRecommended)
    {
        QdmValidator.validateResourceId(negativeDeviceRecommended.getId(), id);
        Optional<NegativeDeviceRecommended> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeDeviceRecommended updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeDeviceRecommended, updateResource);
            updateResource.copy(negativeDeviceRecommended);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeDeviceRecommended, negativeDeviceRecommended);
        return repository.save(negativeDeviceRecommended);
    }

    @DeleteMapping("/NegativeDeviceRecommended/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeDeviceRecommended par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeDeviceRecommended/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
