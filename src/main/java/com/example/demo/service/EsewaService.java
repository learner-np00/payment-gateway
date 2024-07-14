package com.example.demo.service;

import reactor.core.publisher.Mono;

public interface EsewaService {
    Mono<String> initiatePayment(String amount, String refId);
}
