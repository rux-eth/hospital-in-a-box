package com.hospitalinabox.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TimelineEvent(
        OffsetDateTime timestamp,
        String type, // ADMIT, OBSERVATION, DISCHARGE
        String title, // Short label
        String description, // More details
        UUID encounterId, // Optional reference
        UUID observationId // Optional reference
) {
}