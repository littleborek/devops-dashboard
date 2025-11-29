package com.berk.devopsdashboard.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServerResponse {
    private Long id;
    private String name;
    private String ipAddress;
    private String operatingSystem;
    private String location;
    private String status;
    private String customCertificate;
    private String category;
    private int lastResponseTime;
    private boolean maintenanceMode;
}