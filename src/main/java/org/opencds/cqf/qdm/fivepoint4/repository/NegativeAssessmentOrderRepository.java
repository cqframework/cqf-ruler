package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeAssessmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeAssessmentOrderRepository extends JpaRepository<NegativeAssessmentOrder, String>
{
    @Nonnull
    Optional<NegativeAssessmentOrder> findBySystemId(@Nonnull String id);
}
