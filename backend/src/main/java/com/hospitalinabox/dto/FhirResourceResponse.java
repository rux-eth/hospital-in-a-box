package com.hospitalinabox.dto;

public record FhirResourceResponse(
        String resourceType,
        String id,
        String body) {
}