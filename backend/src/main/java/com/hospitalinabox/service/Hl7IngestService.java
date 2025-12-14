package com.hospitalinabox.service;

import com.hospitalinabox.domain.entity.AuditLogEntity;
import com.hospitalinabox.domain.entity.Hl7MessageEntity;
import com.hospitalinabox.domain.repository.AuditLogRepository;
import com.hospitalinabox.domain.repository.Hl7MessageRepository;
import com.hospitalinabox.dto.Hl7IngestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class Hl7IngestService {

    private final Hl7MessageRepository hl7MessageRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public Hl7IngestResponse ingestRawMessage(String rawMessage) {
        // For now we don't parse HL7 – just store it.
        Hl7MessageEntity message = Hl7MessageEntity.builder()
                .messageControlId(null) // will be filled when we parse MSH-10
                .messageType("UNKNOWN") // will be filled when we parse MSH-9
                .payload(rawMessage)
                .receivedAt(OffsetDateTime.now())
                .build();

        message = hl7MessageRepository.save(message);

        AuditLogEntity audit = AuditLogEntity.builder()
                .hl7Message(message)
                .status("RECEIVED")
                .errorMessage(null)
                .details(null)
                .createdAt(OffsetDateTime.now())
                .build();

        audit = auditLogRepository.save(audit);

        return new Hl7IngestResponse(message.getId(), audit.getId(), audit.getStatus());
    }
}