package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicBirthdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PatientCharacteristicBirthdateRepository extends JpaRepository<PatientCharacteristicBirthdate, String>
{
    @Nonnull
    Optional<PatientCharacteristicBirthdate> findBySystemId(@Nonnull String id);
}
