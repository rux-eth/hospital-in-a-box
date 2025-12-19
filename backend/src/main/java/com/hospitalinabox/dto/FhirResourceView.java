package com.hospitalinabox.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FhirResourceView(
        UUID id,
        String resourceType,
        String resourceId,
        OffsetDateTime eventTime,
        String body) {
}