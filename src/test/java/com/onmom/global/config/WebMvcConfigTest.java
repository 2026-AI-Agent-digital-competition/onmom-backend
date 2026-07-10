package com.onmom.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.onmom.global.auth.CurrentUserIdArgumentResolver;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebMvcConfigTest {

    private final CurrentUserIdArgumentResolver argumentResolver = mock(CurrentUserIdArgumentResolver.class);

    @Test
    void registersCurrentUserArgumentResolver() {
        WebMvcConfig config = new WebMvcConfig(argumentResolver, List.of("http://localhost:5173"));
        List<HandlerMethodArgumentResolver> resolvers = new java.util.ArrayList<>();

        config.addArgumentResolvers(resolvers);

        assertThat(resolvers).containsExactly(argumentResolver);
    }

    @Test
    void configuresCorsForApiEndpoints() {
        WebMvcConfig config = new WebMvcConfig(
                argumentResolver,
                List.of("http://localhost:5173", "https://app.onmom.example.com")
        );
        TestCorsRegistry registry = new TestCorsRegistry();

        config.addCorsMappings(registry);

        CorsConfiguration cors = registry.configurations().get("/api/**");
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins())
                .containsExactly("http://localhost:5173", "https://app.onmom.example.com");
        assertThat(cors.getAllowedMethods())
                .containsExactly("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(cors.getAllowCredentials()).isFalse();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }

    private static class TestCorsRegistry extends CorsRegistry {

        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}
