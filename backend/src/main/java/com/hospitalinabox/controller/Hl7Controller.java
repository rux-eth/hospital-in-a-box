package com.hospitalinabox.controller;

import com.hospitalinabox.dto.Hl7IngestResponse;
import com.hospitalinabox.dto.Hl7MessageView;
import com.hospitalinabox.domain.entity.Hl7MessageEntity;
import com.hospitalinabox.domain.repository.Hl7MessageRepository;
import com.hospitalinabox.service.Hl7IngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hl7")
@RequiredArgsConstructor
public class Hl7Controller {

    private final Hl7IngestService hl7IngestService;
    private final Hl7MessageRepository hl7MessageRepository;

    @PostMapping(
            path = "/messages",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Hl7IngestResponse ingest(@RequestBody String hl7Body) {
        return hl7IngestService.ingestRawMessage(hl7Body);
    }

    @GetMapping(path = "/messages/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Hl7MessageView getMessage(@PathVariable UUID id) {
        Hl7MessageEntity entity = hl7MessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("HL7 message not found: " + id));

        return new Hl7MessageView(
                entity.getId(),
                entity.getMessageType(),
                entity.getMessageControlId(),
                entity.getPayload(),
                entity.getReceivedAt()
        );
    }
}