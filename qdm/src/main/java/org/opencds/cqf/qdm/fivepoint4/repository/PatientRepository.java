package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String>
{
    @Nonnull
    Optional<Patient> findBySystemId(@Nonnull String id);
}
