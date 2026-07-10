package com.onmom.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GlobalConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(RestClientConfig.class);

    @Test
    void registersRequiredInfrastructureBeans() {
        contextRunner.run(context -> {
            assertThat(context.getBean(ObjectMapper.class)).isNotNull();
            assertThat(context.getBean(RestClient.Builder.class)).isNotNull();
        });
    }
}
