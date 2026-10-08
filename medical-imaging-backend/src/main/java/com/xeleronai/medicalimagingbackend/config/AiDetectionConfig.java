package com.xeleronai.medicalimagingbackend.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Client HTTP vers ai-detection-service. Read-timeout très large : voir
 * ai.detection.read-timeout-seconds (application.properties) — l'inférence
 * locale mesurée prend jusqu'à ~31 min.
 */
@Configuration
public class AiDetectionConfig {

    @Bean
    public RestClient aiDetectionRestClient(
            @Value("${ai.detection.base-url}") String baseUrl,
            @Value("${ai.detection.read-timeout-seconds}") long readTimeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
