package com.hospitalinabox.service;

import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdtA03ServiceTest {

    private Hl7ParsingService hl7ParsingService;

    @Mock
    PatientRepository patientRepository;

    @Mock
    EncounterRepository encounterRepository;

    @Mock
    FhirResourceService fhirResourceService;

    private AdtA03Service adtA03Service;

    @BeforeEach
    void setUp() {
        hl7ParsingService = new Hl7ParsingService();
        adtA03Service = new AdtA03Service(
                hl7ParsingService,
                patientRepository,
                encounterRepository,
                fhirResourceService);
    }

    @Test
    void processAdtA03_setsEncounterFinished() throws Exception {
        String hl7A03 = """
                MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|202501021000||ADT^A03|MSGID1235|P|2.5
                PID|1||12345^^^HOSPITAL^MR||DOE^JOHN^^^^^L||19800101|M
                PV1|1|I|WARD^ROOM^BED|||||||||||||||VN12345|||||||||||||||||||||||||202501021030
                """;

        PatientEntity patient = new PatientEntity();
        patient.setId(UUID.randomUUID());
        patient.setMrn("12345");

        when(patientRepository.findByMrn("12345"))
                .thenReturn(Optional.of(patient));

        EncounterEntity encounter = EncounterEntity.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .encounterIdentifier("VN12345")
                .status("INPROGRESS")
                .admitTime(OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .build();

        when(encounterRepository.findFirstByPatientAndEncounterIdentifierOrderByAdmitTimeDesc(
                any(), any()))
                .thenReturn(Optional.of(encounter));

        // Act
        adtA03Service.processAdtA03(hl7A03);

        // Assert
        assertThat(encounter.getStatus()).isEqualTo("FINISHED");
        assertThat(encounter.getDischargeTime()).isNotNull();
        verify(encounterRepository).save(encounter);
        verify(fhirResourceService).createEncounterResource(encounter);
    }
}