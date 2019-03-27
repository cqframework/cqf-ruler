package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeLaboratoryTestPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeLaboratoryTestPerformedRepository extends JpaRepository<NegativeLaboratoryTestPerformed, String>
{
    @Nonnull
    Optional<NegativeLaboratoryTestPerformed> findBySystemId(@Nonnull String id);
}
