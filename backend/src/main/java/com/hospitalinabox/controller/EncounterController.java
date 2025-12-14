package com.hospitalinabox.controller;

import com.hospitalinabox.domain.entity.EncounterEntity;
import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.EncounterRepository;
import com.hospitalinabox.domain.repository.PatientRepository;
import com.hospitalinabox.dto.EncounterSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;

    // GET /api/encounters?patientId=...
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EncounterSummary> listByPatient(@RequestParam UUID patientId) {
        PatientEntity patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        return encounterRepository.findByPatient(patient).stream()
                .map(this::toSummary)
                .toList();
    }

    private EncounterSummary toSummary(EncounterEntity e) {
        return new EncounterSummary(
                e.getId(),
                e.getEncounterIdentifier(),
                e.getStatus(),
                e.getEncounterClass(),
                e.getAdmitTime(),
                e.getDischargeTime(),
                e.getReason());
    }
}