package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveImmunizationAdministered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveImmunizationAdministeredRepository extends JpaRepository<PositiveImmunizationAdministered, String>
{
    @Nonnull
    Optional<PositiveImmunizationAdministered> findBySystemId(@Nonnull String id);
}
