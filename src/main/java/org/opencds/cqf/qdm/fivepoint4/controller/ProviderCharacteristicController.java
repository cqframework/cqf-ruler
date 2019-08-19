package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.ProviderCharacteristicRepository;
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
public class ProviderCharacteristicController implements Serializable
{
    private final ProviderCharacteristicRepository repository;

    @Autowired
    public ProviderCharacteristicController(ProviderCharacteristicRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/ProviderCharacteristic")
    public List<ProviderCharacteristic> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            ProviderCharacteristic exampleType = new ProviderCharacteristic();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<ProviderCharacteristic> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/ProviderCharacteristic/{id}")
    public @ResponseBody
    ProviderCharacteristic getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: ProviderCharacteristic/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/ProviderCharacteristic")
    public @ResponseBody ProviderCharacteristic create(@RequestBody @Valid ProviderCharacteristic providerCharacteristic)
    {
        QdmValidator.validateResourceTypeAndName(providerCharacteristic, providerCharacteristic);
        return repository.save(providerCharacteristic);
    }

    @PutMapping("/ProviderCharacteristic/{id}")
    public ProviderCharacteristic update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid ProviderCharacteristic providerCharacteristic)
    {
        QdmValidator.validateResourceId(providerCharacteristic.getId(), id);
        Optional<ProviderCharacteristic> update = repository.findById(id);
        if (update.isPresent())
        {
            ProviderCharacteristic updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(providerCharacteristic, updateResource);
            updateResource.copy(providerCharacteristic);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(providerCharacteristic, providerCharacteristic);
        return repository.save(providerCharacteristic);
    }

    @DeleteMapping("/ProviderCharacteristic/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        ProviderCharacteristic par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: ProviderCharacteristic/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
