package com.hospitalinabox.service;

import com.hospitalinabox.domain.entity.AuditLogEntity;
import com.hospitalinabox.domain.entity.Hl7MessageEntity;
import com.hospitalinabox.domain.repository.AuditLogRepository;
import com.hospitalinabox.domain.repository.Hl7MessageRepository;
import com.hospitalinabox.dto.Hl7IngestResponse;
import com.hospitalinabox.dto.Hl7Metadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class Hl7IngestService {

    private final Hl7MessageRepository hl7MessageRepository;
    private final AuditLogRepository auditLogRepository;
    private final Hl7ParsingService hl7ParsingService;
    private final AdtA01Service adtA01Service;
    private final AdtA03Service adtA03Service;
    private final OruR01Service oruR01Service;

    @Transactional
    public Hl7IngestResponse ingestRawMessage(String rawMessage) {
        Hl7MessageEntity message = Hl7MessageEntity.builder()
                .payload(rawMessage)
                .receivedAt(OffsetDateTime.now())
                .build();

        String status;
        String details = null;
        String error = null;

        try {
            Hl7Metadata metadata = hl7ParsingService.extractMetadata(rawMessage);
            message.setMessageType(metadata.messageType());
            message.setMessageControlId(metadata.messageControlId());
            status = "PARSED";
            details = "messageType=" + metadata.messageType()
                    + ", messageControlId=" + metadata.messageControlId();

            try {
                if ("ADT^A01".equals(metadata.messageType())) {
                    adtA01Service.processAdtA01(rawMessage);
                    details += ", processed=ADT^A01";
                } else if ("ADT^A03".equals(metadata.messageType())) {
                    adtA03Service.processAdtA03(rawMessage);
                    details += ", processed=ADT^A03";
                } else if ("ORU^R01".equals(metadata.messageType())) {
                    oruR01Service.processOruR01(rawMessage);
                    details += ", processed=ORU^R01";
                }
            } catch (Exception e) {
                // Optional: log stacktrace for debugging
                e.printStackTrace();
                status = "PROCESS_FAILED";
                error = e.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            message.setMessageType("UNKNOWN");
            message.setMessageControlId(null);
            status = "PARSE_FAILED";
            error = e.getMessage();
        }

        message = hl7MessageRepository.save(message);

        AuditLogEntity audit = AuditLogEntity.builder()
                .hl7Message(message)
                .status(status)
                .errorMessage(error)
                .details(details)
                .createdAt(OffsetDateTime.now())
                .build();

        audit = auditLogRepository.save(audit);

        return new Hl7IngestResponse(message.getId(), audit.getId(), audit.getStatus());
    }
}