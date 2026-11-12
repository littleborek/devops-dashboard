package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.repository.ServerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrewAiServiceImpl implements AiService {

    private final RestTemplate restTemplate = createRestTemplate();
    private final ObjectMapper objectMapper;
    private final ServerRepository serverRepository;

    @Value("${CREW_AI_ENDPOINT:http://localhost:8000}")
    private String defaultEndpoint;

    private static RestTemplate createRestTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(300_000);
        return new RestTemplate(factory);
    }

    private URI resolveCrewAiEndpoint(String endpointUrl) {
        String url = endpointUrl == null || endpointUrl.isBlank() ? defaultEndpoint : endpointUrl;
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("Invalid CrewAI URL");
            }
            String path = uri.getRawPath().replaceAll("/+$", "");
            int port = uri.getPort();
            if (!path.endsWith("/crew/analyze")) {
                path = path.replaceFirst("/v1(?:/chat/completions)?$", "") + "/api/v1/crew/analyze";
                if (port == 1234 || (port == -1 && "http".equals(uri.getScheme()))) {
                    port = 8000;
                }
            }
            return UriComponentsBuilder.fromUri(uri).port(port).replacePath(path).build(true).toUri();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CrewAI endpoint");
        }
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
        
        URI targetUrl = resolveCrewAiEndpoint(endpointUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", prompt);
        requestBody.put("context", systemContext.toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(targetUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response);
            if ("success".equals(root.path("status").asText()) && root.path("result").isTextual()
                    && !root.path("result").asText().isBlank()) {
                return root.path("result").asText();
            }
        } catch (ResourceAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            log.warn("CrewAI service connection failed ({})", cause.getClass().getSimpleName());
            if (cause instanceof SocketTimeoutException) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "CrewAI request timed out");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "CrewAI service unavailable");
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 504) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "CrewAI request timed out");
            }
            log.warn("CrewAI service returned HTTP {}", e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("CrewAI service request failed ({})", e.getClass().getSimpleName());
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "CrewAI could not complete the request");
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
