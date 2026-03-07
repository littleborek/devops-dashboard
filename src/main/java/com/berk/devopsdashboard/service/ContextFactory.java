package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.entity.Server;
import org.springframework.stereotype.Component;

@Component
public class ContextFactory {

    /**
     * Packages the current server status and an optional event log into a prompt
     * for the AI.
     *
     * @param server       The target server.
     * @param eventContext Specific error or event context (e.g., "CPU spiked to
     *                     99%").
     * @param recentLogs   N number of recent logs from the server.
     * @return A formatted narrative prompt.
     */
    public String buildDiagnosticPrompt(Server server, String eventContext, String recentLogs) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert Site Reliability Engineer (SRE) and AI Diagnostic Assistant.\n");
        prompt.append("Analyze the following system context and provide a concise Root Cause Analysis (RCA) ");
        prompt.append("and suggested remediation steps.\n\n");

        prompt.append("=== System State ===\n");
        prompt.append("Server Name: ").append(server.getName()).append("\n");
        prompt.append("OS: ").append(server.getOperatingSystem()).append("\n");
        prompt.append("CPU Usage: ")
                .append(server.getCpuUsage() != null ? String.format("%.2f%%", server.getCpuUsage()) : "Unknown")
                .append("\n");
        prompt.append("RAM Usage: ")
                .append(server.getRamUsage() != null ? String.format("%.2f%%", server.getRamUsage()) : "Unknown")
                .append("\n");
        prompt.append("Disk Usage: ")
                .append(server.getDiskUsage() != null ? String.format("%.2f%%", server.getDiskUsage()) : "Unknown")
                .append("\n");
        prompt.append("Load Avg: ").append(server.getLoadAvg() != null ? server.getLoadAvg() : "Unknown")
                .append("\n\n");

        if (eventContext != null && !eventContext.isEmpty()) {
            prompt.append("=== Incident Event ===\n");
            prompt.append(eventContext).append("\n\n");
        }

        if (recentLogs != null && !recentLogs.isEmpty()) {
            prompt.append("=== Recent Relevant Logs ===\n");
            prompt.append(recentLogs).append("\n\n");
        }

        prompt.append("Instructions:\n");
        prompt.append("1. What is the most likely cause of this issue based on the provided metrics and logs?\n");
        prompt.append(
                "2. What specific commands or actions should be taken to resolve it? (Keep it actionable and brief)");

        return prompt.toString();
    }
}
