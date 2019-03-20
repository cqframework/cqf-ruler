package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicEthnicity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PatientCharacteristicEthnicityRepository extends JpaRepository<PatientCharacteristicEthnicity, String>
{
    @Nonnull
    Optional<PatientCharacteristicEthnicity> findBySystemId(@Nonnull String id);
}
