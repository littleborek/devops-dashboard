package com.berk.devopsdashboard.service;

public interface AiService {
    /**
     * Sends a prompt to the AI provider and returns the analysis.
     *
     * @param prompt      The diagnostic prompt.
     * @param apiKey      The API key (if required).
     * @param endpointUrl The specific endpoint url (for local or custom models).
     * @return The AI's response text.
     */
    String analyze(String prompt, String apiKey, String endpointUrl);

    /**
     * @return The identifier of the provider this service implements (e.g.,
     *         "LOCAL", "CLOUD").
     */
    String getProviderType();

    /**
     * Streams the AI's response text token by token.
     *
     * @param prompt      The diagnostic prompt.
     * @param apiKey      The API key (if required).
     * @param endpointUrl The specific endpoint url.
     * @return A reactive stream of text chunks.
     */
    reactor.core.publisher.Flux<String> analyzeStream(String prompt, String apiKey, String endpointUrl);
}
