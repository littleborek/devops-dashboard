package com.berk.devopsdashboard.service;

import reactor.core.publisher.Flux;

public interface AiService {
    /**
     * Sends a prompt to the AI provider and returns the analysis.
     *
     * @param prompt      The diagnostic prompt.
     * @param serverId    Target server ID for context scoping (optional).
     * @param apiKey      The API key (if required).
     * @param endpointUrl The specific endpoint url (for local or custom models).
     * @return The AI's response text.
     */
    String analyze(String prompt, Long serverId, String apiKey, String endpointUrl);
    
    // Maintain backwards compatibility with a default implementation
    default String analyze(String prompt, String apiKey, String endpointUrl) {
        return analyze(prompt, null, apiKey, endpointUrl);
    }

    /**
     * @return The identifier of the provider this service implements (e.g.,
     *         "LOCAL", "CLOUD").
     */
    String getProviderType();

    /**
     * Streams the AI's response text token by token.
     *
     * @param prompt      The diagnostic prompt.
     * @param serverId    Target server ID for context scoping (optional).
     * @param apiKey      The API key (if required).
     * @param endpointUrl The specific endpoint url.
     * @return A reactive stream of text chunks.
     */
    Flux<String> analyzeStream(String prompt, Long serverId, String apiKey, String endpointUrl);

    default Flux<String> analyzeStream(String prompt, String apiKey, String endpointUrl) {
        return analyzeStream(prompt, null, apiKey, endpointUrl);
    }
}
