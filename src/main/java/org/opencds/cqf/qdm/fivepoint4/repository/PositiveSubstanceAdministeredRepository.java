package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveSubstanceAdministered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveSubstanceAdministeredRepository extends JpaRepository<PositiveSubstanceAdministered, String>
{
    @Nonnull
    Optional<PositiveSubstanceAdministered> findBySystemId(@Nonnull String id);
}
