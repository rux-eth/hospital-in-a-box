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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdtA01ServiceTest {

    private Hl7ParsingService hl7ParsingService;

    @Mock
    PatientRepository patientRepository;

    @Mock
    EncounterRepository encounterRepository;

    @Mock
    FhirResourceService fhirResourceService;

    private AdtA01Service adtA01Service;

    @Captor
    ArgumentCaptor<PatientEntity> patientCaptor;

    @Captor
    ArgumentCaptor<EncounterEntity> encounterCaptor;

    private String hl7AdtA01;

    @BeforeEach
    void setUp() {
        hl7ParsingService = new Hl7ParsingService();
        adtA01Service = new AdtA01Service(
                hl7ParsingService,
                patientRepository,
                encounterRepository,
                fhirResourceService);

        hl7AdtA01 = """
                MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|202501011200||ADT^A01|MSGID1234|P|2.5
                PID|1||12345^^^HOSPITAL^MR||DOE^JOHN^^^^^L||19800101|M
                PV1|1|I|WARD^ROOM^BED|||||||||||||||VN12345|||||||||||||||||||||||||202501011215
                """;
    }

    @Test
    void processAdtA01_createsPatientAndEncounter() throws Exception {
        when(patientRepository.findByMrn("12345"))
                .thenReturn(Optional.empty());

        when(patientRepository.save(any(PatientEntity.class)))
                .thenAnswer(invocation -> {
                    PatientEntity p = invocation.getArgument(0);
                    p.setId(UUID.randomUUID());
                    if (p.getCreatedAt() == null) {
                        p.setCreatedAt(java.time.OffsetDateTime.now());
                    }
                    return p;
                });

        when(encounterRepository.save(any(EncounterEntity.class)))
                .thenAnswer(invocation -> {
                    EncounterEntity e = invocation.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        // Act
        adtA01Service.processAdtA01(hl7AdtA01);

        // Assert patient
        verify(patientRepository).save(patientCaptor.capture());
        PatientEntity savedPatient = patientCaptor.getValue();
        assertThat(savedPatient.getMrn()).isEqualTo("12345");
        assertThat(savedPatient.getFirstName()).isEqualTo("JOHN");
        assertThat(savedPatient.getLastName()).isEqualTo("DOE");
        assertThat(savedPatient.getBirthDate()).isEqualTo(LocalDate.of(1980, 1, 1));

        // Assert encounter (don’t over-assert visit number; just core fields)
        verify(encounterRepository).save(encounterCaptor.capture());
        EncounterEntity savedEncounter = encounterCaptor.getValue();
        assertThat(savedEncounter.getStatus()).isEqualTo("INPROGRESS");
        assertThat(savedEncounter.getEncounterClass()).isEqualTo("INPATIENT");
        assertThat(savedEncounter.getAdmitTime()).isNotNull();

        verify(fhirResourceService).createOrUpdatePatientResource(any(PatientEntity.class));
        verify(fhirResourceService).createEncounterResource(any(EncounterEntity.class));
    }
}