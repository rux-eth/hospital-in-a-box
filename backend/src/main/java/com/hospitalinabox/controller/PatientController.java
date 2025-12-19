package com.hospitalinabox.controller;

import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.FhirResourceEntity;
import com.hospitalinabox.domain.entity.ObservationEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.FhirResourceRepository;
import com.hospitalinabox.domain.repository.ObservationRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import com.hospitalinabox.dto.EncounterSummary;
import com.hospitalinabox.dto.FhirResourceView;
import com.hospitalinabox.dto.ObservationSummary;
import com.hospitalinabox.dto.PatientSummary;
import com.hospitalinabox.dto.TimelineEvent;
import com.hospitalinabox.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;
    private final FhirResourceRepository fhirResourceRepository;
    private final TimelineService timelineService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PatientSummary> listPatients() {
        return patientRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping(path = "/{id}/encounters", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EncounterSummary> listEncounters(@PathVariable UUID id) {
        PatientEntity patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));

        return encounterRepository.findByPatient(patient).stream()
                .map(this::toEncounterSummary)
                .toList();
    }

    @GetMapping(path = "/{id}/observations", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ObservationSummary> listObservations(@PathVariable UUID id) {
        PatientEntity patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));

        return observationRepository.findByPatient(patient).stream()
                .map(this::toObservationSummary)
                .toList();
    }

    @GetMapping(path = "/{id}/timeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TimelineEvent> getTimeline(@PathVariable UUID id) {
        return timelineService.getTimelineForPatient(id);
    }

    @GetMapping(path = "/{id}/fhir-resources", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FhirResourceView> listFhirResources(@PathVariable UUID id) {
        // Ensure patient exists
        patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));

        return fhirResourceRepository.findByPatientId(id).stream()
                .map(this::toFhirView)
                .toList();
    }

    // ---- mappers ----

    private PatientSummary toSummary(PatientEntity p) {
        return new PatientSummary(
                p.getId(),
                p.getMrn(),
                p.getFirstName(),
                p.getLastName(),
                p.getBirthDate(),
                p.getGender());
    }

    private EncounterSummary toEncounterSummary(EncounterEntity e) {
        return new EncounterSummary(
                e.getId(),
                e.getEncounterIdentifier(),
                e.getStatus(),
                e.getEncounterClass(),
                e.getAdmitTime(),
                e.getDischargeTime(),
                e.getReason());
    }

    private ObservationSummary toObservationSummary(ObservationEntity o) {
        return new ObservationSummary(
                o.getId(),
                o.getCode(),
                o.getDisplay(),
                o.getValue(),
                o.getUnit(),
                o.getEffectiveTime());
    }

    private FhirResourceView toFhirView(FhirResourceEntity e) {
        return new FhirResourceView(
                e.getId(),
                e.getResourceType(),
                e.getResourceId(),
                e.getEventTime(),
                e.getBody());
    }
}