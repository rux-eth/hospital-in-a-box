package com.hospitalinabox.service;

import com.hospitalinabox.dto.Hl7Metadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class Hl7ParsingServiceTest {

    private final Hl7ParsingService service = new Hl7ParsingService();

    @Test
    void extractMetadata_AdtA01() throws Exception {
        String hl7 = """
                MSH|^~\\&|APP|FAC|RECAPP|RECFAC|202501011200||ADT^A01|MSGID1234|P|2.5
                PID|1||12345^^^HOSPITAL^MR||DOE^JOHN^^^^^L||19800101|M
                """;

        Hl7Metadata metadata = service.extractMetadata(hl7);

        assertThat(metadata.messageType()).isEqualTo("ADT^A01");
        assertThat(metadata.messageControlId()).isEqualTo("MSGID1234");
    }

    @Test
    void extractMetadata_OruR01() throws Exception {
        String hl7 = """
                MSH|^~\\&|LAB_APP|LAB_FAC|RECAPP|RECFAC|202501011300||ORU^R01|MSGID2001|P|2.5
                PID|1||99999^^^HOSPITAL^MR||DOE^JANE^^^^^L||19900101|F
                """;

        Hl7Metadata metadata = service.extractMetadata(hl7);

        assertThat(metadata.messageType()).isEqualTo("ORU^R01");
        assertThat(metadata.messageControlId()).isEqualTo("MSGID2001");
    }
}