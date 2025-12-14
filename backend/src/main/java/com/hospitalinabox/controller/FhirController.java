package com.hospitalinabox.controller;

import com.hospitalinabox.domain.entity.FhirResourceEntity;
import com.hospitalinabox.domain.repository.FhirResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/fhir")
@RequiredArgsConstructor
public class FhirController {

    private final FhirResourceRepository fhirResourceRepository;

    @GetMapping(path = "/{resourceType}/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFhirResourceRaw(
            @PathVariable String resourceType,
            @PathVariable String id) {
        FhirResourceEntity entity = fhirResourceRepository
                .findByResourceTypeAndResourceId(resourceType, id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "FHIR resource not found"));

        return entity.getBody();
    }
}