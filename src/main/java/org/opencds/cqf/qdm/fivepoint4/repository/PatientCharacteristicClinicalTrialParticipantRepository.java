package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PatientCharacteristicClinicalTrialParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PatientCharacteristicClinicalTrialParticipantRepository extends JpaRepository<PatientCharacteristicClinicalTrialParticipant, String>
{
    @Nonnull
    Optional<PatientCharacteristicClinicalTrialParticipant> findBySystemId(@Nonnull String id);
}
