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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdtA03Service {

    private final Hl7ParsingService hl7ParsingService;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final FhirResourceService fhirResourceService;

    public void processAdtA03(String rawMessage) throws HL7Exception {
        Message message = hl7ParsingService.parseMessage(rawMessage);
        Terser terser = new Terser(message);

        // Patient MRN from PID-3
        String mrn = terser.get("/PID-3-1");
        if (mrn == null || mrn.isBlank()) {
            return;
        }

        Optional<PatientEntity> patientOpt = patientRepository.findByMrn(mrn);
        if (patientOpt.isEmpty()) {
            // We don't know this patient; nothing to update
            return;
        }
        PatientEntity patient = patientOpt.get();

        // Visit number: PV1-19, fallback to PV1-17 (our sample)
        String visitNumber = terser.get("/PV1-19-1");
        if (visitNumber == null || visitNumber.isBlank()) {
            visitNumber = terser.get("/PV1-17-1");
        }

        // Discharge datetime: PV1-45, fallback to PV1-39, then MSH-7
        String dischargeDateTimeStr = terser.get("/PV1-45");
        if (dischargeDateTimeStr == null || dischargeDateTimeStr.isBlank()) {
            dischargeDateTimeStr = terser.get("/PV1-39");
        }
        if (dischargeDateTimeStr == null || dischargeDateTimeStr.isBlank()) {
            dischargeDateTimeStr = terser.get("/MSH-7");
        }

        Optional<EncounterEntity> encounterOpt = encounterRepository
                .findFirstByPatientAndEncounterIdentifierOrderByAdmitTimeDesc(
                        patient, visitNumber);

        if (encounterOpt.isEmpty()) {
            // No matching encounter – could log this in audit details later
            return;
        }

        EncounterEntity encounter = encounterOpt.get();
        encounter.setStatus("FINISHED");
        encounter.setDischargeTime(parseHl7DateTime(dischargeDateTimeStr));

        encounterRepository.save(encounter);

        // Update FHIR Encounter
        fhirResourceService.createEncounterResource(encounter);
    }

    private OffsetDateTime parseHl7DateTime(String value) {
        if (value == null || value.isBlank())
            return null;
        String datePart = value.substring(0, Math.min(8, value.length()));
        LocalDate date = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (value.length() <= 8) {
            return date.atStartOfDay().atOffset(ZoneOffset.UTC);
        }

        String timePart = value.substring(8, Math.min(12, value.length())); // HHmm
        LocalTime time = LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HHmm"));

        return OffsetDateTime.of(date, time, ZoneOffset.UTC);
    }
}