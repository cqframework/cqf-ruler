package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.AllergyIntolerance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface AllergyIntoleranceRepository extends JpaRepository<AllergyIntolerance, String>
{
    @Nonnull
    Optional<AllergyIntolerance> findBySystemId(@Nonnull String id);
}
