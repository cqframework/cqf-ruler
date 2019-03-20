package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PatientCharacteristicRepository extends JpaRepository<PatientCharacteristic, String>
{
    @Nonnull
    Optional<PatientCharacteristic> findBySystemId(@Nonnull String id);
}
