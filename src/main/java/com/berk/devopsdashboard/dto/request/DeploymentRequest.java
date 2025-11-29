package com.berk.devopsdashboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeploymentRequest {
    @NotBlank
    private String applicationName;
    
    @NotBlank
    private String version;
    
    private String logs; 
    

    private Long serverId; 
}