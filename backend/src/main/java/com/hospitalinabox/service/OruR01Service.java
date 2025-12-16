package com.hospitalinabox.service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;
import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.ObservationEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.ObservationRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OruR01Service {

    private final Hl7ParsingService hl7ParsingService;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;
    private final FhirResourceService fhirResourceService;

    public void processOruR01(String rawMessage) throws HL7Exception {
        Message message = hl7ParsingService.parseMessage(rawMessage);
        Terser terser = new Terser(message);

        // -----------------------------
        // Patient (PID inside groups)
        // ORU_R01 structure: PATIENT_RESULT / PATIENT / PID
        // -----------------------------
        String mrn = safeGet(terser, "/PATIENT_RESULT/PATIENT/PID-3-1");
        if (mrn == null || mrn.isBlank()) {
            // As a fallback, try direct PID (non-standard for ORU but ok for demo)
            mrn = safeGet(terser, "/PID-3-1");
        }

        if (mrn == null || mrn.isBlank()) {
            return;
        }

        Optional<PatientEntity> patientOpt = patientRepository.findByMrn(mrn);
        if (patientOpt.isEmpty()) {
            // In a real system we might create the patient; here we skip
            return;
        }
        PatientEntity patient = patientOpt.get();

        // -----------------------------
        // Encounter (PV1 inside groups)
        // ORU_R01: PATIENT_RESULT / PATIENT / VISIT / PV1
        // -----------------------------
        String visitNumber = safeGet(terser, "/PATIENT_RESULT/PATIENT/VISIT/PV1-19-1");
        if (visitNumber == null || visitNumber.isBlank()) {
            visitNumber = safeGet(terser, "/PATIENT_RESULT/PATIENT/VISIT/PV1-17-1");
        }
        if (visitNumber == null || visitNumber.isBlank()) {
            // Fallback – some ORU messages omit PV1; we can still store observation without
            // encounter
            visitNumber = null;
        }

        EncounterEntity encounter = null;
        if (visitNumber != null && !visitNumber.isBlank()) {
            encounter = encounterRepository
                    .findFirstByPatientAndEncounterIdentifierOrderByAdmitTimeDesc(patient, visitNumber)
                    .orElse(null);
        }

        // -----------------------------
        // Observation (OBX inside ORDER_OBSERVATION/OBSERVATION)
        // ORU_R01: PATIENT_RESULT / ORDER_OBSERVATION / OBSERVATION / OBX
        // -----------------------------
        String code = safeGet(terser, "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-3-1");
        String display = safeGet(terser, "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-3-2");

        String value = safeGet(terser, "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-5");
        String unit = safeGet(terser, "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-6-1");

        String effectiveDateTimeStr = safeGet(terser, "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-14");
        if (effectiveDateTimeStr == null || effectiveDateTimeStr.isBlank()) {
            effectiveDateTimeStr = safeGet(terser, "/MSH-7");
        }

        if (code == null && value == null) {
            // Nothing meaningful to store
            return;
        }

        ObservationEntity observation = ObservationEntity.builder()
                .patient(patient)
                .encounter(encounter)
                .code(code)
                .display(display)
                .value(value)
                .unit(unit)
                .effectiveTime(parseHl7DateTime(effectiveDateTimeStr))
                .build();

        observation = observationRepository.save(observation);

        // Create FHIR Observation
        fhirResourceService.createObservationResource(observation);
    }

    /**
     * Helper to safely call Terser.get and treat HL7Exception as null.
     */
    private String safeGet(Terser terser, String path) {
        try {
            return terser.get(path);
        } catch (HL7Exception e) {
            return null;
        }
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