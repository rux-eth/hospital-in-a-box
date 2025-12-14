package com.hospitalinabox.service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import com.hospitalinabox.dto.Hl7Metadata;
import org.springframework.stereotype.Service;

@Service
public class Hl7ParsingService {

    private final PipeParser parser;

    public Hl7ParsingService() {
        this.parser = new PipeParser();
        // Disable strict validation for our demo messages
        this.parser.setValidationContext(ValidationContextFactory.noValidation());
    }

    // NEW: parse full message
    public Message parseMessage(String rawMessage) throws HL7Exception {
        String normalized = rawMessage
                .replace("\r\n", "\r")
                .replace("\n", "\r");
        return parser.parse(normalized);
    }

    // Existing: extract metadata from raw string
    public Hl7Metadata extractMetadata(String rawMessage) throws HL7Exception {
        Message message = parseMessage(rawMessage);
        Terser terser = new Terser(message);

        String msgCode = terser.get("/MSH-9-1");
        String triggerEvent = terser.get("/MSH-9-2");
        String messageType = (msgCode != null && triggerEvent != null)
                ? msgCode + "^" + triggerEvent
                : null;

        String controlId = terser.get("/MSH-10");

        return new Hl7Metadata(messageType, controlId);
    }
}