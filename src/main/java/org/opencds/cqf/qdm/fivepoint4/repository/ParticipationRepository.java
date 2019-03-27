package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, String>
{
    @Nonnull
    Optional<Participation> findBySystemId(@Nonnull String id);
}
