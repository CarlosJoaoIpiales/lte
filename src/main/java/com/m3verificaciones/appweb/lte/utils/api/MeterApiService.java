package com.m3verificaciones.appweb.lte.utils.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MeterApiService {
    private static final Logger log = LoggerFactory.getLogger(MeterApiService.class);
    private final WebClient webClient;

    public MeterApiService(WebClient.Builder webClientBuilder, @Value("${api.meter.base-url}") String apiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(apiBaseUrl).build();
    }

    public Mono<JsonNode> getMeterDetailsByImei(String imei){
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/meter/details")
                        .queryParam("deviceId", imei)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error fetching meter details for IMEI {}: {}", imei, e.getMessage()))
                .onErrorReturn(JsonNodeFactory.instance.objectNode());
    }

    public Mono<Void> updateLastCommunication(String meterUniqueKey){
        String lastCommunication = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        return webClient.put()
                .uri(uriBuilder -> uriBuilder.path("/meter/{key}/last-communication")
                        .queryParam("lastCommunication", lastCommunication)
                        .build(meterUniqueKey))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Last Communication updated: key={}", meterUniqueKey))
                .doOnError(e -> log.error("Error updating last communication for meter {}: {}", meterUniqueKey, e.getMessage()));
    }
}
