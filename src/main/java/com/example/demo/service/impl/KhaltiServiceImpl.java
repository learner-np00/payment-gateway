package com.example.demo.service.impl;

import com.example.demo.dto.CallBackRequest;
import com.example.demo.dto.KhaltiRequest;
import com.example.demo.dto.KhaltiResponse;
import com.example.demo.entity.CustomerInfo;
import com.example.demo.entity.Payment;
import com.example.demo.repository.CustomerInfoRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.KhaltiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class KhaltiServiceImpl implements KhaltiService {

    @Autowired
    private WebClient.Builder webClientBuilder;
    private final CustomerInfoRepository customerInfoRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Mono<String> verifyPayment(KhaltiRequest khaltiRequest) {
        return Mono.fromCallable(() -> {
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("return_url", khaltiRequest.getReturnUrl());
                    requestBody.put("website_url", khaltiRequest.getWebsiteUrl());
                    requestBody.put("amount", khaltiRequest.getAmount() * 100);
                    requestBody.put("purchase_order_id", khaltiRequest.getPurchaseOrderId());
                    requestBody.put("purchase_order_name", khaltiRequest.getPurchaseOrderName());
                    if (khaltiRequest.getCustomerInfo() != null) {
                        requestBody.put("customer_info", khaltiRequest.getCustomerInfo());
                    }

                    CustomerInfo customerInfo = new CustomerInfo();
                    customerInfo.setName(khaltiRequest.getCustomerInfo().getName());
                    customerInfo.setEmail(khaltiRequest.getCustomerInfo().getEmail());
                    customerInfo.setPhone(khaltiRequest.getCustomerInfo().getPhone());
                    CustomerInfo savedCustomerInfo = customerInfoRepository.save(customerInfo);

                    Payment payment = new Payment();
                    payment.setPaymentType("Khalti");
                    payment.setPaymentStatus("Pending");
                    payment.setPaymentAmount(String.valueOf(khaltiRequest.getAmount()));
                    payment.setPaymentTime(LocalDateTime.now());
                    payment.setCustomerInfo(savedCustomerInfo);
                    Payment savedPayment = paymentRepository.save(payment);

                    return new PaymentContext(requestBody, savedPayment);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(context -> {
                    Map<String, Object> requestBody = context.requestBody();
                    Payment savedPayment = context.payment();

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
                            .bodyToMono(KhaltiResponse.class)
                            .doOnNext(response -> log.info("Received response: {}", response))
                            .flatMap(khaltiResponse ->
                                    Mono.fromCallable(() -> {
                                        savedPayment.setPidx(khaltiResponse.getPidx());
                                        Payment updatedPayment = paymentRepository.save(savedPayment);
                                        log.info("Updated payment with pidx: {}", updatedPayment.getPidx());
                                        return khaltiResponse.getPayment_url();
                                    }).subscribeOn(Schedulers.boundedElastic())
                            )
                            .doOnError(error -> log.error("Error occurred: ", error));
                });
    }

    @Override
    public ResponseEntity<String> handleCallback(CallBackRequest callBackRequest) {
        String pidx = callBackRequest.getPidx();
        String status = callBackRequest.getStatus();
        double amount = callBackRequest.getAmount();
        double totalAmount = callBackRequest.getTotalAmount();
        try {
            log.info("Received callback request: pidx=" + pidx + ", status=" + status + ", amount=" + amount + ", totalAmount=" + totalAmount);

            boolean isVerified = verifyPayment(pidx);

            if (isVerified) {
                log.info("Payment verified successfully for pidx=" + pidx);

                Payment payment = paymentRepository.findByPidx(pidx).orElseThrow(
                        () -> new RuntimeException("Payment not found for pidx=" + pidx)
                );
                payment.setPaymentStatus("Completed");
                paymentRepository.save(payment);

                return ResponseEntity.ok("Payment verified");
            } else {
                log.info("Payment verification failed for pidx=" + pidx);

                Payment payment = paymentRepository.findByPidx(pidx).orElseThrow(
                        () -> new RuntimeException("Payment not found for pidx=" + pidx)
                );
                payment.setPaymentStatus(status);
                paymentRepository.save(payment);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment verification failed");
            }
        } catch (Exception e) {
            log.error("Error occurred while handling callback: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error occurred");
        }
    }

    private boolean verifyPayment(String pidx) {
        String lookupUrl = "https://a.khalti.com/api/v2/epayment/lookup/";
        String secretKey = "live_secret_key_68791341fdd94846a146f0457ff7b455";
        RestTemplate restTemplate = new RestTemplate();

        String requestPayload = "{\"pidx\": \"" + pidx + "\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Key " + secretKey);

        HttpEntity<String> entity = new HttpEntity<>(requestPayload, headers);

        log.info("Sending request to " + lookupUrl + " with payload: " + requestPayload);

        try {
            ResponseEntity<String> response = restTemplate.exchange(lookupUrl, HttpMethod.POST, entity, String.class);
            log.info("Received response: " + response.getBody());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("status").asText();

            return "Completed".equals(status);
        } catch (Exception e) {
            log.error("Error occurred while verifying payment: " + e.getMessage(), e);
            return false;
        }
    }

    public record PaymentContext(Map<String, Object> requestBody, Payment payment) { }

}
