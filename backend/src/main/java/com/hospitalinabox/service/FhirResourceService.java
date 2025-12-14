package com.hospitalinabox.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.FhirResourceEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.FhirResourceRepository;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FhirResourceService {

    private final FhirContext fhirContext;
    private final FhirResourceRepository fhirResourceRepository;

    private IParser jsonParser() {
        return fhirContext.newJsonParser().setPrettyPrint(false);
    }

    @Transactional
    public void createOrUpdatePatientResource(PatientEntity patient) {
        String fhirId = patient.getId().toString();

        Patient fhirPatient = new Patient();
        fhirPatient.setId(fhirId);

        // identifier: MRN
        Identifier id = new Identifier();
        id.setSystem("http://hospital-in-a-box.local/mrn");
        id.setValue(patient.getMrn());
        fhirPatient.addIdentifier(id);

        // name
        fhirPatient.addName()
                .setFamily(patient.getLastName())
                .addGiven(patient.getFirstName());

        // birthDate
        if (patient.getBirthDate() != null) {
            fhirPatient.setBirthDate(java.sql.Date.valueOf(patient.getBirthDate()));
        }

        // gender
        if (patient.getGender() != null) {
            switch (patient.getGender().toLowerCase()) {
                case "male" -> fhirPatient.setGender(AdministrativeGender.MALE);
                case "female" -> fhirPatient.setGender(AdministrativeGender.FEMALE);
                default -> fhirPatient.setGender(AdministrativeGender.UNKNOWN);
            }
        }

        String json = jsonParser().encodeResourceToString(fhirPatient);

        // Use "now" as event time; don’t depend on createdAt being non-null
        OffsetDateTime eventTime = OffsetDateTime.now();

        FhirResourceEntity entity = FhirResourceEntity.builder()
                .resourceType("Patient")
                .resourceId(fhirId)
                .patient(null)
                .eventTime(eventTime)
                .body(json)
                .build();

        fhirResourceRepository.findByResourceTypeAndResourceId("Patient", fhirId)
                .ifPresentOrElse(existing -> {
                    existing.setBody(json);
                    existing.setEventTime(eventTime);
                    fhirResourceRepository.save(existing);
                }, () -> fhirResourceRepository.save(entity));
    }

    @Transactional
    public void createEncounterResource(EncounterEntity encounter) {
        String fhirId = encounter.getId().toString();

        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId(fhirId);

        // status
        if (encounter.getStatus() != null) {
            switch (encounter.getStatus().toUpperCase()) {
                case "INPROGRESS" -> fhirEncounter.setStatus(Encounter.EncounterStatus.INPROGRESS);
                case "FINISHED" -> fhirEncounter.setStatus(Encounter.EncounterStatus.FINISHED);
                default -> fhirEncounter.setStatus(Encounter.EncounterStatus.UNKNOWN);
            }
        }

        // class
        if (encounter.getEncounterClass() != null) {
            String code = switch (encounter.getEncounterClass().toUpperCase()) {
                case "INPATIENT" -> "IMP";
                case "OUTPATIENT" -> "AMB";
                case "EMERGENCY" -> "EMER";
                default -> "UNKNOWN";
            };
            fhirEncounter.setClass_(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                    .setCode(code));
        }

        // subject reference to Patient
        if (encounter.getPatient() != null) {
            String patientFhirId = encounter.getPatient().getId().toString();
            fhirEncounter.setSubject(new Reference("Patient/" + patientFhirId));
        }

        // period
        Period period = new Period();
        if (encounter.getAdmitTime() != null) {
            period.setStart(java.util.Date.from(encounter.getAdmitTime().toInstant()));
        }
        if (encounter.getDischargeTime() != null) {
            period.setEnd(java.util.Date.from(encounter.getDischargeTime().toInstant()));
        }
        fhirEncounter.setPeriod(period);

        String json = jsonParser().encodeResourceToString(fhirEncounter);

        // Prefer admitTime as eventTime; fall back to now
        OffsetDateTime eventTime = encounter.getAdmitTime() != null
                ? encounter.getAdmitTime()
                : OffsetDateTime.now();

        FhirResourceEntity entity = FhirResourceEntity.builder()
                .resourceType("Encounter")
                .resourceId(fhirId)
                .patient(encounter.getPatient())
                .eventTime(eventTime)
                .body(json)
                .build();

        fhirResourceRepository.findByResourceTypeAndResourceId("Encounter", fhirId)
                .ifPresentOrElse(existing -> {
                    existing.setBody(json);
                    existing.setEventTime(eventTime);
                    existing.setPatient(encounter.getPatient());
                    fhirResourceRepository.save(existing);
                }, () -> fhirResourceRepository.save(entity));
    }
}