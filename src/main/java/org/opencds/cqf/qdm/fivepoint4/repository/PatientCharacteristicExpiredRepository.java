package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicExpired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PatientCharacteristicExpiredRepository extends JpaRepository<PatientCharacteristicExpired, String>
{
    @Nonnull
    Optional<PatientCharacteristicExpired> findBySystemId(@Nonnull String id);
}
