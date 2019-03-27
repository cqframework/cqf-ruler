package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositivePhysicalExamPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositivePhysicalExamPerformedRepository extends JpaRepository<PositivePhysicalExamPerformed, String>
{
    @Nonnull
    Optional<PositivePhysicalExamPerformed> findBySystemId(@Nonnull String id);
}
