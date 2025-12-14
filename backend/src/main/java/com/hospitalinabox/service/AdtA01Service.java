package com.hospitalinabox.service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;
import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdtA01Service {

    private final Hl7ParsingService hl7ParsingService;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;

    @Transactional
    public void processAdtA01(String rawMessage) throws HL7Exception {
        Message message = hl7ParsingService.parseMessage(rawMessage);
        Terser terser = new Terser(message);

        // ---- Patient fields (PID) ----
        String mrn = terser.get("/PID-3-1"); // CX.1
        String lastName = terser.get("/PID-5-1"); // XPN.1
        String firstName = terser.get("/PID-5-2"); // XPN.2
        String birthDateStr = terser.get("/PID-7"); // YYYYMMDD
        String genderCode = terser.get("/PID-8"); // M/F/…

        if (mrn == null || mrn.isBlank()) {
            // In real system we'd NACK this; here we just return
            return;
        }

        // Upsert patient by MRN
        Optional<PatientEntity> existingOpt = patientRepository.findByMrn(mrn);
        PatientEntity patient = existingOpt.orElseGet(PatientEntity::new);
        patient.setMrn(mrn);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setBirthDate(parseHl7Date(birthDateStr));
        patient.setGender(mapGender(genderCode));

        patient = patientRepository.save(patient);

        // ---- Encounter fields (PV1) ----
        String patientClassCode = terser.get("/PV1-2"); // I/O/E etc.
        String visitNumber = terser.get("/PV1-19-1"); // CX.1
        String admitDateTimeStr = terser.get("/PV1-44"); // YYYYMMDD[HHMM[SS]]

        EncounterEntity encounter = EncounterEntity.builder()
                .patient(patient)
                .encounterIdentifier(visitNumber)
                .status("INPROGRESS")
                .encounterClass(mapPatientClass(patientClassCode))
                .admitTime(parseHl7DateTime(admitDateTimeStr))
                .dischargeTime(null)
                .reason(null)
                .build();

        encounterRepository.save(encounter);
    }

    private String mapGender(String hl7Gender) {
        if (hl7Gender == null)
            return null;
        return switch (hl7Gender.toUpperCase()) {
            case "M" -> "male";
            case "F" -> "female";
            default -> "unknown";
        };
    }

    private String mapPatientClass(String hl7Class) {
        if (hl7Class == null)
            return "UNKNOWN";
        return switch (hl7Class.toUpperCase()) {
            case "I" -> "INPATIENT";
            case "O" -> "OUTPATIENT";
            case "E" -> "EMERGENCY";
            default -> "UNKNOWN";
        };
    }

    private java.time.LocalDate parseHl7Date(String value) {
        if (value == null || value.isBlank())
            return null;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.parse(value.substring(0, 8), fmt);
    }

    private java.time.OffsetDateTime parseHl7DateTime(String value) {
        if (value == null || value.isBlank())
            return null;
        String datePart = value.substring(0, Math.min(8, value.length()));
        LocalDate date = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (value.length() <= 8) {
            return date.atStartOfDay().atOffset(ZoneOffset.UTC);
        }

        // Expect at least HHmm
        String timePart = value.substring(8, Math.min(12, value.length())); // up to HHmm
        LocalTime time = LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HHmm"));

        return OffsetDateTime.of(date, time, ZoneOffset.UTC);
    }
}