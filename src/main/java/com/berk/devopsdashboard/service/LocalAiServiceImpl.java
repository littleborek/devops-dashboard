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
public class LocalAiServiceImpl implements AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.builder().build();

    private String resolveEndpoint(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            String envEndpoint = System.getenv("LOCAL_AI_ENDPOINT");
            if (envEndpoint != null && !envEndpoint.trim().isEmpty()) {
                endpointUrl = envEndpoint;
            } else {
                return "http://host.docker.internal:1234/v1/chat/completions";
            }
        }
        
        String url = endpointUrl.trim();
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        
        if (!url.matches(".*:\\d+.*") && !url.contains("/v1")) {
            url = url + ":1234";
        }
        
        url = url.replaceAll("/+$", "");
        
        if (!url.endsWith("/v1/chat/completions")) {
            if (url.endsWith("/v1")) {
                url = url + "/chat/completions";
            } else {
                url = url + "/v1/chat/completions";
            }
        }
        
        return url;
    }

    private String getModelName() {
        String envModel = System.getenv("OPENAI_MODEL_NAME");
        if (envModel != null && !envModel.trim().isEmpty()) {
            return envModel.replace("openai/", "");
        }
        return "mistralai/mistral-nemo-instruct-2407";
    }

    @Override
    public String analyze(String prompt, Long serverId, String apiKey, String endpointUrl) {
        String targetUrl = resolveEndpoint(endpointUrl);
        log.info("Local AI Endpoint: {}", targetUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", getModelName());
        requestBody.put("temperature", 0.7);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are an expert DevOps AI assistant."),
                Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(targetUrl, request, String.class);
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
            log.error("Local AI Unauthorized: {}", e.getMessage());
            return "⚠️ Yetkisiz Erişim (401): Lütfen Ayarlar (AI Assistant Ayarları) menüsünden geçerli bir API Anahtarı girdiğinizden emin olun.";
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Local AI HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "HTTP Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("Local AI request failed. Target: {}: {}", targetUrl, e.getMessage());
            return "Error analyzing with Local AI: " + e.getMessage();
        }
    }

    @Override
    public String getProviderType() {
        return "LOCAL";
    }

    @Override
    public Flux<String> analyzeStream(String prompt, Long serverId, String apiKey, String endpointUrl) {
        String targetUrl = resolveEndpoint(endpointUrl);
        log.info("Local AI Stream Endpoint: {}", targetUrl);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", getModelName());
        requestBody.put("temperature", 0.7);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are an expert DevOps AI assistant."),
                Map.of("role", "user", "content", prompt)));
        requestBody.put("stream", true);

        return webClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> chunk != null && !chunk.trim().equals("[DONE]"))
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
