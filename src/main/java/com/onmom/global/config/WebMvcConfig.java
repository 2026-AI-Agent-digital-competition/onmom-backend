package com.onmom.global.config;

import com.onmom.global.auth.CurrentUserIdArgumentResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
    private final List<String> allowedOrigins;

    public WebMvcConfig(
            CurrentUserIdArgumentResolver currentUserIdArgumentResolver,
            @Value("${onmom.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins
    ) {
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
