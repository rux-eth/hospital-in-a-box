package com.hospitalinabox.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ObservationSummary(
        UUID id,
        String code,
        String display,
        String value,
        String unit,
        OffsetDateTime effectiveTime) {
}