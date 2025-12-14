package com.hospitalinabox.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PatientSummary(
        UUID id,
        String mrn,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender) {
}