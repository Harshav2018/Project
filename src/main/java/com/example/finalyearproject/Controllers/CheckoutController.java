package com.example.finalyearproject.Controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Base64;
import org.json.JSONObject;
import org.json.JSONArray;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Value("${paypal.clientId}")
    private String paypalClientId;

    @Value("${paypal.secret}")
    private String paypalSecret;

    @Value("${paypal.url}")
    private String payPalUrl;

    private final RestTemplate restTemplate;

    public CheckoutController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/Get-Token")
    public ResponseEntity<String> getToken() {
        try {
            String accessToken = getPayPalAccessToken();
            return ResponseEntity.ok(accessToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving token");
        }
    }

    @PostMapping("/Approval")
    public ResponseEntity<?> completeOrder(@RequestBody String data) {
        try {
            JSONObject jsonData = new JSONObject(data);
            String orderId = jsonData.optString("orderId", "");

            if (orderId.isEmpty()) {
                return ResponseEntity.badRequest().body("error: orderId is missing");
            }

            String accessToken = getPayPalAccessToken();
            String url = payPalUrl + "/v2/checkout/orders/" + orderId + "/capture";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                String paypalOrderStatus = jsonResponse.optString("status", "");
                if ("COMPLETED".equals(paypalOrderStatus)) {
                    return ResponseEntity.ok(new JSONObject().put("status", "success").toString());
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONObject().put("status", "Capture failed").toString());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/create-order/{transactionAmount}")
    public ResponseEntity<?> createOrder(@PathVariable("transactionAmount") int transactionAmount) {
        try {
            int totalAmount = transactionAmount == 0 ? 10 : transactionAmount;


            JSONObject createOrderRequest = new JSONObject();
            createOrderRequest.put("intent", "CAPTURE");

            JSONObject amount = new JSONObject();
            amount.put("currency_code", "USD");
            amount.put("value", totalAmount);

            JSONObject purchaseUnit1 = new JSONObject();
            purchaseUnit1.put("amount", amount);

            JSONArray purchaseUnits = new JSONArray();
            purchaseUnits.put(purchaseUnit1);

            createOrderRequest.put("purchase_units", purchaseUnits);

            String accessToken = getPayPalAccessToken();

            String url = payPalUrl + "/v2/checkout/orders";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(createOrderRequest.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                String paypalOrderId = jsonResponse.optString("id", "");
                return ResponseEntity.ok(response.getBody());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONObject().put("Id", "").toString());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getPayPalAccessToken() throws Exception {
        String url = payPalUrl + "/v1/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(paypalClientId, paypalSecret);  // ✅ Correct way to set Basic Auth
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // ✅ Required for OAuth

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials"); // ✅ Correct request format

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            JSONObject jsonResponse = new JSONObject(response.getBody());
            return jsonResponse.optString("access_token", "");
        } else {
            throw new RuntimeException("Failed to get PayPal access token: " + response.getBody());
        }

    }
}