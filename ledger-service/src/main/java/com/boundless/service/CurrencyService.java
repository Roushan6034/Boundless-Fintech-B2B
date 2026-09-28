package com.boundless.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class CurrencyService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BigDecimal getRate(String from, String to) {
        if (from.equalsIgnoreCase(to)) return BigDecimal.ONE;
        try {
            String url = "https://api.exchangerate-api.com/v4/latest/" + from;
            JsonNode rates = objectMapper.readTree(restTemplate.getForObject(url, String.class)).get("rates");
            return BigDecimal.valueOf(rates.get(to.toUpperCase()).asDouble());
        } catch (Exception e) {
            log.warn("FX API failed, using fallback. {}", e.getMessage());
            if ("USD".equals(from) && "INR".equals(to)) return new BigDecimal("83.50");
            if ("INR".equals(from) && "USD".equals(to)) return new BigDecimal("0.012");
            if ("EUR".equals(from) && "INR".equals(to)) return new BigDecimal("91.00");
            return BigDecimal.ONE;
        }
    }

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        return amount.multiply(getRate(from, to)).setScale(2, RoundingMode.HALF_UP);
    }
}
