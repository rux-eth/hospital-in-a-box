package com.hospitalinabox.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_hl7_message", columnList = "hl7_message_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hl7_message_id", nullable = false)
    private Hl7MessageEntity hl7Message;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // e.g. RECEIVED, SUCCESS, FAILED

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Lob
    @Column(name = "details")
    private String details; // optional JSON/string diagnostics

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}