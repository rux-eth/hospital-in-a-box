package com.hospitalinabox.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

    @Bean
    public FhirContext fhirContext() {
        // R4 is the most widely used version in production today
        return FhirContext.forR4();
    }
}