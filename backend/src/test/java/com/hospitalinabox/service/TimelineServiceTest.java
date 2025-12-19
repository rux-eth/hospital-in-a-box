package com.hospitalinabox.service;

import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.ObservationEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.ObservationRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import com.hospitalinabox.dto.TimelineEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {

    @Mock
    PatientRepository patientRepository;

    @Mock
    EncounterRepository encounterRepository;

    @Mock
    ObservationRepository observationRepository;

    @InjectMocks
    TimelineService timelineService;

    @Test
    void getTimelineForPatient_mergesAndSortsEvents() {
        UUID patientId = UUID.randomUUID();
        PatientEntity patient = new PatientEntity();
        patient.setId(patientId);

        when(patientRepository.findById(patientId))
                .thenReturn(java.util.Optional.of(patient));

        EncounterEntity enc = EncounterEntity.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .encounterIdentifier("VN12345")
                .encounterClass("INPATIENT")
                .status("FINISHED")
                .admitTime(OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .dischargeTime(OffsetDateTime.of(2025, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC))
                .build();

        ObservationEntity obs = ObservationEntity.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .encounter(enc)
                .code("GLUCOSE")
                .display("Glucose")
                .value("105")
                .unit("mg/dL")
                .effectiveTime(OffsetDateTime.of(2025, 1, 1, 12, 59, 0, 0, ZoneOffset.UTC))
                .build();

        when(encounterRepository.findByPatient(patient))
                .thenReturn(List.of(enc));
        when(observationRepository.findByPatient(patient))
                .thenReturn(List.of(obs));

        // Act
        List<TimelineEvent> events = timelineService.getTimelineForPatient(patientId);

        // Assert
        assertThat(events).hasSize(3);

        assertThat(events.get(0).type()).isEqualTo("ADMIT");
        assertThat(events.get(1).type()).isEqualTo("OBSERVATION");
        assertThat(events.get(2).type()).isEqualTo("DISCHARGE");

        assertThat(events.get(1).title()).contains("Glucose");
        assertThat(events.get(1).description()).contains("105");
    }
}