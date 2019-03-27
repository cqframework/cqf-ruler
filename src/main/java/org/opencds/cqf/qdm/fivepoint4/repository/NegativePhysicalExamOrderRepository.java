package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativePhysicalExamOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativePhysicalExamOrderRepository extends JpaRepository<NegativePhysicalExamOrder, String>
{
    @Nonnull
    Optional<NegativePhysicalExamOrder> findBySystemId(@Nonnull String id);
}
