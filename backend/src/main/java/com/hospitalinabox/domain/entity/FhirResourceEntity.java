package com.hospitalinabox.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fhir_resources",
       indexes = {
           @Index(name = "idx_fhir_resource_type_id", columnList = "resource_type, resource_id"),
           @Index(name = "idx_fhir_patient", columnList = "patient_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FhirResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType; // Patient, Encounter, Observation

    @Column(name = "resource_id", nullable = false, length = 64)
    private String resourceId; // FHIR logical id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @Column(name = "event_time")
    private OffsetDateTime eventTime;

    @Lob
    @Column(name = "body", nullable = false)
    private String body; // FHIR JSON

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}