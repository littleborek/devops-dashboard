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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudAiServiceImpl implements AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public String analyze(String prompt, Long serverId, String apiKey, String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            endpointUrl = "https://api.openai.com/v1/chat/completions";
        } else if (endpointUrl.endsWith("/v1") || endpointUrl.endsWith("/v1/")) {
            endpointUrl = endpointUrl.endsWith("/") ? endpointUrl + "chat/completions"
                    : endpointUrl + "/chat/completions";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4-turbo"); // Default fallback, can be parameterized later
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(endpointUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            } else if (root.has("error")) {
                return "API Error: " + root.path("error").asText();
            } else {
                return "Beklenmeyen API yanıtı: " + response;
            }
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("Cloud AI Unauthorized: {}", e.getMessage());
            return "⚠️ Yetkisiz Erişim (401): Lütfen Ayarlar (AI Assistant Ayarları) menüsünden geçerli bir API Anahtarı girdiğinizden emin olun.";
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Cloud AI HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "HTTP Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("Cloud AI request failed: {}", e.getMessage());
            return "Error analyzing with Cloud AI: " + e.getMessage();
        }
    }

    @Override
    public String getProviderType() {
        return "CLOUD";
    }

    @Override
    public Flux<String> analyzeStream(String prompt, Long serverId, String apiKey, String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            endpointUrl = "https://api.openai.com/v1/chat/completions";
        } else if (endpointUrl.endsWith("/v1") || endpointUrl.endsWith("/v1/")) {
            endpointUrl = endpointUrl.endsWith("/") ? endpointUrl + "chat/completions"
                    : endpointUrl + "/chat/completions";
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4-turbo"); // Default fallback
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("stream", true); // Enable native streaming

        return webClient.post()
                .uri(endpointUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> chunk != null && !chunk.equals("[DONE]"))
                .map(chunk -> {
                    try {
                        JsonNode root = objectMapper.readTree(chunk);
                        JsonNode choices = root.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");
                            if (delta.has("content")) {
                                return delta.path("content").asText();
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(content -> !content.isEmpty())
                .onErrorResume(e -> Flux.just("[STREAM_ERROR] " + e.getMessage()));
    }
}
