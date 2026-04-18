package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.AiService;
import com.berk.devopsdashboard.service.ContextFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final List<AiService> aiServices;
    private final ContextFactory contextFactory;
    private final ServerRepository serverRepository;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeIssue(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-AI-Provider", defaultValue = "CLOUD") String provider,
            @RequestHeader(value = "X-AI-Key", required = false) String apiKey,
            @RequestHeader(value = "X-AI-Endpoint", required = false) String endpointUrl) {

        Long serverId = Long.valueOf(payload.get("serverId").toString());
        String eventContext = (String) payload.getOrDefault("eventContext", "");
        String recentLogs = (String) payload.getOrDefault("recentLogs", "");

        Optional<Server> optionalServer = serverRepository.findById(serverId);
        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Server not found"));
        }

        Server server = optionalServer.get();
        String prompt = contextFactory.buildDiagnosticPrompt(server, eventContext, recentLogs);

        AiService aiService = aiServices.stream()
                .filter(s -> s.getProviderType().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Provider Type: " + provider));

        String analysis = aiService.analyze(prompt, serverId, apiKey, endpointUrl);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatQuery(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-AI-Provider", defaultValue = "CLOUD") String provider,
            @RequestHeader(value = "X-AI-Key", required = false) String apiKey,
            @RequestHeader(value = "X-AI-Endpoint", required = false) String endpointUrl) {

        String query = payload.get("query");
        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
        }

        AiService aiService = aiServices.stream()
                .filter(s -> s.getProviderType().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Provider Type: " + provider));

        String response = aiService.analyze(query, apiKey, endpointUrl);
        return ResponseEntity.ok(Map.of("response", response));
    }

    @PostMapping(value = "/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<String> chatQueryStream(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-AI-Provider", defaultValue = "CLOUD") String provider,
            @RequestHeader(value = "X-AI-Key", required = false) String apiKey,
            @RequestHeader(value = "X-AI-Endpoint", required = false) String endpointUrl) {

        String query = payload.get("query");
        if (query == null || query.isEmpty()) {
            return reactor.core.publisher.Flux.error(new IllegalArgumentException("Query is required"));
        }

        AiService aiService = aiServices.stream()
                .filter(s -> s.getProviderType().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Provider Type: " + provider));

        return aiService.analyzeStream(query, apiKey, endpointUrl);
    }
}
