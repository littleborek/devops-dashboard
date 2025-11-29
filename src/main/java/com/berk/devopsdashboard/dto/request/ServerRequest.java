package com.berk.devopsdashboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServerRequest {
    
    @NotBlank(message = "Sunucu adı boş olamaz")
    private String name;

    @NotBlank(message = "Adres boş olamaz")
    private String ipAddress; 

    @NotBlank(message = "İşletim sistemi bilgisi gereklidir")
    private String operatingSystem;

    private String location;
    
    private String customCertificate;
    
    private String category;
    
    private boolean maintenanceMode;
}