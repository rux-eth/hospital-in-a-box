package com.hospitalinabox.service;

import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.ObservationEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.ObservationRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import com.hospitalinabox.dto.TimelineEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;

    public List<TimelineEvent> getTimelineForPatient(UUID patientId) {
        PatientEntity patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        List<EncounterEntity> encounters = encounterRepository.findByPatient(patient);
        List<ObservationEntity> observations = observationRepository.findByPatient(patient);

        List<TimelineEvent> events = new ArrayList<>();

        // Encounters → ADMIT / DISCHARGE events
        for (EncounterEntity enc : encounters) {
            if (enc.getAdmitTime() != null) {
                events.add(new TimelineEvent(
                        enc.getAdmitTime(),
                        "ADMIT",
                        "Admitted (" + nullSafe(enc.getEncounterClass()) + ")",
                        "Visit: " + nullSafe(enc.getEncounterIdentifier()),
                        enc.getId(),
                        null));
            }

            if (enc.getDischargeTime() != null) {
                events.add(new TimelineEvent(
                        enc.getDischargeTime(),
                        "DISCHARGE",
                        "Discharged",
                        "Visit: " + nullSafe(enc.getEncounterIdentifier()),
                        enc.getId(),
                        null));
            }
        }

        // Observations → OBSERVATION events
        for (ObservationEntity obs : observations) {
            OffsetDateTime ts = obs.getEffectiveTime();
            if (ts == null) {
                // If no timestamp, skip from timeline
                continue;
            }

            String title = "Observation: " + nullSafe(obs.getDisplay(), obs.getCode());
            String desc = "Value: " + nullSafe(obs.getValue()) + " " + nullSafe(obs.getUnit());

            UUID encounterId = obs.getEncounter() != null ? obs.getEncounter().getId() : null;

            events.add(new TimelineEvent(
                    ts,
                    "OBSERVATION",
                    title,
                    desc,
                    encounterId,
                    obs.getId()));
        }

        // Sort by timestamp ascending
        events.sort(Comparator.comparing(TimelineEvent::timestamp));

        return events;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String nullSafe(String primary, String fallback) {
        if (primary != null && !primary.isBlank())
            return primary;
        return fallback == null ? "" : fallback;
    }
}