package com.hospitalinabox.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.FhirResourceEntity;
import com.hospitalinabox.domain.entity.ObservationEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.FhirResourceRepository;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FhirResourceService {

    private final FhirResourceRepository fhirResourceRepository;

    // HAPI FHIR context – heavy, so keep one per app
    private static final FhirContext FHIR_CTX = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CTX.newJsonParser().setPrettyPrint(false);

    // -------------------------
    // Patient
    // -------------------------

    public void createOrUpdatePatientResource(PatientEntity patientEntity) {
        String resourceType = "Patient";
        String resourceId = patientEntity.getId().toString();

        // Build FHIR Patient
        Patient patient = new Patient();
        patient.setId(resourceId);

        // Identifier: MRN
        if (patientEntity.getMrn() != null) {
            patient.addIdentifier()
                    .setSystem("http://hospital-in-a-box.example/mrn")
                    .setValue(patientEntity.getMrn());
        }

        // Name
        HumanName name = new HumanName();
        name.setFamily(patientEntity.getLastName());
        if (patientEntity.getFirstName() != null) {
            name.addGiven(patientEntity.getFirstName());
        }
        patient.addName(name);

        // Gender
        if (patientEntity.getGender() != null) {
            switch (patientEntity.getGender().toLowerCase()) {
                case "male" -> patient.setGender(Enumerations.AdministrativeGender.MALE);
                case "female" -> patient.setGender(Enumerations.AdministrativeGender.FEMALE);
                default -> patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
            }
        }

        // BirthDate
        if (patientEntity.getBirthDate() != null) {
            patient.setBirthDate(java.sql.Date.valueOf(patientEntity.getBirthDate()));
        }

        // Serialize
        String json = JSON_PARSER.encodeResourceToString(patient);

        // eventTime: use patient createdAt
        OffsetDateTime eventTime = patientEntity.getCreatedAt();

        upsertFhirResource(resourceType, resourceId, patientEntity.getId(), eventTime, json);
    }

    // -------------------------
    // Encounter
    // -------------------------

    public void createEncounterResource(EncounterEntity encounterEntity) {
        String resourceType = "Encounter";
        String resourceId = encounterEntity.getId().toString();

        Encounter enc = new Encounter();
        enc.setId(resourceId);

        // Subject reference to Patient
        PatientEntity patient = encounterEntity.getPatient();
        if (patient != null && patient.getId() != null) {
            enc.setSubject(new Reference("Patient/" + patient.getId()));
        }

        // Status
        if (encounterEntity.getStatus() != null) {
            switch (encounterEntity.getStatus().toUpperCase()) {
                case "INPROGRESS" -> enc.setStatus(Encounter.EncounterStatus.INPROGRESS);
                case "FINISHED" -> enc.setStatus(Encounter.EncounterStatus.FINISHED);
                default -> enc.setStatus(Encounter.EncounterStatus.UNKNOWN);
            }
        }

        // Class (inpatient/outpatient/emergency)
        if (encounterEntity.getEncounterClass() != null) {
            Coding cls = new Coding();
            cls.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
            switch (encounterEntity.getEncounterClass().toUpperCase()) {
                case "INPATIENT" -> {
                    cls.setCode("IMP");
                    cls.setDisplay("inpatient encounter");
                }
                case "OUTPATIENT" -> {
                    cls.setCode("AMB");
                    cls.setDisplay("ambulatory");
                }
                case "EMERGENCY" -> {
                    cls.setCode("EMER");
                    cls.setDisplay("emergency");
                }
                default -> {
                    cls.setCode("UNK");
                    cls.setDisplay("unknown");
                }
            }
            enc.setClass_(cls);
        }

        // Period: admitTime / dischargeTime
        Period period = new Period();
        if (encounterEntity.getAdmitTime() != null) {
            period.setStart(java.util.Date.from(encounterEntity.getAdmitTime().toInstant()));
        }
        if (encounterEntity.getDischargeTime() != null) {
            period.setEnd(java.util.Date.from(encounterEntity.getDischargeTime().toInstant()));
        }
        enc.setPeriod(period);

        // Identifier (visit number)
        if (encounterEntity.getEncounterIdentifier() != null) {
            enc.addIdentifier()
                    .setSystem("http://hospital-in-a-box.example/visit-number")
                    .setValue(encounterEntity.getEncounterIdentifier());
        }

        String json = JSON_PARSER.encodeResourceToString(enc);

        // eventTime: prefer admitTime, else dischargeTime, else now
        OffsetDateTime eventTime = encounterEntity.getAdmitTime();
        if (eventTime == null) {
            eventTime = encounterEntity.getDischargeTime();
        }
        if (eventTime == null) {
            eventTime = OffsetDateTime.now(ZoneOffset.UTC);
        }

        UUID patientId = patient != null ? patient.getId() : null;
        upsertFhirResource(resourceType, resourceId, patientId, eventTime, json);
    }

    // -------------------------
    // Observation
    // -------------------------

    public void createObservationResource(ObservationEntity observationEntity) {
        String resourceType = "Observation";
        String resourceId = observationEntity.getId().toString();

        Observation obs = new Observation();
        obs.setId(resourceId);

        // Subject (patient)
        PatientEntity patient = observationEntity.getPatient();
        if (patient != null && patient.getId() != null) {
            obs.setSubject(new Reference("Patient/" + patient.getId()));
        }

        // Encounter
        EncounterEntity encounter = observationEntity.getEncounter();
        if (encounter != null && encounter.getId() != null) {
            obs.setEncounter(new Reference("Encounter/" + encounter.getId()));
        }

        // Code
        CodeableConcept code = new CodeableConcept();
        if (observationEntity.getCode() != null || observationEntity.getDisplay() != null) {
            Coding coding = new Coding();
            coding.setSystem("http://hospital-in-a-box.example/lab-codes");
            coding.setCode(observationEntity.getCode());
            coding.setDisplay(observationEntity.getDisplay());
            code.addCoding(coding);
            obs.setCode(code);
        }

        // Value
        String value = observationEntity.getValue();
        if (value != null) {
            // Try numeric quantity, else string
            try {
                double numeric = Double.parseDouble(value);
                Quantity q = new Quantity();
                q.setValue(numeric);
                if (observationEntity.getUnit() != null) {
                    q.setUnit(observationEntity.getUnit());
                }
                obs.setValue(q);
            } catch (NumberFormatException nfe) {
                obs.setValue(new StringType(value));
            }
        }

        // Effective time
        if (observationEntity.getEffectiveTime() != null) {
            obs.setEffective(new DateTimeType(
                    java.util.Date.from(observationEntity.getEffectiveTime().toInstant())));
        }

        String json = JSON_PARSER.encodeResourceToString(obs);

        // eventTime: use effectiveTime, else now
        OffsetDateTime eventTime = observationEntity.getEffectiveTime();
        if (eventTime == null) {
            eventTime = OffsetDateTime.now(ZoneOffset.UTC);
        }

        UUID patientId = patient != null ? patient.getId() : null;
        upsertFhirResource(resourceType, resourceId, patientId, eventTime, json);
    }

    // -------------------------
    // Helper: upsert FHIR resource row
    // -------------------------

    private void upsertFhirResource(
            String resourceType,
            String resourceId,
            UUID patientId,
            OffsetDateTime eventTime,
            String jsonBody) {
        Optional<FhirResourceEntity> existingOpt = fhirResourceRepository.findByResourceTypeAndResourceId(resourceType,
                resourceId);

        FhirResourceEntity entity = existingOpt.orElseGet(FhirResourceEntity::new);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setEventTime(eventTime);
        entity.setBody(jsonBody);

        if (patientId != null) {
            PatientEntity stubPatient = new PatientEntity();
            stubPatient.setId(patientId);
            entity.setPatient(stubPatient);
        } else {
            entity.setPatient(null);
        }

        fhirResourceRepository.save(entity);
    }
}