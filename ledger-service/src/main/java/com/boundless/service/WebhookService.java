package com.boundless.service;

import com.boundless.entity.WebhookConfig;
import com.boundless.repository.WebhookConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {
    private final WebhookConfigRepository webhookRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void fire(String companyName, String event, Map<String, Object> data) {
        for (WebhookConfig hook : webhookRepo.findByCompanyNameAndActiveTrue(companyName)) {
            if (hook.getEvents() != null && !hook.getEvents().contains(event)) continue;
            try {
                Map<String, Object> payload = Map.of("event", event, "timestamp", new Date().toString(), "data", data);
                String json = objectMapper.writeValueAsString(payload);
                String sig = hmac(hook.getSecret(), json);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Boundless-Signature", "sha256=" + sig);
                headers.set("X-Boundless-Event", event);

                new RestTemplate().postForEntity(hook.getWebhookUrl(), new HttpEntity<>(json, headers), String.class);
                log.info("✅ Webhook fired to {} for {}", hook.getWebhookUrl(), event);
            } catch (Exception e) {
                log.error("❌ Webhook failed for {}: {}", hook.getWebhookUrl(), e.getMessage());
            }
        }
    }

    private String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes()));
        } catch (Exception e) { return "error"; }
    }
}
