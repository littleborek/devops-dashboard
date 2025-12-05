package com.berk.devopsdashboard.entity;

import com.berk.devopsdashboard.entity.enums.ServerStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    private String ipAddress;

    private String operatingSystem;
    
    private String location;
    
    private String category;

    @Enumerated(EnumType.STRING)
    private ServerStatus status;
    
    private Double cpuUsage;
    private Double ramUsage;
    private String totalRam;

    private Integer lastResponseTime;

    @Column(length = 2000)
    private String customCertificate;

    @Builder.Default
    private Boolean maintenanceMode = false;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    public boolean isMaintenanceMode() {
        return Boolean.TRUE.equals(this.maintenanceMode);
    }
}