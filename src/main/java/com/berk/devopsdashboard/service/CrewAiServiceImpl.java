package com.berk.devopsdashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrewAiServiceImpl implements AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Override
    public String analyze(String prompt, String apiKey, String endpointUrl) {
        // Varsayılan CrewAI Python Servis adresi (Lokal 8000 portu)
        if (endpointUrl == null || endpointUrl.isEmpty() || endpointUrl.contains(":1234")) {
            endpointUrl = "http://localhost:8000/api/v1/crew/analyze";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", prompt);
        requestBody.put("context", "DevOps Dashboard Sistem Analizi");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.info("CrewAI Servisi çağrılıyor: {}", endpointUrl);
            String response = restTemplate.postForObject(endpointUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("result")) {
                // CrewAI'ın final raporunu alıyoruz
                return root.path("result").asText();
            } else {
                return "CrewAI Servis Hatası: " + response;
            }
        } catch (Exception e) {
            log.error("CrewAI isteği başarısız: {}", e.getMessage());
            return "CrewAI Servisine ulaşılamadı (Python servisi çalışıyor mu?). Hata: " + e.getMessage();
        }
    }

    @Override
    public String getProviderType() {
        return "CREW_AI";
    }

    @Override
    public Flux<String> analyzeStream(String prompt, String apiKey, String endpointUrl) {
        // CrewAI ajanları arası müzakere sürerken stream yapması kompleks bir konu.
        // Şimdilik final sonucu tek seferde dönüyoruz.
        return Flux.just(analyze(prompt, apiKey, endpointUrl));
    }
}
