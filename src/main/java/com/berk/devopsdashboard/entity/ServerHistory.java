package com.berk.devopsdashboard.entity;

import com.berk.devopsdashboard.entity.enums.ServerStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "server_history")
public class ServerHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    @JsonIgnore
    private Server server;

    private LocalDateTime checkTime;
    
    @Enumerated(EnumType.STRING)
    private ServerStatus status;
    
    private int responseTime;
}