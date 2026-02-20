package com.berk.devopsdashboard.dto.response;

import com.berk.devopsdashboard.entity.enums.ServerStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ServerResponse {
    private Long id;
    private String name;
    private String ipAddress;
    private String operatingSystem;
    private String location;
    private String category;
    private ServerStatus status;
    private Integer lastResponseTime;
    private Double cpuUsage;
    private Double ramUsage;
    private String totalRam;

    private Double cpuUsageThreshold;
    private Double ramUsageThreshold;

    private String customCertificate;
    private Boolean maintenanceMode;
    private Boolean skipSslCheck;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}