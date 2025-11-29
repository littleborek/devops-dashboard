package com.berk.devopsdashboard.entity;

import com.berk.devopsdashboard.entity.enums.ServerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String ipAddress;

    private String operatingSystem;

    private String location;

    @Enumerated(EnumType.STRING)
    private ServerStatus status;

    @Column(columnDefinition = "TEXT") 
    private String customCertificate;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Deployment> deployments;
    
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'Genel'")
    private String category;
    
    @Column(columnDefinition = "integer default 0")
    private int lastResponseTime;
    
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServerHistory> historyLogs;
    
    @Column(columnDefinition = "boolean default false")
    private boolean maintenanceMode;
}