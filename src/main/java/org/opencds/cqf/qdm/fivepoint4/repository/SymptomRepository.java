package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.Symptom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface SymptomRepository extends JpaRepository<Symptom, String>
{
    @Nonnull
    Optional<Symptom> findBySystemId(@Nonnull String id);
}
