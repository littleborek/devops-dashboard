package com.berk.devopsdashboard.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class K8sPodDTO {
    private String name;
    private String namespace;
    private String phase;
    private String nodeName;
    private String hostIP;
    private OffsetDateTime creationTimestamp;

    public String getStatusColor() {
        return switch (phase) {
            case "Running" -> "green-500";
            case "Pending" -> "yellow-500";
            case "Succeeded" -> "blue-500";
            case "Failed", "Unknown" -> "red-500";
            default -> "gray-500";
        };
    }
}