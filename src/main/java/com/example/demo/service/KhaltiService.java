package com.example.demo.service;

import com.example.demo.dto.CallBackRequest;
import com.example.demo.dto.KhaltiRequest;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface KhaltiService {
    Mono<String> verifyPayment(KhaltiRequest khaltiRequest);
    ResponseEntity<String> handleCallback(CallBackRequest callBackRequest);
}
