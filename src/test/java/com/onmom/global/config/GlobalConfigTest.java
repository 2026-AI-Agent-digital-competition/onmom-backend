package com.onmom.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestClient;

class GlobalConfigTest {

    @Test
    void registersRequiredInfrastructureBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                JacksonConfig.class,
                RestClientConfig.class
        )) {
            assertThat(context.getBean(ObjectMapper.class)).isNotNull();
            assertThat(context.getBean(RestClient.Builder.class)).isNotNull();
        }
    }
}
