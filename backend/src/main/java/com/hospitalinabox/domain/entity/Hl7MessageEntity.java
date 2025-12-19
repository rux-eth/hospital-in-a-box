package com.hospitalinabox.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hl7_messages", indexes = {
        @Index(name = "idx_hl7_message_control_id", columnList = "message_control_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hl7MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // From MSH-10
    @Column(name = "message_control_id", length = 64)
    private String messageControlId;

    // e.g., ADT^A01, ADT^A03, ORU^R01
    @Column(name = "message_type", length = 32)
    private String messageType;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = OffsetDateTime.now();
        }
    }
}