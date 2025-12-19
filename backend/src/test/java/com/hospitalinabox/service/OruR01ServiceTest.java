package com.hospitalinabox.service;

import com.hospitalinabox.domain.entity.ObservationEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.ObservationRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OruR01ServiceTest {

    private Hl7ParsingService hl7ParsingService;

    @Mock
    PatientRepository patientRepository;

    @Mock
    EncounterRepository encounterRepository;

    @Mock
    ObservationRepository observationRepository;

    @Mock
    FhirResourceService fhirResourceService;

    private OruR01Service oruR01Service;

    @Captor
    ArgumentCaptor<ObservationEntity> observationCaptor;

    @BeforeEach
    void setUp() {
        hl7ParsingService = new Hl7ParsingService();
        oruR01Service = new OruR01Service(
                hl7ParsingService,
                patientRepository,
                encounterRepository,
                observationRepository,
                fhirResourceService);
    }

    @Test
    void processOruR01_createsObservation() throws Exception {
        String hl7Oru = """
                MSH|^~\\&|LAB_APP|LAB_FAC|RECEIVING_APP|RECEIVING_FAC|202501011300||ORU^R01|MSGID2001|P|2.5
                PID|1||12345^^^HOSPITAL^MR||DOE^JOHN^^^^^L||19800101|M
                PV1|1|I|WARD^ROOM^BED|||||||||||||||VN12345
                OBR|1|||GLUCOSE^GLUCOSE TEST
                OBX|1|NM|GLUCOSE^Glucose||105|mg/dL|70-110|N|||F|||202501011259
                """;

        PatientEntity patient = new PatientEntity();
        patient.setId(UUID.randomUUID());
        patient.setMrn("12345");
        when(patientRepository.findByMrn("12345"))
                .thenReturn(Optional.of(patient));

        when(observationRepository.save(any(ObservationEntity.class)))
                .thenAnswer(invocation -> {
                    ObservationEntity o = invocation.getArgument(0);
                    o.setId(UUID.randomUUID());
                    return o;
                });

        // Act
        oruR01Service.processOruR01(hl7Oru);

        // Assert
        verify(observationRepository).save(observationCaptor.capture());
        ObservationEntity saved = observationCaptor.getValue();

        assertThat(saved.getPatient()).isSameAs(patient);
        assertThat(saved.getCode()).isEqualTo("GLUCOSE");
        assertThat(saved.getDisplay()).isEqualTo("Glucose");
        assertThat(saved.getValue()).isEqualTo("105");
        assertThat(saved.getUnit()).isEqualTo("mg/dL");
        assertThat(saved.getEffectiveTime()).isNotNull();

        verify(fhirResourceService).createObservationResource(saved);
        // We don't care whether it's linked to an Encounter in this unit test
    }
}