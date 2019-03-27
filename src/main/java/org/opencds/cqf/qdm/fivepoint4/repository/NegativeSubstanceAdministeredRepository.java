package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeSubstanceAdministered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeSubstanceAdministeredRepository extends JpaRepository<NegativeSubstanceAdministered, String>
{
    @Nonnull
    Optional<NegativeSubstanceAdministered> findBySystemId(@Nonnull String id);
}
