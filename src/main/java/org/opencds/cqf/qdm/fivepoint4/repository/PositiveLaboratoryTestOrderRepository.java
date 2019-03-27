package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveLaboratoryTestOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveLaboratoryTestOrderRepository extends JpaRepository<PositiveLaboratoryTestOrder, String>
{
    @Nonnull
    Optional<PositiveLaboratoryTestOrder> findBySystemId(@Nonnull String id);
}
