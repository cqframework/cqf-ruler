package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeProcedureRecommended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeProcedureRecommendedRepository extends JpaRepository<NegativeProcedureRecommended, String>
{
    @Nonnull
    Optional<NegativeProcedureRecommended> findBySystemId(@Nonnull String id);
}
