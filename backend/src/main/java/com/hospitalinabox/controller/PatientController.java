package com.hospitalinabox.controller;

import com.hospitalinabox.domain.entity.PatientEntity;
import com.hospitalinabox.domain.repository.PatientRepository;
import com.hospitalinabox.dto.PatientSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientRepository patientRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PatientSummary> listPatients() {
        return patientRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    private PatientSummary toSummary(PatientEntity p) {
        return new PatientSummary(
                p.getId(),
                p.getMrn(),
                p.getFirstName(),
                p.getLastName(),
                p.getBirthDate(),
                p.getGender());
    }
}