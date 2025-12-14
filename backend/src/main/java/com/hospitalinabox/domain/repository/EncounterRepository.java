package com.hospitalinabox.domain.repository;

import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EncounterRepository extends JpaRepository<EncounterEntity, UUID> {

    List<EncounterEntity> findByPatient(PatientEntity patient);
}