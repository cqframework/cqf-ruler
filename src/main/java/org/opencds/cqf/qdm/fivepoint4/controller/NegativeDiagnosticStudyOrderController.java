package org.opencds.cqf.qdm.fivepoint4.controller;

import org.opencds.cqf.qdm.fivepoint4.exception.ResourceNotFound;
import org.opencds.cqf.qdm.fivepoint4.model.*;
import org.opencds.cqf.qdm.fivepoint4.repository.NegativeDiagnosticStudyOrderRepository;
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
public class NegativeDiagnosticStudyOrderController implements Serializable
{
    private final NegativeDiagnosticStudyOrderRepository repository;

    @Autowired
    public NegativeDiagnosticStudyOrderController(NegativeDiagnosticStudyOrderRepository repository)
    {
        this.repository = repository;
    }

    @GetMapping("/NegativeDiagnosticStudyOrder")
    public List<NegativeDiagnosticStudyOrder> getAll(@RequestParam(name = "patientId", required = false) String patientId)
    {
        if (patientId == null)
        {
            return repository.findAll();
        }
        else {
            NegativeDiagnosticStudyOrder exampleType = new NegativeDiagnosticStudyOrder();
            Id pId = new Id();
            pId.setValue(patientId);
            exampleType.setPatientId(pId);
            ExampleMatcher matcher = ExampleMatcher.matchingAny().withMatcher("patientId.value", ExampleMatcher.GenericPropertyMatchers.exact());
            Example<NegativeDiagnosticStudyOrder> example = Example.of(exampleType, matcher);

            return repository.findAll(example);
        }
    }

    @GetMapping("/NegativeDiagnosticStudyOrder/{id}")
    public @ResponseBody
    NegativeDiagnosticStudyOrder getById(@PathVariable(value = "id") String id)
    {
        return repository.findBySystemId(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Read Failed: NegativeDiagnosticStudyOrder/%s not found", id),
                                new ResourceNotFound()
                        )
                );
    }

    @PostMapping("/NegativeDiagnosticStudyOrder")
    public @ResponseBody NegativeDiagnosticStudyOrder create(@RequestBody @Valid NegativeDiagnosticStudyOrder negativeDiagnosticStudyOrder)
    {
        QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyOrder, negativeDiagnosticStudyOrder);
        return repository.save(negativeDiagnosticStudyOrder);
    }

    @PutMapping("/NegativeDiagnosticStudyOrder/{id}")
    public NegativeDiagnosticStudyOrder update(@PathVariable(value = "id") String id,
                          @RequestBody @Valid NegativeDiagnosticStudyOrder negativeDiagnosticStudyOrder)
    {
        QdmValidator.validateResourceId(negativeDiagnosticStudyOrder.getId(), id);
        Optional<NegativeDiagnosticStudyOrder> update = repository.findById(id);
        if (update.isPresent())
        {
            NegativeDiagnosticStudyOrder updateResource = update.get();
            QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyOrder, updateResource);
            updateResource.copy(negativeDiagnosticStudyOrder);
            return repository.save(updateResource);
        }

        QdmValidator.validateResourceTypeAndName(negativeDiagnosticStudyOrder, negativeDiagnosticStudyOrder);
        return repository.save(negativeDiagnosticStudyOrder);
    }

    @DeleteMapping("/NegativeDiagnosticStudyOrder/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") String id)
    {
        NegativeDiagnosticStudyOrder par =
                repository.findById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        String.format("Delete Failed: NegativeDiagnosticStudyOrder/%s not found", id),
                                        new ResourceNotFound()
                                )
                        );

        repository.delete(par);

        return ResponseEntity.ok().build();
    }
}
