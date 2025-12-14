package com.hospitalinabox.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Hl7MessageView(
                UUID id,
                String messageType,
                String messageControlId,
                String payload,
                OffsetDateTime receivedAt) {
}