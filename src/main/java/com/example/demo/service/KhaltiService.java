package com.example.demo.service;

import com.example.demo.dto.KhaltiRequest;
import reactor.core.publisher.Mono;

public interface KhaltiService {
    Mono<String> verifyPayment(KhaltiRequest khaltiRequest);
}
