package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveAssessmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveAssessmentOrderRepository extends JpaRepository<PositiveAssessmentOrder, String>
{
    @Nonnull
    Optional<PositiveAssessmentOrder> findBySystemId(@Nonnull String id);
}
