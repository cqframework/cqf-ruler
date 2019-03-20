package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicSex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PatientCharacteristicSexRepository extends JpaRepository<PatientCharacteristicSex, String>
{
    @Nonnull
    Optional<PatientCharacteristicSex> findBySystemId(@Nonnull String id);
}
