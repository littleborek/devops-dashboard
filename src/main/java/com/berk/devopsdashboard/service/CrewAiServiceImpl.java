package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.repository.ServerRepository;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrewAiServiceImpl implements AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final ServerRepository serverRepository;

    private String resolveCrewAiEndpoint(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            String envEndpoint = System.getenv("CREW_AI_ENDPOINT");
            if (envEndpoint != null && !envEndpoint.trim().isEmpty()) {
                endpointUrl = envEndpoint;
            } else {
                return "http://host.docker.internal:8000/api/v1/crew/analyze";
            }
        }
        
        String url = endpointUrl.trim();
        if (url.contains(":1234")) {
            url = url.replace(":1234", ":8000");
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        
        if (url.contains("/v1/chat/completions")) {
            url = url.replace("/v1/chat/completions", "");
        } else if (url.endsWith("/v1") || url.endsWith("/v1/")) {
            url = url.replaceAll("/v1/?$", "");
        }
        
        if (!url.matches(".*:\\d+.*")) {
            url = url + ":8000";
        }
        
        url = url.replaceAll("/+$", "");
        if (!url.endsWith("/api/v1/crew/analyze")) {
            if (!url.contains("/crew/analyze")) {
                url = url + "/api/v1/crew/analyze";
            }
        }
        
        return url;
    }

    @Override
    public String analyze(String prompt, Long serverId, String apiKey, String endpointUrl) {
        // Gathering server data to create a rich context
        StringBuilder systemContext = new StringBuilder("Current System Status:\n");
        
        if (serverId != null) {
            // Include only the relevant server in context
            serverRepository.findById(serverId).ifPresent(s -> {
                systemContext.append(String.format("- Server: %s, Status: %s, CPU: %s%%, RAM: %s%%, OS: %s\n",
                        s.getName(), s.getStatus(), s.getCpuUsage(), s.getRamUsage(), s.getOperatingSystem()));
            });
        } else {
            // Include all servers for general context
            serverRepository.findAll().forEach(s -> {
                systemContext.append(String.format("- Server: %s, Status: %s, CPU: %s%%, RAM: %s%%, OS: %s\n",
                        s.getName(), s.getStatus(), s.getCpuUsage(), s.getRamUsage(), s.getOperatingSystem()));
            });
        }
        
        String targetUrl = resolveCrewAiEndpoint(endpointUrl);
// ... rest of the method

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", prompt);
        requestBody.put("context", systemContext.toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.info("CrewAI Servisi çağrılıyor: {}", targetUrl);
            String response = restTemplate.postForObject(targetUrl, request, String.class);
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
    public Flux<String> analyzeStream(String prompt, Long serverId, String apiKey, String endpointUrl) {
        // CrewAI ajanları arası müzakere sürerken stream yapması kompleks bir konu.
        // Şimdilik final sonucu tek seferde dönüyoruz.
        return Mono.fromCallable(() -> analyze(prompt, serverId, apiKey, endpointUrl))
                   .subscribeOn(Schedulers.boundedElastic())
                   .flux();
    }
}
