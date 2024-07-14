package com.example.demo.service.impl;

import com.example.demo.dto.KhaltiRequest;
import com.example.demo.service.KhaltiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class KhaltiServiceImpl implements KhaltiService {
    @Autowired
    private WebClient.Builder webClientBuilder;

    @Override
    public Mono<String> verifyPayment(KhaltiRequest khaltiRequest) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("return_url", khaltiRequest.getReturnUrl());
        requestBody.put("website_url", khaltiRequest.getWebsiteUrl());
        requestBody.put("amount", khaltiRequest.getAmount() * 100);
        requestBody.put("purchase_order_id", khaltiRequest.getPurchaseOrderId());
        requestBody.put("purchase_order_name", khaltiRequest.getPurchaseOrderName());
        if (khaltiRequest.getCustomerInfo() != null) {
            requestBody.put("customer_info", khaltiRequest.getCustomerInfo());
        }

        log.info("Initiating payment with request body: {}", requestBody);

        String khaltiSecretKey = "live_secret_key_68791341fdd94846a146f0457ff7b455";
        String url = "https://a.khalti.com/api/v2/epayment/initiate/";

        return webClientBuilder.build()
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Key " + khaltiSecretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            log.error("4xx error: {}", responseBody);
                            return Mono.error(new RuntimeException("4xx error: " + responseBody));
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            log.error("5xx error: {}", responseBody);
                            return Mono.error(new RuntimeException("5xx error: " + responseBody));
                        }))
                .bodyToMono(String.class)
                .doOnNext(response -> log.info("Received response: {}", response))
                .doOnError(error -> log.error("Error occurred: ", error)
                );
    }
}
