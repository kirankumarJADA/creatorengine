package com.creatorengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final AppProperties props;

    public CorsConfig(AppProperties props) {
        this.props = props;
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        AppProperties.Cors c = props.getCors();

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(splitCsv(c.getAllowedOrigins()));
        cfg.setAllowedMethods(splitCsv(c.getAllowedMethods()));
        cfg.setAllowedHeaders(splitCsv(c.getAllowedHeaders()));
        cfg.setExposedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(c.isAllowCredentials());
        cfg.setMaxAge(c.getMaxAge());

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);

        return src;
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}