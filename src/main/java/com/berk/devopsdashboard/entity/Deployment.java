package com.berk.devopsdashboard.entity;

import com.berk.devopsdashboard.entity.enums.DeploymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deployments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment extends BaseEntity {

    @Column(nullable = false)
    private String applicationName;

    @Column(nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    private DeploymentStatus status;

    @Column(columnDefinition = "TEXT")
    private String logs;
    private String triggeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    @JsonIgnore 
    private Server server;
}