package com.example.demo.controller;

import com.example.demo.dto.CallBackRequest;
import com.example.demo.dto.KhaltiRequest;
import com.example.demo.service.EsewaService;
import com.example.demo.service.KhaltiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController {
    @Autowired
    private EsewaService esewaService;

    @Autowired
    private KhaltiService khaltiService;

    private static final String LOOKUP_URL = "https://khalti.com/epayment/lookup/";

    @PostMapping("/esewa/initiate")
    public Mono<String> initiateEsewaPayment(@RequestParam String amount, @RequestParam String refId) {
        return esewaService.initiatePayment(amount, refId);
    }

    @PostMapping("/khalti/initiate")
    public Mono<String> verifyKhaltiPayment(@RequestBody KhaltiRequest khaltiRequest) {
        return khaltiService.verifyPayment(khaltiRequest);
    }

    @GetMapping("/test")
    public String test() {
        return "Hello World!";
    }

    @PostMapping("/khalti/callback")
    public ResponseEntity<String> handleCallback(@ModelAttribute CallBackRequest callBackRequest) {
        return khaltiService.handleCallback(callBackRequest);
    }
}
