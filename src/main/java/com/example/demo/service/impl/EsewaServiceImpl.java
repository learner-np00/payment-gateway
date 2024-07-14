package com.example.demo.service.impl;

import com.example.demo.service.EsewaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
@Slf4j
public class EsewaServiceImpl implements EsewaService {
    @Autowired
    private WebClient.Builder webClientBuilder;
    private final String merchantCode = "YOUR_MERCHANT_CODE";
    private final String url = "https://esewa.com.np/epay/main";

    public Mono<String> initiatePayment(String amount, String refId) {
        String requestBody = String.format("amt=%s&scd=%s&pid=%s&su=%s&fu=%s",
                amount, merchantCode, refId, "YOUR_SUCCESS_URL", "YOUR_FAILURE_URL");

        return webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);
    }
}
