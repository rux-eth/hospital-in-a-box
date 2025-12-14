package com.hospitalinabox.dto;

public record Hl7Metadata(
                String messageType, // e.g. "ADT^A01"
                String messageControlId // from MSH-10
) {
}