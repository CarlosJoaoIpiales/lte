package com.m3verificaciones.appweb.lte.utils.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MessageApiService {

    private static final Logger log = LoggerFactory.getLogger(MessageApiService.class);
    private final WebClient webClient;

    public MessageApiService(WebClient.Builder webClientBuilder, @Value("${api.message.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<String> createMessage(ObjectNode messageBody){
        return webClient.post()
                .uri("/message")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(messageBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(r -> log.info("Save Message Succes"))
                .doOnError(e -> log.error("Error creating message: {}", e.getMessage()));
    }
    
}