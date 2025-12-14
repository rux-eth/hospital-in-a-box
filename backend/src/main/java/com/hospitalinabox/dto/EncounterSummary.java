package com.hospitalinabox.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EncounterSummary(
        UUID id,
        String encounterIdentifier,
        String status,
        String encounterClass,
        OffsetDateTime admitTime,
        OffsetDateTime dischargeTime,
        String reason) {
}