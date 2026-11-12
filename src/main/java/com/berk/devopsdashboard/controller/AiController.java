package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.AiService;
import com.berk.devopsdashboard.service.ContextFactory;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    public record ChatRequest(@NotBlank String query) {}

    public record AnalysisRequest(@NotNull @Positive Long serverId, String eventContext, String recentLogs) {}

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeIssue(
            @Valid @RequestBody AnalysisRequest payload,
            @RequestHeader(value = "X-AI-Provider", defaultValue = "CLOUD") String provider,
            @RequestHeader(value = "X-AI-Key", required = false) String apiKey,
            @RequestHeader(value = "X-AI-Endpoint", required = false) String endpointUrl) {

        Long serverId = payload.serverId();
        String eventContext = payload.eventContext() == null ? "" : payload.eventContext();
        String recentLogs = payload.recentLogs() == null ? "" : payload.recentLogs();

        Optional<Server> optionalServer = serverRepository.findById(serverId);
        if (optionalServer.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Server not found"));
        }

        Server server = optionalServer.get();
        String prompt = contextFactory.buildDiagnosticPrompt(server, eventContext, recentLogs);

        AiService aiService = aiServices.stream()
                .filter(s -> s.getProviderType().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown AI provider"));

        String analysis = aiService.analyze(prompt, serverId, apiKey, endpointUrl);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatQuery(
            @Valid @RequestBody ChatRequest payload,
            @RequestHeader(value = "X-AI-Provider", defaultValue = "CLOUD") String provider,
            @RequestHeader(value = "X-AI-Key", required = false) String apiKey,
            @RequestHeader(value = "X-AI-Endpoint", required = false) String endpointUrl) {

        String query = payload.query().strip();

        AiService aiService = aiServices.stream()
                .filter(s -> s.getProviderType().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown AI provider"));

        String response = aiService.analyze(query, apiKey, endpointUrl);
        return ResponseEntity.ok(Map.of("response", response));
    }

    @PostMapping(value = "/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<String> chatQueryStream(
            @Valid @RequestBody ChatRequest payload,
            @RequestHeader(value = "X-AI-Provider", defaultValue = "CLOUD") String provider,
            @RequestHeader(value = "X-AI-Key", required = false) String apiKey,
            @RequestHeader(value = "X-AI-Endpoint", required = false) String endpointUrl) {

        String query = payload.query().strip();

        AiService aiService = aiServices.stream()
                .filter(s -> s.getProviderType().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown AI provider"));

        return aiService.analyzeStream(query, apiKey, endpointUrl);
    }
}
