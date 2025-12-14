package com.hospitalinabox.domain.repository;

import com.hospitalinabox.domain.entity.FhirResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FhirResourceRepository extends JpaRepository<FhirResourceEntity, UUID> {

    Optional<FhirResourceEntity> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    List<FhirResourceEntity> findByPatientId(UUID patientId);
}