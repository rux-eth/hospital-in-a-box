package com.hospitalinabox.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "encounters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "encounter_identifier", length = 64)
    private String encounterIdentifier;

    @Column(name = "status", length = 32)
    private String status; // INPROGRESS, FINISHED

    @Column(name = "class", length = 32)
    private String encounterClass; // INPATIENT, OUTPATIENT, EMERGENCY, etc.

    @Column(name = "admit_time")
    private OffsetDateTime admitTime;

    @Column(name = "discharge_time")
    private OffsetDateTime dischargeTime;

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}