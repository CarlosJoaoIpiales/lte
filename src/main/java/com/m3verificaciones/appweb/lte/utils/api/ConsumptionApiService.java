package com.m3verificaciones.appweb.lte.utils.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

@Service
public class ConsumptionApiService {
    
    private static final Logger log = LoggerFactory.getLogger(ConsumptionApiService.class);
    private final WebClient webClient;

    public ConsumptionApiService(WebClient.Builder webClientBuilder, @Value("${api.consumption.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<String> createConsumption(String imei, String serial, double consumption, String model, String diameter){
        Map<String, Object> body = new HashMap<>();
        body.put("deviceId", imei);
        body.put("serial", serial);
        body.put("dateConsumption", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        body.put("consumptionValue", consumption);
        body.put("model", model);
        body.put("diameter", diameter);

        return webClient.post()
                .uri("")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> 
                    Mono.error(new RuntimeException("Client error: " + r.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, r -> 
                    Mono.error(new RuntimeException("Server error: " + r.statusCode())))
                .bodyToMono(String.class)
                .doOnSuccess(r -> log.info("Save Consumption Succes"))
                .doOnError(e -> log.error("Error creating consumption: {}", e.getMessage()));
    }
}
