package com.hospitalinabox.dto;

import java.util.UUID;

// Java 21 record = concise immutable DTO
public record Hl7IngestResponse(
        UUID hl7MessageId,
        UUID auditLogId,
        String status
) {}