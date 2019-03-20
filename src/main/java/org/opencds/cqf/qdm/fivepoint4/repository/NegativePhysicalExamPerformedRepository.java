package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativePhysicalExamPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativePhysicalExamPerformedRepository extends JpaRepository<NegativePhysicalExamPerformed, String>
{
    @Nonnull
    Optional<NegativePhysicalExamPerformed> findBySystemId(@Nonnull String id);
}
