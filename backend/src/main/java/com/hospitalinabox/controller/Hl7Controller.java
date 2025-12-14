package com.hospitalinabox.controller;

import com.hospitalinabox.dto.Hl7IngestResponse;
import com.hospitalinabox.service.Hl7IngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hl7")
@RequiredArgsConstructor
public class Hl7Controller {

    private final Hl7IngestService hl7IngestService;

    // Accept raw HL7 message as text/plain
    @PostMapping(
            path = "/messages",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Hl7IngestResponse ingest(@RequestBody String hl7Body) {
        return hl7IngestService.ingestRawMessage(hl7Body);
    }
}