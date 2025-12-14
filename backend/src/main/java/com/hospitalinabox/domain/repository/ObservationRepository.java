package com.hospitalinabox.domain.repository;

import com.hospitalinabox.domain.entity.ObservationEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObservationRepository extends JpaRepository<ObservationEntity, UUID> {

    List<ObservationEntity> findByPatient(PatientEntity patient);
}