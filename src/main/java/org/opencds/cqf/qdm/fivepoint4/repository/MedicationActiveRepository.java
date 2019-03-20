package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.MedicationActive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface MedicationActiveRepository extends JpaRepository<MedicationActive, String>
{
    @Nonnull
    Optional<MedicationActive> findBySystemId(@Nonnull String id);
}
