package com.example.demo.controller;

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
    public ResponseEntity<String> handleCallback(
            @RequestParam("pidx") String pidx,
            @RequestParam(value = "txnId", required = false) String txnId,
            @RequestParam("amount") double amount,
            @RequestParam("total_amount") double total_amount,
            @RequestParam("status") String status,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "tidx", required = false) String tidx,
            @RequestParam("purchase_order_id") String purchase_order_id,
            @RequestParam("purchase_order_name") String purchase_order_name,
            @RequestParam(value = "transaction_id", required = false) String transaction_id) {

        try {
            // Log the callback request for debugging
            System.out.println("Received callback request: pidx=" + pidx + ", status=" + status + ", amount=" + amount + ", totalAmount=" + total_amount);

            // Perform payment verification using the lookup API
            boolean isVerified = verifyPayment(pidx);

            if (isVerified) {
                // Handle successful verification logic
                System.out.println("Payment verified successfully for pidx=" + pidx);
                return ResponseEntity.ok("Payment verified");
            } else {
                // Handle failed verification logic
                System.out.println("Payment verification failed for pidx=" + pidx);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment verification failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error occurred");
        }
    }

    private boolean verifyPayment(String pidx) {
        String secretKey = "live_secret_key_68791341fdd94846a146f0457ff7b455";

// Set the lookup URL
        String lookupUrl = "https://a.khalti.com/api/v2/epayment/lookup/";

        RestTemplate restTemplate = new RestTemplate();

        // Create the request payload
        String requestPayload = "{\"pidx\": \"" + pidx + "\"}";

        // Create headers and set content type to application/json
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Key " + secretKey); // Ensure you set your secret key here

        // Create HttpEntity containing headers and payload
        HttpEntity<String> entity = new HttpEntity<>(requestPayload, headers);

        // Log request for debugging
        System.out.println("Sending request to " + lookupUrl + " with payload: " + requestPayload);

        try {
            // Send the request to the lookup API
            ResponseEntity<String> response = restTemplate.exchange(lookupUrl, HttpMethod.POST, entity, String.class);

            // Log response for debugging
            System.out.println("Received response: " + response.getBody());

            // Parse the response and check the payment status
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("status").asText();

            return "Completed".equals(status);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
